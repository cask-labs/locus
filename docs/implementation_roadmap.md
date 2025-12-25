# Implementation Roadmap

This document defines the step-by-step implementation plan for the Locus project. It is designed to prioritize the establishment of a robust validation infrastructure ("Maintenance") and a functional application skeleton before proceeding with feature development in the order defined by the Behavioral Specifications.

## Phase 0: Foundation & Validation Infrastructure

**Goal:** Establish the long-term maintenance infrastructure and a basic functional skeleton.
**Exit Criteria:** The application is installable ("Hello World"), the multi-module structure is in place, and all validation scripts are operational.

### 1. Project Scaffolding
*   **Initialization:** Initialize the Android Gradle project with Kotlin DSL.
*   **Modules:** Create the strict multi-module hierarchy:
    *   `:app` (UI & Framework Glue)
    *   `:core:domain` (Pure Kotlin Business Logic)
    *   `:core:data` (Repositories & Data Sources)
    *   `:core:testing` (Shared Test Doubles)
*   **Build Variants:** Configure `standard` (Google Play) and `foss` (F-Droid) flavors.
*   **Versioning:** Implement automated versioning based on Git Tags.

### 2. Validation Infrastructure (Maintenance)
*   **Automation Scripts:** Implement the core scripts defined in `docs/technical_discovery/specs/automation_scripts_spec.md`:
    *   `scripts/setup_ci_env.sh`: Installs pinned dependencies.
    *   `scripts/run_local_validation.sh`: Runs the full local test suite.
    *   `scripts/verify_security.sh`: Runs security checks (SAST, Secrets).
*   **Tooling:** Configure `ktlint`, `detekt`, `shellcheck`, and `pre-commit` hooks.
*   **CI Pipeline:** Configure the GitHub Actions workflow (`.github/workflows/validation.yml`) to execute Tiers 1-3.

### 3. Functional Skeleton (Shared Architecture)
*   **Dependency Injection:** Set up Hilt/Dagger graph (Application Component).
*   **Database:** Create the empty `AppDatabase` (Room) and provide it via DI.
*   **Network:** Create the base `NetworkModule` (AWS SDK Client configuration, User Agent) and provide it via DI.
*   **Domain:** Define base types (`LocusResult<T>`, `AppError`).
*   **UI:** Implement a placeholder "Hello World" Activity/Screen to verify the app launches.

---

## Phase 1: Onboarding & Identity

**Reference:** `docs/behavioral_specs/01_onboarding_identity.md`
**Goal:** The user can generate keys, provision resources, and successfully authenticate.

*   **Domain:** Implement `AuthRepository` and `ProvisioningUseCase`.
*   **Data:** Implement `EncryptedSharedPreferences` for secure key storage.
*   **Infrastructure:** Implement `CloudFormationClient` for stack deployment.
*   **UI:** Implement the Onboarding Flow (Welcome, Credential Entry, Provisioning Progress, Success).
*   **Logic:** Implement the "Setup Trap" to enforce onboarding completion.

---

## Phase 2: Intelligent Tracking

**Reference:** `docs/behavioral_specs/02_intelligent_tracking.md`
**Goal:** The application can collect location data locally.

*   **Domain:** Implement `LocationRepository` and `TrackingManager`.
*   **Data:** Implement `LocationEntity`, `LocationDao`, and `RoomLocationDataSource`.
*   **Service:** Implement the `TrackerService` (Foreground Service) and Notification Channel.
*   **Logic:** Implement the State Machine (Idle -> Tracking).

---

## Phase 3: Cloud Synchronization

**Reference:** `docs/behavioral_specs/03_cloud_synchronization.md`
**Goal:** The application can upload buffered data to S3.

*   **Domain:** Implement `SyncRepository` and `PerformSyncUseCase`.
*   **Data:** Implement `S3StorageDataSource` (PutObject, Gzip compression).
*   **Worker:** Implement `SyncWorker` (WorkManager) with constraints (Network, Battery).
*   **Logic:** Implement the "File-Based" naming convention and duplicate prevention.

---

## Phase 4: Adaptive Battery Safety

**Reference:** `docs/behavioral_specs/04_adaptive_battery_safety.md`
**Goal:** The application respects battery constraints and pauses operations when necessary.

*   **Domain:** Implement `BatteryRepository` and `PowerManagementUseCase`.
*   **Logic:** Implement the `TrafficGuardrail` (50MB limit) and Low Battery logic (<15% Pause, <3% Deep Sleep).
*   **Integration:** Inject battery checks into `TrackerService` and `SyncWorker`.

---

## Phase 5: System Status & Feedback

**Reference:** `docs/behavioral_specs/05_system_status_feedback.md`
**Goal:** The user has visibility into the system state via the Dashboard and Notifications.

*   **UI:** Implement the `DashboardScreen` (Status Card, Stats Grid).
*   **UI:** Implement the `SettingsScreen` (Data management, Version info).
*   **Logic:** Connect the `StatusRepository` to the UI to show real-time state (e.g., "Tracking", "Paused").
*   **Notifications:** Implement the dynamic Persistent Notification updates.

---

## Phase 6: Historical Visualization

**Reference:** `docs/behavioral_specs/06_historical_visualization.md`
**Goal:** The user can view their historical data on a map.

*   **Domain:** Implement `HistoryRepository` and `GetTrackHistoryUseCase`.
*   **Data:** Implement the "File-Based Cache" (downloading and caching S3 Gzip files).
*   **UI:** Implement the `MapScreen` using `osmdroid` (Tiles, Polylines).
*   **UI:** Implement the Calendar Picker and Day Summary Bottom Sheet.

---

## Phase 7: Service Reliability

**Reference:** `docs/behavioral_specs/07_service_reliability.md`
**Goal:** The system automatically recovers from failures.

*   **Domain:** Implement `ServiceHealthRepository`.
*   **Worker:** Implement the `WatchdogWorker` (periodic health check).
*   **Logic:** Implement the "Circuit Breaker" logic (restart service if dead, stop if crashing repeatedly).
*   **Feedback:** Implement the "Service Instability" dashboard card.

---

## Phase 8: Offboarding & Resource Cleanup

**Reference:** `docs/behavioral_specs/08_offboarding.md`
**Goal:** The user can safely decommission their cloud resources and reset the application.

*   **UI:** Implement the Offboarding Flow (Credential Entry, Stack Selection, Destruction Console).
*   **Domain:** Implement `OffboardingRepository` (Stack Listing, Bucket Emptying, Stack Deletion).
*   **Logic:** Implement the "Cleanup Trap" and Resume capabilities.
*   **Integration:** Add the entry point to `SettingsScreen` (Danger Zone).
