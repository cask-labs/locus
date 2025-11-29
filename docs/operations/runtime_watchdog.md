# Runtime Watchdog (Self-Healing)

The **Runtime Watchdog** is an internal autonomous subsystem designed to detect and recover from inconsistent application states without user intervention. It acts as the "Immune System" of the Locus application.

## 1. Core Responsibilities

The Watchdog operates on a periodic schedule (e.g., every 15 minutes) via `WorkManager` and verifies the following **Invariants**:

1.  **Service State:** IF `TrackingState` is `RECORDING`, THEN `LocationService` must be `RUNNING`.
2.  **Permission Integrity:** IF `TrackingState` is `RECORDING`, THEN `ACCESS_BACKGROUND_LOCATION` must be `GRANTED`.
3.  **Data Flow:** IF `TrackingState` is `RECORDING` AND Device is `MOVING`, THEN `TrackPointCount` must have increased since the last check.
4.  **Upload Health:** IF `BufferAge` > 4 hours AND `Network` is `CONNECTED`, THEN `LastUploadTimestamp` must be < 1 hour ago.

## 2. Recovery Actions

If an invariant is violated, the Watchdog executes a graded response:

| Violation | Action | User Notification |
| :--- | :--- | :--- |
| **Service Dead** | Restart `LocationService` | None (Silent Recovery) |
| **Permissions Lost** | Stop Tracking, Set State `PAUSED` | **Subtle Indicator:** "Tracking Paused: Permissions Needed" |
| **Sensor Freeze** | Restart Sensor Listeners | None (Silent Recovery) |
| **Upload Stuck** | Trigger "Manual Sync" Logic | None (Silent Retry) |

## 3. Implementation Details

### 3.1. The Watchdog Worker
*   **Component:** `WatchdogWorker` (Extends `CoroutineWorker`).
*   **Triggers:**
    *   Periodic: Every 15 minutes (Minimum interval for `WorkManager`).
    *   Event-Driven: On `BOOT_COMPLETED`, on `MY_PACKAGE_REPLACED` (App Update).
*   **Constraints:** Requires `BATTERY_NOT_LOW` (unless in Critical Recovery mode).

### 3.2. State Verification
To avoid "False Positives" (e.g., user is stationary, so no points recorded), the Watchdog queries the **Motion Activity** history.
*   *Logic:* "If Accelerometer says 'Still' for 15 minutes, then 0 new points is **Valid**."
*   *Logic:* "If Accelerometer says 'Walking' for 15 minutes, then 0 new points is **Invalid** -> Restart GPS."

## 4. Telemetry Integration

Every Watchdog intervention is a high-priority telemetry event.
*   **Log:** "Watchdog: Restarted Service (Reason: Service Dead while Recording)."
*   **Destination:** Pushed to `User S3` immediately to aid in debugging "Heisenbugs."
