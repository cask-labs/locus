# Locus (Project Title TBD)

A **Sovereign, High-Precision Location Tracker** for Android.

## 🚀 Core Features
*   **1Hz Tracking:** Captures your location every single second.
*   **User-Owned Data:** Data is stored directly in **your** AWS S3 bucket. No third-party servers.
*   **Offline-First:** Buffers data locally and syncs when possible.
*   **Battery Efficient:** Uses batch uploading and safety cutoffs to preserve battery.
*   **Privacy:** No analytics (except optional crash reporting), no tracking, no ads.

## 🛠 Architecture
*   **Client:** Native Android (Kotlin) + Room Database + Foreground Service.
*   **Cloud:** AWS S3 (User Provided).
*   **Map:** OpenStreetMap.

## 📦 Getting Started (The "Bootstrap" Flow)
1.  **Create AWS Access:**
    *   Go to your AWS Console -> IAM.
    *   Create a user (e.g., `LocusApp`).
    *   Attach the policy found in `docs/iam-bootstrap-policy.json`.
    *   Generate Access Keys.
2.  **Install App:**
    *   Build the APK from source.
    *   Install on your Android device.
3.  **Setup:**
    *   Open App -> Settings.
    *   Paste your AWS Access Keys.
    *   The app will automatically create the S3 bucket and start tracking.

## 📄 Documentation
*   [Architecture Plan](PLANNING.md)
*   [IAM Policy](docs/technical_discovery/iam-bootstrap-policy.json)

## ⚖️ License
GPLv3
