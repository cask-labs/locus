# Implementation Plan - Task 1: Documentation & UI Foundation

**Goal:** Fix architectural inconsistencies in documentation and establish the Material Design 3 foundation (Theme, Color, Type) required for UI development.

## Prerequisites: Human Action Steps
*None required.*

## Implementation Steps

### Step 1: Align Documentation
**Goal:** Remove conflicting "Foreground Service" references from UI requirements to match the "WorkManager" architecture decision.

**Action:**
- Edit `docs/requirements/ui_feedback.md`.
- Locate the "Provisioning Status" section (5.5).
- Replace the phrase "Foreground Service" with "High Priority Background Task".

**Verification:**
- Run `grep "Foreground Service" docs/requirements/ui_feedback.md` and ensure it returns no results (or only relevant ones if any remain, but the conflicting one should be gone).

### Step 2: Establish UI Tokens (Color & Type)
**Goal:** Define the primitive design tokens for the application, strictly following `docs/technical_discovery/design_guidelines.md`.

**Action:**
- Create `app/src/main/kotlin/com/locus/android/ui/theme/Color.kt`:
  - Define a static fallback palette with a "Locus Blue" primary (e.g., `#2196F3`) for Android < 12 (Agent Recommendation).
  - Define Light and Dark scheme containers for this fallback palette.
- Create `app/src/main/kotlin/com/locus/android/ui/theme/Type.kt`:
  - Define standard Material 3 Typography using the default system font family (Roboto).

**Verification:**
- `ls app/src/main/kotlin/com/locus/android/ui/theme/Color.kt`
- `ls app/src/main/kotlin/com/locus/android/ui/theme/Type.kt`

### Step 3: Implement App Theme
**Goal:** Create the root `LocusTheme` composable.

**Action:**
- Create `app/src/main/kotlin/com/locus/android/ui/theme/Theme.kt`:
  - Implement `LocusTheme` function.
  - **Requirement:** Use `dynamicLightColorScheme(context)` and `dynamicDarkColorScheme(context)` on Android 12+ (API 31+).
  - **Requirement:** Fallback to the static "Locus Blue" scheme on older versions.
  - Support Dark/Light mode switching (isSystemInDarkTheme).
  - Apply `MaterialTheme` using the colors and typography defined in Step 2.

**Verification:**
- `ls app/src/main/kotlin/com/locus/android/ui/theme/Theme.kt`

### Step 4: Final Verification
**Goal:** Ensure the changes do not break the build.

**Action:**
- Run `./gradlew :app:assembleDebug` to verify that the new theme files compile correctly and are accessible.

**Verification:**
- Build succeeds with `BUILD SUCCESSFUL`.

## Spec Alignment

| Component | Spec Requirement |
| :--- | :--- |
| `ui_feedback.md` | Fixes conflict with `background_processing_spec.md` (Provisioning Worker) |
| `LocusTheme` | Required foundation for `01_onboarding_identity.md` screens |
| `Color.kt` | "Subtle by Default" & "Dark Mode" support (`design_guidelines.md`) |
| `Type.kt` | "Standard Material Type Scale" (`design_guidelines.md`) |

## Validation Strategy
- **Manual Verification:** Check file existence and grep for documentation strings.
- **Build Verification:** Ensure the app compiles with the new theme files.

## Definition of Done
- [ ] `ui_feedback.md` no longer references "Foreground Service" for provisioning.
- [ ] `com.locus.android.ui.theme` package exists and contains `Color.kt`, `Type.kt`, `Theme.kt`.
- [ ] `LocusTheme` uses Dynamic Colors on API 31+ and Locus Blue fallback on older versions.
- [ ] `./gradlew :app:assembleDebug` passes.
