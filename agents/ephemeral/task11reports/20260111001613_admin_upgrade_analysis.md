# Analysis of Admin Upgrade Plan (Task 11)

**Task:** Phase 1, Task 11 (Admin Upgrade Flow)
**Target Plan:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`

## Summary
The proposed plan for Task 11 is architecturally sound and correctly identifies the "Single Conditional Template" strategy as the preferred approach over swapping templates. This prevents potential data loss scenarios where replacing a stack with a new template might trigger resource replacement (e.g., S3 Buckets) due to Logical ID mismatches.

However, deep analysis reveals critical gaps in data persistence and regional configuration that must be addressed to make the plan executable and robust.

---

## Critical Findings & Resolutions

### F1: Missing Stack Identity Persistence
**Severity: Critical**
**Finding:** The plan correctly identifies that `RuntimeCredentials` currently lacks `stackName`. This field is absolutely critical because the `UpdateStack` API requires the stack name (or ID) to target the correct resource. Without it, the application cannot perform an upgrade on an existing installation.
**Impact:** If `stackName` is not persisted during the *initial* provisioning (Task 6), the Admin Upgrade feature will be impossible for those users without manual intervention or risky discovery heuristics.
**Resolution:**
*   **Endorse:** The addition of `stackName` to `RuntimeCredentials` in Task 11 is correct.
*   **Back-Propagate:** This requirement must be explicitly flagged for **Task 6 (Provisioning Use Cases)** and **Task 8 (UI)**. The `ProvisioningUseCase` must capture the `StackName` from the input/output and pass it to `AuthRepository` for storage.
*   **Action:** I will treat the update to `RuntimeCredentials` as a mandatory dependency for Task 6 execution, even though it is defined in Task 11's plan.

### F2: Regional Affinity Mismatch
**Severity: High**
**Finding:** The `network_infrastructure_spec.md` states that the Bootstrap Module (used for CloudFormation) defaults to `us-east-1` to simplify stack creation. However, the `RuntimeCredentials` object contains a specific `region` (matching the bucket).
If a user's stack is deployed in a region other than `us-east-1` (e.g., `eu-central-1`), the default Bootstrap Client will fail to find the stack during the `UpdateStack` call, as it will look in `us-east-1`.
**Impact:** `UpdateStack` will fail with `StackNotFound` for any non-us-east-1 users.
**Resolution:**
*   The `UpgradeAccountUseCase` must explicitly configure the `CloudFormationClient` to use the region stored in `RuntimeCredentials`, overriding the default `us-east-1`.
*   Alternatively, `CloudFormationClient.updateStack` must accept a `region` parameter. Given the client is likely a singleton or scoped instance, re-configuring it might be complex. Passing the region to the method is preferred if the underlying SDK client is region-aware or cheap to recreate.
*   **Recommendation:** Update the plan to specify that `UpgradeAccountUseCase` retrieves the region from `RuntimeCredentials` and passes it to the infrastructure layer.

### F3: Recovery Flow Conflict (Risk Assessment)
**Severity: High (Future Risk)**
**Finding:** The "Single Template" strategy endorsed by this plan has a side effect on the System Recovery (Task 6) flow. The current `locus-stack.yaml` contains an unconditional `AWS::S3::Bucket` resource. CloudFormation `CreateStack` will **fail** if it attempts to create a bucket that already exists (which is exactly what happens during "Link Existing Store").
**Impact:** While this doesn't block *Admin Upgrade* (where the stack already exists), it means the "Single Template" cannot be used "as is" for Recovery without modification.
**Resolution:**
*   For Task 11 (Admin Upgrade), the plan is valid.
*   **Flag for Task 6:** The `locus-stack.yaml` will likely need a condition (e.g., `CreateBucket`) to support the Recovery flow, or Recovery must use a different mechanism (e.g., Import Existing Resources).
*   **Plan Update:** I will modify the `locus-stack.yaml` change description in Task 11 to suggest using `Fn::If` for the Admin Policy, which aligns with the "Conditional Resource" pattern needed for Recovery later.

---

## Detailed Implementation Recommendations

### 1. `locus-stack.yaml` Modification
The plan specifies adding an `IsAdmin` parameter. The implementation details should be explicit to ensure validity:

```yaml
Parameters:
  IsAdmin:
    Type: String
    Default: "false"
    AllowedValues: ["true", "false"]

Conditions:
  AdminEnabled: !Equals [!Ref IsAdmin, "true"]

Resources:
  LocusPolicy:
    Type: AWS::IAM::Policy
    Properties:
      # ...
      PolicyDocument:
        Statement:
          # Standard Runtime Statement (Always present)
          - Effect: Allow
            Action: [s3:PutObject, s3:GetObject, s3:ListBucket]
            Resource: ...
          # Admin Statement (Conditional)
          - Fn::If:
            - AdminEnabled
            - Effect: Allow
              Action: [tag:GetResources]
              Resource: "*"
            - !Ref "AWS::NoValue"
```
*Note: S3 List/Get permissions might need to be broadened for Admin, or a second Statement added. Using `Fn::If` inside the Statement list is the cleanest approach.*

### 2. Client Capabilities
The plan must strictly require `CAPABILITY_NAMED_IAM` for `updateStack`, just as it is required for `createStack`, because the template creates named IAM users (`locus-user-${StackName}`).

---

## Conclusion
The plan `11-admin-upgrade-plan.md` is approved with the following required amendments to be reflected in the implementation:
1.  **Strictly enforce** `stackName` persistence in `RuntimeCredentials` starting from Task 6.
2.  **Require Region Awareness** in `UpgradeAccountUseCase` to prevent cross-region update failures.
3.  **Use `Fn::If`** logic for the CloudFormation template modifications.
