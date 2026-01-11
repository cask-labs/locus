# Analysis of Admin Upgrade Plan (Task 11)

**Task:** Phase 1, Task 11 (Admin Upgrade Flow)
**Target Plan:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`
**Assumption:** Tasks 1-10 are complete; the codebase reflects the state defined in their respective plans.

## Summary
The plan correctly identifies the need for an in-place CloudFormation update ("Single Template" strategy). However, it relies on a data field (`stackName`) that does not exist in the current `RuntimeCredentials` schema (defined in Task 2/5).

Since we assume previous tasks are complete and immutable, Task 11 must implement a **Discovery Mechanism** to retrieve the missing `stackName` at runtime using the elevated permissions available during the upgrade process.

## Critical Findings & Resolutions

### F1: Missing Stack Identity (Logic Gap)
**Severity: Critical**
**Finding:** The plan instructs `UpgradeAccountUseCase` to "Target stack using `creds.stackName`". However, `RuntimeCredentials` (from Task 5) does not contain this field, and we cannot assume it was added previously.
**Impact:** The upgrade will fail because the `UpdateStack` API requires the Stack Name (or ID), and the app doesn't know it.
**Resolution:**
*   **Do NOT** force a schema migration for legacy data.
*   **Implement Discovery:** In `UpgradeAccountUseCase`, before calling `updateStack`:
    1.  Use the user-provided **Bootstrap Keys** (which have high privileges).
    2.  Call `s3:GetBucketTagging` on the `bucketName` (which *is* known in `RuntimeCredentials`).
    3.  Extract the value of the auto-generated tag: **`aws:cloudformation:stack-name`**.
    4.  Use this value as the target `StackName` for the update.
*   **Persistence:** Once the upgrade succeeds, save this discovered `stackName` into the *new* `RuntimeCredentials` object (which involves updating the data class definition in Task 11 as planned).

### F2: Template Modification Safety
**Severity: High**
**Finding:** The plan specifies modifying `locus-stack.yaml` to add Admin permissions.
**Risk:** If the IAM Policy resource is replaced (instead of modified in-place), it could theoretically cause a momentary permission lapse, though unlikely to be fatal. More importantly, we must ensure the `LocusDataBucket` is **NOT** replaced.
**Resolution:**
*   Explicitly mandate the use of the **`Fn::If`** intrinsic function within the `LocusPolicy` `Statement` list.
    *   *Condition:* `AdminEnabled`.
    *   *True:* Include the Admin Statement (`tag:GetResources`, etc.).
    *   *False:* `Ref: AWS::NoValue` (removes the block).
*   This ensures the `LocusPolicy` Logical ID remains stable, and the `LocusDataBucket` is untouched.

## Detailed Recommendations

### Updated Logic: `UpgradeAccountUseCase`
```kotlin
// Pseudo-code for the revised logic
suspend fun upgradeAccount(bootstrapKeys: BootstrapCredentials) {
    val currentCreds = authRepository.getCredentials() // Has bucketName, but no stackName

    // 1. Discovery (The Fix)
    // Use Bootstrap Keys (Admin) to inspect the bucket
    val bootstrapS3 = awsClientFactory.createS3(bootstrapKeys)
    val tags = bootstrapS3.getBucketTagging(currentCreds.bucketName)
    val stackName = tags["aws:cloudformation:stack-name"]
        ?: throw ProvisioningException("Could not identify CloudFormation stack for bucket")

    // 2. Update Stack
    val bootstrapCfn = awsClientFactory.createCloudFormation(bootstrapKeys)
    bootstrapCfn.updateStack(
        stackName = stackName,
        template = loadAsset("locus-stack.yaml"),
        parameters = mapOf(
            "StackName" to stackName,
            "IsAdmin" to "true"
        )
    )

    // 3. Persist New State
    authRepository.saveCredentials(
        currentCreds.copy(
            stackName = stackName, // Now we have it!
            isAdmin = true
        )
    )
}
```

## Conclusion
The plan is **Approved** with the specific modification to **Step 7 (Create Upgrade Account Use Case)** to include the "Stack Name Discovery" logic described above. This removes the dependency on non-existent historical data.
