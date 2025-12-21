# Android Client Architecture

The Android application acts as both the data collector and the infrastructure controller. It implements the behaviors defined in the Functional Requirements.

## 1. System Requirements
*   **Minimum SDK:** API 28 (Android 9.0 Pie).
*   **Target SDK:** Latest Stable (e.g., API 34).
*   **Language:** Kotlin (Strict).

## 2. Build Strategy (Privacy & Variants)
To strictly satisfy both the "Privacy-First" (FOSS) and "Ease of Development" (Beta) requirements, the application utilizes **Product Flavors**:

*   **Standard (`standard`):**
    *   **External Dependencies:** Includes **Firebase Crashlytics** and **Google Play Services** (Strictly for Activity Recognition).
    *   **Goal:** Used for the Play Store and internal Beta testing to gather community crash statistics (Opt-In).
    *   **Dependency List:**
        *   `com.google.firebase:firebase-crashlytics`
        *   `com.google.android.gms:play-services-location`
*   **FOSS (`foss`):**
    *   **External Dependencies:** **Zero.** All proprietary libraries are stripped at compile time.
    *   **Goal:** Used for F-Droid and privacy-focused manual installation.
    *   **Mechanism:** Uses "No-Op" stubs for the Community Telemetry interface.
    *   **Implication:** FOSS users lose access to "Community Health Stats" (aggregated crash reports and performance metrics, e.g., ANRs).

## 3. Provisioner (Setup)
*   **Role:** Handles the one-time setup and infrastructure creation.
*   **Components:** UI Wizards, AWS SDK (CloudFormation client).
*   **Responsibilities:**
    *   Accept user credentials (Bootstrap Keys).
    *   Deploy the CloudFormation Stack to create the S3 Bucket.
    *   Validate unique Device IDs and Stack Names.
    *   Transition to Runtime Keys upon success.
*   **Implements Requirements:** [Setup & Onboarding](../requirements/setup_onboarding.md)

## 4. Tracker Service (The Engine)
*   **Role:** Performs the "Always-on" data collection.
*   **Component:** `ForegroundService`.
*   **Key Mechanisms:**
    *   **Wake Locks:** Uses `PARTIAL_WAKE_LOCK` transiently during batch processing, allowing CPU sleep between batches.
    *   **Location Strategy:**
        *   **Standard:** Native Android `LocationManager` using hardware batching (10s interval, 2m max latency).
        *   **Provider:** Uses `GPS_PROVIDER` (High Accuracy) or `NETWORK_PROVIDER` based on availability, unified across all flavors.
    *   **Stationary Manager:**
        *   **Primary:** Significant Motion Sensor (`TYPE_SIGNIFICANT_MOTION`). This is a specific hardware interrupt that wakes the Application Processor (AP) from suspend only when movement is detected, avoiding the battery cost of continuous polling.
        *   **Fallback:** Periodic Burst Sampling (if hardware sensor is missing). The system uses `AlarmManager` to wake the CPU every few minutes (e.g., 5 minutes), samples the accelerometer at 10Hz for a short burst (e.g., 5 seconds), and resumes active tracking if variance exceeds a threshold.
        *   **Restriction:** While Battery < 3% (Critical), Periodic Burst Sampling is disabled to maximize survival (Deep Sleep).
    *   **Passive Heartbeat:** Uses `AlarmManager` to wake once per hour and write a current timestamp to SharedPreferences, proving to the Watchdog that the service thread is alive and not deadlocked.
    *   **Battery Monitor:** BroadcastReceiver to trigger "Battery Safety Protocol" state changes.
*   **Output:** Writes raw `Location` objects to the local Room Database (Android's standard SQLite abstraction library).
*   **Implements Requirements:** [Data Collection & Tracking](../requirements/data_collection.md)

## 5. Reliability Layer (The Watchdog)
*   **Role:** Ensures the Tracker Service remains active despite aggressive OEM battery optimizations (e.g., Samsung/Huawei killing background processes).
*   **Component:** `WorkManager` (PeriodicWorkRequest, 15-minute interval).
*   **Logic:**
    1.  **Zombie Check:** Verifies `LocationService` is running AND `LastHeartbeatTimestamp` is < 90 minutes old.
    2.  **State Recovery:** If Stopped or Zombie, attempts to restart the service immediately.
    3.  **Circuit Breaker:** If restart fails 3 consecutive times, triggers a "Tracking Failed" Fatal Error notification.
    4.  **Upload Rescue:** If buffer > 4 hours old AND Battery > 15%, triggers a rescue upload.

## 6. Sync & Telemetry Worker (The Uploader)
*   **Role:** Handles reliable data transport (Tracks & Logs).
*   **Component:** `WorkManager` (PeriodicWorkRequest).
*   **Responsibilities:**
    *   **Dual Dispatch:** Uploads Tracks to S3 and Telemetry to both S3 and Community (if Standard/Opt-In).
    *   **Logical Isolation:** Although scheduled by the same `WorkManager` job for battery efficiency, Sync and Telemetry must execute in separate `try/catch` blocks. A crash in the Telemetry uploader **must not** prevent Track data from syncing, and vice versa.
    *   **Streaming Uploads:** Stream data directly from the Room DB through a Gzip compressor.
    *   **Buffer Management:** Enforce a **500MB Soft Limit** (FIFO eviction).
    *   **Transport:** Upload to S3 using the Runtime Keys.
*   **Implements Requirements:** [Data Storage](../requirements/data_storage.md) & [Telemetry](specs/telemetry_spec.md)

## 7. Visualizer (The View)
*   **Role:** Provides the user interface for exploring history.
*   **Components:** `osmdroid` MapView, Local Cache (File System).
*   **Responsibilities:**
    *   **Lazy-Load Indexing:** Maintain a local index of available dates. Verify against S3 using Prefix Search (`tracks/YYYY/MM/`) only when the user requests a specific month.
    *   **Rendering:** Draw tracks on the map using Bitmap Tiles (OSMDroid), applying downsampling for performance.
    *   **Caching:** Store downloaded track files locally to support offline viewing.
*   **Implements Requirements:** [Visualization & History](../requirements/visualization.md)

## 8. Local Data Persistence
*   **Role:** Intermediate buffer and state storage.
*   **Components:**
    *   **Room Database:** Stores pending location points and application logs.
    *   **EncryptedSharedPreferences:** Stores sensitive AWS credentials (Runtime Keys), configuration (Device ID), and the **Telemetry Salt**.

## 9. Battery Impact Analysis
To ensure transparency and manage user expectations, we estimate the battery impact of the "Always On" architecture.

*   **High Impact (GPS/Network):**
    *   *Raw GPS:* Moderate drain (~2-5% per hour with batching).
    *   *Mitigation:* Hardware Batching allows the GPS radio to operate autonomously while the Application Processor (CPU) sleeps. Stationary Mode completely suspends GPS when not moving.
*   **Medium Impact (Wake Locks):**
    *   *CPU Awake:* Minimal impact.
    *   *Mitigation:* By removing the constant `PARTIAL_WAKE_LOCK` requirement and relying on batching, the CPU stays in deep sleep for ~90% of the active tracking time.
*   **Low Impact (Uploads):**
    *   *Radio usage:* Cellular radio power is high but bursty.
    *   *Mitigation:* Batching uploads via `WorkManager` allows the radio to sleep for long intervals.
