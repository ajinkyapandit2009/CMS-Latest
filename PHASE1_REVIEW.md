# Enhancement Phase 1: dashboard and navigation

## Completed scope

- Existing citizen, administrator, and officer pages now share an authenticated dashboard layout with a persistent green sidebar and top controls.
- Desktop sidebar collapse, mobile drawer, backdrop/Escape dismissal, keyboard focus containment, active navigation, skip link, user identity, existing notification bell, My Profile, and logout are implemented.
- Admin and officer sidebar section links point into their existing dashboard workflows. Dedicated list/details and officer-management pages remain later phases.
- `/profile` displays current first/last name, email, and role; it is a read-only placeholder.
- Central scrolling resets on pathname changes, including back/forward navigation, while retaining section anchors and avoiding resets for local filters.
- Raise a Complaint actions are visible to citizens. Public visitors see a sign-in action; other roles see a dashboard action on the homepage. The existing backend citizen-only enforcement is preserved.
- `/share-location/:token` remains public and outside the authenticated layout.

## Files created

- `cmsfrontend/cms-frontend/src/components/ScrollToTop.jsx`
- `cmsfrontend/cms-frontend/src/components/DashboardLayout.jsx`
- `cmsfrontend/cms-frontend/src/components/DashboardSidebar.jsx`
- `cmsfrontend/cms-frontend/src/components/DashboardTopbar.jsx`
- `cmsfrontend/cms-frontend/src/components/DashboardIcon.jsx`
- `cmsfrontend/cms-frontend/src/pages/ProfilePlaceholder.jsx`
- `cmsfrontend/cms-frontend/src/services/dashboardNavigation.js`
- `cmsfrontend/cms-frontend/src/styles/dashboard-layout.css`
- `cmsfrontend/cms-frontend/scripts/phase1-navigation.test.mjs`
- `cmsbackend/src/main/java/com/cmsbackend/security/RepositoryRestSecurityConfig.java`
- `cmsbackend/src/test/java/com/cmsbackend/security/RepositoryRestSecurityConfigTest.java`
- `cmsbackend/src/test/java/com/cmsbackend/security/jwt/AuthFilterLoggingTest.java`
- `cmsbackend/src/test/java/com/cmsbackend/security/ComplaintPhotoAuthorizationTest.java`
- This report.

## Files modified

- `.gitignore`
- `cmsfrontend/cms-frontend/src/App.jsx`
- `cmsfrontend/cms-frontend/src/components/ProtectedRoute.jsx`
- `cmsfrontend/cms-frontend/src/components/Navbar.jsx`
- `cmsfrontend/cms-frontend/src/components/NotificationBell.jsx`
- `cmsfrontend/cms-frontend/src/pages/Home.jsx`
- `cmsfrontend/cms-frontend/src/pages/AdminDashboardPlaceholder.jsx` (existing-section anchors only)
- `cmsfrontend/cms-frontend/src/pages/OfficerDashboardPlaceholder.jsx` (existing-section anchor only)
- `cmsbackend/src/main/java/com/cmsbackend/security/jwt/AuthFilter.java`
- `cmsbackend/src/main/java/com/cmsbackend/repositories/ComplaintPhotoUploadRepository.java`
- `cmsbackend/src/main/java/com/cmsbackend/services/impl/ComplaintPhotoServiceImpl.java`
- `IMPLEMENTATION_PROGRESS.md`

## Security corrections

- The actual notification file contained a 100 ms interval; it now uses the intended 30,000 ms interval. Dropdown/read/unread/read-all behavior is preserved; no popups were added.
- Removed full-URI debug logging from `AuthFilter`; QR tokens no longer enter stdout through that filter.
- Confirmed from the cached Spring Data REST 5.1.1 source that its default detection strategy exports public repository interfaces and exposes methods by default. The project's catch-all security rule required only authentication for generated repository routes. Added a configuration that rejects every repository from REST export and disables default method exposure. JPA injection and explicit controller APIs are unaffected. Live endpoint verification still requires a running backend.
- Added a pessimistic write lock to owner-scoped photo lookup inside the existing consumption transaction, preventing simultaneous successful consumption of the same upload.
- Unassociated draft photos are owner-only. Once associated with a complaint, its owner, administrators, and the assigned officer retain access.
- Ignored local `*-key.pem`, `.key`, `.p12`, `.pfx`, and frontend-local PEM files. Git confirms both existing private-key files are ignored and no key files are tracked. No key contents were displayed or committed.
- No new HTTP APIs or SQL migrations were introduced. No MySQL changes were performed.

## Verification

- `npm.cmd run lint`: passed with no warnings after corrections.
- `npm.cmd run build`: passed.
- `node --test scripts/phase1-navigation.test.mjs`: 20 passed. These are isolated server-rendered route/visibility checks using fixtures; they do not test browser effects or authenticate against a live backend.
- `git check-ignore` confirmed existing local SSL keys are excluded; `git ls-files` found no tracked key files.
- Targeted `mvnw.cmd ... test`: failed before Maven startup with the known `Cannot index into a null array` wrapper error. New Java security tests have not run; backend compilation and runtime behavior remain unverified.
- Browser automation stopped because it could not verify the current browser URL. No interactive browser/mobile test is reported as passed.
- A dev-server launch found port 5173 already in use. No existing server process was stopped.

## Manual review checklist

1. Start/restart the backend with its existing environment and certificates so the new security configuration is loaded. No migration is needed for this phase.
2. While logged out, open each dashboard URL, `/profile`, and `/complaints/new`. Confirm login redirection and no protected content.
3. Log in as each role. Confirm its existing dashboard data/actions, actual first and last name, notification bell, profile entry, and logout.
4. Confirm only citizens see the reporting action. Open `/complaints/new` as admin/officer and confirm redirection to the permitted dashboard. Backend complaint creation must still reject those roles.
5. Navigate dashboard > profile > dashboard; as citizen also visit the complaint form. Confirm the sidebar persists and active navigation follows the route.
6. Scroll down, navigate to another page, then use browser back/forward. Confirm page navigation starts at the top; local filtering does not reset scrolling. Confirm homepage and sidebar section links reach their intended sections.
7. At 320, 375, 768, and 900 px widths, open the mobile sidebar, navigate, press Escape, and click its backdrop. Check keyboard focus and that controls fit without horizontal overflow. Above 900 px, collapse/expand the sidebar and navigate between pages.
8. Open the notification dropdown, mark one/all read, and confirm the badge updates. Observe that periodic unread-count requests occur approximately every 30 seconds. No alert popup should appear.
9. Open a valid `/share-location/:token` on a phone without login. Confirm the minimal public page and permission only after pressing Share My Location. Perform the existing camera/GPS/address/photo/QR flow during regression testing.
10. With an authenticated test account, GET generated repository routes such as `/users`, `/complaints`, `/notifications`, `/complaintPhotoUploads`, and `/locationSharingSessions`. They must not expose repository data (404/405/denial is expected). Confirm explicit APIs such as `/auth/me`, `/api/complaints/my`, and role-specific admin/officer APIs retain their normal behavior.
11. Verify draft-photo authorization and associated-photo owner/admin/assigned-officer access. Concurrency testing of upload consumption should use an isolated test environment.

## Follow-up

Phase 2 remains pending review. Separate follow-up items include stale camera/upload/address responses after retake, camera permission cleanup races, employee-number concurrency, registration validation, and end-to-end QR/session concurrency tests. No complaint list/details redesign or automatic assignment was implemented in this phase.
