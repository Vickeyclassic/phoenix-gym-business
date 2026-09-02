# SaaS roadmap

V2 deliberately operates as one gym. It does not claim tenant isolation.

For multi-gym SaaS, first add an immutable `tenant_id` to users, members, plans, payments, attendance, settings and audit logs; backfill and validate it; add compound tenant uniqueness/indexes; then enforce tenant predicates in repositories and authorization tests. Use defense in depth such as database row-level controls where supported. Follow with tenant-scoped object storage, per-tenant keys/branding, a super-admin control plane, subscription billing, quotas, data export/deletion and tenant-aware observability.

Later product tracks: payment gateway adapters with signed webhooks and idempotency, biometric device credentials calling a narrow check-in API, mobile apps, custom domains, messaging reminders and multi-location reporting. Each requires a threat model and migration plan before implementation.
