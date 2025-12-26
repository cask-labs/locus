# Background Processing Specification

## Overview
Reliable background processing is the core feature of Locus. The app must run continuously to capture location data, regardless of screen state, while respecting system constraints (Doze, App Standby).

## Components

### 1. Tracker Service (`TrackerService`)
*   **Type:** `ForegroundService`.
*   **Permission:** `FOREGROUND_SERVICE_LOCATION`.
*   **Responsibility:** The primary engine for data collection.
    *   Holds the `LocationManager` listener.
    *   Buffers points to the local `Room` database (Circular Buffer).
    *   Updates the Persistent Notification (Status, Stats).
*   **Lifecycle:**
    *   **Start:** Explicitly started via `Context.startForegroundService()` when the user taps "Start Tracking" or upon Boot (if previously active).
    *   **Stop:** Stopped only when the user taps "Stop Tracking" (User Explicit).
    *   **Crash:** If the process is killed by the OS, `START_STICKY` ensures the system attempts to recreate it.

#### State Machine
*   **Active:** GPS requesting updates (HIGH_ACCURACY).
*   **Stationary:** Accelerometer monitoring only (No GPS). Triggered by `SignificantMotion` or Activity Recognition.
*   **Paused:** Traffic Guardrail or Low Battery (Environmental Pause). Service runs but data collection is suspended.
*   **Stopping:** Transient state during shutdown.

#### Battery Safety Strategy
*   **Wake Locks:** The service utilizes a `PARTIAL_WAKE_LOCK` **only** during the brief processing window (receiving a location -> writing to DB). It does **not** hold a continuous lock.
*   **Intervals:**
    *   **Normal:** 10s (Active).
    *   **Low Battery (<15%):** 30s.
    *   **Critical (<3%):** Service strictly pauses (Deep Sleep).

### 2. Provisioning Worker (`ProvisioningWorker`)
*   **Type:** `CoroutineWorker` (WorkManager).
*   **Configuration:** `OneTimeWorkRequest` + `Expedited` + `setForeground()`.
*   **Responsibility:**
    *   Executes the CloudFormation deployment for Onboarding/Recovery.
    *   Ensures the process completes reliably even if the app is backgrounded (Android 14+ Compliance).
    *   Updates a Notification via `setForeground()` to inform the user of progress.
*   **Security:** Reads credentials from `AuthRepository` (Secure Storage), strictly avoiding input data serialization.

### 3. Sync Worker (`SyncWorker`)
*   **Type:** `WorkManager` (Periodic).
*   **Configuration:** `PeriodicWorkRequest` (15 minutes).
*   **Constraints:** `NetworkType.CONNECTED` + `BatteryNotLow`.
*   **Responsibility:**
    *   Reads buffered points from `Room`.
    *   Generates NDJSON.gz files.
    *   Uploads to S3 (and Community if enabled).
    *   Mark uploaded ranges in `CursorEntity`.
*   **Traffic Guardrail:** Checks `TrafficGuardrail` (50MB/day limit) before attempting upload.

### 4. Watchdog Worker (`WatchdogWorker`)
*   **Type:** `WorkManager` (Periodic).
*   **Configuration:** `PeriodicWorkRequest` (15 minutes).
*   **Responsibility:** The "Liveness Check".
    *   **Check:** Is `ServiceHealthRepository.shouldBeRunning()` true?
    *   **Verify:** Is `TrackerService` actually running?
    *   **Action:** If `Should == True` but `Is == False`, it attempts to restart the service.
    *   **Circuit Breaker:** If restart fails 3 times in 1 hour, it sets a "Service Instability" flag and notifies the user to manually intervene.
