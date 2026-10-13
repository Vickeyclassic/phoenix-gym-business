# Decisions to explain in a walkthrough

## Authorization

`JwtFilter` validates the signed token, checks the persistent revoked-token repository, then reads the current user's active state and role. Disabling a user or changing a role therefore affects existing sessions. The backend is the permission boundary; the React navigation is a convenience.

Logout saves the JWT identifier in `revoked_tokens`. Password-reset accounts are restricted to changing their password and a small set of authentication/public routes until they complete the change. Login throttling is currently in process memory, so a distributed deployment would need a shared limiter.

## Data and migrations

Flyway owns the production schema; Hibernate validates it. Member removal preserves historical financial/attendance records by archiving members with history. Business time is calculated in Asia/Kolkata. The private B.G. Fitness application remains a separate repository and installation.

## Frontend

Pages now have separate modules. Session and branding live in `App`, navigation/layout in `Shell`, and loading/reload/stale-response handling in `useLoad`. Shared fields, tables and panels avoid repeating the layout code across pages.

## Demonstrate, then explain

Show the [demo walkthrough](DEMO.md), point to the relevant source, and explain why those checks are on the server. Tests and screenshots are supporting evidence; practice explaining the failure paths and current limitations in your own words.
