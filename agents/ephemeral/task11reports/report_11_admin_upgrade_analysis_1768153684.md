# Task 11 Analysis: Admin Upgrade Plan

## Findings

### 1. Critical Gap: CloudFormation Template and Output Key Mismatch
**Severity:** Critical (Blocker)
**Location:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md` -> Section 2 (Data Layer)
**Analysis:**
A deep analysis of the existing codebase (`RecoverAccountUseCase.kt`, `InfrastructureConstants.kt`, and `locus-stack.yaml`) reveals multiple critical mismatches that will cause both Provisioning and Recovery to fail immediately:

1.  **Output Key Mismatch:**
    - `InfrastructureConstants.kt` defines `OUT_RUNTIME_ACCESS_KEY = "RuntimeAccessKeyId"` and `OUT_RUNTIME_SECRET_KEY = "RuntimeSecretAccessKey"`.
    - `locus-stack.yaml` defines outputs as `AccessKeyId` and `SecretAccessKey`.
    - **Impact:** The application will fail to parse the stack outputs, resulting in a "Deployment Failed: Invalid stack outputs" error during any provisioning attempt.

2.  **Recovery Mismatch (Link Existing Bucket):**
    - `RecoverAccountUseCase.kt` passes a `"BucketName"` parameter to the stack creation process, intending to link an existing bucket.
    - `locus-stack.yaml` **does not accept** a `BucketName` parameter.
    - **Impact:** CloudFormation will likely fail due to unknown parameters, or if it proceeds, it will create a **new** bucket (as defined in the template `Resources`), ignoring the user's intent to link an existing store. This violates requirement **R1.1100**.

**Resolution:**
The plan must be expanded to explicitly fix these infrastructure defects in Step 2:
- **Output Keys:** Standardize on the keys defined in `locus-stack.yaml` (`AccessKeyId`, `SecretAccessKey`) by updating `InfrastructureConstants.kt`, OR update the YAML to match the Kotlin constants. Updating the YAML is safer to avoid breaking binary compatibility if we had legacy users, but since we don't, updating `InfrastructureConstants.kt` to match the simpler YAML keys is preferred.
- **Template Logic:** Update `locus-stack.yaml` to:
    - Add an optional `BucketName` parameter.
    - Add a `CreateNewBucket` condition (`!Equals [!Ref BucketName, ""]`).
    - Condition the `LocusDataBucket` resource on `CreateNewBucket`.
    - Update `LocusPolicy` and `Outputs` to reference `!If [CreateNewBucket, !Ref LocusDataBucket, !Ref BucketName]`.

### 2. Gap: Missing Error Handling for Background Worker
**Severity:** Medium
**Location:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md` -> Section 4 (UI Implementation)
**Analysis:**
The plan details the "Happy Path" where the UI observes `authState` for success (Step 14). However, the upgrade process is executed by `ProvisioningWorker` (Step 12), which runs asynchronously. If the worker fails (e.g., CloudFormation rollback), the `authState` will simply remain `Authenticated(isAdmin=false)`, potentially leaving the user without feedback.
**Resolution:**
Update Step 12/14 to include:
- `AdminUpgradeViewModel` must observe the `WorkInfo` of the dispatched worker.
- Handle `WorkInfo.State.FAILED` by extracting the error message and displaying it via a Snackbar or Dialog.

### 3. Confirmation: Tag-Based Discovery Strategy
**Severity:** Positive Confirmation
**Analysis:**
The plan proposes adding a `LocusStackName` tag to the bucket. While `RecoverAccountUseCase` currently uses `aws:cloudformation:stack-name` (via `TAG_STACK_NAME`), explicitly defining a project-controlled tag is a robust enhancement that decouples our logic from AWS internal behavior. This is approved.

### 4. Minor: Settings Entry Point
**Severity:** Low
**Analysis:**
The plan proposes adding a Settings icon to `DashboardScreen` (Step 13). Verification confirms that the persistent Bottom Navigation Bar is **not yet implemented** (MainActivity switches directly between Onboarding and Dashboard). Therefore, this is a necessary temporary measure to expose the Settings/Upgrade feature.

## Conclusion
The Admin Upgrade plan is strategically sound but sits on top of a currently broken infrastructure layer (Provisioning/Recovery). The plan must be expanded to include the necessary fixes to `locus-stack.yaml` and `InfrastructureConstants.kt` to ensure the underlying platform is functional before building the upgrade feature upon it.
