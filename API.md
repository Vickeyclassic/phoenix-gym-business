# API summary

All `/api` endpoints except `POST /auth/login` require `Authorization: Bearer <access token>`. Health is public at `/actuator/health`.

| Area | Endpoints | Access |
|---|---|---|
| Authentication | `POST /auth/login`, `GET /auth/me`, `POST /auth/change-password`, `POST /auth/logout` | authenticated except login |
| Staff | `GET/POST/PUT /users`, `POST /users/{id}/reset-password` | OWNER |
| Members | `GET/POST /members`, `GET/PUT/DELETE /members/{id}`, `GET /members/{id}/profile` | STAFF; delete ADMIN/OWNER |
| Plans | `GET /plans`, `POST/PUT /plans` | read STAFF; write ADMIN/OWNER |
| Payments | `GET /payments`, `POST /payments/renew`, `GET /payments/{id}/receipt.pdf` | authenticated |
| Attendance | `GET /attendance`, `POST /attendance/check-in/{memberId}`, `POST /attendance/check-out/{memberId}` | authenticated |
| Dashboard/settings | `GET /dashboard`, `GET/PUT /settings` | settings write ADMIN/OWNER |
| Audit/reports | `GET /audit-logs`, `GET /reports/{members,payments}.xlsx` | ADMIN/OWNER |

Validation errors use `{timestamp,status,message,path,fieldErrors}`. Pagination uses Spring `page`, `size` and `sort` parameters.
