# Analysis of Admin Upgrade Plan (Task 11)

**Task:** Phase 1, Task 11 (Admin Upgrade Flow)
**Target Plan:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`

## Summary
The proposed plan for Task 11 is architecturally sound in its "Single Conditional Template" strategy, which correctly prevents data loss risks associated with replacing CloudFormation templates. However, deep analysis reveals **critical dependency and timing issues** regarding where and when `RuntimeCredentials` and `locus-stack.yaml` are modified. If these changes are implemented only in Task 11, the preceding Task 6 (Provisioning) will fail to persist the necessary data, rendering Task 11 impossible to execute for those users.

---

## Critical Findings & Resolutions

### F1: Schema Update Timing (Stack Persistence)
**Severity: Blocking**
**Finding:** The plan schedules the addition of `stackName` to `RuntimeCredentials` in **Task 11 (Step 3)**. However, `RuntimeCredentials` are created and persisted during **Task 6 (Provisioning Use Cases)**.
If `stackName` is not part of the schema and not populated during Task 6, users onboarded before Task 11 is deployed (or simply using the flow defined in Task 6) will have `RuntimeCredentials` *without* a `stackName`.
**Impact:** When these users later attempt an "Admin Upgrade" (Task 11), the `UpgradeAccountUseCase` will fail because it cannot identify the CloudFormation stack to update (missing `stackName`).
**Resolution:**
*   **Move Schema Change:** The definition of `RuntimeCredentials` in **Task 2** and its implementation in **Task 5/6** MUST include `stackName` from the start.
*   **Update Task 6:** The `ProvisioningUseCase` and `RecoverAccountUseCase` in Task 6 must be updated to explicitly capture the `StackName` from CloudFormation outputs and save it.
*   **Action:** Update the Task 11 plan to acknowledge this dependency is effectively a *prerequisite* check, or explicitly modify the plans for Tasks 2 & 6 now to include this field.

### F2: Template Compatibility with Recovery (Task 6)
**Severity: High**
**Finding:** Task 11 proposes adding an `IsAdmin` parameter to `locus-stack.yaml` to conditionally enable permissions. However, **Task 6 (Recovery Flow)** requires reusing this same template to create an IAM User for an *existing* bucket.
The current `locus-stack.yaml` contains an unconditional `AWS::S3::Bucket` resource.
*   **Scenario:** In Task 6, `RecoverAccountUseCase` calls `createStack` with this template.
*   **Failure:** CloudFormation attempts to create the bucket. Since the bucket exists (Recovery scenario), the stack creation fails with `BucketAlreadyExists`.
**Impact:** The "Single Template" strategy breaks the Recovery flow unless the template supports skipping bucket creation.
**Resolution:**
*   **Enhance Template Strategy:** The `locus-stack.yaml` must support *two* dimensions of conditionality:
    1.  `CreateBucket` (Boolean, Default: True): Set to `False` for Recovery.
    2.  `IsAdmin` (Boolean, Default: False): Set to `True` for Upgrade.
*   **Timing:** This template modification must be applied in **Task 4 (Assets)** or **Task 6**, not delayed until Task 11.
*   **Task 11 Adjustment:** Task 11 should focus on *using* the `IsAdmin` parameter, assuming the template infrastructure is robust, or applying the *delta* for Admin if strictly separating. Ideally, the robust template is defined early.

### F3: Regional Strategy Clarification
**Severity: Low (Architectural Note)**
**Finding:** The `network_infrastructure_spec.md` and Task 4 plan hardcode `CloudFormationClient` to `us-east-1`.
*   **Validation:** This simplifies Phase 1 significantly. `RuntimeCredentials` will store `region` (S3 Bucket Region) and `stackName`.
*   **Constraint:** `UpgradeAccountUseCase` (Task 11) calls `stackProvisioningService.updateAndPollStack`. Since the underlying client is hardcoded to `us-east-1`, it will correctly find the stack (which was created in `us-east-1`).
*   **Note:** If the architecture ever changes to allow Stacks in other regions, `RuntimeCredentials` would need to store `stackRegion` separately. For now, the hardcoding holds.
*   **Resolution:** No change needed to the plan, but the implementation of `UpgradeAccountUseCase` must ensure it doesn't accidentally try to use `RuntimeCredentials.region` to configure the CloudFormation client.

---

## Detailed Plan Updates

### 1. Retroactive Requirements for Task 2 & 6
The following requirements are identified as *missing* from the earlier task plans but are discovered here as dependencies for Task 11.

*   **RuntimeCredentials Schema:**
    ```kotlin
    data class RuntimeCredentials(
        // ... existing fields ...
        val stackName: String, // MUST be added in Task 2
        val isAdmin: Boolean = false
    )
    ```

*   **CloudFormation Template (Task 4/6):**
    ```yaml
    Parameters:
      CreateBucket:
        Type: String
        Default: "true"
        AllowedValues: ["true", "false"]
      IsAdmin: ...
    Conditions:
      PerformBucketCreation: !Equals [!Ref CreateBucket, "true"]
    Resources:
      LocusDataBucket:
        Condition: PerformBucketCreation
        Type: 'AWS::S3::Bucket'
        # ...
    ```

### 2. Task 11 Plan Amendments
*   **Step 3 (Update Runtime Credentials):** Rephrase to "Verify `stackName` exists (added in Task 2/6). Add `isAdmin` field if missing."
*   **Step 2 (Modify CloudFormation Template):** Rephrase to "Ensure template supports `IsAdmin`. Verify `CreateBucket` condition exists (from Task 6). Add `AdminEnabled` condition and `Fn::If` logic to Policy."

---

## Conclusion
The Admin Upgrade plan is **Approved** with the strict condition that the **Data persistence (`stackName`) and Template flexibility (`CreateBucket`)** are implemented in the preceding Tasks (2, 4, 6). Deferring these to Task 11 will cause the Recovery flow to fail and prevent early adopters from upgrading.
