# Project Notes & Analysis

## 1. Multi-Bucket Visualization & Access Levels
*   **Requirement:** Enable the application to view data from multiple Locus buckets within the same AWS account ("Admin" mode), while restricting standard operation to the single bucket used for data recording ("Normal" mode).
*   **Feasibility:** High. AWS IAM supports resource-based policies and user-based policies that can be scoped to specific buckets or wildcard patterns (e.g., `arn:aws:s3:::locus-*`).
*   **Best Practice:** **Principle of Least Privilege**. The "Normal" user (or the runtime credentials on the device) should only have write access to their specific bucket and read access for history. "Admin" access should be a separate, deliberate elevation of privilege, ideally using temporary credentials.
*   **Recommendation:**
    *   Define two distinct IAM Policies: `LocusRecorderPolicy` (Write to specific bucket) and `LocusViewerPolicy` (Read from `locus-*`).
    *   During the Bootstrap/Onboarding phase, allow the user to choose their intent.
    *   If "Admin" is selected, the app requests `s3:ListBuckets` permission to discover all `locus-` buckets.
    *   Ensure the "Admin" feature is gated behind the initial setup (Bootstrap) to prevent accidental runtime scope expansion.

## 2. Cache Eviction Policies
*   **Requirement:** Prevent downloaded track data from consuming excessive device storage.
*   **Feasibility:** High. Android provides APIs for checking available disk space and managing cache directories.
*   **Best Practice:** **LRU (Least Recently Used)** eviction. When the cache hits a size limit (e.g., 500MB) or a time limit (e.g., data older than 90 days), delete the oldest accessed files first.
*   **Recommendation:**
    *   Implement a cache manager that runs periodically (via `WorkManager`).
    *   Maintain a local index (Room DB) of downloaded files with a `last_accessed` timestamp.
    *   Set a hard configurable limit (default 500MB).
    *   Ensure "Stationary" or "Buffer" data (not yet uploaded) is never evicted.

## 3. Manual IAM Credential Creation (No CloudFormation)
*   **Requirement:** Update documentation to support a setup flow that does not rely on CloudFormation, possibly for users who prefer manual control or have restricted permissions.
*   **Feasibility:** High. All AWS resources can be created manually or via CLI.
*   **Best Practice:** **Transparency**. While IaC (Infrastructure as Code) like CloudFormation is preferred for reliability, providing a manual "break-glass" procedure increases trust and accessibility.
*   **Recommendation:**
    *   Create a "Manual Setup Guide" in `docs/manual_setup.md`.
    *   List exact JSON policy documents required.
    *   Provide AWS CLI commands as an alternative to Console screenshots (CLI is less likely to become outdated visually).

## 4. User-Configurable Recording Frequency
*   **Requirement:** Allow users to adjust how often the app records location points.
*   **Feasibility:** High. The Android `LocationRequest` API allows setting intervals.
*   **Best Practice:** **User Choice with Sensible Defaults**. Provide presets (e.g., "High Accuracy" (1s), "Balanced" (10s), "Power Saver" (60s)) rather than raw millisecond inputs.
*   **Recommendation:**
    *   Add a Settings screen for "Tracking Preferences".
    *   Update the `ForegroundService` to restart the location listener when preferences change.
    *   Warn the user about battery impact for high-frequency settings.

## 5. Independence from Google Play Services
*   **Requirement:** Ensure functionality on de-Googled devices (e.g., GrapheneOS, LineageOS).
*   **Feasibility:** High.
*   **Best Practice:** **Standard Standards**. Use `android.location.LocationManager` (Platform API) instead of `com.google.android.gms.location.FusedLocationProviderClient`. Use `osmdroid` for maps instead of Google Maps SDK.
*   **Recommendation:**
    *   Avoid any dependency on `com.google.android.gms`.
    *   Test on an Android Virtual Device (AVD) image that does not have Google APIs installed ("AOSP" image).
    *   Use OpenStreetMap (OSM) for all visualization.

## 6. README & Documentation Updates
*   **Requirement:** Keep the README current with project status.
*   **Feasibility:** High.
*   **Best Practice:** **Living Documentation**. The README should be the "Entry Point" and link to specific docs.
*   **Recommendation:**
    *   Update `README.md` to link to this `NOTES.md`.
    *   Ensure the "Getting Started" section reflects the current "Implementation Definition" phase.

## 7. Lambda Compression for Daily Objects
*   **Requirement:** Compress multiple small JSON objects for a single day into a single Gzipped file to save storage and reduce GET request costs/latency.
*   **Feasibility:** High. Common S3 pattern.
*   **Best Practice:** **Event-Driven Compute**. Trigger a Lambda function on a schedule (e.g., daily at 02:00 UTC) or on object creation (complex for batching). Scheduled is better for daily aggregation.
*   **Recommendation:**
    *   Deploy a Python/Go Lambda function.
    *   Script: List objects for `tracks/YYYY/MM/DD-1/`. Download, concatenate NDJSON, GZIP, Upload `tracks/YYYY/MM/DD-1/archive.json.gz`. Delete originals (optional/careful).
    *   **Caution:** Deleting originals is risky. Maybe move them to a `glacier` class or just keep the archive as an "optimized view".

## 8. App Ecosystem Evaluation
*   **Requirement:** Analyze existing location tracking applications to identify feature gaps, privacy models, and storage architectures, ensuring Locus provides a distinct and superior value proposition (User-Owned Data).
*   **Feasibility:** High. The market is well-defined.
*   **Best Practice:** **Competitor Analysis**. Understand the landscape to avoid redundancy and clarify the unique selling point (USP).
*   **Recommendation:**
    *   **OwnTracks:** Open-source, self-hosted. Relies on MQTT/HTTP and a running server (e.g., Mosquitto + Recorder). *Locus Differentiator:* Serverless architecture (Direct-to-S3) removing maintenance overhead.
    *   **Traccar:** Enterprise-grade fleet management. Powerful but complex to set up and manage for a single user. *Locus Differentiator:* Simplicity and focus on personal archival.
    *   **Google Maps Timeline:** Convenient but privacy-invasive. Data is mined. *Locus Differentiator:* Absolute data sovereignty and encryption.
    *   Create a `docs/ecosystem_comparison.md` to map feature parity and unique advantages.
