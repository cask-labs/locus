# Runtime Watchdog (Self-Healing)

The **Runtime Watchdog** is an internal autonomous subsystem designed to detect and recover from inconsistent application states without user intervention. It acts as the "Immune System" of the Locus application.

## 1. Core Responsibilities

The Watchdog operates on a periodic schedule (e.g., every 15 minutes) via `WorkManager` and verifies the following **Invariants**:

1.  **Service Health (Unresponsive Check):**
    *   IF `TrackingState` is `RECORDING`, THEN `LastHeartbeatTimestamp` must be < 45 minutes old.
    *   *Note: This single check covers both "Service Dead" (process died) and "Zombie" (thread deadlocked) states.*
2.  **Permission Integrity:** IF `TrackingState` is `RECORDING`, THEN `ACCESS_BACKGROUND_LOCATION` must be `GRANTED`.
3.  **Upload Health:** IF `BufferAge` > 4 hours AND `Network` is `CONNECTED` AND `Battery` > 15%, THEN `LastUploadTimestamp` must be < 1 hour ago.

## 2. Recovery Actions

If an invariant is violated, the Watchdog executes a graded response:

| Violation | Action | User Notification |
| :--- | :--- | :--- |
| **Service Unresponsive** (Stale Heartbeat) | Restart `LocationService` (Stop/Start) | None (Silent Recovery)* |
| **Permissions Lost** | Stop Tracking, Set State `PAUSED` | **Tier 3 Fatal Error:** "Locus • Service Halted" (Sound/Vibration) |
| **Upload Stuck** | Trigger Sync (Strict Profile) | None (Silent Retry) |
| **Service Start Fail** | Increment Fail Count (Circuit Breaker) | None (Until 3 Strikes) |

*\*Note: On Android 12+, Silent Recovery may fail due to background start restrictions (see 3.2).*

## 3. Implementation Details

### 3.1. The Passive Heartbeat (Unresponsive Detection)
To detect "Zombie" services (where the process is alive but the thread is deadlocked) or "Dead" services (process killed) without waking the device unnecessarily:
*   **The Service:** When `RECORDING`, uses a **Coroutine Loop** (`Dispatchers.IO` + `delay`) to write the current timestamp to a lightweight file (SharedPreferences) **every 15 minutes**. This loop runs within the Foreground Service's scope.
*   **The Watchdog:** Reads this timestamp every 15 minutes.
    *   **Logic:** IF `CurrentTime - LastHeartbeat > 45 minutes` (3 missed beats), THEN the Service is presumed Unresponsive.
    *   **Action:** Restart the Service (`stopService()` followed by `startService()`).

### 3.2. Android 12+ Background Restrictions
On Android 12 (API 31)+, `WorkManager` cannot start a Foreground Service while the app is in the background.

*   **Strategy:**
    1.  Attempt `startForegroundService()`.
    2.  Catch `ForegroundServiceStartNotAllowedException` (or check SDK version).
    3.  **Immediate Fallback:** This exception is structural and cannot be fixed by retries.
        *   **Bypass Circuit Breaker:** Do not increment retry count.
        *   **Action:** Post a **High Priority Notification** (as defined in [Notifications Spec](ui/notifications.md)):
            *   **Title:** "Locus • Service Halted"
            *   **Text:** "Tracking Paused Unexpectedly"
            *   **Intent Action:** `com.locus.android.ACTION_RESUME_TRACKING`
            *   **Behavior:** Tapping the notification triggers the `BroadcastReceiver` which calls `startForegroundService()` (allowed from user interaction).

### 3.3. The Watchdog Worker
*   **Component:** `WatchdogWorker` (Extends `CoroutineWorker`).
*   **Triggers:**
    *   **Periodic:** Every 15 minutes (Minimum interval for `WorkManager`).
    *   **Event-Driven:**
        *   `BOOT_COMPLETED`: Reschedules the Watchdog and checks state after device restart.
        *   `MY_PACKAGE_REPLACED`: Reschedules the Watchdog after an app update.
*   **Constraints:**
    *   **Uploads:** Only attempts upload recovery if `Battery > 15%` (Strict Mode). This ensures the recovery process does not drain a low battery.
    *   **Checks:** Always runs (low cost) to verify safety invariants.

### 3.4. Storage Reliability
*   **EncryptedSharedPreferences:** Used for sensitive data (e.g., Auth Tokens) and the Telemetry Salt.
*   **Fallback:** If `EncryptedSharedPreferences` fails to initialize (common on some OEM devices), the system must:
    *   **Auth Tokens:** Fail Hard (Security requirement).
    *   **Telemetry Salt:** Fallback to standard `Context.MODE_PRIVATE` SharedPreferences (Availability requirement).
    *   **Heartbeat Timestamp:** Use standard `Context.MODE_PRIVATE` (Performance requirement).

### 3.5. Circuit Breaker (Anti-Loop)
To prevent infinite restart loops when a fatal bug exists within the Service itself:
1.  **Scope:** Strictly covers **Service Instability** (Crashes, Hangs, Start Failures). It does *not* cover Permissions or Network issues.
2.  **Counter:** The Watchdog maintains a `ConsecutiveRestartCount` (persisted in SharedPreferences).
3.  **Success:** If the Service runs successfully for > 15 minutes (verified by a fresh Heartbeat), reset `ConsecutiveRestartCount` to 0.
4.  **Failure:** If the Watchdog must restart the service (due to Unresponsive state), increment `ConsecutiveRestartCount`.
5.  **Strike Three (Trip):** IF `ConsecutiveRestartCount >= 3`:
    *   **Stop Retrying.**
    *   **Action:** Trigger **Tier 3 Fatal Error**: "Tracking Failed: Service Unstable."
6.  **Reset Condition:** The Circuit Breaker is reset to Closed (0) **only** when the user manually opens the application or toggles tracking, acknowledging the error.

## 4. Telemetry Integration

Every Watchdog intervention is a high-priority telemetry event.
*   **Log:** "Watchdog: Restarted Service (Reason: Heartbeat Timeout > 45min)."
*   **Destination:** Pushed to `User S3` via `PerformSyncUseCase(SyncType.REGULAR)`. This respects battery constraints while attempting to upload the diagnostic data.
