# Behavioral Specification: Cloud Synchronization

**Bounded Context:** This specification governs the transport of data from the device to the cloud, including upload schedules, retry logic, traffic guardrails, and data format standards.

**Prerequisite:** Depends on **[Intelligent Tracking](02_intelligent_tracking.md)** (data source) and **[Onboarding & Identity](01_onboarding_identity.md)** (credentials).
**Downstream:** Provides data for **[Historical Visualization](06_historical_visualization.md)**.
**Constraint:** Subject to overrides by **[Adaptive Battery Safety](04_adaptive_battery_safety.md)**.

---

## 1. Upload Scheduling & Triggers
*   **While** the tracking service is active, the system **shall** schedule periodic synchronization jobs via the system scheduler at a regular interval (default 15 minutes).
*   **When** a synchronization job runs, the system **shall** attempt to upload all pending buffered track data to the user's remote storage.
*   **When** the user initiates a "Manual Sync" (Sync Now), the system **shall** immediately attempt an upload, overriding standard battery constraints (unless critical).
*   **If** the device is offline during a scheduled sync, **then** the system **shall** silently defer the operation to the next scheduled interval.

## 2. Traffic Guardrail (Cost Safety)
*   **While** the system is operating, it **shall** track the total cumulative upload size for the current day.
*   **If** the cumulative upload size exceeds 50MB in a single day, **then** the system **shall** enter "Paused (Data Limit)" state and cease all further automatic uploads for that day.
*   **When** the "Manual Sync" is triggered, the system **shall** bypass the 50MB Traffic Guardrail and force the upload.
*   **When** the date changes (local midnight), the system **shall** reset the daily traffic counter and resume standard operation.

## 3. Data Format & Integrity
*   **When** preparing data for upload, the system **shall** serialize the data into a standard, newline-delimited text format.
*   **When** uploading data, the system **shall** compress the payload.
*   **When** generating the upload filename, the system **shall** include the unique Device ID and a timestamp to guarantee global uniqueness and prevent data collisions.
*   **When** uploading Track data, the system **shall** apply a retention policy lock to the object for a duration of 100 years.
*   **When** uploading Diagnostic Log data, the system **shall not** apply retention policy locks, allowing for standard lifecycle deletion.
*   **When** a track segment is successfully uploaded, the system **shall** delete the corresponding records from the local track buffer.

## 4. Community Telemetry
*   **Where** the user has opted into Community Telemetry, the system **shall** upload anonymized crash and performance reports to the community endpoint.
*   **When** uploading to the community endpoint, the system **shall** hash the Device ID to decouple the data from the user's persistent identity.
*   **If** the Community upload fails, **then** the system **shall** treat it as a non-fatal error and proceed with the primary storage upload.
