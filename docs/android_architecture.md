# Android Client Architecture

The Android application acts as both the data collector and the infrastructure controller. It implements the behaviors defined in the Functional Requirements.

## 1. Build Strategy (Privacy & Variants)
To strictly satisfy both the "Privacy-First" (FOSS) and "Ease of Development" (Beta) requirements, the application utilizes **Product Flavors**:

*   **Standard (`standard`):**
    *   **External Dependencies:** Includes Firebase Crashlytics and Google Play Services.
    *   **Goal:** Used for the Play Store and internal Beta testing to gather community crash statistics (Opt-In).
*   **FOSS (`foss`):**
    *   **External Dependencies:** **Zero.** All proprietary libraries are stripped at compile time.
    *   **Goal:** Used for F-Droid and privacy-focused manual installation.
    *   **Mechanism:** Uses "No-Op" stubs for the Community Telemetry interface.

## 2. Provisioner (Setup)
*   **Role:** Handles the one-time setup and infrastructure creation.
*   **Components:** UI Wizards, AWS SDK (CloudFormation client).
*   **Responsibilities:**
    *   Accept user credentials (Bootstrap Keys).
    *   Deploy the CloudFormation Stack to create the S3 Bucket.
    *   Validate unique Device IDs and Stack Names.
    *   Transition to Runtime Keys upon success.
*   **Implements Requirements:** [Setup & Onboarding](requirements/setup_onboarding.md)

## 3. Tracker Service (The Engine)
*   **Role:** Performs the "Always-on" data collection.
*   **Component:** `ForegroundService`.
*   **Key Mechanisms:**
    *   **Wake Locks:** Uses `PARTIAL_WAKE_LOCK` to ensure CPU uptime.
    *   **Location Strategy:**
        *   **Primary:** Fused Location Provider (Google Play Services) for battery efficiency, rapid fix, and indoor accuracy.
        *   **Fallback:** Raw Android Location Manager (GPS/Network) if Play Services are unavailable or disabled by the user.
    *   **Stationary Manager:**
        *   **Primary:** Significant Motion Sensor (`TYPE_SIGNIFICANT_MOTION`). This is a specific hardware interrupt that wakes the Application Processor (AP) from suspend only when movement is detected, avoiding the battery cost of continuous polling.
        *   **Fallback:** Periodic Burst Sampling (if hardware sensor is missing). The system uses `AlarmManager` to wake the CPU every few minutes (e.g., 5 minutes), samples the accelerometer at 10Hz for a short burst (e.g., 5 seconds), and resumes active tracking if variance exceeds a threshold.
    *   **Battery Monitor:** BroadcastReceiver to trigger "Battery Safety Protocol" state changes.
*   **Output:** Writes raw `Location` objects to the local Room Database (Android's standard SQLite abstraction library).
*   **Implements Requirements:** [Data Collection & Tracking](requirements/data_collection.md)

## 4. Reliability Layer (The Watchdog)
*   **Role:** Ensures the Tracker Service remains active despite aggressive OEM battery optimizations (e.g., Samsung/Huawei killing background processes).
*   **Component:** `WorkManager` (PeriodicWorkRequest, 15-minute interval).
*   **Logic:**
    1.  Check if `TrackerService` is running.
    2.  If **Running**: Do nothing.
    3.  If **Stopped**: Attempt to restart the service immediately.
    4.  If **Restart Fails**: Trigger a "Tracking Stopped" notification to alert the user.

## 5. Sync & Telemetry Worker (The Uploader)
*   **Role:** Handles reliable data transport (Tracks & Logs).
*   **Component:** `WorkManager` (PeriodicWorkRequest).
*   **Responsibilities:**
    *   **Dual Dispatch:** Uploads Tracks to S3 and Telemetry to both S3 and Community (if Standard/Opt-In).
    *   **Streaming Uploads:** Stream data directly from the Room DB through a Gzip compressor.
    *   **Buffer Management:** Enforce a **500MB Soft Limit** (FIFO eviction).
    *   **Transport:** Upload to S3 using the Runtime Keys.
*   **Implements Requirements:** [Data Storage](requirements/data_storage.md) & [Telemetry](operations/telemetry.md)

## 6. Visualizer (The View)
*   **Role:** Provides the user interface for exploring history.
*   **Components:** `osmdroid` MapView, Local Cache (File System).
*   **Responsibilities:**
    *   **Lazy-Load Indexing:** Maintain a local index of available dates. Verify against S3 using Prefix Search (`tracks/YYYY/MM/`) only when the user requests a specific month.
    *   **Rendering:** Draw tracks on the map using Bitmap Tiles (OSMDroid), applying downsampling for performance.
    *   **Caching:** Store downloaded track files locally to support offline viewing.
*   **Implements Requirements:** [Visualization & History](requirements/visualization.md)

## 7. Local Data Persistence
*   **Role:** Intermediate buffer and state storage.
*   **Components:**
    *   **Room Database:** Stores pending location points and application logs.
    *   **EncryptedSharedPreferences:** Stores sensitive AWS credentials (Runtime Keys), configuration (Device ID), and the **Telemetry Salt**.

## 8. Battery Impact Analysis
To ensure transparency and manage user expectations, we estimate the battery impact of the "Always On" architecture.

*   **High Impact (GPS/Network):**
    *   *Raw GPS:* Heavy drain (~5-10% per hour active).
    *   *Mitigation:* Fused Location Provider (FLP) drastically reduces this by using low-power WiFi scanning. Stationary Mode completely suspends GPS when not moving.
*   **Medium Impact (Wake Locks):**
    *   *CPU Awake:* `PARTIAL_WAKE_LOCK` keeps the CPU active.
    *   *Mitigation:* Significant Motion Sensor allows the CPU to enter Deep Sleep during stationary periods (e.g., sitting at a desk, sleeping).
*   **Low Impact (Uploads):**
    *   *Radio usage:* Cellular radio power is high but bursty.
    *   *Mitigation:* Batching uploads via `WorkManager` allows the radio to sleep for long intervals.
