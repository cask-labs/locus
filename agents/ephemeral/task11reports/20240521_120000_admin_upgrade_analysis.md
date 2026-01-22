# Report: Analysis of Admin Upgrade Plan

**Analysis Date:** 2024-05-21
**Source Document:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`
**Scope:** Identify gaps or problems in the context of "No Legacy Users / No Existing Install Base", incorporating user feedback regarding Recovery intent.

## Findings

### 1. Critical Flaw: Data Deletion Risk in Upgrade Flow
**Severity:** Critical
**Description:** The CloudFormation template logic for preserving the bucket relies on the `BucketName` parameter being **empty** for the Stack that owns the bucket resource ("Owner").
- If `BucketName` is empty, `CreateNewBucket` is True, and the `LocusDataBucket` resource is active.
- If `BucketName` is provided (e.g. "my-bucket"), `CreateNewBucket` is False, and the `LocusDataBucket` resource is **removed** from the stack definition.
**Technical Impact:**
- Regardless of whether the user is an "Owner" (Standard) or a "Takeover" (Recovered), if the `UpgradeAccountUseCase` inadvertently passes the existing bucket name to the `BucketName` parameter during an `UpdateStack` operation, CloudFormation will attempt to **DELETE** the bucket.
- This is a counter-intuitive behavior where providing the bucket name causes it to be lost from the stack's management.
**Resolution:**
- The `UpgradeAccountUseCase` must explicitly pass `BucketName=""` (Empty String) when performing an upgrade on a stack that owns the bucket resource.
- This ensures the `LocusDataBucket` resource remains part of the stack definition.

### 2. Strategic Divergence: Linker (Current) vs. Takeover (User Intent)
**Severity:** Critical
**Description:**
- **Current Architecture:** The codebase (`RecoverAccountUseCase.kt`) currently implements a **"Linker"** strategy. It creates a *new* CloudFormation stack (`locus-user-UUID`) that links to the existing bucket. This new stack does *not* own the `LocusDataBucket` resource, meaning it cannot modify bucket-level properties (like adding tags or changing policies) required for the Admin Upgrade.
- **User Intent:** The user has clarified that "Recovery should assume an existing stack" and that they want to "assert which bucket/stack is theirs". This implies a **"Takeover"** strategy where the app reclaims ownership of the *original* stack (`locus-user-OldDevice`).
**Technical Impact:**
- If we proceed with the current "Linker" implementation, the Admin Upgrade will fail for recovered users because their stack lacks the `LocusDataBucket` resource to apply the required changes.
- To fulfill the user's intent, the Recovery logic must be fundamentally changed from `CreateStack` (New) to `UpdateStack` (Existing).
**Resolution:**
- **Pivot to Takeover Strategy:** Modify `RecoverAccountUseCase` to perform an `UpdateStack` operation on the existing stack name (discovered via the `aws:cloudformation:stack-name` tag on the bucket).
- **Key Rotation Requirement:** Since `UpdateStack` does not automatically rotate IAM Access Keys if the user resource is unchanged, we must force key rotation to invalidate the lost credentials.
    - **Mechanism:** Add a `KeySerial` or `UserSuffix` parameter to `locus-stack.yaml` (e.g., `UserName: !Sub "locus-user-${StackName}-${KeySerial}"`).
    - **Logic:** During recovery, generate a new UUID for `KeySerial` and pass it to `UpdateStack`. This forces CloudFormation to create a new `AWS::IAM::User` and `AWS::IAM::AccessKey`, effectively rotating credentials and ensuring the recovered user has exclusive access.

### 3. Confirmed Fix: Infrastructure Constants Mismatch
**Severity:** Validated
**Description:** The plan correctly identifies that `InfrastructureConstants.OUT_RUNTIME_ACCESS_KEY` (`RuntimeAccessKeyId`) mismatches the CloudFormation Output (`AccessKeyId`).
**Resolution:** The proposed fix in the plan is correct.

## Recommendations

1.  **Adopt Takeover Strategy for Recovery:**
    - Update `RecoverAccountUseCase` to:
        1. Scan for the `aws:cloudformation:stack-name` tag on the target bucket.
        2. Execute `stackProvisioningService.updateAndPollStack` targeting that stack name.
        3. Pass `BucketName=""` (to preserve bucket ownership/resource).
        4. Pass a new `KeySerial` (to force credential rotation).
2.  **Modify CloudFormation Template:**
    - Update `locus-stack.yaml` to include a `KeySerial` parameter (Default: "1") and append it to the `UserName` property.
    - This enables the safe "Takeover" of stacks.
3.  **Ensure Safe Upgrade Logic:**
    - `UpgradeAccountUseCase` must also pass `BucketName=""` to avoid the deletion trap identified in Finding #1.
    - `RuntimeCredentials` should include `stackName` (as planned) to facilitate future upgrades.

## Conclusion
By pivoting to the "Takeover" strategy, we align the architecture with the user's mental model ("I own this stack") and simplify the permissions model (everyone is an Owner/Admin candidate), while solving the critical issue of credential rotation on recovery.
