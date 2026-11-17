# SQL reports

Eight PostgreSQL queries that answer the questions a bank's risk, operations and security teams ask about FraudWatch data.
A demo data set (one month, 20 customers, a dozen fraud scenarios) makes them meaningful out of the box.

## Run them

```bash
docker compose up -d                       # starts PostgreSQL and the API (Flyway creates the schema)
export PGHOST=localhost PGUSER=fraudwatch PGPASSWORD=fraudwatch PGDATABASE=fraudwatch
psql -f sql/demo-data.sql                  # once
psql -f sql/reports.sql
```

`./sql/check.sh` rebuilds a database from scratch, runs everything and fails if a check does not hold. CI runs it on every push.

## The reports

| # | Question | SQL used |
|---|---|---|
| 1 | How much activity per day, and what share needed a human review? | `GROUP BY`, `COUNT(*) FILTER`, time zones |
| 2 | Which fraud rules are worth their noise (precision per rule)? | `LEFT JOIN`, conditional aggregates, `NULLIF` |
| 3 | How fast does each analyst close alerts? | CTE, `percentile_cont` (median), intervals |
| 4 | What is waiting in the alert queue? | 4-table join, `string_agg ... ORDER BY` |
| 5 | Statement of an account with its running balance | Window function `SUM() OVER (ORDER BY ...)` |
| 6 | Does every stored balance match its transactions? | CTE + `LEFT JOIN` anti-mismatch, expected 0 rows |
| 7 | Recompute the impossible-travel rule in SQL and compare with the engine | `LAG()` over a partition, `EXISTS` |
| 8 | Which usernames are being brute-forced? | `HAVING` on a filtered count |

## What the demo data shows

- **Rule precision** (report 2): `IMPOSSIBLE_TRAVEL` and `FOREIGN_COUNTRY` are right 60 % of the time; the two false positives are online purchases from foreign websites. `LARGE_AMOUNT` produced one false positive (an inheritance) and no confirmed fraud. `VELOCITY` fires but never reaches the review threshold on its own.
- **Analyst workload** (report 3): median review time 4 h for one analyst, 1 h for the other; one alert waited 26 h over a weekend, which is why the median is shown next to the average.
- **Reconciliation** (report 6) returns no rows: every balance matches its ledger, including reversed fraud and released false positives.
- **Cross-check** (report 7): the 7 impossible-travel cases found by SQL are exactly the 7 flagged by the Java engine.

Demo users (`cust01` to `cust20`, `analyst2`) cannot log in: their password hash is a placeholder.
