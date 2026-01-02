# Onboarding UI Execution Plan Analysis

**Date:** 2026-01-02
**Subject:** Discrepancy Analysis of Task 9 (Onboarding UI) Plan vs. Authoritative Specifications

## Executive Summary
A deep analysis of `agents/ephemeral/phase1-onboarding/09-onboarding-ui-execution-plan.md` was performed against the authoritative documentation (`docs/technical_discovery/specs/ui/onboarding.md` and `docs/behavioral_specs/01_onboarding_identity.md`).

Several critical discrepancies were identified that would have resulted in non-compliant implementation if execution had proceeded as originally planned.

## Findings

### 1. UI Presentation Discrepancy (Provisioning)
*   **Original Plan:** Proposed a simple `CircularProgressIndicator` with a generic message for the provisioning screen.
*   **Authoritative Spec:** `docs/technical_discovery/specs/ui/onboarding.md` (Section 3.7) explicitly mandates a **"Detailed Log-Style List"**.
    *   **Requirement:** The UI must display specific steps (e.g., "Validating CloudFormation Template", "Creating Storage Stack", "Provisioning Resources") with status indicators (Pending, In Progress, Success).
*   **Impact:** The original plan would have failed to meet the user feedback requirements defined in the UI specifications.

### 2. User Flow Omission (Permissions Phase)
*   **Original Plan:** Routed the user directly from `Provisioning Success` to the `Dashboard`.
*   **Authoritative Spec:** `docs/behavioral_specs/01_onboarding_identity.md` (R1.1550) and `docs/technical_discovery/user_flows/onboarding.md` mandate a **"Two-Step Dance"** for location permissions (Foreground -> Background) *after* provisioning and *before* the Dashboard.
*   **Impact:** Critical functionality (Tracking) would have failed or violated Android 14+ permission policies, as the user would arrive at the Dashboard without granting necessary permissions.

### 3. Architecture/Persistence Gap (The "Setup Trap")
*   **Original Plan:** Relied on transient runtime state (`ViewModel`) to manage the flow.
*   **Authoritative Spec:** `docs/technical_discovery/specs/ui/onboarding.md` (Section 2) requires a **"State Machine Persister"** using `EncryptedSharedPreferences`.
    *   **Requirement:** The system must "trap" the user in the onboarding flow (Provisioning or Permissions) even if the application is killed and relaunched (Process Death).
*   **Impact:** Without this persistence, a user could bypass the mandatory setup or permission steps by simply restarting the app.

### 4. Domain Model Insufficiency
*   **Finding:** The existing `ProvisioningState` (Deploying, Success, Failure) was too coarse to support the required "Detailed Log-Style List".
*   **Resolution Required:** The domain model needs to be granularized to include states like `ValidatingTemplate`, `CreatingStack`, `GeneratingKeys`, etc., to drive the specific UI elements.

## Recommendations for Execution
1.  **Update Domain Layer:** Refactor `ProvisioningState` to include granular progress steps.
2.  **Implement Persistence:** Add `isOnboardingComplete` flags in `ConfigurationRepository` to enforce the "Trap".
3.  **Revise UI Plan:** Build the `ProvisioningScreen` with the required list adapter/layout and create the missing `PermissionForegroundScreen` and `PermissionBackgroundScreen`.
4.  **Update Navigation:** Insert the Permission screens into the nav graph between Provisioning and Dashboard.
