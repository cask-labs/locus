# Report: Analysis of Admin Upgrade Plan

**File:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`
**Timestamp:** 20260110170222
**Author:** Jules

## Executive Summary
The proposed Admin Upgrade plan (`11-admin-upgrade-plan.md`) correctly identifies the need for a unified CloudFormation template and in-place updates. However, deep analysis reveals critical technical gaps in how AWS stack updates are handled, how credentials are securely managed during background processing, and the efficacy of the proposed IAM policy for cross-device discovery.

## Findings & Resolutions

### 1. Stack Name vs. Device Name Ambiguity
**Finding:** The plan proposes persisting `deviceName` in `RuntimeCredentials` to reconstruct the stack name. However, `locus-stack.yaml` defines a `StackName` *parameter* which is distinct from the CloudFormation *Stack ID* or *Name*. While `locus-user-${StackName}` constructs the IAM User, the CloudFormation Stack itself might be named differently (e.g., `locus-Pixel7`).
**Justification:** `CloudFormationClient.updateStack` requires the exact CloudFormation Stack Name (or ID) to target the resource. Relying on a display name (`deviceName`) risks targeting errors or failures if the user modified the name during initial setup or if normalization rules differ.
**Resolution:** Explicitly add a `stackName` field to `RuntimeCredentials` (and its DTO/Entity). This field should store the authoritative AWS Stack Name used during creation/discovery, ensuring 100% reliable targeting for updates.

### 2. Insecure Credential Passing to Worker
**Finding:** The plan implies passing Bootstrap Keys (Secret Key) to the `ProvisioningWorker` via `InputData` or a similar mechanism implicitly ("Validate keys -> Dispatch ProvisioningWorker").
**Justification:** `WorkManager` persists `InputData` to disk. Storing high-privilege Secret Keys in plain text on disk is a security vulnerability.
**Resolution:** The plan must explicitly mandate the use of `SecureStorage` for passing these credentials.
*   **Step:** ViewModel saves Bootstrap Keys to `SecureStorage` (e.g., as `BootstrapCredentials`).
*   **Step:** Worker is enqueued with a simple flag (e.g., `mode=ADMIN_UPGRADE`).
*   **Step:** Worker retrieves keys from `SecureStorage`.
*   **Step:** Worker clears keys from `SecureStorage` upon completion.

### 3. IAM Policy Limitations for Object Access
**Finding:** The plan proposes allowing `s3:GetObject` on resources conditional on `s3:ResourceTag/LocusRole: DeviceBucket`.
**Justification:** In AWS IAM, the `s3:GetObject` action targets the *Object*, not the *Bucket*. Standard S3 objects do not inherit tags from their parent bucket for the purpose of IAM `ResourceTag` conditions. Therefore, an Admin policy restricted by `s3:ResourceTag` will fail to grant read access to objects unless every object is individually tagged (which is not implemented and costly).
**Resolution:** Refine the IAM strategy.
*   **Discovery:** `s3:ListBucket` *can* use `s3:ResourceTag` (targeting the bucket). This restricts *visibility* to valid Locus buckets.
*   **Access:** Grant `s3:GetObject` on `*` (or a broader scope). Since the Admin can only discover buckets they have list permission for, and they are the Account Owner, this broader read permission is acceptable and necessary. Alternatively, rely on the fact that `s3:ListBucket` is the primary gatekeeper for the application's "Discovery" feature.

### 4. CloudFormation Update Parameter Completeness
**Finding:** `CloudFormationClient.updateStack` replaces the parameter list. The plan mentions passing `IsAdmin="true"` but does not explicitly state that the original `StackName` parameter must be re-supplied.
**Justification:** Failure to include the `StackName` parameter (or specifying `UsePreviousValue`) during `updateStack` will cause the update to fail or revert to default (if one existed, which it doesn't).
**Resolution:** `UpgradeAccountUseCase` must explicitly include the `StackName` parameter (retrieved from `RuntimeCredentials.stackName` or `deviceName`) in the update request payload: `mapOf("IsAdmin" to "true", "StackName" to currentStackName)`.

### 5. Redundant Key Rotation
**Finding:** The plan suggests calling `promoteToRuntimeCredentials` to "save the new keys".
**Justification:** The CloudFormation update is "in-place" and uses the existing `LocusUser` (logical ID unchanged). Therefore, the Access Key ID and Secret Key *do not change*. Rotating them unnecessarily introduces race conditions.
**Resolution:** Clarify that the "Upgrade" step strictly updates the local `isAdmin` flag in `RuntimeCredentials` and refreshes the in-memory `AuthState`. It should *not* attempt to replace the cryptographic keys unless the stack update forced a user replacement (which it shouldn't).

### 6. Restart UX
**Finding:** The plan proposes a "Soft Restart" of `MainActivity`.
**Justification:** This is jarring and unnecessary. The application architecture relies on reactive streams (`AuthState`).
**Resolution:** Update `AuthRepository` to emit the new `Authenticated(isAdmin=true)` state. The Settings screen and Map screen should observe this state and reactively reveal Admin features (e.g., "All Devices" filter) without restarting the Activity.

## Conclusion
The Admin Upgrade is a high-risk operation involving IAM changes. The plan must be updated to address these security (Credential Storage) and reliability (IAM Policy, Stack Naming) issues before implementation begins.
