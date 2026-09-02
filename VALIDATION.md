# Validation record

## Automated checks

- Frontend ESLint: pass.
- Frontend production build: pass.
- Frontend unit tests cover API error handling, role navigation, business-form validation, password policy and filter construction.
- Backend unit tests cover password policy. A broader MockMvc suite covers login, disabled users, invalid tokens, logout revocation, rate limiting, OWNER/ADMIN/STAFF authorization, validation, duplicates, missing IDs and all report exports.

## Environment limitation

The current Windows Codex host denies Java's ZIP filesystem access while `javac` closes dependency JARs (`AccessDeniedException`). This prevents the new MockMvc suite from compiling here despite successful application-source compilation in earlier passes. Docker is not installed. A local MySQL 8 service is running, but its root password is not available to this task, so a disposable database cannot be created safely.

Consequently, real MySQL startup/restart persistence and full end-to-end browser validation remain required on a normal developer or CI host. Do not load commercial data until those gates pass.

## Required acceptance command

```powershell
cd backend
mvn test
cd ..\frontend
npm ci
npm run lint
npm run test
npm run build
docker compose up --build
```

Then execute the OWNER, ADMIN and STAFF workflow checklist from the project brief against disposable data, restart the backend, verify persistence and repeat Flyway startup.
