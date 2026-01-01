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
    *   **Disable Default Initializer:** Ensure `tools:node="remove"` is used for `androidx.work.impl.WorkManagerInitializer` in `AndroidManifest.xml` (or verify if Hilt handles this, standard practice is manual config for Hilt).
*   **Create Notification Channel:** `app/src/main/kotlin/com/locus/android/LocusApp.kt`
    *   In `onCreate()`, create the `channel_tracking` notification channel (Low Importance) using `NotificationManager`. This ensures the Worker can post its foreground notification immediately.

### Step 2: Update AuthRepository Contract
**Goal:** Enable secure retrieval of Bootstrap Credentials within the background worker.

*   **Modify Interface:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt`
    *   Add `suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>`
*   **Update Implementation:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
    *   Implement using `secureStorage.getBootstrapCredentials()`
*   **Create Test Double:** `core/testing/src/main/kotlin/com/locus/core/testing/repository/FakeAuthRepository.kt`
    *   **Create File:** If it does not exist.
    *   Implement the full `AuthRepository` interface (stubs for methods not used in this task, functional mock for `getBootstrapCredentials`).

### Step 3: Implement ProvisioningWorker
**Goal:** Create the WorkManager worker.

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
        *   Icon: `@drawable/ic_stat_tracking` (fallback: `@mipmap/ic_launcher` if unavailable).
    2.  **Credentials:** Call `authRepository.getBootstrapCredentials()`. Fail if missing.
    3.  **Dispatch:**
        *   If `PROVISION`: Call `provisioningUseCase(creds, input.deviceName)`
        *   If `RECOVER`: Call `recoverAccountUseCase(creds, input.bucketName)`
    4.  **Result:** Return `Result.success()` or `Result.failure()`.

## Alignment Mapping

*   **R1.600 (Provisioning Background Task):** Implemented by `ProvisioningWorker` running as a Foreground Service.
*   **R1.1350 (Recovery Background Task):** Implemented by `ProvisioningWorker` handling the `RECOVER` mode.
*   **R1.700 (Use Bootstrap Keys):** `AuthRepository.getBootstrapCredentials()` allows the worker to access keys.
*   **UI/Notifications Spec:** Adheres to the "Locus • [State]" title format and reuses `channel_tracking`.

## Testing Strategy

### Unit Tests
*   **File:** `app/src/test/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorkerTest.kt`
*   **Tools:** `Robolectric`, `WorkManagerTestInitHelper` (requires `work-testing` dependency), `Mockk`.
*   **Cases:**
    *   **Provisioning Success:** Verify `ProvisioningUseCase` is called and `Result.success()` returned.
    *   **Recovery Success:** Verify `RecoverAccountUseCase` is called and `Result.success()` returned.
    *   **Missing Credentials:** Verify failure if Repo returns error.
    *   **UseCase Failure:** Verify `Result.failure()` if UseCase returns error.
    *   **Notification:** Verify `setForeground` is called.

## Completion Criteria

*   `app/build.gradle.kts` includes `work-testing`.
*   `LocusApp` implements `Configuration.Provider`.
*   `FakeAuthRepository` is created and usable.
*   `ProvisioningWorker` exists, compiles, and passes tests (> 70% coverage).
*   `./scripts/run_local_validation.sh` passes.
