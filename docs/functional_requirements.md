# Functional Requirements

This document defines the implementation-agnostic functional requirements for the Locus project. It serves as the source of truth for *what* the system must do, independent of *how* it is implemented.

## 1. Data Collection & Tracking

### 1.1. Location Recording
*   **Precision:** The system must be capable of recording geospatial location data (Latitude, Longitude, Altitude) at a frequency of 1Hz (once per second).
*   **Independence:** The system must acquire location data without reliance on proprietary third-party location APIs (e.g., Google Play Services FusedLocationProvider) to ensure autonomy.
*   **Persistence:** The system must ensure continuous data collection even when the application is backgrounded or the device is in a sleep state, utilizing necessary system mechanisms (e.g., Wake Locks) to prevent OS termination.

### 1.2. Sensor Fusion & Optimization
*   **Dynamic Sampling:** The system must record auxiliary environmental sensors (accelerometer, magnetometer, barometer) only when the device speed exceeds a defined threshold (e.g., 4.5 m/s) to optimize storage and power.
*   **Stationary Mode (Sleep Mode):** To conserve battery, the system must automatically suspend GPS acquisition if no movement is detected for a defined period (e.g., 5 minutes).
*   **Wake-on-Motion:** The system must automatically resume GPS acquisition immediately upon detecting movement via the accelerometer while in Stationary Mode.

### 1.3. Battery Safety Protocol
The system must adapt its behavior based on the device's remaining battery capacity to prevent critical depletion:
*   **Low Battery (< 10%):** Reduce recording frequency (e.g., to 10s interval) and pause automatic data uploads.
*   **Critical Battery (< 3%):** Further reduce recording frequency (e.g., to 60s interval).
*   **Recovery (> 15%):** Resume normal recording and upload schedules automatically.

## 2. Data Storage & Management

### 2.1. Local Buffering
*   **Reliability:** The system must store captured data in a persistent local buffer immediately upon capture to prevent data loss in case of application termination or power failure.
*   **Retention:** Data must remain in the local buffer until it is successfully confirmed as stored in the remote destination.

### 2.2. Remote Synchronization
*   **User Ownership:** The system must transmit data exclusively to a storage repository owned and controlled by the user (e.g., a personal AWS S3 Bucket).
*   **Periodic Sync:** The system must attempt to batch and upload buffered data at regular intervals (e.g., every 15 minutes).
*   **Manual Sync:** The system must provide a mechanism for the user to force an immediate upload. This action must override non-critical battery safety checks.
*   **Immutability:** The system must configure the remote storage to prevent the modification or deletion of uploaded history files (e.g., using Object Lock/Compliance Mode).

### 2.3. Data Format
*   **Interoperability:** Data must be stored in a standard, open, text-based format (NDJSON) to ensure future readability.
*   **Efficiency:** Data bundles must be compressed (e.g., Gzip) before transmission to minimize network usage and storage costs.

## 3. Visualization & History

### 3.1. Map Interface
*   **Offline Capability:** The visualization engine must render map data using an open source (e.g., OpenStreetMap) that supports offline caching, removing dependencies on online-only API keys.
*   **Signal Quality:** The interface must differentiate and visualize the quality of the location signal (e.g., via a heat map or color coding) and data source (GPS vs. WiFi/Cellular if applicable).

### 3.2. History Retrieval
*   **Remote Verification:** The history view must source data exclusively from the remote storage (or a local cache of verified remote data) to confirm data sovereignty and successful upload. Local buffer data should not be mixed into the "History" view until it is uploaded.
*   **Lazy Loading:** The system must index the existence of historical data (e.g., which days have tracks) without downloading the full track data, to save bandwidth.
*   **Write-Through Indexing:** Upon successfully uploading a new batch, the system must immediately update its local history index to reflect the new data without re-querying the remote server.

## 4. Setup & Onboarding

### 4.1. Infrastructure Provisioning
*   **Automated Setup:** The system must provide a mechanism to automatically provision the necessary remote infrastructure (e.g., storage buckets, permissions) using user-supplied credentials.
*   **Idempotency:** The provisioning process must handle re-runs gracefully, checking for existing resources to avoid duplication or errors.

### 4.2. Identity Management
*   **Unique Device ID:** The system must generate a new, unique identifier for every installation to prevent data collisions ("Split Brain") when multiple devices (or re-installs) write to the same storage.
*   **Credential Handling:** The system must support the use of temporary, high-privilege credentials for the initial setup (Bootstrap) and switch to restricted, low-privilege credentials for ongoing operation (Runtime).

## 5. User Interface & Feedback

### 5.1. Status Indication
*   **Transparency:** The system must provide a persistent, visible indicator (e.g., notification) of its current status, including recording state, satellite lock, and unsynced buffer size.
*   **Subtle Errors:** Transient errors (e.g., temporary network loss) must be communicated unobtrusively to avoid user alarm fatigue.

### 5.2. Permissions
*   **Guidance:** The system must guide the user through the necessary permission grants (Location, Background Execution), providing clear rationale and direct links to system settings where required.
