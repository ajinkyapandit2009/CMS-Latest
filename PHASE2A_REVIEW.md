# Phase 2A - authorized complaint lists and details

## Implemented

- `/complaints` provides a shared complaint list with search, status/priority filters, and newest/oldest/title/priority sorting. It uses the existing citizen, officer, or administrator list API according to the authenticated role.
- Search/filter/sort state is stored in the URL. Detail links retain it for the Back to complaints action.
- `/complaints/:complaintId` remains inside the existing authenticated sidebar layout. It shows the existing complaint data, protected photograph, coordinates, address, status, priority, timestamps, and authorized participant information.
- Existing citizen/admin/officer cards are reused for details. Administrator assignment/priority updates and officer status/remarks continue using the existing mutation APIs and refresh the authorized record after success.
- Dashboard complaint titles now open the dedicated detail page. Existing dashboard summaries and actions remain in place.
- Empty, loading, retry, invalid-ID, unavailable-record, and session-expired messages are included. The table supports keyboard scrolling on smaller screens.
- Statistics-card changes, image modals, complaint history, notification navigation changes, and other enhancement phases are not included.

## API contract and authorization

New endpoint: `GET /api/complaints/{complaintId}`. It requires existing JWT authentication and checks the current database user in `ComplaintServiceImpl`:

- Citizen: complaint must belong to that citizen.
- Officer: complaint must currently be assigned to that officer.
- Administrator: may read any complaint.
- Inaccessible and absent complaints both return 404; the response is mapped only after authorization.
- Assigned officers receive citizen names, but not citizen email contact information. Owners and administrators retain that email field.

Existing lists are reused unchanged:

- `GET /api/complaints/my`
- `GET /api/officer/complaints/assigned`
- `GET /api/admin/complaints`

Search/filter/sort operates on the authorized arrays returned by those APIs. No pagination, new database tables, migrations, dependencies, or HTTPS configuration changes were introduced.

## Files created

- `cmsfrontend/cms-frontend/src/components/ComplaintList.jsx`
- `cmsfrontend/cms-frontend/src/pages/ComplaintListPage.jsx`
- `cmsfrontend/cms-frontend/src/pages/ComplaintDetailsPage.jsx`
- `cmsfrontend/cms-frontend/src/services/complaintListUtils.js`
- `cmsfrontend/cms-frontend/src/styles/complaint-pages.css`
- `cmsfrontend/cms-frontend/scripts/phase2a-complaints.test.mjs`
- `cmsbackend/src/test/java/com/cmsbackend/services/ComplaintReadAuthorizationTest.java`
- This report.

## Files modified

- `cmsfrontend/cms-frontend/src/App.jsx`
- `cmsfrontend/cms-frontend/src/components/DashboardSidebar.jsx`
- `cmsfrontend/cms-frontend/src/components/DashboardTopbar.jsx`
- `cmsfrontend/cms-frontend/src/components/ComplaintCard.jsx`
- `cmsfrontend/cms-frontend/src/components/AdminComplaintCard.jsx`
- `cmsfrontend/cms-frontend/src/components/OfficerComplaintCard.jsx`
- `cmsfrontend/cms-frontend/src/services/complaintService.js`
- `cmsbackend/src/main/java/com/cmsbackend/controller/ComplaintController.java`
- `cmsbackend/src/main/java/com/cmsbackend/services/ComplaintService.java`
- `cmsbackend/src/main/java/com/cmsbackend/services/impl/ComplaintServiceImpl.java`
- `IMPLEMENTATION_PROGRESS.md`

## Checks performed

- `npm.cmd run lint`: passed.
- `npm.cmd run build`: passed.
- `node --test scripts/phase2a-complaints.test.mjs`: 13 passed. Covers search, combined filters, sorting, date fallbacks, empty states, generated links, sidebar/active navigation for each role, anonymous access, malformed IDs, and role-specific service URLs/bearer headers using test fixtures.
- `node --test scripts/phase1-navigation.test.mjs`: 20 passed.
- `mvnw.cmd -Dtest=ComplaintReadAuthorizationTest test`: Maven failed before startup with `Cannot index into a null array`. Java compilation and the new backend tests did not execute.
- Tests using fixtures and server rendering are not live browser/API/SQL or hardware verification.

## Manual steps

1. Restart Spring Boot to load the new detail endpoint; no SQL migration is needed. Start the frontend with the existing HTTPS setup.
2. Log in as each role and open its complaint list from the sidebar. Verify the correct accessible records, search, both filters, and every sort option.
3. Open a record, verify the sidebar stays visible, check all metadata/photo/coordinates/address, and return to the filtered list.
4. As admin, change priority and assign/reassign a permitted complaint from details. As its assigned officer, update status/remarks. Verify the record refreshes and existing notifications appear.
5. Attempt another citizen's complaint and an officer's unassigned/unrelated complaint via direct detail URLs and authenticated API requests. Expect 404 and no record data. Verify administrators can access both assigned and unassigned complaints. Check anonymous requests are denied.
6. Test narrow-screen table scrolling, keyboard navigation, browser back/forward, and mobile sidebar behavior.
7. Regression-test existing camera/GPS, automatic address lookup, photo upload, public phone-sharing QR flow, protected images, and HTTPS on actual devices.

Stop after Phase 2A for review.
