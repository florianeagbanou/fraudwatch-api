#!/usr/bin/env bash
# Builds a fresh database from the Flyway migration, loads the demo data, runs every report,
# and fails if the reconciliation report finds a balance that does not match its ledger.
# Used by CI; also works locally against any PostgreSQL (PGHOST, PGPORT, PGUSER, PGPASSWORD, PGDATABASE).
set -euo pipefail
cd "$(dirname "$0")/.."

export PGTZ=America/Toronto
psql_run() { psql -X -v ON_ERROR_STOP=1 "$@"; }

psql_run -q -f src/main/resources/db/migration/V1__init.sql
psql_run -q -f sql/demo-data.sql
psql_run -f sql/reports.sql > sql-report.txt

mismatches=$(psql_run -tA -c "
  SELECT count(*) FROM account a
  WHERE a.balance <> coalesce((SELECT sum(CASE WHEN status = 'BLOCKED' THEN 0
                                               WHEN type = 'DEPOSIT' THEN amount
                                               ELSE -amount END)
                               FROM bank_transaction t WHERE t.account_id = a.id), 0)")
unflagged=$(psql_run -tA -c "
  WITH o AS (SELECT id, country, created_at,
                    lag(country) OVER w AS pc, lag(created_at) OVER w AS pa
             FROM bank_transaction WINDOW w AS (PARTITION BY account_id ORDER BY created_at, id))
  SELECT count(*) FROM o
  WHERE country <> pc AND created_at - pa < interval '2 hours'
    AND NOT EXISTS (SELECT 1 FROM transaction_rule_hit h WHERE h.transaction_id = o.id AND h.code = 'IMPOSSIBLE_TRAVEL')")

echo "Balance mismatches: $mismatches, impossible travel missed by the engine: $unflagged"
test "$mismatches" -eq 0 && test "$unflagged" -eq 0
echo "All SQL checks passed. Full output in sql-report.txt"
