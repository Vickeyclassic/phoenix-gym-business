# Phoenix Gym Business

Phoenix Gym Business is a single-gym operations platform for owners, administrators and staff. Run this project at http://localhost:5173 using its own Docker stack and database. It is the maintained Business V2 project, separate from the older Management V1 demo.

## Try the demo

Run `node scripts/demo.mjs` from this repository with Docker Desktop and Node.js 22+. Open http://localhost:5175 and use the generated local credentials. See [Demo guide](docs/DEMO.md).

## Screenshots

![Application dashboard](docs/screenshots/dashboard.png)

[View screenshot gallery](docs/screenshots/README.md). All records shown are synthetic demo data.

See [Design walkthrough](docs/DESIGN.md) for the key implementation decisions and tradeoffs.

## Features

- OWNER, ADMIN and STAFF accounts with active state, role changes, forced password reset and last login
- JWT authentication, persistent logout revocation, rate limiting, BCrypt and strict API authorization
- Member profiles, plan configuration, renewals, payment snapshots, attendance and activity history
- Business settings that drive branding, currency, reminders and receipts
- Management dashboard with real revenue, attendance and membership charts
- Server-filtered audit logs and five Excel reports: members, payments, attendance, expiring memberships and revenue
- Responsive React UI with working create/edit/delete/renew/check-in/check-out flows, protected role-aware navigation, loading, empty and error states

## Architecture

`React/Vite -> Axios/JWT -> Spring Security -> REST services -> JPA/Flyway -> MySQL 8`

The backend is Java 21/Spring Boot. The frontend is React/Vite. Flyway owns all schema changes; Hibernate only validates in production.

See [Frontend structure](docs/FRONTEND.md) for page, component and data-hook responsibilities.

## Local setup

Requirements: Java 21, Maven 3.9+, Node 22+, and MySQL 8. Copy `.env.example` to `.env` and replace every placeholder.

```powershell
cd backend
$env:DB_URL='jdbc:mysql://localhost:3306/phoenix_business?serverTimezone=UTC'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='your-local-password'
$env:JWT_SECRET='a-random-secret-with-at-least-32-characters'
$env:BOOTSTRAP_OWNER_PASSWORD='StrongOwner1Password'
mvn spring-boot:run
```

In another terminal:

```powershell
cd frontend
$env:VITE_API_URL='http://localhost:8080/api'
npm install
npm run dev
```

Or set the `.env` values and run `docker compose up --build`. The first start creates an OWNER only when the user table is empty; that owner must change the bootstrap password.

## Testing

```powershell
cd backend; mvn test
cd ../frontend; npm ci; npm run lint; npm run test; npm run build
```

CI runs the same gates. See [API.md](API.md), [SECURITY.md](SECURITY.md), [BUSINESS_DEPLOYMENT.md](BUSINESS_DEPLOYMENT.md), [BACKUP_RESTORE.md](BACKUP_RESTORE.md), and [V1_TO_BUSINESS_MIGRATION.md](V1_TO_BUSINESS_MIGRATION.md).

## Migration from V1

V2 is not an in-place rewrite. The manual, rollback-first staging importer preserves legacy identifiers and financial history without copying credentials. Follow [V1_TO_BUSINESS_MIGRATION.md](V1_TO_BUSINESS_MIGRATION.md). V1 remains deployed and unchanged.

## Production considerations

Use an always-on backend and paid managed MySQL with automated backups. Free sleeping services are unsuitable for commercial operations. Configure HTTPS, exact CORS origins, uptime alerts and restoration drills before admitting real customer data.