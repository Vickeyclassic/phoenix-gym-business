# Frontend structure

The application entry point is `frontend/src/main.jsx`. It mounts `App` and loads the global styles.

- `App.jsx`: session validation, branding provider and login/application selection.
- `components/Shell.jsx`: navigation, session controls, toast feedback and page layout.
- `pages/`: dashboard, members, plans, payments, attendance, reports, staff, audit, business settings and password change.
- `components/ui.jsx`: shared fields, tables, panels, modal and pagination components.
- `context/Branding.jsx`: shared business identity and logo rendering.
- `hooks/useLoad.js`: request lifecycle, stale-response protection and reload behaviour.
- `utils/`: dates, pagination URLs and browser downloads.
- Existing `api.js`, `logic.js`, `branding.js` and `password.js` retain their API and validation responsibilities.

The extraction preserves business rules and API contracts. Permissions remain enforced by the backend; frontend navigation checks only determine what is shown.

Run from `frontend/`:

```sh
npm ci
npm run lint
npm run test
npm run build
```
