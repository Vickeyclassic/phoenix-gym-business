# Phoenix V1 to Business V2 migration

V1 remains untouched. Migration is an explicit operator-run export, staging, validation and cutover process—not an application-startup migration.

## Supported mappings

| V1 data | Business V2 transformation |
|---|---|
| Member ID | `V1-{legacy_id}` member code, providing idempotency |
| Name, phone, email | Trimmed; email lower-cased; blank values become null |
| Join/start/expiry dates | Preserved; absent join date becomes import date |
| Membership type | Case-insensitive match to configured plan; unmatched values remain noted as legacy data |
| Status | INACTIVE preserved; otherwise expiry date determines ACTIVE/EXPIRED |
| Payments | Amount/date preserved; immutable plan-name snapshot added; receipt uses existing value or `V1-{legacy_id}` |
| Attendance | Check-in/out preserved and marked `V1_IMPORT` |

Passwords, password hashes, JWT secrets and token deny-lists are intentionally not imported. Recreate staff accounts through the OWNER screen and issue temporary passwords. Unknown V1 custom columns, file uploads and malformed/duplicate phone/email values require manual review.

## Procedure

1. Back up V1 using its existing provider. Create a separate disposable Business V2 database.
2. Export only reviewed member, payment and attendance columns to CSV. Never export secrets.
3. Apply Business V2 Flyway migrations normally and create the bootstrap OWNER.
4. Open `tools/v1-import-from-staging.sql`. Create its staging tables, load the CSVs with a privileged database client, then run the script unchanged: it ends with `ROLLBACK`.
5. Review duplicate phone/email conflicts, unmatched plans, invalid dates, orphan payment/attendance rows and the displayed counts.
6. After resolving every discrepancy, change only the final `ROLLBACK` to `COMMIT` in a controlled copy and run once.
7. Re-run the script with `ROLLBACK`: idempotency guards should show no additional importable records.
8. Compare per-table counts and financial totals, generate all reports, sample member timelines and test attendance.
9. Schedule a maintenance window, repeat against a freshly backed-up production V2 database, verify, then switch users to V2. Retain V1 read-only during the agreed rollback window.

The staging tool uses unique legacy keys and `NOT EXISTS` guards. It must never be exposed through an HTTP endpoint.
