# Backup and restore runbook

Prefer provider-managed encrypted daily backups with point-in-time recovery and retention aligned to local law. Monitor backup jobs and run a restoration drill into an isolated database at least quarterly.

Manual logical backup (credentials are prompted, never put them on the command line):

```bash
mysqldump --host=DB_HOST --port=DB_PORT --user=DB_USER --password --single-transaction --routines --triggers --set-gtid-purged=OFF phoenix_business > phoenix_business_YYYYMMDD.sql
```

Encrypt the result and store it outside the application host. Record its SHA-256 digest. To restore, create an empty isolated database, import, then start the matching application release:

```bash
mysql --host=RESTORE_HOST --port=RESTORE_PORT --user=RESTORE_USER --password phoenix_business_restore < phoenix_business_YYYYMMDD.sql
```

Verify Flyway history, table counts, newest members/payments/audit rows, foreign-key integrity, owner login, receipt/report generation and `/actuator/health`. Never test restore over production. For Aiven, enable service backups, verify the recovery window in the console, restrict network access, and follow its point-in-time restore procedure to a new service.
