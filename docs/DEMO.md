# Run the portfolio demo

This demo uses its own Docker project, MySQL database and volume. It is separate from Phoenix Business installations and the private B.G. Fitness project.

Requirements: Docker Desktop running, Node.js 22+, and internet access for the first build.

From the repository root:

```sh
node scripts/demo.mjs
```

Open **http://localhost:5175**. Sign in with the generated username/password in `.demo/credentials.json`. That file and the signing/database secrets in `.demo/env` are ignored by Git. Each local demo receives unique credentials; no production credentials are needed.

The launcher builds the actual application, waits for the backend, and seeds synthetic members, plans, renewals, attendance and a staff account only when the demo member directory is empty. It handles the new demo owner's required first password change through the API. Subsequent starts preserve the demo records and credentials.

The database has no host port. The frontend and API bind only to loopback, on ports 5175 and 18081. If those ports are occupied, stop this demo or change both the Compose ports and launcher configuration together.

Stop without deleting records:

```sh
node scripts/demo.mjs stop
```

## Suggested walkthrough

1. Dashboard: membership status, revenue, attendance and upcoming expiries.
2. Members: search, profile and renewal/payment history.
3. Attendance: select a demo member and review check-in/out history.
4. Reports: export a filtered Excel report.
5. Users / Staff and Audit Logs: explain permissions and recorded actions.

Screenshots under `screenshots/` show this application with synthetic records. They are not client data or evidence of commercial deployment.

This is a local recruiter walkthrough, not a hosted public demo. Production deployment requirements remain in [BUSINESS_DEPLOYMENT.md](../BUSINESS_DEPLOYMENT.md).
