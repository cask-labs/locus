# Implementation Plan Analysis - Task 7: Provisioning Worker

**Plan under review:** `agents/ephemeral/phase1-onboarding/07-provisioning-worker-plan.md`

## Testing Deep Dive

The current plan focuses heavily on **Tier 2 (Unit)** testing of the Worker class itself but lacks specificity on Integration (Tier 2/3) and Device Validation (Tier 5), which are critical for `WorkManager` implementations that rely on OS-level scheduling.

### 1. Tier 2: Unit Testing (Worker Logic)
*   **Current Plan:** Uses `Robolectric` and `WorkManagerTestInitHelper`.
*   **Critique:** This is good for verifying the `doWork()` logic, but `StandardTestDispatcher` in Coroutines can sometimes mask race conditions in `setForeground()` calls.
*   **Recommendation:**
    *   **Test Progress Updates:** Ensure the test verifies `setProgress()` is called if the UseCase emits intermediate steps (see Critical Decision #2).
    *   **Test Cancellation:** Verify `onStopped()` behavior. If the generic UseCase is cancelled, does the Worker handle the `CancellationException` correctly to avoid leaving the Stack in an indeterminate state?

### 2. Tier 2: Integration Testing (Repository <-> Worker)
*   **Current Plan:** Mocking the UseCase.
*   **Critique:** There is a gap in testing how the `AuthRepository` state interacts with the `WorkManager` state.
*   **Recommendation:**
    *   **State Synchronization Test:** A test is needed where the `AuthRepository` is initialized, detects a *pre-existing* running Worker (simulating process restart), and correctly updates its exposed `ProvisioningState` flow without user intervention.

### 3. Tier 4: Infrastructure Audit (Real CloudFormation)
*   **Current Plan:** None.
*   **Critique:** The worker triggers real CloudFormation stacks.
*   **Recommendation:**
    *   **Manual Verification:** The developer must verify that the `ProvisioningWorker` correctly handles a "Stack Already Exists" error (e.g., from a partial previous run) versus a fresh run.

### 4. Tier 5: Device Farm (OS Constraints)
*   **Current Plan:** None.
*   **Critique:** `WorkManager` behavior varies significantly by OEM (Samsung, Xiaomi) regarding background execution and "Expedited" jobs.
*   **Recommendation:**
    *   **Doze Mode Simulation:** Run a test case where the device is forced into Doze mode (`adb shell dumpsys deviceidle force-idle`) while the provisioning is running to ensure the `ForegroundService` properly exempts the worker from network restrictions.

---

## Critical Decisions & Proposed Approaches

The following decision points are not explicitly addressed in the plan but are critical for a robust implementation.

### 1. Decision: State Rehydration (Handling Process Death)
**Context:** `WorkManager` continues running even if the App Process (UI/ViewModel/Repo) is killed. When the user re-opens the app during provisioning, the `AuthRepository` (if purely in-memory) will be reset to `Uninitialized`, creating a disconnect.
**Proposed Approach:**
*   **Logic:** Upon initialization, `AuthRepository` must query `WorkManager.getWorkInfosForUniqueWork("provisioning")`.
*   **Behavior:**
    *   If state is `RUNNING` or `ENQUEUED`: Immediately transition internal `ProvisioningState` to `Working`.
    *   If state is `SUCCEEDED`: Transition to `Success` (or trigger the finalization step).
    *   If state is `FAILED`: Transition to `Error` and extract the failure reason from `WorkInfo.outputData`.
**Justification:** Prevents "Split Brain" where the background worker is succeeding but the UI shows the start screen or an error.

### 2. Decision: Granular Progress Reporting ("Log-Style" UI)
**Context:** The Memory explicitly states the UI requires a **Detailed 'Log-Style' List** (e.g., "Creating Bucket", "Waiting for Stack"). The current plan only handles `Result.success/failure`.
**Proposed Approach:**
*   **UseCase Update:** `ProvisioningUseCase` should emit a `Flow<ProvisioningStep>` or accept a callback, rather than just returning `Unit`.
*   **Worker Update:** The Worker must collect this Flow and call `setProgress(workDataOf("step_name" to step.name, "step_index" to step.index))` for every emission.
*   **UI Update:** The UI must observe `WorkInfo.progress` via the Repository to render the live log list.
**Justification:** Meets the specific UI requirement for transparency during the long-running process (5-10 mins).

### 3. Decision: Android 14 Foreground Service Type
**Context:** Android 14 (API 34) strictly enforces `foregroundServiceType`. The plan mentions `channel_tracking` but not the specific service type attribute required in the Manifest and `startForeground`.
**Proposed Approach:**
*   **Manifest:** Declare `android:foregroundServiceType="dataSync"` for the `SystemJobService` (WorkManager's proxy).
*   **Implementation:** In `createForegroundInfo()`, pass `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC`.
**Justification:** "dataSync" is the most appropriate type for uploading/downloading configuration to a server. Failing to declare this will cause a `SecurityException` on Android 14+.

### 4. Decision: Error Persistence vs. "Clean Reset"
**Context:** Memory states failures must execute a "Clean Reset" (wipe creds) but **persist logs**.
**Proposed Approach:**
*   **Worker Logic:** In the `catch` block for a Fatal Error:
    1.  Write the full exception stack trace to `LogRepository` (Database) with a specific tag (e.g., `PROVISIONING_FAIL`).
    2.  Call `AuthRepository.clearBootstrapCredentials()` (Clean Reset).
    3.  Return `Result.failure(workDataOf("error_message" to friendlyMessage))`.
**Justification:** Ensures that when the UI resets to the "Input Credentials" screen, the user (or support) can still access the *historical* log of why it failed via the Logs screen, satisfying the "Clean Reset" requirement without losing diagnostic data.

### 5. Decision: Cancellation Strategy
**Context:** CloudFormation stacks stuck in `CREATE_IN_PROGRESS` are difficult to handle if the client simply walks away.
**Proposed Approach:**
*   **Policy:** The Provisioning Worker should be **Non-Cancellable** by the user via the UI. The "Back" button should be disabled or minimize the app (Run in Background).
*   **Timeout:** Rely on the explicit 10-minute timeout in the UseCase (as defined in Domain Spec) rather than `WorkManager` generic timeouts.
**Justification:** Allowing user cancellation during a CloudFormation stack creation often leaves "Zombie Stacks" (`ROLLBACK_FAILED`) that require manual AWS Console intervention to fix, which defeats the purpose of the easy onboarding.
