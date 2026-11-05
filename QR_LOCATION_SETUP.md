# QR location sharing setup

## Required backend secrets

Database passwords and JWT signing keys are not stored in source control. Set them before starting Spring Boot. `CMS_JWT_SECRET` must be a sufficiently long Base64-encoded signing key compatible with the existing JWT implementation.

```powershell
$env:CMS_DB_PASSWORD = '<your MySQL password>'
$env:CMS_JWT_SECRET = '<your Base64 JWT signing key>'
```

Optional configuration overrides include `CMS_DB_URL`, `CMS_DB_USERNAME`, `CMS_PHOTO_STORAGE_DIR`, `CMS_PHOTO_MAX_SIZE_BYTES`, `CMS_PHOTO_MAX_ACCURACY_METERS`, and `CMS_LOCATION_SHARING_EXPIRY_MINUTES`. The checked-in defaults use `uploads/complaints`, 5 MB, 50 m, and five minutes respectively.

## Required MySQL migration

Spring uses `spring.jpa.hibernate.ddl-auto=validate`, so the QR session table must be created manually before restarting the backend.

In MySQL Workbench:

1. Connect to the MySQL server used by CMS.
2. Choose **File > Open SQL Script** and open `cmsbackend/src/main/resources/db/migration/V2__create_location_sharing_sessions.sql`.
3. Set `cms` as the default schema, or run `USE cms;` before the script.
4. Execute the entire script once.
5. Confirm that `location_sharing_sessions` exists, then restart Spring Boot.

## Cross-device URL configuration

The backend generates the one-time phone URL from `CMS_FRONTEND_BASE_URL`. Set it to the frontend origin that the phone can reach. Allow that same origin through CORS:

```powershell
$env:CMS_FRONTEND_BASE_URL = 'https://your-phone-reachable-frontend.example'
$env:CMS_ALLOWED_ORIGINS = 'https://your-phone-reachable-frontend.example'
```

Build or start the frontend with `VITE_API_BASE_URL` pointing to a phone- and computer-reachable backend origin.

Mobile browsers normally allow geolocation only in a secure context. For physical phone testing, use a trusted HTTPS frontend and HTTPS API/reverse proxy; a plain `http://<LAN-IP>` URL is not sufficient on most phones. Do not put JWTs or user data in the QR URL.

## Cleanup strategy

Expired sessions are rejected and changed to `EXPIRED` when polled or submitted. The migration indexes `status` and `expires_at`, so old terminal rows can be removed periodically with an operational cleanup query such as:

```sql
DELETE FROM location_sharing_sessions
WHERE status IN ('COMPLETED', 'EXPIRED', 'CANCELLED')
  AND expires_at < CURRENT_TIMESTAMP - INTERVAL 7 DAY;
```

Review the retention period for your deployment before running cleanup. No scheduled deletion job is installed by this phase.
