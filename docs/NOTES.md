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

## 2. IAM Strategy Verification
*   **Requirement:** Verify that the project's IAM permissions adhere to AWS best practices and strictly defined security requirements.
*   **Feasibility:** Medium. Requires deep knowledge of AWS IAM nuances.
*   **Best Practice:** **Policy Simulation & Analysis**. Use tools like AWS Access Analyzer or `pmapper` to visualize access paths.
*   **Recommendation:**
    *   Audit current policy templates against the "Least Privilege" rule.
    *   Specifically check for `*` actions on `*` resources (e.g., `s3:*` on `*`).
    *   Document the justification for every permission granted in `docs/infrastructure.md`.

## 3. User-Configurable Recording Frequency
*   **Requirement:** Allow users to adjust how often the app records location points.
*   **Feasibility:** High. The Android `LocationRequest` API allows setting intervals.
*   **Best Practice:** **User Choice with Sensible Defaults**. Provide presets (e.g., "High Accuracy" (1s), "Balanced" (10s), "Power Saver" (60s)) rather than raw millisecond inputs.
*   **Recommendation:**
    *   Add a Settings screen for "Tracking Preferences".
    *   Update the `ForegroundService` to restart the location listener when preferences change.
    *   Warn the user about battery impact for high-frequency settings.

## 4. Independence from Google Play Services
*   **Requirement:** Ensure functionality on de-Googled devices (e.g., GrapheneOS, LineageOS).
*   **Feasibility:** High.
*   **Best Practice:** **Standard Standards**. Use `android.location.LocationManager` (Platform API) instead of `com.google.android.gms.location.FusedLocationProviderClient`. Use `osmdroid` for maps instead of Google Maps SDK.
*   **Recommendation:**
    *   Avoid any dependency on `com.google.android.gms`.
    *   Test on an Android Virtual Device (AVD) image that does not have Google APIs installed ("AOSP" image).
    *   Use OpenStreetMap (OSM) for all visualization.

## 5. Silent Failure Detection
*   **Requirement:** Detect and alert if tracking stops unexpectedly (e.g., due to OS kill or crash) without user knowledge.
*   **Feasibility:** Medium. The OS can be aggressive with background services.
*   **Best Practice:** **Watchdog / Heartbeat**. A separate component should periodically check if the primary service is running/updating.
*   **Recommendation:**
    *   Use `WorkManager` for a periodic "Health Check" task (e.g., every 15 minutes).
    *   Check the timestamp of the last recorded location in the database.
    *   If the gap exceeds a threshold (e.g., 20 minutes) and the user didn't manually stop tracking, trigger a high-priority notification: "Tracking appears to have stopped."

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

## 9. Privacy Zones & Location Obfuscation
*   **Requirement:** Obscure sensitive locations (e.g., user's home or office) in the visualization to protect privacy when viewing or sharing data, similar to Strava's "Privacy Zone" feature (e.g., masking the last 500m).
*   **Feasibility:** High. Can be implemented in the client-side visualization logic.
*   **Best Practice:** **Non-Destructive Presentation**. Store the raw, high-precision data in S3 (as the user owns it and it is their archival record). Apply the privacy mask *only* at the rendering layer or during specific "Share/Export" operations.
*   **Recommendation:**
    *   Add a "Privacy Zones" section in Settings allowing users to define coordinates and a radius (e.g., "Home", 500m).
    *   In the *Visualizer*, filter or clip track segments that intersect these zones.
    *   Ensure the raw data on S3 remains untouched to preserve the integrity of the personal archive.

## 10. Guided Credential Setup Documentation
*   **Requirement:** Create comprehensive documentation providing step-by-step instructions for users to set up their AWS credentials, particularly for those who may not be familiar with AWS IAM.
*   **Feasibility:** High. Purely a documentation effort.
*   **Best Practice:** **Visual Guides**. Users often struggle with the AWS Console interface. Screenshots or very clear "Click here, type this" instructions are essential. Alternatively, provide a script that uses the AWS CLI to automate the process.
*   **Recommendation:**
    *   Create `docs/credential_setup_guide.md`.
    *   Detail the "Two-Key" architecture: Bootstrap Keys (High privilege, used once) vs Runtime Keys (Restricted, generated by app).
    *   Provide clear steps for creating the initial IAM User with necessary permissions.
    *   Include troubleshooting tips for common errors (e.g., 403 Forbidden).

## 11. Reverse Geocoding & Place Labeling
*   **Requirement:** Convert raw GPS coordinates into human-readable addresses or place names (e.g., "Home", "123 Main St") to improve the utility of the history visualization.
*   **Feasibility:** High.
*   **Best Practice:** **Privacy-First Geocoding**. Avoid sending the user's location history to third-party APIs (like Google Geocoding API) which tracks usage.
*   **Recommendation:**
    *   **Option A (Online):** Use OpenStreetMap's Nominatim service, but respecting their usage policy (User-Agent header, cache heavily, limited rate).
    *   **Option B (Offline):** Use an offline geocoder library (e.g., a localized extract of OSM data) if APK size permits.
    *   **Option C (User-Defined):** Allow users to manually label specific coordinates (similar to Privacy Zones) and only display those labels.
    *   **Selected Approach:** Prioritize Option C (User Labels) for sensitive places, and explore Option A (Nominatim) for general addresses with a strict opt-in consent and aggressive local caching.
