# Behavioral Specification: Cloud Synchronization

**Bounded Context:** This specification governs the transport of data from the device to the cloud, including upload schedules, retry logic, traffic guardrails, and data format standards.

**Prerequisite:** Depends on **[Intelligent Tracking](02_intelligent_tracking.md)** (data source) and **[Onboarding & Identity](01_onboarding_identity.md)** (credentials).
**Downstream:** Provides data for **[Historical Visualization](06_historical_visualization.md)**.
**Constraint:** Subject to overrides by **[Adaptive Battery Safety](04_adaptive_battery_safety.md)**.

---

## Upload Scheduling & Triggers
*   **R3.100** **While** the tracking service is active, the system **shall** schedule periodic synchronization jobs via the system scheduler at a regular interval (default 15 minutes).
*   **R3.200** **When** a synchronization job runs, the system **shall** attempt to upload all pending buffered track data to the user's remote storage.
*   **R3.300** **When** the user initiates a "Manual Sync" (Sync Now), the system **shall** immediately attempt an upload, overriding standard battery constraints (unless critical).
*   **R3.400** **If** the device is offline during a scheduled sync, **then** the system **shall** silently defer the operation to the next scheduled interval.

## Traffic Guardrail (Cost Safety)
*   **R3.500** **While** the system is operating, it **shall** track the total cumulative upload size for the current day.
*   **R3.600** **If** the cumulative upload size exceeds 50MB in a single day, **then** the system **shall** enter "Paused (Data Limit)" state and cease all further automatic uploads for that day.
*   **R3.700** **When** the "Manual Sync" is triggered, the system **shall** bypass the 50MB Traffic Guardrail and force the upload.
*   **R3.800** **When** the date changes (local midnight), the system **shall** reset the daily traffic counter and resume standard operation.

## Data Format & Integrity
*   **R3.900** **When** preparing data for upload, the system **shall** serialize the data into a standard, newline-delimited text format.
*   **R3.1000** **When** uploading data, the system **shall** compress the payload.
*   **R3.1100** **When** generating the upload filename, the system **shall** include the unique Device ID and a timestamp to guarantee global uniqueness and prevent data collisions.
*   **R3.1200** **When** uploading Track data, the system **shall** apply a retention policy lock to the object for a duration of 100 years.
*   **R3.1300** **When** uploading Diagnostic Log data, the system **shall not** apply retention policy locks, allowing for standard lifecycle deletion.
*   **R3.1400** **When** a track segment is successfully uploaded, the system **shall** delete the corresponding records from the local track buffer.

## Community Telemetry
*   **R3.1500** **Where** the user has opted into Community Telemetry, the system **shall** upload anonymized crash and performance reports to the community endpoint.
*   **R3.1600** **When** uploading to the community endpoint, the system **shall** hash the Device ID to decouple the data from the user's persistent identity.
*   **R3.1700** **If** the Community upload fails, **then** the system **shall** treat it as a non-fatal error and proceed with the primary storage upload.
