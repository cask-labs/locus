# Implementation Plan - Task 9: Onboarding UI - Execution

**Status:** Planned
**Feature:** Onboarding UI (Provisioning, Permissions & Success)
**Task:** 9 (Execute Onboarding UI)
**Specs:**
- `docs/behavioral_specs/01_onboarding_identity.md`
- `docs/technical_discovery/specs/ui/onboarding.md`
- `docs/technical_discovery/user_flows/onboarding.md`

## Prerequisites: Human Action Steps

No automated refactoring required.

## Implementation Steps

### Step 1: Domain State Refinement (Safe Refactoring)
**Goal:** Refactor `ProvisioningState` to support the required "Log-Style" UI and ensure tests are updated simultaneously to prevent build breakage.

1.  **Refactor `ProvisioningState`:**
    -   Change the sealed hierarchy to support granular updates.
    -   Example: `data class Working(val currentStep: String, val history: List<String>) : ProvisioningState()` instead of individual object states for every step.
    -   **Constraint:** The `history` list must be implemented as a **Bounded Circular Buffer** with a fixed capacity of 100 items, and this limit must be defined and used in code as a named constant (for example, `MAX_PROVISIONING_HISTORY_SIZE = 100`) to prevent unbounded memory growth during long sessions.
2.  **Update `ProvisioningUseCase` & `RecoverAccountUseCase`:**
    -   Emit granular `Working` states with descriptive strings (e.g., "Creating S3 Bucket", "Deploying CloudFormation").
3.  **Update Domain Tests (Crucial):**
    -   Immediately refactor `ProvisioningUseCaseTest.kt` and `RecoverAccountUseCaseTest.kt`.
    -   Update assertions to check for the `Working` state and its properties rather than specific object types.
    -   **Validation:** Ensure `core:domain` tests pass before proceeding.

### Step 2: Main Architecture & Routing (The Persistent Setup Trap)
**Goal:** Implement the "Setup Trap" using persistent storage to ensure users cannot bypass permissions by restarting the app.

1.  **Update `AuthRepository` (Persistence):**
    -   Implement `getOnboardingStage()` and `setOnboardingStage(stage)` backed by `EncryptedSharedPreferences` (or similar secure persistence).
    -   Stages: `IDLE`, `PROVISIONING`, `PERMISSIONS_PENDING`, `COMPLETE`.
    -   **Error Handling:**
        -   **Read:** Wrap in `try/catch`. On failure (e.g., Keystore issues):
            -   If **Authenticated**: Return `PERMISSIONS_PENDING` (Fail-Secure).
            -   If **Unauthenticated**: Return `IDLE` (Fail-Safe for fresh installs).
        -   **Write:** Wrap in `try/catch`. Log failures but do not crash; maintain state in-memory for the current session.
2.  **Update `MainViewModel`:**
    -   Expose the persistent `onboardingStage`.
    -   Logic: If `stage == PERMISSIONS_PENDING`, lock the user into the Permission flow.
3.  **Update `MainActivity`:**
    -   Routing Logic:
        -   If `stage == COMPLETE` && `AuthState == Authenticated` -> `Dashboard`
        -   If `stage == PERMISSIONS_PENDING` -> `OnboardingNavigation(start = PERMISSIONS)`
        -   Else -> `OnboardingNavigation(start = WELCOME)`

### Step 3: UI Implementation (Detailed Feedback & Permissions)
**Goal:** Build the visual feedback screens and the mandatory permission flow.

1.  **Create `ProvisioningScreen` (Log-Style):**
    -   **UI:** Use a `LazyColumn` to display the list of steps from `ProvisioningState.Working`.
    -   Show a spinner for the current active step and checkmarks for completed steps.
    -   **State:** Observe `ProvisioningState` from the Repo/ViewModel.
2.  **Create `SuccessScreen`:**
    -   **Action:** "Continue" button -> triggers navigation to `Permissions`.
    -   **Logic:** Updates persistent stage to `PERMISSIONS_PENDING`.
3.  **Create `PermissionScreen` (The Two-Step Dance):**
    -   **Step 1:** Explain & Request Foreground Location (`ACCESS_FINE_LOCATION`).
    -   **Step 2:** Explain & Request Background Location (`ACCESS_BACKGROUND_LOCATION` - added in API 29).
        -   **Note:** While API 29 introduced the permission, **Android 11 (API 30+)** enforces separate requests and mandates redirecting the user to System Settings.
    -   **Logic:** Once granted, call `viewModel.completeOnboarding()` (sets stage to `COMPLETE`).
    -   **Denial Handling:**
        -   **Standard Denial:** Show rationale and keep "Continue/Retry" button available.
        -   **Permanent Denial:** If "Don't ask again" is detected, redirect the user to **App Settings**.

### Step 4: Integration & Navigation Wiring
**Goal:** Connect the flows in the correct order: Provisioning -> Success -> Permissions -> Dashboard.

1.  **Update `OnboardingDestinations`:**
    -   Add `PROVISIONING`, `SUCCESS`, and `PERMISSIONS`.
2.  **Update `OnboardingNavigation`:**
    -   Route `PROVISIONING` -> `SUCCESS` on completion.
    -   Route `SUCCESS` -> `PERMISSIONS` on click.
    -   Route `PERMISSIONS` -> `DASHBOARD` (via `onOnboardingComplete` callback to Main).
3.  **Update `NewDeviceViewModel` & `RecoveryViewModel`:**
    -   Ensure `onDeploy()` triggers the navigation to `PROVISIONING`.

### Step 5: Verification & Resilience Testing

1.  **Unit Tests:**
    -   Verify `ProvisioningUseCase` emits the correct sequence of granular states.
2.  **Process Death Test (The Trap):**
    -   Run Provisioning -> Reach Success/Permissions screen.
    -   **Kill the App** (Force Stop).
    -   Relaunch.
    -   **Verify:** App opens directly to `PermissionScreen` (or `Success`), NOT the Welcome screen or Dashboard.
3.  **UI Verification:**
    -   Verify the "Log-Style" list updates in real-time.
    -   Verify the Permission flow handles the Android system dialogs correctly.
    -   Verify "Go to Dashboard" works only after permissions are settled.

### Step 6: Pre-commit & Submission

1.  **Run Validation Script:**
    -   Execute `./scripts/run_local_validation.sh` to ensure all tests (including new Domain tests) pass and linting is correct.
2.  **Submit PR:**
    -   Create a Pull Request with the changes.
