-- Demo data for the SQL reports: one month (October 2026) of activity for 20 customers.
--
-- Normal daily spending is generated, then a dozen hand-written fraud scenarios are added
-- with their rule hits, alerts and analyst reviews. Everything is deterministic: same data on every run.
--
-- Run ONCE on a database created by the application (Flyway schema):
--   psql -h localhost -U fraudwatch -d fraudwatch -f sql/demo-data.sql

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM app_user WHERE username = 'cust01') THEN
        RAISE EXCEPTION 'Demo data already loaded';
    END IF;
END $$;

BEGIN;

-- ---------------------------------------------------------------------------
-- Users and accounts
-- ---------------------------------------------------------------------------

-- These accounts cannot log in: the hash is not a real BCrypt hash.
INSERT INTO app_user (username, password_hash, role, created_at)
SELECT 'cust' || lpad(g::text, 2, '0'), 'demo-only-not-a-real-hash', 'USER', TIMESTAMPTZ '2026-09-01 09:00-04'
FROM generate_series(1, 20) AS g;

-- The application may already have created "analyst" at startup: keep that one.
INSERT INTO app_user (username, password_hash, role, created_at)
VALUES ('analyst', 'demo-only-not-a-real-hash', 'ANALYST', TIMESTAMPTZ '2026-09-01 09:00-04'),
       ('analyst2', 'demo-only-not-a-real-hash', 'ANALYST', TIMESTAMPTZ '2026-09-01 09:00-04')
ON CONFLICT (username) DO NOTHING;

INSERT INTO account (account_number, owner_id, balance, currency, home_country, created_at)
SELECT 'FW-' || (1000000000 + u.id * 7919),
       u.id,
       0,
       'CAD',
       CASE u.username WHEN 'cust19' THEN 'FR' WHEN 'cust20' THEN 'US' ELSE 'CA' END,
       u.created_at
FROM app_user u
WHERE u.username LIKE 'cust%';

-- ---------------------------------------------------------------------------
-- Fraud scenarios (written first, so normal traffic can avoid those days)
-- ---------------------------------------------------------------------------

CREATE TEMP TABLE scenario (
    username     text,
    type         text,
    amount       numeric(19, 2),
    merchant     text,
    country      text,
    at           timestamptz,
    rules        text[],      -- fraud rules the engine would have triggered
    review       text,        -- analyst decision, NULL = still open
    reviewer     text,
    review_after interval
);

INSERT INTO scenario VALUES
    -- Card used abroad 35 minutes after a coffee in Montreal: real fraud
    ('cust03', 'PAYMENT', 25.00, 'Cafe Olimpico', 'CA', '2026-10-07 12:00-04', '{}', NULL, NULL, NULL),
    ('cust03', 'PAYMENT', 640.00, 'Galeries Lafayette', 'FR', '2026-10-07 12:35-04', '{FOREIGN_COUNTRY,IMPOSSIBLE_TRAVEL}', 'CONFIRMED_FRAUD', 'analyst', '3 hours'),
    -- US online store: looks like travel, but it is just a website
    ('cust05', 'PAYMENT', 18.00, 'STM', 'CA', '2026-10-09 09:10-04', '{}', NULL, NULL, NULL),
    ('cust05', 'PAYMENT', 85.00, 'Amazon.com', 'US', '2026-10-09 10:00-04', '{FOREIGN_COUNTRY,IMPOSSIBLE_TRAVEL}', 'FALSE_POSITIVE', 'analyst2', '1 hour'),
    -- Cash withdrawal in London 20 minutes after a purchase in Montreal
    ('cust08', 'PAYMENT', 40.00, 'IGA', 'CA', '2026-10-12 14:00-04', '{}', NULL, NULL, NULL),
    ('cust08', 'WITHDRAWAL', 900.00, 'ATM London', 'GB', '2026-10-12 14:20-04', '{FOREIGN_COUNTRY,IMPOSSIBLE_TRAVEL}', 'CONFIRMED_FRAUD', 'analyst', '5 hours'),
    -- A large, unusual deposit that turns out to be an inheritance
    ('cust11', 'DEPOSIT', 15000.00, 'Estate transfer', 'CA', '2026-10-15 11:00-04', '{LARGE_AMOUNT,UNUSUAL_AMOUNT}', 'FALSE_POSITIVE', 'analyst', '26 hours'),
    -- Big purchase at 3 a.m.: account takeover
    ('cust12', 'PAYMENT', 2200.00, 'Online electronics', 'CA', '2026-10-18 03:12-04', '{UNUSUAL_AMOUNT,NIGHT_ACTIVITY}', 'CONFIRMED_FRAUD', 'analyst2', '8 hours'),
    -- Burst of small game purchases: noted by the engine, below the review threshold
    ('cust15', 'PAYMENT', 9.99, 'Game store', 'CA', '2026-10-20 16:00-04', '{}', NULL, NULL, NULL),
    ('cust15', 'PAYMENT', 9.99, 'Game store', 'CA', '2026-10-20 16:01-04', '{}', NULL, NULL, NULL),
    ('cust15', 'PAYMENT', 9.99, 'Game store', 'CA', '2026-10-20 16:03-04', '{}', NULL, NULL, NULL),
    ('cust15', 'PAYMENT', 9.99, 'Game store', 'CA', '2026-10-20 16:04-04', '{}', NULL, NULL, NULL),
    ('cust15', 'PAYMENT', 9.99, 'Game store', 'CA', '2026-10-20 16:06-04', '{}', NULL, NULL, NULL),
    ('cust15', 'PAYMENT', 9.99, 'Game store', 'CA', '2026-10-20 16:07-04', '{VELOCITY}', NULL, NULL, NULL),
    ('cust15', 'PAYMENT', 9.99, 'Game store', 'CA', '2026-10-20 16:09-04', '{VELOCITY}', NULL, NULL, NULL),
    -- Large foreign purchase right after a local one: blocked, confirmed
    ('cust17', 'PAYMENT', 30.00, 'Metro', 'CA', '2026-10-22 10:00-04', '{}', NULL, NULL, NULL),
    ('cust17', 'PAYMENT', 1200.00, 'Online electronics', 'NG', '2026-10-22 10:45-04', '{FOREIGN_COUNTRY,IMPOSSIBLE_TRAVEL,UNUSUAL_AMOUNT}', 'CONFIRMED_FRAUD', 'analyst', '2 hours'),
    -- French customer paying a Canadian website
    ('cust19', 'PAYMENT', 22.00, 'Boulangerie Paul', 'FR', '2026-10-24 15:00-04', '{}', NULL, NULL, NULL),
    ('cust19', 'PAYMENT', 60.00, 'Simons.ca', 'CA', '2026-10-24 15:50-04', '{FOREIGN_COUNTRY,IMPOSSIBLE_TRAVEL}', 'FALSE_POSITIVE', 'analyst2', '30 minutes'),
    -- Travel abroad with no recent home transaction: only a weak signal
    ('cust10', 'PAYMENT', 55.00, 'Trattoria Roma', 'IT', '2026-10-05 13:30-04', '{FOREIGN_COUNTRY}', NULL, NULL, NULL),
    -- Small purchase at night
    ('cust06', 'PAYMENT', 45.00, 'Couche-Tard', 'CA', '2026-10-30 02:30-04', '{NIGHT_ACTIVITY}', NULL, NULL, NULL),
    -- Still waiting for an analyst
    ('cust02', 'PAYMENT', 15.00, 'Pharmaprix', 'CA', '2026-10-27 13:00-04', '{}', NULL, NULL, NULL),
    ('cust02', 'PAYMENT', 75.00, 'Hotel Cancun', 'MX', '2026-10-27 13:30-04', '{FOREIGN_COUNTRY,IMPOSSIBLE_TRAVEL}', NULL, NULL, NULL),
    ('cust09', 'PAYMENT', 20.00, 'SAQ', 'CA', '2026-10-30 12:00-04', '{}', NULL, NULL, NULL),
    ('cust09', 'PAYMENT', 1800.00, 'Luxury watches', 'FR', '2026-10-30 12:10-04', '{FOREIGN_COUNTRY,IMPOSSIBLE_TRAVEL,UNUSUAL_AMOUNT}', NULL, NULL, NULL);

-- ---------------------------------------------------------------------------
-- Normal traffic: payroll every two weeks, everyday purchases, 8 a.m. to 8 p.m.
-- hashtext() gives stable "random" values, so the data is the same on every run.
-- ---------------------------------------------------------------------------

INSERT INTO bank_transaction (account_id, type, amount, merchant, country, status, risk_score, created_at)
SELECT a.id,
       CASE WHEN d % 14 = 0 THEN 'DEPOSIT' ELSE 'PAYMENT' END,
       CASE WHEN d % 14 = 0 THEN 2500.00
            ELSE (10 + abs(hashtext(a.id || ':' || d)) % 120) + 0.99 END,
       CASE WHEN d % 14 = 0 THEN 'Payroll'
            ELSE (ARRAY['Metro', 'IGA', 'Cafe Olimpico', 'STM', 'Pharmaprix', 'Amazon.ca', 'SAQ', 'Couche-Tard'])
                     [1 + abs(hashtext(d || ':' || a.id)) % 8] END,
       a.home_country,
       'APPROVED',
       0,
       TIMESTAMPTZ '2026-10-01 08:00-04'
           + make_interval(days => d,
                           hours => abs(hashtext('h' || a.id || d)) % 12,
                           mins => abs(hashtext('m' || a.id || d)) % 60)
FROM account a
CROSS JOIN generate_series(0, 29) AS d
WHERE NOT EXISTS (  -- keep scenario days clean, so the stories stay easy to read
    SELECT 1
    FROM scenario s JOIN app_user u ON u.username = s.username
    WHERE u.id = a.owner_id
      AND (s.at AT TIME ZONE 'America/Toronto')::date = DATE '2026-10-01' + d
      AND d % 14 <> 0);

-- ---------------------------------------------------------------------------
-- Scenario transactions, their rule hits, alerts and reviews
-- ---------------------------------------------------------------------------

CREATE TEMP TABLE rule_points (code text PRIMARY KEY, points int);
INSERT INTO rule_points VALUES
    ('LARGE_AMOUNT', 40), ('UNUSUAL_AMOUNT', 30), ('VELOCITY', 35),
    ('FOREIGN_COUNTRY', 15), ('IMPOSSIBLE_TRAVEL', 45), ('NIGHT_ACTIVITY', 10);

-- Score and decision computed exactly like the Java engine: sum of points, capped at 100,
-- flagged from 40, blocked from 70.
CREATE TEMP TABLE scored AS
SELECT s.*,
       a.id AS account_id,
       least(100, coalesce((SELECT sum(p.points) FROM rule_points p WHERE p.code = ANY (s.rules)), 0)) AS risk
FROM scenario s
JOIN app_user u ON u.username = s.username
JOIN account a ON a.owner_id = u.id;

-- Final status after the analyst's decision (same table as AlertService.review):
--   flagged + confirmed fraud  -> money reversed, BLOCKED
--   blocked + false positive   -> money released, APPROVED
INSERT INTO bank_transaction (account_id, type, amount, merchant, country, status, risk_score, created_at)
SELECT account_id, type, amount, merchant, country,
       CASE
           WHEN risk >= 70 AND review = 'FALSE_POSITIVE' THEN 'APPROVED'
           WHEN risk >= 70 THEN 'BLOCKED'
           WHEN risk >= 40 AND review = 'CONFIRMED_FRAUD' THEN 'BLOCKED'
           WHEN risk >= 40 AND review = 'FALSE_POSITIVE' THEN 'APPROVED'
           WHEN risk >= 40 THEN 'FLAGGED'
           ELSE 'APPROVED'
       END,
       risk,
       at
FROM scored;

INSERT INTO transaction_rule_hit (transaction_id, code, points, reason)
SELECT t.id, p.code, p.points, 'Demo scenario: ' || lower(replace(p.code, '_', ' '))
FROM scored s
JOIN bank_transaction t ON t.account_id = s.account_id AND t.created_at = s.at AND t.amount = s.amount
JOIN rule_points p ON p.code = ANY (s.rules);

INSERT INTO fraud_alert (transaction_id, status, created_at, reviewed_by, reviewed_at, resolution_note)
SELECT t.id,
       coalesce(s.review, 'OPEN'),
       s.at,
       s.reviewer,
       s.at + s.review_after,
       CASE s.review
           WHEN 'CONFIRMED_FRAUD' THEN 'Customer did not recognize the transaction'
           WHEN 'FALSE_POSITIVE' THEN 'Customer confirmed the transaction'
       END
FROM scored s
JOIN bank_transaction t ON t.account_id = s.account_id AND t.created_at = s.at AND t.amount = s.amount
WHERE s.risk >= 40;

-- ---------------------------------------------------------------------------
-- Balances: the account balance must equal its ledger (report 6 checks it)
-- ---------------------------------------------------------------------------

UPDATE account a
SET balance = coalesce((
    SELECT sum(CASE WHEN t.status = 'BLOCKED' THEN 0
                    WHEN t.type = 'DEPOSIT' THEN t.amount
                    ELSE -t.amount END)
    FROM bank_transaction t
    WHERE t.account_id = a.id), 0);

-- ---------------------------------------------------------------------------
-- Audit log: normal logins, plus a brute-force attempt on cust04
-- ---------------------------------------------------------------------------

INSERT INTO audit_event (occurred_at, actor, action, details)
SELECT TIMESTAMPTZ '2026-10-01 07:30-04' + make_interval(days => d), 'cust' || lpad(c::text, 2, '0'), 'LOGIN_SUCCESS', NULL
FROM generate_series(0, 29) AS d
CROSS JOIN generate_series(1, 20) AS c
WHERE (c + d) % 3 = 0;

INSERT INTO audit_event (occurred_at, actor, action, details)
SELECT TIMESTAMPTZ '2026-10-16 02:14-04' + make_interval(secs => 20 * g), 'cust04', 'LOGIN_FAILURE', 'Failed login'
FROM generate_series(1, 5) AS g;

INSERT INTO audit_event (occurred_at, actor, action, details)
VALUES ('2026-10-16 02:16:00-04', 'cust04', 'LOGIN_LOCKED', 'Login refused, username locked'),
       ('2026-10-16 02:16:20-04', 'cust04', 'LOGIN_LOCKED', 'Login refused, username locked'),
       ('2026-10-21 08:05:00-04', 'cust13', 'LOGIN_FAILURE', 'Failed login'),
       ('2026-10-21 08:05:30-04', 'cust13', 'LOGIN_SUCCESS', NULL);

COMMIT;
