# User Interface & Feedback Requirements

## 5.1. Philosophy
*   **Transparency:** The system must provide constant, clear visibility into its internal state (recording, syncing, buffering) without requiring active user interrogation.
*   **Subtle by Default:** The system must prioritize user attention; routine operations and transient errors must be communicated unobtrusively (e.g., text-only updates), avoiding alarm fatigue. Audio or vibration alerts are reserved strictly for Fatal Errors or critical user interventions.

## 5.2. Persistent Status (Notification)
*   **Visibility:** The system must maintain a persistent notification to indicate active background execution and prevent OS termination.
*   **Format:** The notification content must follow the strict format: `[Recording Status] • [Sync Status]`.
    *   *Examples:* "Tracking (High Accuracy) • Synced", "Tracking (Offline) • Buffered: 140 pts", "Paused: Low Battery • Synced".
*   **Subtle Indicators:** Transient issues (e.g., "Searching for GPS...", "Waiting for Network") must be displayed solely within this notification text, without triggering system-level alerts.

## 5.3. Error Handling Hierarchy
The system must adhere to a strict hierarchy for communicating errors to the user:
*   **Tier 1: Transient Errors (Silent):** Self-correcting issues (e.g., Network Timeout, S3 500) must be handled silently with exponential backoff. The only visible indicator is the incrementing "Buffered" count in the Persistent Status.
*   **Tier 2: Environmental Pauses (Passive):** Issues requiring state changes (e.g., "Airplane Mode", "Low Battery") must update the Persistent Status text (e.g., "Paused: Waiting for Network") but must not generate sound or vibration.
*   **Tier 3: Fatal Errors (Active):** Critical failures requiring user intervention must trigger a distinct, high-priority system notification (with sound/vibration) that links directly to a resolution screen.
    *   *Examples:* AWS 403 Access Denied, Permission Revoked (Watchdog), Service Fatal Error (Watchdog Circuit Breaker).

## 5.4. Dashboard & Operational Feedback
*   **Status Dashboard:** The main application view must display:
    *   **Local Buffer:** The count of data points currently waiting on the device.
    *   **Last Sync:** Relative time since the last successful upload (e.g., "5 minutes ago").
    *   **Connection State:** The current network eligibility status (e.g., "Ready", "No Internet").
*   **Manual Sync Feedback:**
    *   **Progress:** Initiating a manual sync must show immediate visual feedback (e.g., "Uploading batch 1 of 3...").
    *   **Success:** A successful sync must display a confirmation message (e.g., Toast "Sync Complete") and update the "Last Sync" time immediately.
    *   **Failure:** A failed manual sync must display a specific, actionable error message (e.g., "Upload failed: Check Internet Connection") to allow the user to rectify the issue.

## 5.5. Onboarding & Validation
*   **Input Validation:** The system must validate the "Device Name" against existing CloudFormation Stacks to ensure uniqueness within the account. Duplicate names must trigger a blocking error message requiring a new name.
*   **Dry Run Results:** The system must display the specific outcome of the initial credential check (e.g., "Success", "Invalid Signature", "Permission Denied") before allowing the user to proceed.
*   **Provisioning Status:** Long-running infrastructure setup tasks must be executed as a high-priority background task with a visible notification (Android "Foreground Service") to reassure the user that the process is active even if the screen is turned off.

## 5.6. Visualization
*   **Data Availability:** The calendar view must visually highlight days that contain verified historical data (based on the local cache) to guide the user to available tracks.
*   **Source Verification:** The map interface must strictly exclude data currently in the local buffer, rendering only tracks confirmed to be in S3, thereby validating the "Remote Storage" requirement.
*   **Signal Quality:** The system must support a "Heat Map" visualization overlay that uses color coding to represent signal strength and visual differentiation (e.g., stroke style) to distinguish between WiFi and Cellular location sources.

## 5.7. System Alerts & Permissions
*   **Permission Guidance:** The system must provide clear rationale screens ("Rationale UI") before requesting permissions, explaining *why* the permission is needed.
*   **Deep Linking:** If a critical permission (e.g., "Allow all the time") is denied or revoked, the system must provide a direct link to the specific App Settings screen to facilitate re-enabling it.
*   **Storage Warnings:** The system must trigger an alert if the local buffer size exceeds a safety threshold (e.g., 10,000 points), indicating a potential risk of data loss.

## 5.8. Accessibility (A11y)
*   **Content Descriptions:** All non-text visual elements (e.g., map markers, signal strength icons, dashboard icons) must have meaningful content descriptions for screen readers.
*   **Touch Targets:** Interactive elements (e.g., "Sync Now" button, calendar dates) must have a minimum touch target size of 48x48dp to ensure usability for all users.
*   **Color Contrast:** Text and critical visual indicators (including the Heat Map) must meet standard contrast ratios (e.g., WCAG AA) or provide alternative distinguishing features (e.g., patterns/icons) to support users with color vision deficiencies.
*   **Dynamic Text:** The application interface must respect the system-wide font scaling settings, ensuring all text remains legible and containers expand appropriately without truncation.
