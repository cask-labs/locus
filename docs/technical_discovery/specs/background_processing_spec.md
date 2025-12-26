# Background Processing Specification

## Overview
Reliable background processing is the core feature of Locus. The app must run continuously to capture location data, regardless of screen state.

## Components

### 1. Tracker Service (`TrackerService`)
*   **Type:** `ForegroundService`.
*   **Permission:** `FOREGROUND_SERVICE_LOCATION`.
*   **Responsibility:**
    *   Holds the `LocationManager` listener.
    *   Buffers points to `Room` database.
    *   Updates the Persistent Notification.
*   **Lifecycle:** Starts on Dashboard load (or boot). Runs indefinitely until "Stop" is pressed.

### 2. Provisioning Worker (`ProvisioningWorker`)
*   **Type:** `CoroutineWorker` (WorkManager).
*   **Configuration:** `OneTimeWorkRequest` + `Expedited` + `setForeground()`.
*   **Responsibility:**
    *   Executes the CloudFormation deployment.
    *   Ensures the process completes reliably even if the app is backgrounded.
    *   Updates a Notification via `setForeground()` to inform the user of progress.
*   **Lifecycle:** Enqueued when user confirms setup. Runs until success or failure.

### 3. Sync Worker (`SyncWorker`)
*   **Type:** `WorkManager` (Periodic).
*   **Responsibility:**
    *   Reads buffered points from `Room`.
    *   Generates NDJSON.gz files.
    *   Uploads to S3.
*   **Schedule:** Every 15 minutes (approx).

### 4. Watchdog Worker (`WatchdogWorker`)
*   **Type:** `WorkManager` (Periodic).
*   **Responsibility:**
    *   Checks if `TrackerService` should be running (is `Authenticated` + `TrackingEnabled`?).
    *   Checks if it *is* running.
    *   Restarts it if missing.

## Constraints & Rules
*   **Battery Saver:** The service must acquire a `PARTIAL_WAKE_LOCK` only while processing a batch of locations, then release it.
*   **Doze Mode:** We rely on `WorkManager` for network tasks, which respects Doze windows. The `TrackerService` continues to receive location updates via the `LocationManager` (which wakes the CPU briefly).
