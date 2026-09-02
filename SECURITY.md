# Security

- Access tokens expire after 15 minutes by default and contain a unique ID. Logout persists that ID in the revocation table.
- Passwords are BCrypt cost 12; password input and hashes never appear in API view models or audit summaries.
- Five failed logins per IP/username trigger a 15-minute lock. Commercial multi-replica deployments must replace the in-process counter with managed Redis or gateway rate limiting.
- Backend authorization is authoritative. STAFF cannot manage users, plans, settings, audit logs, reports or destructive member actions.
- CORS accepts only the configured exact frontend origins. CSRF is disabled only because the API uses bearer headers and no auth cookies.
- Do not log request bodies or authorization headers. Store secrets only in deployment secret managers and rotate JWT/database/bootstrap values after suspected disclosure.
- Receipt and report exports are authenticated and scoped; there is no browser-accessible database dump endpoint.
- Request DTOs whitelist writable fields for users, members, plans and business settings; IDs, password hashes and timestamps cannot be mass-assigned.

Report vulnerabilities privately to the repository owner. Apply dependency updates, run CI, review Flyway SQL and test authorization before each release.
