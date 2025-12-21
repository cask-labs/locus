# Locus

**A High-Precision Location Tracker for Android.**

## 🎯 Core Philosophy
*   **Data Ownership:** Your location history belongs to you. Data is stored directly in your own AWS S3 bucket. No third-party servers.
*   **High Precision:** Designed for 1Hz tracking to capture every turn, not just "significant changes."
*   **Battery Efficiency:** Smart batching and deep-sleep handling to ensure all-day battery life even with high-fidelity tracking.
*   **Offline-First:** Robust local buffering ensures no data is lost when connectivity drops.

## 📚 Documentation
The project is heavily documented to ensure robustness and maintainability.
*   **[PLANNING.md](PLANNING.md):** The central index for all documentation.
*   **[Technical Discovery](docs/technical_discovery/):** Deep dives into Architecture, Infrastructure, and Security.
*   **[Behavioral Specs](docs/behavioral_specs/):** Detailed functional specifications for each feature.

## 🛠 Architecture
*   **Client:** Native Android (Kotlin)
    *   **Architecture:** Clean Architecture (Domain/Data/UI separation)
    *   **Pattern:** MVVM / MVI
    *   **Tech Stack:** Compose, Room, WorkManager, Hilt, Coroutines
*   **Cloud:** AWS S3 (User Provided) + CloudFormation (for setup)
*   **Map:** OpenStreetMap (via osmdroid)

## ⚖️ License
GPLv3
