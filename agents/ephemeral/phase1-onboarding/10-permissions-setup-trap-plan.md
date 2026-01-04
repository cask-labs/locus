# Task 10 Implementation Plan: Permissions & Setup Trap

**Goal:** Implement the "Permission Trap" and finalize the Onboarding flow, ensuring the user cannot enter the main application without granting necessary Location permissions. It adheres to Android 11+ (API 30+) best practices for two-stage location requests and ensures Android 14+ compatibility.

## Prerequisites
- **Human Actions:** None.
- **Dependencies:** `AuthRepository` (Implemented), `MainActivity` routing (Implemented).

## Alignment Mapping
| Requirement ID | Requirement Summary | Implementation Component |
| :--- | :--- | :--- |
| **R1.1550** | Request permissions in two distinct stages (Foreground then Background). | `PermissionViewModel` (Sequence logic) + `PermissionScreen` (Two UI steps). |
| **R1.1555** | If Foreground denied, inform user and do not request Background. | `PermissionViewModel` (Stops flow on denial) + `PermissionScreen` (Shows "Permission Required" state). |
| **R1.1560** | **Permission Trap:** Force user back to permission screen on next launch. | `MainActivity` (Routes based on `PERMISSIONS_PENDING` persisted state) + `PermissionScreen` (Blocking UI). |
| **R1.1900** | **Setup Trap:** Restore last known provisioning state. | `AuthRepository` (Persists `OnboardingStage`) + `MainActivity`. |
| **R1.1600** | Manual confirmation ("Go to Dashboard"). | `PermissionViewModel` handles the final transition to `COMPLETE` after permissions are granted. |
| **R1.1800** | **Start Tracking:** Automatically start services upon Dashboard entry. | `MainActivity` (Triggers `StartTrackingUseCase`). |
| **Architecture**| Android 14 FG Service Compliance | Manifest includes `FOREGROUND_SERVICE_LOCATION` and `FOREGROUND_SERVICE_DATA_SYNC`. |
| **Architecture**| Android 13 Notification Compliance | `PermissionViewModel` requests `POST_NOTIFICATIONS` (Optional). |

## Implementation Steps

### Phase 1: Manifest, Repository & ViewModel
**Goal:** Declare permissions, fix state recovery logic, and encapsulate UI logic.

1.  **Update `AndroidManifest.xml`**
    -   Add `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`.
    -   Add `FOREGROUND_SERVICE_LOCATION` (Android 14 compliance for Tracker).
    -   Add `FOREGROUND_SERVICE_DATA_SYNC` (Android 14 compliance for Provisioning).
    -   Add `POST_NOTIFICATIONS` (Android 13 compliance).
    -   *Verification:* Read Manifest file.

2.  **Update `AuthRepositoryImpl` (State Correction)**
    -   **Self-Healing Logic:** Implement in `initialize()`. Check for inconsistent state.
        -   *Logic:* `if (authState == Authenticated && onboardingStage != COMPLETE) { onboardingStage = PERMISSIONS_PENDING }`.
        -   *Justification:* Ensures users who finished provisioning but didn't complete permissions are "trapped" back to the permission flow, without resetting fully onboarded users.
    -   *Verification:* Read Repository file.

3.  **Create `PermissionViewModel.kt`** (`features/onboarding/viewmodel` - create dir if missing)
    -   Inject `AuthRepository`.
    -   State: `PermissionUiState` (ForegroundPending, BackgroundPending, DeniedForever, Granted).
    -   **Logic:**
        -   **Precision:** Enforce `ACCESS_FINE_LOCATION`. Treat "Coarse Only" as a denial/education state.
        -   **Notifications:** Request `POST_NOTIFICATIONS` alongside Foreground Location (API 33+). **Mark as Optional**: Proceed even if denied.
        -   **Two-Stage:** Request FG -> If Granted, Request BG.
        -   **Completion:** Call `completeOnboarding()` (updates repo to `COMPLETE`).
        -   **Resume:** `onResume` checks system permissions to auto-advance.
    -   *Verification:* Read ViewModel file.

### Phase 2: UI Implementation (Compose)
**Goal:** Build the two-stage permission screen with educational context.

4.  **Update `PermissionScreen.kt`** (`features/onboarding/ui`)
    -   Refactor to use `PermissionViewModel`.
    -   Composables:
        -   `ForegroundPermissionContent`: Explains tracking & alerts. Button -> `launcher.launch(FINE + COARSE + NOTIFICATIONS)`.
        -   `BackgroundPermissionContent`: Explains "Always Allow".
            -   **UX:** Button first attempts `launcher.launch(ACCESS_BACKGROUND_LOCATION)`.
            -   **Fallback:** If denied or system ignores (`shouldShowRequestPermissionRationale` indicates permanent denial), launch `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.
        -   `PermanentDenialContent`: Explains the app is blocked. Button -> Open App Settings.
    -   Lifecycle: Observe `ON_RESUME` to trigger `viewModel.checkPermissions()`.
    -   *Verification:* Read UI file.

### Phase 3: Integration, Routing & Services
**Goal:** Connect the UI to the App Navigation, enforce the trap, and ensure services start.

5.  **Create `TrackerService.kt`** (Stub if missing)
    -   Create a basic `ForegroundService` in `com.locus.android.services` (or appropriate package).
    -   Implement `start()` helper method or Companion Object Intent factory.
    -   *Verification:* Read Service file.

6.  **Create `StartTrackingUseCase`** (`core/domain/usecase`)
    -   **Goal:** Encapsulate the logic for starting background processes.
    -   **Logic:**
        -   Invoke `TrackerService.start()`.
        -   Schedule `WatchdogWorker` (if applicable/stubbed).
    -   *Verification:* Read UseCase file.

7.  **Update `OnboardingDestinations.kt`** (Navigation)
    -   Update `PERMISSIONS` composable route to use `PermissionViewModel`.
    -   Connect `onPermissionsGranted` -> `viewModel.completeOnboarding()`.
    -   *Verification:* Read Navigation file.

8.  **Update `MainActivity.kt`**
    -   **Routing Verification:** Ensure `PERMISSIONS_PENDING` stage maps to `OnboardingDestinations.PERMISSIONS`.
    -   **Service Trigger:** Add `LaunchedEffect` monitoring the transition to `COMPLETE`.
        -   Action: Invoke `StartTrackingUseCase`.
    -   *Verification:* Read MainActivity file.

### Phase 4: Verification & Testing
**Goal:** Ensure robustness across API levels and user behaviors.

9.  **Create `PermissionViewModelTest.kt`**
    -   Test flows:
        -   Coarse Grant -> Treated as pending/failure.
        -   Foreground Grant -> Background Pending.
        -   Notification Denial -> Proceeds to Background/Complete (Non-blocking).
        -   All Granted -> Complete.
    -   *Verification:* Read Test file.

10. **Execute Tests**
    -   Run `./gradlew testDebugUnitTest`.

## Completion Criteria
- Unit tests pass.
- Manifest contains all required permissions and FG service types.
- "Self-Healing" logic correctly targets only incomplete setups.
- Services (Tracker/Watchdog) start immediately upon reaching the Dashboard via `StartTrackingUseCase`.
- Precise Location is mandatory; Notifications are optional.
