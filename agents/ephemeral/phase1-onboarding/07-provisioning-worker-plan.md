# Implementation Plan - Task 7: Provisioning Worker

**Feature:** Onboarding & Identity
**Goal:** Implement the background worker responsible for orchestrating AWS CloudFormation provisioning and account recovery, ensuring process resilience.

## Prerequisites: Human Action Steps

*   None.

## Implementation Steps

### Step 1: Configure Application & Dependencies
**Goal:** Prepare the `LocusApp` and build environment for WorkManager.

*   **Modify Version Catalog:** `gradle/libs.versions.toml`
    *   Add `androidx-work-testing` library definition (version ref `work-runtime-ktx`).
*   **Modify Build:** `app/build.gradle.kts`
    *   Add `testImplementation(libs.androidx.work.testing)` to ensure worker tests can run.
*   **Modify Application:** `app/src/main/kotlin/com/locus/android/LocusApp.kt`
    *   Implement `Configuration.Provider` interface.
    *   Inject `HiltWorkerFactory` via `@Inject lateinit var workerFactory: HiltWorkerFactory`.
    *   Implement `getWorkManagerConfiguration()` to return `Configuration.Builder().setWorkerFactory(workerFactory).build()`.
    *   **Disable Default Initializer:** Ensure `tools:node="remove"` is used for `androidx.work.impl.WorkManagerInitializer` in `AndroidManifest.xml`.
    *   **Create Notification Channels:**
        *   Create `channel_setup` (ID: `setup_status`, Importance: LOW, Name: "Setup Status").
        *   Create `channel_tracking` (ID: `tracking_status`, Importance: LOW, Name: "Tracking Status").
*   **Modify Manifest:** `app/src/main/AndroidManifest.xml`
    *   Update the `SystemForegroundService` declaration to include `android:foregroundServiceType="dataSync"` to comply with Android 14.

### Step 2: Update Specifications & AuthRepository Contract
**Goal:** Enable secure retrieval of Bootstrap Credentials and state rehydration.

*   **Update Domain Spec:** `docs/technical_discovery/specs/domain_layer_spec.md`
    *   Add `suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>` to `AuthRepository`.
*   **Modify Interface:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt`
    *   Add `suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>`.
*   **Modify Implementation:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
    *   Implement `getBootstrapCredentials()` using `secureStorage`.
    *   **Implement "Setup Trap":** In `initialize()`, query `WorkManager.getWorkInfosForUniqueWork("provisioning")`.
        *   If `RUNNING` or `ENQUEUED`: Emit `ProvisioningState.Working("Resuming setup...")`.
        *   If `FAILED`: Emit `ProvisioningState.Error(lastError)`.
*   **Create Test Double:** `core/testing/src/main/kotlin/com/locus/core/testing/repository/FakeAuthRepository.kt`
    *   Implement the full `AuthRepository` interface including the new method.

### Step 3: Refactor Use Case for Progress
**Goal:** Enable granular "Log-Style" progress reporting.

*   **Modify Use Case:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/onboarding/ProvisioningUseCase.kt`
    *   Change return type to `Flow<ProvisioningStep>` or accept a `onProgress: (String) -> Unit` callback.
    *   (Alternatively, for simplicity with Workers): Have the UseCase call `AuthRepository.updateProvisioningState(Working(step))` directly, and just return `Result`.
    *   *Decision:* Let's use the `AuthRepository.updateProvisioningState` approach as it keeps the Worker simple. The Worker just watches for final result.

### Step 4: Implement ProvisioningWorker
**Goal:** Create the WorkManager worker with robust error handling and retry logic.

*   **Create File:** `app/src/main/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorker.kt`
*   **Dependencies:** `AuthRepository`, `ProvisioningUseCase`, `RecoverAccountUseCase`
*   **Configuration:**
    *   `CoroutineWorker`
    *   Use `@HiltWorker` and `@AssistedInject`.
    *   Input Data Constants: `KEY_MODE`, `KEY_DEVICE_NAME`, `KEY_BUCKET_NAME`
    *   Mode Enum: `PROVISION`, `RECOVER`
*   **Logic (`doWork`):**
    1.  **Notification:** Call `setForeground` immediately.
        *   Channel: `channel_setup`
        *   Type: `FOREGROUND_SERVICE_TYPE_DATA_SYNC`
        *   Title: `Locus • Setup`
        *   Body: `Starting...`
    2.  **Credentials:** Call `authRepository.getBootstrapCredentials()`. Fail if missing.
    3.  **Dispatch & Execution:**
        *   Try:
            *   If `PROVISION`: Call `provisioningUseCase` (which updates repo state).
            *   If `RECOVER`: Call `recoverAccountUseCase`.
            *   Return `Result.success()`
        *   Catch (DomainException):
            *   **Transient (Network/Timeout):** Return `Result.retry()` (WorkManager handles backoff).
            *   **Fatal (Auth/Validation):**
                *   **CRITICAL:** Call `authRepository.updateProvisioningState(ProvisioningState.Error(msg))` to notify the UI.
                *   **DO NOT** automatically clear credentials (R1.1000).
                *   Return `Result.failure()`.
        *   Catch (Unknown):
            *   Update State -> Error.
            *   Return `Result.failure()`.

## Alignment Mapping

*   **R1.600 (Provisioning Background Task):** Implemented by `ProvisioningWorker` with `FOREGROUND_SERVICE_TYPE_DATA_SYNC`.
*   **R1.1000 (No Auto-Delete):** Worker sets Error state but does NOT clear credentials.
*   **R1.1900 (Setup Trap):** Implemented in `AuthRepository.initialize()`.
*   **UI/Notifications Spec:** Uses `channel_setup` (Low Importance).

## Testing Strategy

### Unit Tests
*   **File:** `app/src/test/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorkerTest.kt`
*   **Tools:** `Robolectric`, `WorkManagerTestInitHelper`, `Mockk`.
*   **Cases:**
    *   **Success:** Verify UseCase called -> `Result.success()`.
    *   **Transient Error:** Mock UseCase throwing `NetworkError` -> verify `Result.retry()`.
    *   **Fatal Error:** Mock UseCase throwing `AuthError` -> verify `updateProvisioningState(Error)` called -> `Result.failure()`.
    *   **Notification:** Verify `setForeground` called with correct Icon/Channel and **Service Type**.

## Completion Criteria

*   `gradle/libs.versions.toml` includes `work-testing`.
*   `AndroidManifest.xml` includes `dataSync` service type.
*   `ProvisioningWorker` correctly handles Retries and State Updates.
*   `AuthRepository` correctly rehydrates state from running workers.
*   Unit tests achieve at least 80% line coverage of the `ProvisioningWorker` class.
