# Report: Comprehensive Analysis of Admin Upgrade Plan

**Analysis Date:** 2024-05-21
**Source Document:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`
**Scope:** Deep analysis of the proposed Admin Upgrade plan, incorporating user feedback ("Takeover" recovery intent) and identifying architectural gaps.

## Executive Summary
The original plan requires significant structural changes to be safe and robust. The primary shift is from a "Linker" model (create new stack) to a **"Takeover" model (update existing stack)** for recovery. This simplifies the architecture but necessitates strict handling of CloudFormation parameters to prevent data loss and ensure security.

**Key Recommendations:**
1.  **Remove `BucketName` Parameter:** It is a safety hazard that can cause bucket deletion.
2.  **Add `KeySerial` Parameter:** Required to force IAM Credential rotation during recovery.
3.  **Use ABAC for Admin Permissions:** Replace broad wildcards with Attribute-Based Access Control.
4.  **Expose `IsAdmin` Output:** Essential for restoring application state after recovery.

---

## Detailed Findings

### 1. Safety: Removal of `BucketName` Parameter
**Severity:** Critical
**Finding:** The `BucketName` parameter in `locus-stack.yaml` creates a conditional logic path where providing a bucket name (intended for linking) causes the template to **remove** the `LocusDataBucket` resource definition. If this parameter is inadvertently passed during an `UpdateStack` operation (e.g., during Admin Upgrade or Recovery), CloudFormation will **DELETE** the user's S3 bucket.
**Resolution:**
- **Remove the `BucketName` parameter** and the `CreateNewBucket` condition entirely.
- The `LocusDataBucket` resource must be unconditional.
- This aligns with the "Stack Owns Bucket" philosophy: the stack definition always includes the bucket it created.

### 2. Architecture: "Takeover" Strategy & `KeySerial`
**Severity:** Critical
**Finding:** The "Takeover" recovery strategy (updating the existing stack) is the correct approach to fulfill user intent. However, performing an `UpdateStack` with identical parameters does not rotate the IAM Access Keys. If a user is recovering because of compromised or lost keys, they will not receive new credentials, leaving the account vulnerable or inaccessible.
**Resolution:**
- **Add `KeySerial` Parameter:** Add a parameter to `locus-stack.yaml` (Default: "1") and append it to the `UserName` resource name (e.g., `locus-user-${StackName}-${KeySerial}`).
- **Recovery Logic:** `RecoverAccountUseCase` must generate a new UUID/Serial and pass it to `UpdateStack`. This forces CloudFormation to replace the `AWS::IAM::User` resource, generating fresh Access Keys and invalidating the old ones.
- **Upgrade Logic:** `UpgradeAccountUseCase` must **preserve** the existing `KeySerial` (by reading stack parameters first) to avoid accidental key rotation that would lock out the running application.

### 3. Persistence: `IsAdmin` Output
**Severity:** High
**Finding:** The plan adds an `IsAdmin` parameter but fails to expose it as an **Output**. When a user recovers their account on a new device, the app logic (`RecoverAccountUseCase`) reads stack outputs to populate `RuntimeCredentials`. If `IsAdmin` is not in the outputs, the recovered device will default to "Standard" mode, losing the Admin capabilities previously paid for/configured.
**Resolution:**
- **Add Output:** Add `IsAdmin` to the `Outputs` section of `locus-stack.yaml`.
- **Logic:** Update `RecoverAccountUseCase` to read this output and populate `RuntimeCredentials.isAdmin`.

### 4. Security: ABAC for Admin Permissions
**Severity:** High
**Finding:** The requirement for Admin users to "discover other device buckets" implies a need for cross-bucket permissions. The current plan mentions `tag:GetResources` but is vague on `s3:ListBucket` or `s3:GetObject`. Granting `s3:GetObject` on `*` (wildcard) is overly permissive and dangerous.
**Resolution:**
- **Implement ABAC:** Use Attribute-Based Access Control in the IAM Policy.
- **Condition:** Allow `s3:GetObject` and `s3:ListBucket` on `Resource: *` **IF AND ONLY IF** the target resource has the tag `LocusRole=DeviceBucket`.
- **Snippet:**
  ```yaml
  Condition:
    StringEquals:
      s3:ResourceTag/LocusRole: DeviceBucket
  ```
- This securely restricts Admin access to only Locus-managed resources.

### 5. Implementation: `UpdateStack` Handling
**Severity:** Medium
**Finding:** The `StackProvisioningService` currently supports `createAndPollStack`. It needs a robust `updateAndPollStack` counterpart.
- **Success Criteria:** Must handle `UPDATE_COMPLETE` and `UPDATE_COMPLETE_CLEANUP_IN_PROGRESS`.
- **No-Op:** Must handle "No updates are to be performed" as a success case (returning existing outputs).
- **Return Values:** Must return the **new** stack outputs (Access Keys) to the caller to support the Key Rotation requirement.

### 6. Logic: Upgrade Flow Safety
**Severity:** Medium
**Finding:** The `UpgradeAccountUseCase` must be designed to be idempotent and non-destructive.
- It should first call `GetStack` to retrieve current parameters (specifically `KeySerial`).
- It should then call `UpdateStack` with `IsAdmin=true` and the *preserved* `KeySerial`.
- This ensures the upgrade only toggles the feature flag without rotating credentials or affecting other stack properties.

## Revised Plan Summary

1.  **Infrastructure:**
    - Remove `BucketName`.
    - Add `KeySerial` (User Suffix).
    - Add `IsAdmin` Output.
    - Update IAM Policy with ABAC conditions.
2.  **Domain:**
    - `RecoverAccountUseCase`: Scan Tag -> Get Stack Name -> Generate New Serial -> `UpdateStack` -> Save New Creds.
    - `UpgradeAccountUseCase`: Get Stack -> Read Serial -> `UpdateStack` (IsAdmin=true) -> Save Updated Creds.
3.  **Service:**
    - Implement `StackProvisioningService.updateAndPollStack`.
