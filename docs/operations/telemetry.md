# Runtime Telemetry & Resilience

This document defines the architecture for runtime observability (logging, crash reporting, health metrics) while strictly adhering to the "User-Owned" and "Privacy-First" principles.

## 1. Core Principles

*   **Fail-Open Design:** The application **shall** continue normal operation (tracking, storing data) even if the telemetry subsystems fail completely (e.g., network unreachable, crash in logger).
*   **Data Sovereignty:** The user's S3 bucket is the **Primary Destination** for all diagnostic data.
*   **Privacy-First Compilation:** The application uses Build Variants to physically exclude proprietary tracking code from FOSS builds.
*   **Voluntary Contribution:** Reporting to a centralized "Community" service is strictly **Opt-In** and **Secondary**.

## 2. Architecture

The telemetry system utilizes a "Dual-Dispatch" model with strict isolation and build-time configuration.

```mermaid
graph TD
    App[Locus Application] -->|Generates| Log[Log/Crash Event]

    subgraph Telemetry Module
        Log -->|1. Dispatch| Buffer[Local Circular Buffer]
        Buffer -->|2. Batch| Dispatcher[Telemetry Dispatcher]
    end

    Dispatcher --x|Try/Catch| S3Worker[S3 Diagnostics Worker]
    Dispatcher --x|Try/Catch| ExtWorker[Community Service Worker]

    S3Worker -->|Upload| UserBucket[(User S3 Bucket)]
    ExtWorker -->|Upload| ExtService["Community Service (Firebase)"]

    style UserBucket fill:#ccf,stroke:#333
    style ExtService fill:#fcc,stroke:#333,stroke-dasharray: 5 5
```
*Note: The above diagram can be rendered using any Markdown editor with Mermaid support.*

### 2.1. Build Variants (Flavors)

To respect user privacy and FOSS principles, the application **shall** be compiled in two flavors:

1.  **Standard (`standard`):**
    *   **Includes:** Firebase Crashlytics (and Google Play Services dependencies).
    *   **Purpose:** Development, Beta Testing, Google Play Store distribution.
    *   **Behavior:** Community telemetry is possible (if opted-in).
2.  **FOSS (`foss`):**
    *   **Excludes:** All proprietary libraries (Firebase, GMS).
    *   **Purpose:** F-Droid, privacy-conscious manual installs.
    *   **Behavior:** The `Community Service Worker` is a "No-Op" stub. Community telemetry settings are hidden or disabled.

## 3. Destinations

### 3.1. Primary: User S3 (Diagnostics)
*   **Purpose:** Full fidelity debugging, crash dumps, and performance metrics.
*   **Path:** `s3://<user-bucket>/diagnostics/YYYY/MM/DD/<device_id>_<timestamp>.ndjson.gz`
*   **Retention:** Controlled by S3 Lifecycle Policy (Strict **30 Days**).
*   **Format:** NDJSON (Newline Delimited JSON) for machine parseability.

#### Log Schema (NDJSON)
Each line in the log file **must** adhere to the following schema:
```json
{
  "ts": "2023-10-27T10:00:00.123Z",  // ISO 8601 Timestamp
  "lvl": "WARN",                     // INFO, WARN, ERROR, FATAL
  "tag": "TrackerService",           // Component Name
  "msg": "GPS signal lost for 30s",  // Human-readable message
  "ctx": {                           // Environmental Context
    "bat": 85,                       // Battery Level %
    "chg": false,                    // Is Charging?
    "net": "wifi",                   // Network Type (wifi, cell, none)
    "perm": true,                    // Is Location Permission Granted?
    "svc": true                      // Is Tracker Service Running?
  },
  "stack": "..."                     // Optional: Stack Trace for Errors
}
```

### 3.2. Secondary: Community Service (Opt-In)
*   **Service:** Firebase Crashlytics (initially), behind an interface to allow future migration (e.g., to Sentry).
*   **Mechanism:** Native SDK integration (Standard Flavor only).
*   **Privacy Strategy:** "Salted Anonymity"
    *   **The Risk:** Linking a crash report (containing app state) to a public track file (containing `device_id`).
    *   **The Solution:** The app generates a random UUID (`telemetry_salt`) at install time, stored in `EncryptedSharedPreferences`.
    *   **The ID:** The ID sent to Firebase is `SHA256(device_id + telemetry_salt)`.
    *   **Result:** Developers can correlate crashes from the same device *without* knowing which User S3 bucket or real-world identity it belongs to.

## 4. Resilience & Fail-Open Requirements

To ensure the "Fail-Open" mandate:

1.  **Isolation:** Telemetry operations **must** run in isolated Coroutine scopes (e.g., `Dispatchers.IO`) wrapped in top-level `try-catch` blocks.
    *   **Requirement:** A crash in the logging subsystem **must never** propagate to the calling thread (e.g., the tracking loop).
    *   **Buffer Safety:** The *Local Circular Buffer* shall use a low-level, robust implementation (e.g., `mmap` or file-append) to maximize the chance of persisting logs even during a process crash (Tombstone support).
2.  **Exception Swallowing:**
    *   **IF** a telemetry upload fails, **THEN** the system **shall** catch the exception, log it to the *Local Circular Buffer*, and discard the **current payload** (the batch of logs attempting upload).
    *   **The system shall not** retry telemetry uploads more than once per batch.
3.  **Circuit Breaking:**
    *   **IF** telemetry uploads fail consecutively for > 5 attempts, **THEN** the Telemetry Module **shall** enter a "Backoff" state for 6 hours.

## 5. Implementation Definition

### 5.1. Local Debug Dashboard
Since S3 logs are remote and compressed, the app **shall** provide a local "Debug View" for immediate troubleshooting:
*   **Source:** Reads from the *Local Circular Buffer* (in-memory or short-term file).
*   **Features:**
    *   Scrollable list of recent logs (last ~500 lines).
    *   Color-coded by level (WARN/ERROR).
    *   "Copy to Clipboard" / "Share" button for manual reporting.

### 5.2. Configuration
*   `enable_community_telemetry`: Boolean (Default: `false`). Only visible/effective in `Standard` build.
*   `enable_s3_diagnostics`: Boolean (Default: `true`).
*   `telemetry_salt`: String (UUID). Generated once on first run.
