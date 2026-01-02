# Analysis of Task 8 Plan (Onboarding)

**Date:** 2026-01-02 13:52:02
**Analyzed File:** `agents/ephemeral/phase1-onboarding/08-task-8-plan.md`
**Reference Specs:**
*   `docs/technical_discovery/specs/ui/onboarding.md`
*   `agents/ephemeral/phase1-onboarding/tasking.md`

## Identified Problems and Proposed Resolutions

### 1. Significant Scope Gap (Missing Screens)
*   **Finding:** The plan only includes `WelcomeScreen` and `CredentialEntryScreen`. `tasking.md` defines Task 9 as covering `ProvisioningScreen` and `SuccessScreen`. This leaves a massive gap in the flow: **`ChoiceScreen`**, **`NewDeviceSetupScreen`** (Path A), and **`RecoveryScreen`** (Path B) are completely missing from both Task 8 and the defined scope of Task 9.
*   **Analysis:** `docs/technical_discovery/specs/ui/onboarding.md` defines the flow as: *Welcome -> Credentials -> Choice -> (New Device OR Recovery) -> Provisioning*. The current task breakdown leaves the middle three screens orphaned.
*   **Resolution:** Expand the scope of **Task 8** to include `ChoiceScreen`, `NewDeviceSetupScreen`, and `RecoveryScreen`.
*   **Justification:** These screens represent the "Configuration/Input" phase of onboarding, similar to `CredentialEntryScreen`. Task 9 is correctly scoped to "Execution" (Provisioning/Worker) and "Success". Grouping all *input* screens into Task 8 ensures that Task 9 can focus purely on the complex asynchronous logic of the Provisioning Worker and UI feedback.

### 2. Misplaced "Key Generation Guide" Feature
*   **Finding:** The plan places the `"Help" link -> Modal Bottom Sheet` feature in the **`CredentialEntryScreen`** section.
*   **Analysis:** `docs/technical_discovery/specs/ui/onboarding.md` (Section 3.1) explicitly states that the **`WelcomeScreen`** contains the help action: *"Help: 'Guide: How to generate AWS Keys' (Opens In-App Bottom Sheet Guide)."*
*   **Resolution:** Move the "Key Generation Guide" implementation step from `CredentialEntryScreen` to `WelcomeScreen` in the plan.
*   **Justification:** Adhering to the spec ensures the user has access to the guide *before* they commit to starting the process, and matches the designed wireframe.

### 3. Missing Clipboard Integration Detail
*   **Finding:** The plan mentions a `pasteJson` function in the ViewModel and a "Paste JSON button" in the UI, but fails to specify *how* the data gets from the system clipboard to the ViewModel.
*   **Analysis:** `docs/technical_discovery/specs/ui/onboarding.md` specifies *"Parses clipboard content"*. Accessing the clipboard requires a `Context` or `ClipboardManager`, which should typically happen in the UI layer (Composable/Activity) and then be passed to the ViewModel. The plan is ambiguous and could lead to implementing `Context` usage inside the ViewModel (a bad practice) or missing the functionality.
*   **Resolution:** Explicitly add a step in the **`CredentialEntryScreen`** implementation to "Retrieve text from `ClipboardManager` (via `LocalContext` or `Context`) on button click and pass to `viewModel.pasteJson()`".
*   **Justification:** Clarifying the technical boundary prevents architectural violations (Context in ViewModel) and ensures the requirement is actually implemented.

### 4. Ambiguous "Dry Run" Logic Location
*   **Finding:** The plan mentions `validateCredentials()` calling `AuthRepository.validateCredentials` in the ViewModel.
*   **Analysis:** While correct, the plan should explicitly verify that this validation logic (which involves network calls) is handled correctly as a suspend function / coroutine launch within the ViewModel, ensuring the UI shows a loading state during this "Dry Run". `tasking.md` mentions this, but the plan's UI step just says "Async trigger".
*   **Resolution:** Enhance the `OnboardingViewModel` step to explicitly mention managing `isLoading` state during the `validateCredentials` call.
*   **Justification:** The "Dry Run" is a network operation; failing to manage UI state will result in an unresponsive UI.

### 5. Missing Navigation Graph Coverage
*   **Finding:** The plan includes "Setup Navigation" but only for the screens implemented in this task.
*   **Analysis:** Since the flow is incomplete (see Problem 1), the `OnboardingGraph` created in this task will be broken or incomplete (dead ends).
*   **Resolution:** If the scope is expanded (Resolution 1), the Graph must cover all input screens. If not, the plan must explicitly state that the Graph will use *temporary placeholders* or *stubs* for the missing screens to allow the flow to be tested up to the break point.
*   **Justification:** Prevents confusion during verification when the flow dead-ends.
