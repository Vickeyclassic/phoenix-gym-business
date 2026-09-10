# API summary

All `/api` endpoints except `POST /auth/login` and the public branding reads below require `Authorization: Bearer <access token>`. Health is public at `/actuator/health`.

| Area | Endpoints | Access |
|---|---|---|
| Authentication | `POST /auth/login`, `GET /auth/me`, `POST /auth/change-password`, `POST /auth/logout` | authenticated except login |
| Staff | `GET/POST/PUT /users`, `POST /users/{id}/reset-password` | OWNER |
| Members | `GET/POST /members`, `GET/PUT/DELETE /members/{id}`, `GET /members/{id}/profile` | STAFF; delete ADMIN/OWNER |
| Plans | `GET /plans`, `POST/PUT /plans` | read STAFF; write ADMIN/OWNER |
| Payments | `GET /payments?from&to&method&memberId`, `POST /payments/renew`, `GET /payments/{id}/receipt.pdf` | authenticated |
| Attendance | `GET /attendance?from&to&memberId`, `POST /attendance/check-in/{memberId}`, `POST /attendance/check-out/{memberId}` | authenticated |
| Dashboard/settings | `GET /dashboard`, `GET/PUT /settings` | settings write ADMIN/OWNER |
| Audit | `GET /audit-logs?user&action&entityType&from&to&page&size` | ADMIN/OWNER |
| Reports | `GET /reports/{members,payments,attendance,expiring,revenue}.xlsx` with relevant date/status/method/member filters | ADMIN/OWNER |

Validation errors use `{timestamp,status,message,path,fieldErrors}`. Pagination uses Spring `page`, `size` and `sort` parameters.

## Public business branding

`GET /api/public/branding` returns only `{ "gymName": "...", "logoUrl": "..." }`
from Business Settings. Login may read this without authentication. Other settings
remain authenticated; settings writes retain OWNER/ADMIN authorization.

For a local logo shared by the UI and PDF receipts, set `APP_BRANDING_DIRECTORY`
to an operator-managed asset directory, then set the existing `logoUrl` setting to
`/api/public/branding-assets/<filename>`. The public asset endpoint accepts only
simple JPEG/PNG filenames under 2 MB and rejects traversal and symlinks outside
that directory. It exposes only branding assets, so keep private files elsewhere.

Browser branding also supports HTTP(S) URLs. Receipts only read local assets and
never fetch remote URLs. A missing or unreadable optional logo does not prevent
receipt generation. Changing Business Settings refreshes the current browser's
identity and title immediately; other open sessions pick it up on page reload.
