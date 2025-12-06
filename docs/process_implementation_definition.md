# Implementation Definition Process

This document outlines the systematic process for defining the technical implementation details of the Locus project. The goal of this process is to bridge the gap between "Requirements" (What) and "Code" (How) by specifying the exact technical structures, patterns, and logic to be used.

## 1. Data Persistence Specification
**Goal:** Define the exact local storage schema and access patterns.
*   **Database Schema:** Define the Room Database Entities (Tables), including column types, primary keys, and indices.
*   **DAOs:** Specify the Data Access Object interfaces, defining exact SQL queries for insertion, retrieval (range queries for syncing), and deletion.
*   **Type Converters:** Identify complex types (e.g., `Instant`, `UUID`) requiring type converters.
*   **Migration Strategy:** Define the strategy for handling future schema changes, even if only `fallbackToDestructiveMigration` for now.

## 2. Domain Layer Specification
**Goal:** Define the business logic independent of the UI and Framework.
*   **Models:** Define the Domain Models (Kotlin Data Classes) that the app operates on, separate from DTOs or Entities.
*   **Repository Interfaces:** Define the strict interfaces for data access (e.g., `LocationRepository`, `SettingsRepository`), detailing function signatures and return types (e.g., `Flow<Location>`).
*   **Use Cases (Interactors):** Identify discrete business logic units (e.g., `CompressAndUploadTracksUseCase`, `FilterValidLocationUseCase`) to keep ViewModels clean.

## 3. Background Processing Specification
**Goal:** Detail the implementation of Android Services and Workers.
*   **Foreground Service Logic:** Specify the exact lifecycle methods (`onStartCommand`, `onDestroy`) and the logic for maintaining the "Always On" state (WakeLocks, Notification Channel setup).
*   **WorkManager Constraints:** Define the `Constraints` object for the Sync Worker (e.g., `NetworkType.CONNECTED`, `BatteryNotLow`).
*   **State Machines:** Define the internal state machines for the Tracker Service (e.g., Idle -> Tracking -> Paused (Low Battery) -> Tracking).

## 4. Network & Infrastructure Specification
**Goal:** Define how the app communicates with AWS.
*   **AWS Client Configuration:** Specify the construction of the AWS S3 Client, including Region handling, Credentials Provider (using the custom keys), and Timeout/Retry configurations.
*   **API Interactions:** Detail the specific AWS SDK calls (e.g., `putObject`, `listObjectsV2`) and how responses/exceptions are mapped to Domain results.
*   **Data Transformation:** Define the exact logic for transforming Domain Models into the NDJSON + Gzip format (InputStreams, BufferedWriters).

## 5. UI/Presentation Specification
**Goal:** Define the View layer implementation using MVVM/MVI.
*   **Screen Composition:** Break down User Flows into specific Screens and Composable hierarchies.
*   **Navigation Graph:** Define the Routes, Arguments, and Deep Links.
*   **State Management:** Define the `UiState` data classes for each screen (e.g., `HistoryUiState.Loading`, `HistoryUiState.Success`).
*   **Events/Intents:** Define the user actions (Events) that flow from UI to ViewModel.

## 6. Security & Permissions Specification
**Goal:** Define the implementation of security protocols.
*   **Permission Flow:** Detail the code path for the "Two-Step Dance" for Background Location (System Dialog -> Rationale -> Settings Intent).
*   **Key Storage:** Specify the use of `EncryptedSharedPreferences` for storing AWS Credentials.
*   **Encryption:** Define how data encryption (if any, beyond HTTPS) is implemented.

## 7. Testing Strategy Specification
**Goal:** Define how the implementation will be verified.
*   **Test Doubles:** Identify which dependencies need Fakes vs. Mocks (e.g., `FakeLocationRepository`).
*   **Critical Paths:** Mark specific flows that require Instrumentation Tests (e.g., Database Migration, File writing).
