# Report: Analysis of Admin Upgrade Plan

**Analysis Date:** 2024-05-21
**Source Document:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`
**Scope:** Identify gaps or problems in the context of "No Legacy Users / No Existing Install Base".

## Findings

### 1. Critical Flaw: Data Deletion Risk in Upgrade Flow
**Severity:** Critical
**Description:** The plan instructs the `UpgradeAccountUseCase` to pass the existing bucket name as a parameter (`BucketName=creds.bucketName`) during the CloudFormation `UpdateStack` operation for an Admin upgrade.
**Technical Impact:**
- The proposed CloudFormation template modification includes a condition `CreateNewBucket` which is `True` only if the `BucketName` parameter is empty.
- The `LocusDataBucket` resource is conditioned on `CreateNewBucket`.
- For the "Owner" stack (Standard User), the bucket was created with an empty `BucketName` parameter.
- If the `BucketName` parameter is changed to a non-empty value during an update, CloudFormation will evaluate `CreateNewBucket` as `False`, remove the `LocusDataBucket` resource from the stack definition, and attempt to **DELETE** the bucket.
- While S3 safety checks prevents deleting non-empty buckets, this will cause the Upgrade operation to fail with a Rollback, breaking the feature.
**Resolution:**
- The `RuntimeCredentials` model must store an `isBucketOwner: Boolean` flag.
- The `UpgradeAccountUseCase` must conditionally set the `BucketName` parameter:
    - If `isBucketOwner == true`: Pass `BucketName=""` (Empty String) to retain ownership.
    - If `isBucketOwner == false`: Pass `BucketName=creds.bucketName` (Link Mode).

### 2. Critical Logic Flaw: Stack Name Persistence in Recovery
**Severity:** High
**Description:** The plan states: *"Recovered Users: ... persist it [StackName] on the S3 bucket itself ... so it can be recovered during the scan process ... RecoverAccountUseCase must use this value."*
**Technical Impact:**
- This instruction conflicts with the project memory rule: *"System Recovery ... mandates a new unique Device ID for every install"*.
- If the "Unique Device ID" rule is followed, the Recovery process creates a **NEW** stack (e.g., `locus-user-DeviceB`) which links to the existing bucket (`BucketA`).
- The `LocusStackName` tag on `BucketA` contains the name of the *original* stack (`locus-user-DeviceA`).
- If `RecoverAccountUseCase` uses the value from the tag (`locus-user-DeviceA`) to populate `RuntimeCredentials.stackName`, the new device will hold a reference to the **wrong stack**.
- Any subsequent attempt to "Upgrade" will fail because the device does not hold credentials to update `locus-user-DeviceA`, and it will not be targeting its own stack (`locus-user-DeviceB`).
**Resolution:**
- Reaffirm the "Unique Device ID / New Stack" strategy for recovery.
- `RecoverAccountUseCase` must populate `RuntimeCredentials.stackName` with the **newly created stack name**, ignoring the tag on the bucket (except for potential audit logging).
- The `LocusStackName` tag on the bucket serves as an immutable record of the *creating* stack (Owner), which is correct behavior.

### 3. Missing Data Field: `isBucketOwner`
**Severity:** High
**Description:** As a consequence of Finding #1, the current `RuntimeCredentials` schema is insufficient to support the "Upgrade" logic safely because it cannot distinguish between an Owner (Standard) and a Linker (Recovered).
**Resolution:**
- Add `val isBucketOwner: Boolean` to `RuntimeCredentials`.
- Set to `true` in `ProvisioningUseCase` (Standard).
- Set to `false` in `RecoverAccountUseCase` (Recovery).
- Persist this value in `SecureStorage`.

### 4. Confirmed Fix: Infrastructure Constants Mismatch
**Severity:** Validated
**Description:** The plan correctly identifies that `InfrastructureConstants.OUT_RUNTIME_ACCESS_KEY` (`RuntimeAccessKeyId`) mismatches the CloudFormation Output (`AccessKeyId`).
**Resolution:** The proposed fix in the plan is correct.

## Recommendations

1.  **Modify Plan Step 4:** Add `isBucketOwner` to `RuntimeCredentials`.
2.  **Modify Plan Step 8 & 9:**
    - `ProvisioningUseCase`: Set `isBucketOwner = true`.
    - `RecoverAccountUseCase`: Set `isBucketOwner = false`. Do **NOT** use the bucket tag for `stackName`; use the new stack name.
    - `UpgradeAccountUseCase`: Implement logic: `val bucketParam = if (creds.isBucketOwner) "" else creds.bucketName`.
3.  **Clarify Recovery Strategy:** Explicitly state that Recovery creates a new stack and links to the bucket, rather than reusing the old stack.
