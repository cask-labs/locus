# Implementation Plan - Task 7: Provisioning Worker

**Feature:** Onboarding & Identity
**Goal:** Implement the background worker responsible for orchestrating AWS CloudFormation provisioning and account recovery, ensuring process resilience.

## Prerequisites: Human Action Steps

*   None.

## Implementation Steps

### Step 1: Update AuthRepository Contract
**Goal:** Enable secure retrieval of Bootstrap Credentials within the background worker.

*   **Modify Interface:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt`
    *   Add `suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>`
*   **Update Implementation:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
    *   Implement using `secureStorage.getBootstrapCredentials()`
*   **Update Test Double:** `core/testing/src/main/kotlin/com/locus/core/testing/repository/FakeAuthRepository.kt`
    *   Add fake implementation.

### Step 2: Implement ProvisioningWorker
**Goal:** Create the WorkManager worker.

*   **Create File:** `app/src/main/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorker.kt`
*   **Dependencies:** `AuthRepository`, `ProvisioningUseCase`, `RecoverAccountUseCase`
*   **Configuration:**
    *   `CoroutineWorker`
    *   Input Data Constants: `KEY_MODE`, `KEY_DEVICE_NAME`, `KEY_BUCKET_NAME`
    *   Mode Enum: `PROVISION`, `RECOVER`
*   **Logic (`doWork`):**
    1.  **Notification:** Call `setForeground` immediately.
        *   Channel: `channel_tracking`
        *   Title: `Locus • Setup`
        *   Body: `Provisioning resources...`
        *   Icon: `@drawable/ic_stat_tracking` (fallback: `@mipmap/ic_launcher` if unavailable, adhering to notification spec).
    2.  **Credentials:** Call `authRepository.getBootstrapCredentials()`. Fail if missing.
    3.  **Dispatch:**
        *   If `PROVISION`: Call `provisioningUseCase(creds, input.deviceName)`
        *   If `RECOVER`: Call `recoverAccountUseCase(creds, input.bucketName)`
    4.  **Result:** Return `Result.success()` or `Result.failure()`.

### Step 3: Worker Module Integration
**Goal:** Ensure Hilt can inject the Worker.

*   **Verify/Update:** Ensure `@HiltWorker` annotation is used on the Worker class.

## Alignment Mapping

*   **R1.600 (Provisioning Background Task):** Implemented by `ProvisioningWorker` running as a Foreground Service (via WorkManager `setForeground`).
*   **R1.1350 (Recovery Background Task):** Implemented by `ProvisioningWorker` handling the `RECOVER` mode.
*   **R1.700 (Use Bootstrap Keys):** `AuthRepository.getBootstrapCredentials()` allows the worker to access keys securely stored during the UI phase.
*   **UI/Notifications Spec:** Adheres to the "Locus • [State]" title format and reuses `channel_tracking`.

## Testing Strategy

### Unit Tests
*   **File:** `app/src/test/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorkerTest.kt`
*   **Tools:** `Robolectric`, `WorkManagerTestInitHelper`, `Mockk`.
*   **Cases:**
    *   **Provisioning Success:** Verify `ProvisioningUseCase` is called and `Result.success()` returned.
    *   **Recovery Success:** Verify `RecoverAccountUseCase` is called and `Result.success()` returned.
    *   **Missing Credentials:** Verify failure if Repo returns error.
    *   **UseCase Failure:** Verify `Result.failure()` if UseCase returns error.
    *   **Notification:** Verify `setForeground` is called (if testable via TestDriver).

## Completion Criteria

*   `AuthRepository` interface supports credential retrieval.
*   `ProvisioningWorker` exists and compiles.
*   Unit tests for worker-related code in the app module pass with > 70% coverage.
*   `./scripts/run_local_validation.sh` passes.
