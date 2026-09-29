# Business deployment

## Phoenix Business production Docker deployment

Use `deploy/production/compose.production.yml` as a standalone stack. The root
`docker-compose.yml` is for local development. Do not combine either file with a
client-specific Compose file. The production stack builds the current backend and
frontend source, publishes only HTTPS/HTTP through Caddy, and persists MySQL,
uploaded branding, and certificates in separate named volumes.

### Configure the deployment host

Install Docker Engine with the Compose v2 plugin on the deployment host. Run the
following commands from the Phoenix Business repository root in a POSIX shell:

```sh
cp deploy/production/.env.example deploy/production/.env.production
chmod 600 deploy/production/.env.production
```

Edit that file before continuing:

- Set `APP_DOMAIN` to your frontend hostname without a scheme or path.
- Set `CORS_ALLOWED_ORIGINS` to its exact HTTPS origin, for example
  `https://app.example.com` (no trailing slash). Additional origins may be comma-separated.
- Set different, strong values for `DB_PASSWORD` and `MYSQL_ROOT_PASSWORD`.
- Set `JWT_SECRET` to at least 32 random bytes; `openssl rand -hex 32` generates
  a suitable value. Never put secrets into a Docker build argument or source file.
- Set `BOOTSTRAP_OWNER_USERNAME` and a first-run `BOOTSTRAP_OWNER_PASSWORD`
  (10–72 characters containing uppercase, lowercase, and a digit).
- Keep `DB_USERNAME=phoenix_app`, not root. `DB_NAME` and the database portion of
  `DB_URL` must agree. The supplied JDBC URL uses the private Docker MySQL host,
  requires TLS, and keeps timestamp transport in UTC; business time remains IST.
- Keep `VITE_API_URL=/api` for this same-domain Caddy stack. It is embedded during
  the frontend build, so changing it requires rebuilding the frontend.
- Set a unique `RELEASE_TAG` for each release. Optional `MYSQL_IMAGE` and
  `CADDY_IMAGE` may use approved immutable image digests.

Point the domain's DNS at this host and allow inbound TCP 80/443. Do not expose
MySQL or backend ports. Caddy obtains and renews HTTPS certificates automatically.

### Deploy and check migration V4

```sh
docker compose --env-file deploy/production/.env.production -f deploy/production/compose.production.yml config --quiet
docker compose --env-file deploy/production/.env.production -f deploy/production/compose.production.yml build backend frontend
docker compose --env-file deploy/production/.env.production -f deploy/production/compose.production.yml up -d --wait --wait-timeout 300
docker compose --env-file deploy/production/.env.production -f deploy/production/compose.production.yml ps
docker compose --env-file deploy/production/.env.production -f deploy/production/compose.production.yml logs --tail=100 backend proxy
docker compose --env-file deploy/production/.env.production -f deploy/production/compose.production.yml exec mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" "$MYSQL_DATABASE" -e "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

Open `https://YOUR_APP_DOMAIN`, sign in with the bootstrap owner, and complete the
required password change. Then clear `BOOTSTRAP_OWNER_PASSWORD` from the environment
file and recreate the backend with the same `up -d --wait` command. Existing owner
accounts are never overwritten by bootstrap settings.

`backend/Dockerfile` copies `src`, and Maven packages
`src/main/resources/db/migration/V4__member_photos_and_india_timezone.sql` into the
application JAR. The production profile enables Flyway migration and validation
on startup before Hibernate validates the schema. A fresh database runs V1–V4;
a Flyway-managed V3 database runs V4 once. No manual V4 SQL execution is needed.
An untracked, non-empty database deliberately fails instead of silently baselining.

### Existing Phoenix production data

Before upgrading, back up the existing Phoenix database and uploaded branding.
This stack's default project name is `phoenix-business-production`; it must use
the existing Phoenix production volume/project when upgrading that stack. Do not
attach another application's volumes. If moving from a differently named stack,
restore the Phoenix backup into this stack before admitting users, or explicitly
map its existing Phoenix-only volumes. Do not use `down -v` during upgrades.
Changing MySQL initialization variables does not change users/passwords in an
already initialized volume; keep the existing credentials or rotate them in MySQL
as a separate controlled operation.

For separate managed hosting, deploy the backend with `SPRING_PROFILES_ACTIVE=prod`,
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, and the
bootstrap variables above. Use the provider's TLS JDBC URL (prefer
`sslMode=VERIFY_IDENTITY` with its trusted CA). Mount writable persistent storage
at `APP_BRANDING_DIRECTORY`. Build the frontend with
`VITE_API_URL=https://YOUR_BACKEND_DOMAIN/api` and set backend CORS to the exact
deployed frontend origin. `JWT_EXPIRATION_MS`, `DB_POOL_SIZE`, and `PORT` are
optional overrides. Never set the frontend API URL to localhost in production.

Docker runtime validation, DNS/TLS provisioning, backups, and the live V4 check
must be completed on the deployment host; Docker is not installed in the current
workspace environment. No production stack has been started here.

## Development and test setup

Use local MySQL 8 or the root Docker Compose file with non-production data. Localhost origins, developer passwords and manually started processes are acceptable only here. Run checks appropriate to the changes in the release pipeline. The root compose file is a convenience environment; use the standalone production file above for production Docker hosting.

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
