# Business deployment

Recommended low-cost commercial topology: Vercel Pro frontend, a paid always-on Render web service (or comparable container platform), and paid managed MySQL/Aiven. Free services that sleep, have ephemeral disks or lack backup guarantees are not suitable for commercial use.

1. Provision managed MySQL in the region nearest the gym, enable TLS, backups and network restrictions.
2. Deploy `backend/Dockerfile`; set the production variables from README, use the provider's internal port, and verify `/actuator/health`.
3. Deploy `frontend`; set `VITE_API_URL=https://api.example.com/api` before build.
4. Add `app.example.com` and `api.example.com`, enable HTTPS, then set backend `CORS_ALLOWED_ORIGINS` to the exact frontend origin and redeploy.
5. Create uptime checks for frontend and backend health, database alerts and log-retention/incident alerts.
6. Test OWNER/ADMIN/STAFF authorization, backup restore, reports and receipts before importing real data.

Cost categories are frontend seat/usage, always-on backend instance, managed database/storage/backups, domains, monitoring and email/SMS if later added. Pricing changes frequently; confirm directly with vendors before purchase.

Required backend variables: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`, `BOOTSTRAP_OWNER_USERNAME`, `BOOTSTRAP_OWNER_PASSWORD`, `PORT`, `DB_POOL_SIZE`. Required frontend build variable: `VITE_API_URL`. Remove or rotate the bootstrap password after the first owner changes it.

Logs must never include secrets. Investigate elevated 5xx/401/429 rates, database pool exhaustion, slow requests and health failures. Use a centralized log service and page an operator for sustained health failures.
