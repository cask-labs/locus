# Runtime Watchdog (Self-Healing)

The **Runtime Watchdog** is an internal autonomous subsystem designed to detect and recover from inconsistent application states without user intervention. It acts as the "Immune System" of the Locus application.

## 1. Core Responsibilities

The Watchdog operates on a periodic schedule (e.g., every 15 minutes) via `WorkManager` and verifies the following **Invariants**:

1.  **Service State (Zombie Check):** IF `TrackingState` is `RECORDING`, THEN `LocationService` must be `RUNNING` AND `LastHeartbeatTimestamp` must be < 90 minutes old.
2.  **Permission Integrity:** IF `TrackingState` is `RECORDING`, THEN `ACCESS_BACKGROUND_LOCATION` must be `GRANTED`.
3.  **Upload Health:** IF `BufferAge` > 4 hours AND `Network` is `CONNECTED` AND `Battery` > 15%, THEN `LastUploadTimestamp` must be < 1 hour ago.

## 2. Recovery Actions

If an invariant is violated, the Watchdog executes a graded response:

| Violation | Action | User Notification |
| :--- | :--- | :--- |
| **Service Dead/Zombie** | Restart `LocationService` | None (Silent Recovery) |
| **Permissions Lost** | Stop Tracking, Set State `PAUSED` | **Tier 3 Fatal Error:** "Tracking Stopped: Permission Revoked" (Sound/Vibration) |
| **Upload Stuck** | Trigger "Manual Sync" Logic | None (Silent Retry) |
| **Service Start Fail** | Increment Fail Count (Circuit Breaker) | None (Until 3 Strikes) |

## 3. Implementation Details

### 3.1. The Passive Heartbeat (Zombie Detection)
To detect "Zombie" services (where the process is alive but the thread is deadlocked) without waking the device unnecessarily:
*   **The Service:** When `RECORDING`, uses `AlarmManager` to wake up **once per hour** and write the current timestamp to a lightweight file (SharedPreferences). This occurs even in Stationary Mode.
*   **The Watchdog:** Reads this timestamp every 15 minutes.
    *   **Logic:** IF `CurrentTime - LastHeartbeat > 90 minutes`, THEN the Service is presumed Dead/Zombie.
    *   **Action:** Kill and Restart the Service.

### 3.2. The Watchdog Worker
*   **Component:** `WatchdogWorker` (Extends `CoroutineWorker`).
*   **Triggers:**
    *   **Periodic:** Every 15 minutes (Minimum interval for `WorkManager`).
    *   **Event-Driven:**
        *   `BOOT_COMPLETED`: Reschedules the Watchdog and checks state after device restart.
        *   `MY_PACKAGE_REPLACED`: Reschedules the Watchdog after an app update.
*   **Constraints:**
    *   **Uploads:** Only attempts upload recovery if `Battery > 15%` to prevent death loops on low battery.
    *   **Checks:** Always runs (low cost) to verify safety invariants.

### 3.3. Circuit Breaker (Anti-Loop)
To prevent infinite restart loops when a fatal bug exists:
1.  **Counter:** The Watchdog maintains a `ConsecutiveRestartCount` (persisted in SharedPreferences).
2.  **Success:** If the Service runs successfully for > 15 minutes (verified by a fresh Heartbeat), reset `ConsecutiveRestartCount` to 0.
3.  **Failure:** If the Watchdog must restart the service, increment `ConsecutiveRestartCount`.
4.  **Strike Three (Trip):** IF `ConsecutiveRestartCount >= 3`:
    *   **Stop Retrying.**
    *   **Action:** Trigger **Tier 3 Fatal Error**: "Tracking Failed: Service Unstable."
5.  **Reset Condition:** The Circuit Breaker is reset to Closed (0) **only** when the user manually opens the application or toggles tracking, acknowledging the error.

## 4. Telemetry Integration

Every Watchdog intervention is a high-priority telemetry event.
*   **Log:** "Watchdog: Restarted Service (Reason: Heartbeat Timeout > 90min)."
*   **Destination:** Pushed to `User S3` immediately to aid in debugging "Heisenbugs."
