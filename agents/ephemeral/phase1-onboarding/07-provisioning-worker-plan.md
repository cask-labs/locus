# Implementation Plan - Task 7: Provisioning Worker

**Feature:** Onboarding & Identity
**Goal:** Implement the background worker responsible for orchestrating AWS CloudFormation provisioning and account recovery, ensuring process resilience.

## Prerequisites: Human Action Steps

*   None.

## Implementation Steps

### Step 1: Configure Application & Dependencies
**Goal:** Prepare the `LocusApp` and build environment for WorkManager.

*   **Modify Build:** `app/build.gradle.kts`
    *   Add `testImplementation(libs.androidx.work.testing)` to ensure worker tests can run.
*   **Modify Application:** `app/src/main/kotlin/com/locus/android/LocusApp.kt`
    *   Implement `Configuration.Provider` interface.
    *   Inject `HiltWorkerFactory` via `@Inject lateinit var workerFactory: HiltWorkerFactory`.
    *   Implement `getWorkManagerConfiguration()` to return `Configuration.Builder().setWorkerFactory(workerFactory).build()`.
    *   **Disable Default Initializer:** Ensure `tools:node="remove"` is used for `androidx.work.impl.WorkManagerInitializer` in `AndroidManifest.xml`.
*   **Create Notification Channel:** `app/src/main/kotlin/com/locus/android/LocusApp.kt`
    *   In `onCreate()`, create the `channel_tracking` notification channel (Low Importance) using `NotificationManager`.
    *   **Note:** We reuse `channel_tracking` (Low Importance) for setup because it is a long-running foreground operation similar to "Recording". While setup is critical, it should not trigger "Alert" level noise (Sound/Vibration).

### Step 2: Update Specifications & AuthRepository Contract
**Goal:** Enable secure retrieval of Bootstrap Credentials within the background worker and ensure documentation integrity.

*   **Update Domain Spec:** `docs/technical_discovery/specs/domain_layer_spec.md`
    *   Add `suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>` to the `AuthRepository` interface definition.
    *   This ensures the contract formally allows the worker to access these credentials.
*   **Modify Interface:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt`
    *   Add `suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>`.
*   **Update Implementation:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
    *   Implement using `secureStorage.getBootstrapCredentials()`.
    *   **Safeguard:** Ensure this returns `Failure` if the state is not `SetupPending` (or similar valid state) to prevent misuse.
*   **Create Test Double:** `core/testing/src/main/kotlin/com/locus/core/testing/repository/FakeAuthRepository.kt`
    *   Implement the full `AuthRepository` interface including the new method.

### Step 3: Implement ProvisioningWorker
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
        *   Channel: `channel_tracking`
        *   Title: `Locus • Setup`
        *   Body: `Provisioning resources...`
        *   Icon: `@drawable/ic_stat_sync` (Represents Upload/Cloud Action).
    2.  **Credentials:** Call `authRepository.getBootstrapCredentials()`. Fail if missing.
    3.  **Dispatch & Execution:**
        *   Try:
            *   If `PROVISION`: Call `provisioningUseCase(creds, input.deviceName)`
            *   If `RECOVER`: Call `recoverAccountUseCase(creds, input.bucketName)`
            *   Return `Result.success()`
        *   Catch (DomainException):
            *   **Transient (Network/Timeout):** Return `Result.retry()` (WorkManager handles backoff).
            *   **Fatal (Auth/Validation):**
                *   **CRITICAL:** Call `authRepository.updateProvisioningState(ProvisioningState.Error(msg))` to notify the UI.
                *   Return `Result.failure()`.
        *   Catch (Unknown):
            *   Update State -> Error.
            *   Return `Result.failure()`.

## Alignment Mapping

*   **R1.600 (Provisioning Background Task):** Implemented by `ProvisioningWorker`.
*   **R1.1350 (Recovery Background Task):** Implemented by `ProvisioningWorker`.
*   **R1.700 (Use Bootstrap Keys):** `AuthRepository.getBootstrapCredentials()` provides access.
*   **UI/Notifications Spec:** Uses `channel_tracking` (Low Importance) with `ic_stat_sync` as recommended.

## Testing Strategy

### Unit Tests
*   **File:** `app/src/test/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorkerTest.kt`
*   **Tools:** `Robolectric`, `WorkManagerTestInitHelper`, `Mockk`.
*   **Cases:**
    *   **Success:** Verify UseCase called -> `Result.success()`.
    *   **Transient Error:** Mock UseCase throwing `NetworkError` -> verify `Result.retry()`.
    *   **Fatal Error:** Mock UseCase throwing `AuthError` -> verify `updateProvisioningState(Error)` called -> `Result.failure()`.
    *   **Notification:** Verify `setForeground` called with correct Icon/Channel.

## Completion Criteria

*   `app/build.gradle.kts` includes `work-testing`.
*   `domain_layer_spec.md` is updated.
*   `ProvisioningWorker` correctly handles Retries and State Updates.
*   Unit tests achieve at least 80% line coverage of the `ProvisioningWorker` class, explicitly covering retry and failure scenarios.
*   `./scripts/run_local_validation.sh` passes.
