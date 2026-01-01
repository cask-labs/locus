# Plan Review: Provisioning Worker Implementation

**Target Plan:** `agents/ephemeral/phase1-onboarding/07-provisioning-worker-plan.md`
**Reviewer:** Jules (AI Agent)
**Date:** 2024-05-24

## Executive Summary

The implementation plan for the `ProvisioningWorker` is generally sound and aligns with the project's architectural goals. However, several critical areas require refinement to ensure robustness, security, and a better user experience. Specifically, the notification strategy, retry policies, and repository contracts need clarification.

## Findings & Recommendations

### 1. Notification Channel Selection

**Problem:**
The plan proposes using `channel_tracking` for the Provisioning Worker's foreground notification.
> Channel: `channel_tracking`
> Title: `Locus • Setup`

**Impact:**
`channel_tracking` is defined as "Low Importance" (Silent, Minimized) in `docs/technical_discovery/specs/ui/notifications.md`. Provisioning is a critical, user-initiated setup process. If the user backgrounds the app, a "Silent" notification might be buried, leading the user to think the process has stalled or finished silently.

**Recommendation:**
*   **Resolution:** Use a **High Importance** channel (or at least `IMPORTANCE_DEFAULT`) for this specific worker to ensure visibility.
*   **Action:**
    *   Update the plan to specify `channel_alerts` (if appropriate for non-error alerts) or define a new channel `channel_setup` with `IMPORTANCE_DEFAULT`.
    *   Alternatively, explicit justification is needed for why "Subtle by Default" applies to the *active setup* phase.

### 2. Retry Policy Ambiguity

**Problem:**
The plan states: "Result: Return Result.success() or Result.failure()". It does not address `Result.retry()`.

**Impact:**
CloudFormation provisioning involves network calls that are susceptible to transient failures (timeouts, temporary connectivity loss). Returning `Result.failure()` immediately for a transient error would abort the entire setup, forcing the user to restart the process manually.

**Recommendation:**
*   **Resolution:** Define a clear Retry Policy for transient errors.
*   **Action:**
    *   Update the plan logic to differentiate between **Permanent Errors** (Auth Failed, Stack Already Exists) and **Transient Errors** (Network Timeout, 503 Service Unavailable).
    *   For Transient Errors, return `Result.retry()` with a defined backoff policy (e.g., Exponential Backoff).
    *   For Permanent Errors, return `Result.failure()` to trigger the UI error state.

### 3. AuthRepository Contract

**Problem:**
The plan correctly identifies that `AuthRepository` is missing `getBootstrapCredentials()`, but the security implications of this method need to be explicit.

**Impact:**
`BootstrapCredentials` are high-privilege keys. Exposing them via a simple getter could be risky if misused.

**Recommendation:**
*   **Resolution:** Ensure the implementation of `getBootstrapCredentials()` includes safeguards or documentation warning against its use outside of the `ProvisioningWorker`.
*   **Action:**
    *   Confirm the method signature in the plan: `suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>`.
    *   Add a note to the plan to verify that this method returns a `Failure` if the state is not `SetupPending`.

### 4. Dependency Verification

**Problem:**
The plan includes a step: "Verify/Update: Ensure `@HiltWorker` annotation is used".

**Status:**
I have verified `app/build.gradle.kts` and confirmed that `androidx.hilt:hilt-work` and `androidx.work:work-runtime-ktx` are already present.

**Recommendation:**
*   **Resolution:** This step is technically complete but good to keep as a checklist item.
*   **Action:** Mark as "Verified" in the plan notes.

### 5. Requirement Mapping

**Problem:**
The plan maps to `R1.600` and `R1.1350`.

**Status:**
These appear to be valid Requirement IDs based on the project structure, likely located in `docs/requirements/onboarding_identity_spec.md`.

**Recommendation:**
*   **Action:** Ensure the implementation explicitly comments these IDs in the code (e.g., `// R1.600: Background Provisioning`) to maintain traceability.

## Proposed Plan Updates

I recommend updating `07-provisioning-worker-plan.md` with the following specific changes:

1.  **Modify Step 2 (Logic):**
    *   Change Notification Channel to `channel_setup` (new) or justify `channel_tracking`.
    *   Add logic branch: "If error is Transient -> Return `Result.retry()`".
2.  **Modify Step 1 (AuthRepository):**
    *   Explicitly state that `getBootstrapCredentials` should validate that the app is in the `SetupPending` state.
