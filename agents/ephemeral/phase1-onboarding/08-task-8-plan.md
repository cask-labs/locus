# 08-task-8-plan.md

## Prerequisites: Human Action Steps
None.

## Phase 0: Technical Review Fixes (PR #300)
**Goal:** Address technical debt and review comments from PR #300 in `AuthRepositoryImpl` to ensure a clean foundation before UI implementation.

1.  **Refactor `AuthRepositoryImpl.kt`**
    *   **File:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
    *   **Issue 1 (Logging):** Replace `e.printStackTrace()` with `android.util.Log.e(...)`.
        *   *Justification:* `printStackTrace` does not log correctly to Android Logcat in production.
    *   **Issue 2 (Imports):** Standardize `DomainException` usage.
        *   *Justification:* Inconsistent use of fully qualified names vs short names reduces readability. Use short names (already imported).
    *   **Validation:** Run `./gradlew :core:data:testDebugUnitTest`.

## Phase 1: Logic Implementation
**Goal:** Implement the state management and business logic for the complete Onboarding "Input" flow (Welcome -> Credentials -> Choice -> Setup/Recovery).

1.  **Create `OnboardingViewModel`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/OnboardingViewModel.kt`
    *   **Logic:**
        *   `OnboardingUiState` data class (credentials, loading, error, event).
        *   `pasteJson(json: String)`: Parses standard AWS CLI output; sets error on failure (R1.150, R1.160). **Note:** This function accepts a raw string; clipboard access is handled in the UI.
        *   `validateCredentials()`: Calls `AuthRepository.validateCredentials` (R1.100).
        *   `checkAuthState()`: Checks `AuthRepository` on init to determine start destination (Basic check only; full "Trap" logic is Task 10).
    *   **Validation:** `read_file` to verify creation.

2.  **Create `NewDeviceViewModel`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/NewDeviceViewModel.kt`
    *   **Logic:**
        *   `validateDeviceName(name: String)`: Checks regex (lowercase, alphanumeric, hyphens).
        *   `checkAvailability(name: String)`: (Mock/Stub for now, or actual call if Repo ready).
    *   **Validation:** `read_file`.

3.  **Create `RecoveryViewModel`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/RecoveryViewModel.kt`
    *   **Logic:**
        *   `loadBuckets()`: Calls `AuthRepository` (or `S3Client`) to list `locus-` buckets.
        *   State management: Loading, Error, List<String>.
    *   **Validation:** `read_file`.

4.  **Test ViewModels**
    *   **File:** `app/src/test/kotlin/com/locus/android/features/onboarding/OnboardingViewModelTest.kt` (and others)
    *   **Action:** Run `./gradlew :app:testDebugUnitTest`.

## Phase 2: UI Implementation
**Goal:** Create the visual components for the complete Input Flow.
**Constraint:** All screens must use a **Centered, Scrollable Column** with `widthIn(max = 600.dp)` to support Tablet/Landscape (R1.Layout).

1.  **Create `WelcomeScreen`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/ui/WelcomeScreen.kt`
    *   **Components:**
        *   Title, Cost Disclaimer (R1.050).
        *   "Get Started" Button.
        *   **Help Guide:** "How to generate AWS Keys" link opening a Modal Bottom Sheet (moved from Credential Screen as per Spec 3.1).
    *   **Validation:** `read_file`.

2.  **Create `CredentialEntryScreen`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/ui/CredentialEntryScreen.kt`
    *   **Components:**
        *   TextFields for keys (R1.300).
        *   "Paste JSON" button: **Logic to retrieve text from `LocalClipboardManager`** (UI Layer) and pass to ViewModel.
        *   "Validate" button (Async trigger with Loading Indicator).
    *   **Validation:** `read_file`.

3.  **Create `ChoiceScreen`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/ui/ChoiceScreen.kt`
    *   **Components:** Two large cards/buttons for "New Device" vs "Recovery".
    *   **Validation:** `read_file`.

4.  **Create `NewDeviceSetupScreen`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/ui/NewDeviceSetupScreen.kt`
    *   **Components:** Device Name input, availability indicator, "Deploy" button.
    *   **Validation:** `read_file`.

5.  **Create `RecoveryScreen`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/ui/RecoveryScreen.kt`
    *   **Components:** List of buckets (Loading/Error/Content states).
    *   **Validation:** `read_file`.

6.  **Setup Navigation**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/OnboardingNavigation.kt`
    *   **Content:** `OnboardingGraph` composable defining routes: `Welcome -> Credentials -> Choice -> (NewDevice | Recovery)`.
    *   **Validation:** `read_file`.

## Phase 3: Integration & Verification
**Goal:** Wire it up and verify.

1.  **Integrate into `MainActivity`**
    *   **File:** `app/src/main/kotlin/com/locus/android/MainActivity.kt`
    *   **Logic:**
        *   Observe `AuthRepository.state`.
        *   Set `startDestination` to `OnboardingGraph` if `Uninitialized` or `SetupPending`.
        *   Set to `Dashboard` if `Authenticated`.
        *   *(Note: The full "Permission Trap" and "Provisioning Resume" logic is delegated to Task 10).*
    *   **Validation:** `read_file` to verify the edit.

2.  **Automated Verification**
    *   **Action:** Run `./gradlew :app:testDebugUnitTest` (Logic).
    *   **Action:** Run `./gradlew :app:lintDebug` (Style/Issues).

3.  **Complete pre commit steps**
    *   Complete pre commit steps to make sure proper testing, verifications, reviews and reflections are done.

## Completion Criteria
*   Unit tests pass for all ViewModels.
*   All 5 screens (Welcome, Creds, Choice, NewDevice, Recovery) exist and respect Tablet layout rules.
*   Navigation graph is complete and compilable.
*   Lint check passes.
