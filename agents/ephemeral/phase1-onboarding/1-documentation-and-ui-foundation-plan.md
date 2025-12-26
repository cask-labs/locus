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
**Goal:** Define the primitive design tokens for the application.

**Action:**
- Create `app/src/main/kotlin/com/locus/android/ui/theme/Color.kt`:
  - Define standard Material 3 color palette (Purple/Blue baseline).
  - Include Light and Dark scheme definitions.
- Create `app/src/main/kotlin/com/locus/android/ui/theme/Type.kt`:
  - Define standard Material 3 Typography using default font family.

**Verification:**
- `ls app/src/main/kotlin/com/locus/android/ui/theme/Color.kt`
- `ls app/src/main/kotlin/com/locus/android/ui/theme/Type.kt`

### Step 3: Implement App Theme
**Goal:** Create the root `LocusTheme` composable.

**Action:**
- Create `app/src/main/kotlin/com/locus/android/ui/theme/Theme.kt`:
  - Implement `LocusTheme` function.
  - Support Dynamic Colors (Material You) for Android 12+ (API 31+).
  - Support Dark/Light mode switching.
  - Apply `MaterialTheme` using the colors and typography defined in Step 2.

**Verification:**
- `ls app/src/main/kotlin/com/locus/android/ui/theme/Theme.kt`

## Spec Alignment

| Component | Spec Requirement |
| :--- | :--- |
| `ui_feedback.md` | Fixes conflict with `background_processing_spec.md` (Provisioning Worker) |
| `LocusTheme` | Required foundation for `01_onboarding_identity.md` screens |
| `Color.kt` | "Subtle by Default" & "Dark Mode" support (Design Guidelines) |

## Validation Strategy
- **Manual Verification:** Check file existence and grep for documentation strings.
- **Build Verification:** Ensure the app compiles with the new theme files (implicitly checked by pre-commit).

## Definition of Done
- [ ] `ui_feedback.md` no longer references "Foreground Service" for provisioning.
- [ ] `com.locus.android.ui.theme` package exists and contains `Color.kt`, `Type.kt`, `Theme.kt`.
- [ ] `LocusTheme` supports Dynamic Colors.
