# 08-task-8-plan.md

## Prerequisites: Human Action Steps
None.

## Phase 1: Logic Implementation
**Goal:** Implement the state management and business logic for the Onboarding flow (Credentials).

1.  **Create `OnboardingViewModel`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/OnboardingViewModel.kt`
    *   **Logic:**
        *   `OnboardingUiState` data class (credentials, loading, error, event).
        *   `pasteJson(json: String)`: Parses standard AWS CLI output; sets error on failure (R1.150, R1.160).
        *   `validateCredentials()`: Calls `AuthRepository.validateCredentials` (R1.100).
        *   `updateCredentials()`: Updates individual fields.
    *   **Validation:** `read_file` to verify creation.

2.  **Test `OnboardingViewModel`**
    *   **File:** `app/src/test/kotlin/com/locus/android/features/onboarding/OnboardingViewModelTest.kt`
    *   **Cases:**
        *   `pasteJson_validInput_updatesState`
        *   `pasteJson_invalidInput_setsError`
        *   `validateCredentials_success_triggersNavigation`
        *   `validateCredentials_failure_setsError`
    *   **Action:** Run `./gradlew :app:testDebugUnitTest`.

## Phase 2: UI Implementation
**Goal:** Create the visual components.

1.  **Create `WelcomeScreen`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/ui/WelcomeScreen.kt`
    *   **Components:** Title, Cost Disclaimer (R1.050), "Get Started" Button.
    *   **Validation:** `read_file`.

2.  **Create `CredentialEntryScreen`**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/ui/CredentialEntryScreen.kt`
    *   **Components:**
        *   TextFields for keys (R1.300).
        *   "Paste JSON" button (R1.150).
        *   "Help" link -> Modal Bottom Sheet (R1.060).
        *   "Validate" button (Async trigger).
    *   **Validation:** `read_file`.

3.  **Setup Navigation**
    *   **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/OnboardingNavigation.kt` (New file).
    *   **Content:** `OnboardingGraph` composable defining routes.
    *   **Validation:** `read_file`.

## Phase 3: Integration & Verification
**Goal:** Wire it up and verify.

1.  **Integrate into `MainActivity`**
    *   **File:** `app/src/main/kotlin/com/locus/android/MainActivity.kt`
    *   **Action:** Add `OnboardingGraph` to the main `NavHost`.
    *   **Validation:** `read_file` to verify the edit.

2.  **Automated Verification**
    *   **Action:** Run `./gradlew :app:testDebugUnitTest` (Logic).
    *   **Action:** Run `./gradlew :app:lintDebug` (Style/Issues).

3.  **Manual Verification (Proxy)**
    *   **Action:** Since I cannot physically tap the screen, I will rely on the Unit Tests and the successful compilation/linting as the definition of done for verification.

## Completion Criteria
*   Unit tests pass for `OnboardingViewModel`.
*   Files exist and content matches requirements (R1.* specs).
*   Lint check passes.
