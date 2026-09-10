# B.G. Fitness — Phase 1 provisioning

This package configures one deployment of the generic Phoenix Business product.
It does not modify master defaults or provision a database. No client credentials
are included. The original official JPEG is preserved byte-for-byte as
`branding/bg-fitness.jpg`; do not redesign or stretch it.

## Apply only during separately approved production provisioning

1. Create an isolated deployment and a **fresh database/volume**. Never copy or
   connect to the Docker DVAL database. Do not reuse its OWNER or secrets.
2. Generate fresh production secrets and bootstrap a new OWNER through the existing
   deployment process. Keep credentials outside Git. Follow BUSINESS_DEPLOYMENT.md
   and SECURITY.md; require the OWNER to change their initial password.
3. Place the supplied `branding` directory on the backend host, readable by the
   service account. Set `APP_BRANDING_DIRECTORY` to its absolute path. For Docker,
   mount it read-only and set the environment variable to the container path.
   Only operator-approved public JPEG/PNG assets (under 2 MB) belong here.
4. Sign in as OWNER and open Business Settings. Set **Gym name** to `B.G. Fitness`
   and **Logo URL** to `/api/public/branding-assets/bg-fitness.jpg`. Save.
   Confirm the client's phone, address, email, tax details and receipt footer
   before entering them; none have been invented. Confirm currency/time zone,
   reminder days and multiple-check-in policy with the owner.
5. Create the client's plans and staff accounts with the correct roles. Verify
   login, dashboard, member CRUD, renewals, payment history, manual check-in/out,
   expiry states, exports, receipt PDFs, audit records and mobile navigation.
6. Verify fresh-database startup, restart persistence, HTTPS, backups and restore
   before commercial data is entered. Deliver application access and OWNER login;
   source code and repository ownership remain with the product maintainer.

The configured identity appears on login, navigation, page header, settings preview
and browser title. Reports retain their settings-based gym-name heading. Receipt
PDFs use the configured name plus the local logo, scaled proportionally to 72 pt.
External HTTP(S) logo URLs remain supported in the browser; receipt generation
never fetches external URLs. Use the local asset URL for logos needed on receipts.
Missing/unreadable receipt logos fall back to a text receipt. The favicon is unchanged.

Future gyms supply another asset directory and update the same Business Settings;
no application source change is required. Name/logo are public branding metadata;
other settings stay authenticated. Existing OWNER/ADMIN settings-write permissions
are preserved.

## Phase 2

The existing attendance table has a VARCHAR(30) `source` defaulting to `MANUAL`,
timestamps and nullable `recorded_by`. It can represent BIOMETRIC/DEVICE sources.
No eSSL integration, device ingestion endpoint or speculative SDK is included.
Phase 2 still needs device/protocol verification, secure ingestion, member mapping,
deduplication, timestamp policy and retry handling. Preserve the current manual flow.

No deployment, production secrets, database writes, commit or push are performed
as part of this preparation.
