# Implementation Plan - Task 9: Onboarding UI - Execution

**Status:** Planned
**Feature:** Onboarding UI (Provisioning & Success)
**Task:** 9 (Execute Onboarding UI)
**Specs:**
- `docs/behavioral_specs/01_onboarding_identity.md`
- `docs/technical_discovery/specs/ui/onboarding.md` (implied)

## Prerequisites: Human Action Steps

No automated refactoring required.

## Implementation Steps

### Step 1: Domain State Refinement
**Goal:** Ensure `ProvisioningState` accurately reflects the entire lifecycle (Deploying -> Finalizing -> Success) to support UI feedback.

1.  **Modify `ProvisioningUseCase`:**
    -   At the start of `invoke`, call `authRepository.updateProvisioningState(ProvisioningState.Deploying)`.
    -   At the successful end, call `authRepository.updateProvisioningState(ProvisioningState.Success)`.
2.  **Modify `RecoverAccountUseCase`:**
    -   Apply the same state updates (`Deploying` -> `Success`).
3.  **Update `AuthRepository` (Interface & Impl):**
    -   Ensure `updateProvisioningState` is accessible and thread-safe.

### Step 2: Main Architecture & Routing (The Provisioning Lock)
**Goal:** Implement the "Lock" pattern to ensure the Success screen is displayed before transitioning to the Dashboard, even though the user is technically authenticated.

1.  **Update `MainViewModel`:**
    -   Observe `authRepository.provisioningState`.
    -   Expose a combined state or separate states to `MainActivity`.
    -   Implement `completeOnboarding()`:
        -   Calls `authRepository.updateProvisioningState(ProvisioningState.Idle)`.
        -   This breaks the "Lock" and allows `AuthState.Authenticated` to take over.
2.  **Update `MainActivity`:**
    -   Modify the content switching logic:
        ```kotlin
        val provisioningState by viewModel.provisioningState.collectAsState()
        val authState by viewModel.authState.collectAsState()

        if (provisioningState is ProvisioningState.Success) {
            OnboardingNavigation(startDestination = PROVISIONING_SUCCESS) // or handle via NavController
        } else if (authState is AuthState.Authenticated) {
            DashboardScreen()
        } else {
            OnboardingNavigation()
        }
        ```
    -   *Refinement:* To preserve navigation stack during the flow, better to keep `OnboardingNavigation` active if `provisioningState` is *anything other than Idle* (or at least if it's Deploying/Success).

### Step 3: UI Implementation
**Goal:** Build the visual feedback screens.

1.  **Create `ProvisioningScreen`:**
    -   **State:** Observes `ProvisioningState` (Deploying, Finalizing, Failure).
    -   **UI:**
        -   `Deploying`: CircularProgressIndicator + "Provisioning your secure cloud infrastructure... (This may take a minute)"
        -   `Finalizing`: "Finalizing setup..."
        -   `Failure`: Error Icon + Error Message + "Retry" button (Navigation Back).
2.  **Create `SuccessScreen`:**
    -   **UI:** Large Checkmark Icon + "You're all set!" + "Your private Locus cloud is ready."
    -   **Action:** "Go to Dashboard" button -> calls `MainViewModel.completeOnboarding()`.

### Step 4: Integration & Navigation Wiring
**Goal:** Connect the flows.

1.  **Update `OnboardingDestinations`:**
    -   Add `PROVISIONING` and `SUCCESS` routes.
2.  **Update `OnboardingNavigation`:**
    -   Add composables for the new routes.
    -   Pass `onOnboardingComplete` down to `SuccessScreen`.
3.  **Update `NewDeviceViewModel` & `RecoveryViewModel`:**
    -   In `onDeploy()`:
        -   Start `ProvisioningWorker` (keep existing logic).
        -   **Navigate immediately** to `OnboardingDestinations.PROVISIONING`.
    -   *Note:* The `ProvisioningScreen` will observe the Repo state (driven by the Worker/UseCase).

### Step 5: Verification

1.  **Unit Tests:**
    -   Verify `ProvisioningUseCase` emits correct state sequence.
    -   Verify `MainViewModel` routing logic (Mock states).
2.  **Manual Verification (Interactive):**
    -   Run app.
    -   Enter valid credentials (mocked or real).
    -   Click Deploy.
    -   Verify "Provisioning..." screen appears.
    -   Verify transition to "Success" screen.
    -   Click "Go to Dashboard".
    -   Verify Dashboard appears.
    -   Restart App -> Verify Dashboard appears immediately.
