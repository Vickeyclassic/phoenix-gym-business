# Business deployment

## Development and test setup

Use local MySQL 8 or Docker Compose with non-production data. Localhost origins, developer passwords and manually started processes are acceptable only here. Run backend tests, frontend tests, ESLint and the production build before every deployment. The compose file is a convenience environment, not a production database architecture.

## Commercial production setup

Recommended low-cost topology: Vercel Pro frontend, a paid always-on Render web service (or equivalent container platform), and paid always-on managed MySQL/Aiven in a nearby region.

Free services that sleep, power off, use ephemeral disks or lack backup guarantees are **not suitable for a paying gym or customer**.

1. Provision managed MySQL, require TLS, enable automated backups/point-in-time recovery and restrict network access.
2. Deploy `backend/Dockerfile`; configure secrets in the platform secret manager and verify `/actuator/health` reports the database UP.
3. Deploy `frontend`; set `VITE_API_URL=https://api.example.com/api` before its build.
4. Attach `app.example.com` and `api.example.com`, enable HTTPS, then set `CORS_ALLOWED_ORIGINS` to the exact frontend origin.
5. Add uptime checks for the UI and health endpoint, database availability alerts, centralized logs and incident contacts.
6. Perform a restore drill and OWNER/ADMIN/STAFF acceptance test before importing real data.

Cost categories include frontend seats/usage, always-on backend compute, managed database/storage/backups, domain/DNS, monitoring, and optional messaging. Confirm current vendor pricing before purchase.

Required backend variables: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`, `BOOTSTRAP_OWNER_USERNAME`, `BOOTSTRAP_OWNER_PASSWORD`, `PORT`, `DB_POOL_SIZE`. Required frontend build variable: `VITE_API_URL`.

Use a random JWT secret of at least 32 bytes and a strong one-time bootstrap password. Remove/rotate the bootstrap value after the owner changes it. For multiple backend replicas, move login throttling to a shared gateway/Redis implementation; persistent token revocation already resides in MySQL.

Never log request bodies, authorization headers or connection strings. Investigate elevated 5xx/401/429 rates, database pool exhaustion, slow requests and health failures.
