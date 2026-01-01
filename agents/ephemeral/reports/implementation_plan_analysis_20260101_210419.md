# Implementation Plan Analysis: Provisioning Worker

**Date:** 2026-01-01 21:04:19
**Source Plan:** agents/ephemeral/phase1-onboarding/07-provisioning-worker-plan.md

## Testing Strategy Deep Dive

To ensure the resilience of the critical "One-Time Setup" flow, testing must go beyond standard unit tests. The `ProvisioningWorker` operates at the intersection of Android's background limits, network I/O, and complex state management.

### 1. Tier 2: Unit Testing (Worker Logic)
**Goal:** Verify the `ProvisioningWorker` correctly orchestrates the Use Cases and maps results to `WorkManager` outputs.
*   **Tooling:** `Robolectric` with `WorkManagerTestInitHelper`.
*   **Scenarios:**
    *   **Happy Path:** Mock `ProvisioningUseCase` returning `Success`. Verify `doWork` returns `Result.success()`.
    *   **Retry Logic:** Mock `ProvisioningUseCase` throwing a `NetworkError` (Transient). Verify `doWork` returns `Result.retry()`.
    *   **Hard Failure:** Mock `ProvisioningUseCase` throwing an `AuthError`. Verify `doWork` returns `Result.failure()` AND calls `AuthRepository.updateProvisioningState(Error)`.
    *   **Foreground Info:** Verify `getForegroundInfo()` (or the internal `setForeground` call) provides the correct Notification ID, Channel, and **Foreground Service Type** (Crucial for Android 14+).

### 2. Tier 2: Integration Testing (State & Persistence)
**Goal:** Verify that the `AuthRepository` (Data Layer) and `ProvisioningWorker` (Background Layer) maintain state consistency, especially regarding the "Setup Trap" (persisting state across process death).
*   **Tooling:** `Robolectric` or `AndroidTest` (Instrumented).
*   **Approach:**
    1.  Initialize `AuthRepository` with `SetupPending`.
    2.  Enqueue `ProvisioningWorker`.
    3.  Simulate a "Process Death" (re-instantiate the Repository/ViewModel).
    4.  **Verification:** The Repository should typically rely on a persistent source (DataStore/File) for the `ProvisioningState`. Check if the re-created Repository reflects the "Error" state if the Worker failed *before* the UI re-connected.

### 3. Tier 3: Resilience Testing (Simulated Scenarios)
**Goal:** Validate behavior under hostile Android environment conditions.
*   **Scenarios:**
    *   **System Kill:** Start the Worker, then force-stop the app (via adb). Restart app.
        *   *Expected:* WorkManager should eventually reschedule the work (if it was `retry`) or the "Setup Trap" should catch the user on next launch.
    *   **No Connectivity:** Start Worker with Airplane Mode ON.
        *   *Expected:* Immediate `Result.retry()` (if transient) or `Result.failure()` handling.

### 4. Tier 5: Device Farm (Background Constraints)
**Goal:** Verify Android 12/13/14 restrictions don't kill the process.
*   **Focus:**
    *   **Foreground Service Type:** Verify `SecurityException` is NOT thrown on Android 14 when `setForeground` is called.
    *   **Notification:** Visually confirm the notification appears and is not dismissible during execution.

---

## Critical Decisions & Recommendations

The following critical decisions are not explicitly defined in the plan and require immediate resolution to meet the project's behavioral specifications and memory requirements.

### 1. Decision: Detailed "Log-Style" Progress Reporting
*   **Context:** The Memory explicitly states the UI must display a **Detailed 'Log-Style' List** (e.g., "Creating Bucket", "Waiting for Stack"). The current plan only handles `Success` or `Failure` at the Worker level.
*   **Problem:** If the `ProvisioningUseCase` blocks until completion, the user sees a stagnant spinner for ~2-5 minutes.
*   **Proposed Approach:**
    *   **Modify `ProvisioningUseCase`:** It should not just return `LocusResult<Unit>`, but should interact with `AuthRepository.updateProvisioningState()` *during* execution to emit granular states (e.g., `ProvisioningState.Working("Creating S3 Bucket...")`).
    *   **Worker Responsibility:** The Worker plan needs to explicitly state that it relies on the Use Case to push these updates to the Repository.
    *   **Justification:** Essential for user trust during long-running cloud operations.

### 2. Decision: Android 14 Foreground Service Type
*   **Context:** Android 14 requires strictly declaring the type of work for Foreground Services.
*   **Problem:** The plan mentions "Configure Application" but doesn't specify the `foregroundServiceType` in the `AndroidManifest.xml`.
*   **Proposed Approach:**
    *   **Type:** Use `dataSync`.
    *   **Implementation:** Ensure the `AndroidManifest.xml` `<service>` entry for `androidx.work.impl.foreground.SystemForegroundService` includes `foregroundServiceType="dataSync"`.
    *   **Justification:** Prevents `SecurityException` on Android 14 devices.

### 3. Decision: Automatic vs. Manual "Clean Reset" on Failure
*   **Context:** The Memory says hard failures must execute a **Clean Reset** (wipe data) but persist logs. The plan says it returns `Result.failure()` and updates state to `Error`.
*   **Problem:** If the Worker returns `Failure`, the specific credentials used might be invalid. If we don't wipe them automatically, the user might be stuck; if we do wipe them automatically, we might wipe valid keys on a server glitch.
*   **Proposed Approach:**
    *   **Manual Trigger:** The Worker should *not* automatically wipe credentials. Instead, it sets the `Error` state.
    *   **UI Responsibility:** The "Error" screen presented to the user should have a primary button "Reset & Try Again" which triggers the `ClearBootstrapCredentialsUseCase`.
    *   **Justification:** Safer UX. Prevents the app from aggressively deleting data if the error was actually a server-side glitch rather than bad user input.

### 4. Decision: Notification Channel Isolation
*   **Context:** The plan proposes reusing `channel_tracking`.
*   **Problem:** `channel_tracking` is designed for the infinite "Always On" loop. Provisioning is a finite, high-stakes operation.
*   **Proposed Approach:**
    *   **Dedicated Channel:** Create a `channel_setup` (ID: `setup_status`).
    *   **Behavior:** Importance `LOW` (to avoid sound), but distinct ID.
    *   **Justification:** Better separation of concerns and prevents edge cases where the Tracker Service tries to start while Provisioning is active.
