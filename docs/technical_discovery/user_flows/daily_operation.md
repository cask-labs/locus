# Daily Operation (Passive)

**Goal:** Continuous, reliable data collection with detailed transparency, resilience, and smart adaptation.

*   **Transparency (Status Indication):**
    *   The app utilizes a persistent notification to confirm active operation.
    *   **Format:** The notification content follows the structure `[Recording Status] • [Sync Status]`.
    *   **Examples:**
        *   *Healthy:* "Tracking (High Accuracy) • Synced"
        *   *Buffering:* "Tracking (Offline) • Buffered: 140 pts"
        *   *Acquiring:* "Searching for GPS..."

*   **Smart Adaptation (Sensing):**
    *   The system dynamically adjusts behavior based on movement and power states.
    *   **Stationary:** The app enters "Sleep Mode" (GPS Off, Accelerometer Monitoring).
        *   *Benefit:* This drastically reduces battery consumption compared to keeping the GPS active, allowing for multi-day battery life when the device is static.
    *   **Moving:** When the accelerometer detects significant movement, the app wakes the GPS and switches to "Full Fidelity Mode" (GPS + Sensors) for detailed tracking.
    *   **Battery Saver Mode:** The app ignores the OS "Battery Saver" toggle and continues to request the Partial Wake Lock to ensure data continuity.

*   **Battery Management:**
    *   **Low Battery (<10%):** Uploads pause, and tracking frequency reduces. The user sees a "Paused: Low Battery" status.
    *   **Critical Battery (<3%):** Tracking frequency reduces to 60s to preserve the phone's remaining life.
    *   **Recovery:** When charged >15%, the app automatically resumes full-fidelity tracking and syncing.

*   **Error Recovery Hierarchy:**
    *   **Tier 1: Transient Errors (Self-Healing):**
        *   *Scenarios:* Network timeouts, S3 5xx errors, GPS signal loss.
        *   *Action:* The app buffers data locally and retries silently with exponential backoff.
        *   *User Feedback:* Invisible to the user, other than a growing "Buffered" count in the notification.
    *   **Tier 2: Environmental Pauses (State Awareness):**
        *   *Scenarios:* Airplane Mode, No Internet, Low Battery.
        *   *Action:* Uploads (or tracking) pause until the condition clears.
        *   *User Feedback:* The notification text updates to explain the pause (e.g., "Paused: Waiting for Network"). No sound or vibration occurs.
    *   **Tier 3: Fatal Errors (Action Required):**
        *   *Scenarios:* AWS Auth Failure (403), Bucket Missing (404), Permission Revoked.
        *   *Action:* The app triggers a "Circuit Breaker," permanently stopping network or tracking attempts to prevent battery drain.
        *   *User Feedback:* A distinct, alerting notification appears (e.g., "Upload Failed: Access Denied"). Tapping it leads the user to the resolution screen.

*   **Resilience (Intervention Loops):**
    *   **Auto-Start:** The service automatically launches on device boot.
    *   **Permission Loss:** If the OS revokes background location, the app immediately fires a high-priority alert demanding user intervention.
    *   **Storage Limits:** If the local buffer exceeds a safety threshold (e.g., 10k points), the system warns the user to prevent data loss.

*   **User Override (Stop/Resume):**
    *   **Intentional Stop:**
        *   **Action:** User taps "Stop Tracking" on the Notification or Dashboard.
        *   **Confirmation:** A dialog ("Stop Tracking?") appears to prevent accidental touches.
        *   **System State:** The Foreground Service stops completely. Wake locks are released.
        *   **Feedback:** Notification is dismissed. Dashboard Status Card updates to "Stopped by User" (Yellow).
    *   **Manual Resume:**
        *   **Action:** User taps "Resume Tracking" on the Dashboard.
        *   **System State:** Service restarts.
        *   **Feedback:** Dashboard Status transitions to "Acquiring GPS..." (Yellow) until a valid fix is obtained, then "Recording" (Green).
