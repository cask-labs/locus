# Report: Analysis of Admin Upgrade Plan

**File:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`
**Timestamp:** 20260110_125753

## Findings

### 1. Critical: Missing Resource Discovery Permissions
**Finding:** The plan proposes updating the IAM policy to allow `s3:ListBucket` on resources tagged with `LocusRole: DeviceBucket`. While this allows *accessing* a bucket if its name is known, it does not allow *discovering* the buckets in the account.
**Justification:** The AWS S3 `ListBucket` action operates on a specific bucket resource. To implement an "Admin Dashboard" that lists all devices (as implied by the Admin Upgrade feature), the application needs to find the names of all buckets tagged `DeviceBucket`. This requires the `tag:GetResources` permission. Without this, the Admin features (viewing other devices) will be non-functional unless bucket names are manually entered.
**Resolution:** Modify the `locus-stack.yaml` change plan to include `tag:GetResources` (Resource Groups Tagging API) in the `LocusPolicy` statement, conditional on `AdminEnabled`.

### 2. Documentation: Spec Divergence
**Finding:** The plan correctly decides to use a single template (`locus-stack.yaml`) with a conditional `IsAdmin` parameter to preserve the Stack's lifecycle. However, the Behavioral Specification (`01_onboarding_identity.md`, R1.2200) explicitly mandates using a separate `locus-admin.yaml`.
**Justification:** Following the spec literally would cause CloudFormation to see the "Admin" stack as a new stack (or require complex template replacement logic), risking `LocusDataBucket` deletion/replacement if the Logical IDs don't match perfectly. The "Single Template" approach is safer and better practice.
**Resolution:** The plan is correct, but the specification is now out of date. The implementation PR should include an update to `docs/behavioral_specs/01_onboarding_identity.md` (R1.2200) to reflect the "Single Template with Parameter" strategy.

### 3. Architecture: Process State vs. Soft Restart
**Finding:** The plan suggests a "Soft Restart" (restarting `MainActivity`) to handle the credential upgrade.
**Justification:** Restarting an Activity does not reset the Application process or Hilt Singletons. `AuthRepository` might hold a cached `AuthState` with the old `RuntimeCredentials` in its `StateFlow`.
**Resolution:** Ensure `UpgradeAccountUseCase` explicitly updates the `AuthRepository` (e.g., via a `refreshCredentials()` or `promoteToRuntimeCredentials()` method) to emit the new `Authenticated` state with `isAdmin=true`. The "Soft Restart" is acceptable for UI reset but must not be relied upon for data layer consistency.

## Proposal

I recommend revising the implementation plan to address these findings, specifically adding the `tag:GetResources` permission and ensuring the repository state is correctly refreshed. I will submit these findings as a PR.
