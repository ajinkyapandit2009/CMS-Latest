# CMS final integration test checklist

Apply both SQL migrations, configure required environment variables, and start the frontend and backend before performing these tests. Record the account, complaint ID, time, and observed result for each test.

## Mobile citizen flow

1. Log in as a citizen and open **Raise a Complaint**.
2. Capture a new camera photo; confirm there is no gallery or manual-coordinate option.
3. Choose **Use This Device's Location**, grant permission, and verify the displayed accuracy meets the configured threshold.
4. Confirm the photo uploads once, submit the complaint, and verify its protected image on the citizen dashboard.
5. Confirm the submission notification appears.

## Laptop and phone flow

1. Log in as a citizen on the laptop, capture a webcam photo, and choose **Get Location Using Phone**.
2. Scan the QR code on a phone. Confirm location permission is not requested until **Share My Location** is pressed.
3. Grant permission and confirm the laptop receives location, closes the modal, and uploads the webcam photo once.
4. Submit the complaint and verify its protected image on the citizen dashboard.
5. Repeat with a simulated photo-upload failure; verify **Retry Upload** works without a new QR scan.

## Administrator and officer flow

1. Log in as an administrator, verify the complaint photograph, assign it, and change priority.
2. Confirm affected users receive notifications.
3. Log in as the assigned officer, verify the photograph, update status with remarks, and confirm the citizen receives a notification.

## Authorization and one-time-token checks

1. Request a complaint image without a JWT and expect authentication failure.
2. Request it as another citizen and as an unassigned officer and expect denial.
3. Request it as its owner, an administrator, and its assigned officer and expect success.
4. Submit location twice with the same QR token; only the first request may succeed.
5. Verify expired and cancelled QR tokens fail.
6. Verify another citizen cannot poll or cancel the session.
