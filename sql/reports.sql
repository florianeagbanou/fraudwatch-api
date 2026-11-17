-- Business and security reports on the FraudWatch database (PostgreSQL).
-- Each query answers one question a bank's risk, operations or security team would ask.
--
--   psql -h localhost -U fraudwatch -d fraudwatch -f sql/reports.sql
--
-- Money in or out of an account, used by several reports:
--   BLOCKED -> 0 (the money never moved), DEPOSIT -> +amount, PAYMENT / WITHDRAWAL -> -amount


-- 1. How much activity per day, and what share of it needed a human review?
--    A transaction "needed review" if the engine scored it 40 or more, whatever the analyst decided later.
\echo '1. Daily volume and review rate'
SELECT (created_at AT TIME ZONE 'America/Toronto')::date                         AS day,
       count(*)                                                                   AS transactions,
       sum(amount)                                                                AS volume_cad,
       count(*) FILTER (WHERE risk_score >= 40)                                   AS needed_review,
       round(100.0 * count(*) FILTER (WHERE risk_score >= 40) / count(*), 2)     AS review_rate_pct
FROM bank_transaction
GROUP BY day
ORDER BY day;


-- 2. Which fraud rules are worth their noise?
--    Precision = share of reviewed alerts the rule took part in that were real fraud.
--    A rule with low precision sends analysts after false alarms.
\echo '2. Precision of each fraud rule'
SELECT h.code                                                                     AS rule,
       count(*)                                                                   AS times_triggered,
       count(a.id)                                                                AS in_alerts,
       count(*) FILTER (WHERE a.status = 'CONFIRMED_FRAUD')                       AS confirmed_fraud,
       count(*) FILTER (WHERE a.status = 'FALSE_POSITIVE')                        AS false_positives,
       round(100.0 * count(*) FILTER (WHERE a.status = 'CONFIRMED_FRAUD')
             / nullif(count(*) FILTER (WHERE a.status IN ('CONFIRMED_FRAUD', 'FALSE_POSITIVE')), 0), 1)
                                                                                  AS precision_pct
FROM transaction_rule_hit h
LEFT JOIN fraud_alert a ON a.transaction_id = h.transaction_id   -- LEFT: rules that never led to an alert stay in the list
GROUP BY h.code
ORDER BY precision_pct DESC NULLS LAST, times_triggered DESC;


-- 3. How fast does each analyst close alerts?
--    The median is less sensitive than the average to one alert left over a weekend.
\echo '3. Analyst review times'
WITH reviewed AS (
    SELECT reviewed_by, reviewed_at - created_at AS time_to_review
    FROM fraud_alert
    WHERE status <> 'OPEN'
)
SELECT reviewed_by                                                                AS analyst,
       count(*)                                                                   AS alerts_closed,
       date_trunc('minute', avg(time_to_review))                                  AS average_time,
       date_trunc('minute', percentile_cont(0.5) WITHIN GROUP (ORDER BY time_to_review)) AS median_time,
       max(time_to_review)                                                        AS slowest
FROM reviewed
GROUP BY reviewed_by
ORDER BY alerts_closed DESC, analyst;


-- 4. What is waiting in the queue right now, oldest first?
\echo '4. Open alerts'
SELECT a.id                                                                       AS alert_id,
       acc.account_number,
       t.amount,
       t.country,
       t.risk_score,
       string_agg(h.code, ', ' ORDER BY h.points DESC)                            AS rules,
       a.created_at                                                               AS opened_at
FROM fraud_alert a
JOIN bank_transaction t ON t.id = a.transaction_id
JOIN account acc        ON acc.id = t.account_id
JOIN transaction_rule_hit h ON h.transaction_id = t.id
WHERE a.status = 'OPEN'
GROUP BY a.id, acc.account_number, t.amount, t.country, t.risk_score, a.created_at
ORDER BY a.created_at;


-- 5. Statement of one account with its running balance (window function).
--    Change the username to look at another customer.
\echo '5. Running balance of cust11'
SELECT t.created_at,
       t.type,
       t.merchant,
       t.amount,
       t.status,
       sum(CASE WHEN t.status = 'BLOCKED' THEN 0
                WHEN t.type = 'DEPOSIT' THEN t.amount
                ELSE -t.amount END)
           OVER (ORDER BY t.created_at, t.id)                                     AS running_balance
FROM bank_transaction t
JOIN account a  ON a.id = t.account_id
JOIN app_user u ON u.id = a.owner_id
WHERE u.username = 'cust11'
ORDER BY t.created_at, t.id;


-- 6. Reconciliation: does every stored balance match the sum of its transactions?
--    Expected result: no rows. Any row is a bug to investigate before anything else.
\echo '6. Reconciliation (expected: 0 rows)'
WITH ledger AS (
    SELECT account_id,
           sum(CASE WHEN status = 'BLOCKED' THEN 0
                    WHEN type = 'DEPOSIT' THEN amount
                    ELSE -amount END) AS computed
    FROM bank_transaction
    GROUP BY account_id
)
SELECT a.account_number,
       a.balance                              AS stored_balance,
       coalesce(l.computed, 0)                AS ledger_balance,
       a.balance - coalesce(l.computed, 0)    AS difference
FROM account a
LEFT JOIN ledger l ON l.account_id = a.id
WHERE a.balance <> coalesce(l.computed, 0);


-- 7. Independent check of the impossible-travel rule, in pure SQL.
--    LAG() looks at the previous transaction of the same account. Every row found here
--    should also have been flagged by the Java engine (last column).
\echo '7. Impossible travel, recomputed with LAG()'
WITH ordered AS (
    SELECT t.id, t.account_id, t.country, t.created_at,
           lag(t.country)    OVER w AS previous_country,
           lag(t.created_at) OVER w AS previous_at
    FROM bank_transaction t
    WINDOW w AS (PARTITION BY t.account_id ORDER BY t.created_at, t.id)
)
SELECT acc.account_number,
       o.previous_country || ' -> ' || o.country                                  AS route,
       o.created_at - o.previous_at                                               AS gap,
       EXISTS (SELECT 1 FROM transaction_rule_hit h
               WHERE h.transaction_id = o.id AND h.code = 'IMPOSSIBLE_TRAVEL')    AS flagged_by_engine
FROM ordered o
JOIN account acc ON acc.id = o.account_id
WHERE o.country <> o.previous_country
  AND o.created_at - o.previous_at < interval '2 hours'
ORDER BY o.created_at;


-- 8. Security: which usernames are being attacked?
\echo '8. Possible brute force on logins'
SELECT actor                                                                      AS username,
       count(*) FILTER (WHERE action = 'LOGIN_FAILURE')                           AS failures,
       count(*) FILTER (WHERE action = 'LOGIN_LOCKED')                            AS refused_while_locked,
       min(occurred_at)                                                           AS first_seen,
       max(occurred_at)                                                           AS last_seen
FROM audit_event
WHERE action IN ('LOGIN_FAILURE', 'LOGIN_LOCKED')
GROUP BY actor
HAVING count(*) FILTER (WHERE action = 'LOGIN_FAILURE') >= 3
ORDER BY failures DESC;
