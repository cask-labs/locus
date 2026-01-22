# Report: Recovery Strategy Alternatives & Recommendation

**Analysis Date:** 2024-05-21
**Context:** Evaluation of architectural strategies for implementing "Account Recovery" (Device Loss/Reinstall) in a way that is compatible with the "Admin Upgrade" feature and the constraint that **Admin privileges are not recoverable** (users must recover to Standard and re-upgrade).

## Objectives
1.  **Regain Access:** Restore access to the existing S3 bucket on a new device.
2.  **Security:** Invalidate old credentials (lost device).
3.  **Consistency:** Ensure the "Downgrade to Standard" constraint is enforced.
4.  **Compatibility:** Must not compromise the data safety guarantees of the Admin Upgrade flow.

---

## Alternatives Analysis

### Strategy A: Takeover (UpdateStack)
**Description:**
The app discovers the existing stack name via S3 tags and executes an `UpdateStack` operation on it.
- **Recovery Action:**
  - `StackName`: `<ExistingStackName>`
  - `KeySerial`: `<NewUUID>` (Forces Key Rotation)
  - `IsAdmin`: `false` (Forces Downgrade to Standard)
- **Constraint Check:**
  - *Admin Recovery:* Effectively blocked. The `UpdateStack` call explicitly overwrites the state to `IsAdmin=false`, forcing the user to re-verify/pay to upgrade later.

**Pros:**
- **Ownership Integrity:** The stack continues to strictly own the bucket. No "orphaned" resources.
- **Data Safety:** Does **not** require the `BucketName` parameter. We rely on the fact that the stack *already* has the bucket defined.
- **Clean Downgrade:** CloudFormation handles the removal of Admin policies and restoration of Standard policies automatically when `IsAdmin` flips to false.

**Cons:**
- **Complexity:** Requires implementing `KeySerial` rotation logic in the template and client.

### Strategy B: Linker (Create New Stack)
**Description:**
The app creates a *new* stack (`locus-user-<NewDeviceID>`) and passes the existing bucket name as a parameter to "link" to it.
- **Recovery Action:**
  - `StackName`: `<NewStackName>`
  - `BucketName`: `<ExistingBucketName>`
  - `IsAdmin`: `false`
- **Constraint Check:**
  - *Admin Recovery:* The new stack is created as Standard.

**Pros:**
- **Simplicity:** Uses the standard "Provisioning" flow (CreateStack).

**Cons:**
- **Safety Violation (Critical):** This strategy **requires** the `BucketName` parameter to exist in the `locus-stack.yaml` template (to tell the new stack "don't create a bucket, use this one").
    - As established in the Admin Upgrade analysis, the existence of this parameter creates a **Data Deletion Foot-Gun** for the Admin Upgrade flow. We cannot safely have it both ways.
- **Split Brain:** The old stack (`locus-user-<OldDeviceID>`) still exists and thinks it owns the bucket.
- **Ownership Gap:** The new stack does *not* own the bucket resource. It cannot apply Tags or Lifecycle configurations to it, meaning a future "Admin Upgrade" on this new stack might fail to apply necessary bucket-level changes (like ABAC tags).

### Strategy C: Resource Import (Create + Import)
**Description:**
The app creates a new stack and instructs CloudFormation to **Import** the existing bucket resource into this new stack.
- **Recovery Action:** Complex API flow involving `CreateChangeSet` with `ChangeSetType=IMPORT`.

**Pros:**
- **Clean State:** Result is a new stack that truly owns the bucket.

**Cons:**
- **Client Complexity (Prohibitive):** CloudFormation Resource Import is a complex, multi-step operation notoriously difficult to automate reliably on a mobile client.
- **Risk:** High risk of "Resource already exists" errors if not handled perfectly.

---

## Recommendation

### **Selected Strategy: Strategy A (Takeover)**

**Justification:**
1.  **Safety Compatibility:** Strategy A is the **only** option that allows us to remove the dangerous `BucketName` parameter from the template. By reusing the existing stack (which already defines the bucket), we eliminate the risk of accidental deletion during updates.
2.  **Enforced Downgrade:** It provides a deterministic way to enforce the "No Admin Recovery" constraint. By passing `IsAdmin=false` during the `UpdateStack` call, we guarantee the infrastructure state resets to the "Standard" baseline, regardless of its previous state.
3.  **Atomic Key Rotation:** The `KeySerial` pattern ensures that the old credentials (on the lost device) are invalidated at the exact moment the new credentials are provisioned, minimizing the vulnerability window.

### Detailed Design for Recovery (Strategy A)

**CloudFormation Template (`locus-stack.yaml`):**
```yaml
Parameters:
  IsAdmin:
    Type: String
    Default: "false"
  KeySerial:
    Type: String
    Default: "1"
    Description: Change this value to force rotation of the Runtime User/Keys.

Resources:
  LocusUser:
    Type: AWS::IAM::User
    Properties:
      # Appending KeySerial forces replacement (rotation) when changed
      UserName: !Sub "locus-user-${StackName}-${KeySerial}"
```

**Client Logic (`RecoverAccountUseCase`):**
1.  **Discovery:** Identify target `StackName` from S3 tags.
2.  **Preparation:** Generate `NewKeySerial` (UUID).
3.  **Execution:** Call `stackProvisioningService.updateAndPollStack`:
    *   `StackName`: `<DiscoveredStackName>`
    *   `KeySerial`: `<NewKeySerial>`
    *   `IsAdmin`: `"false"` **(Explicit Downgrade)**
4.  **Result:**
    *   Old User/Keys deleted.
    *   New User/Keys created and returned.
    *   Admin Policies removed (if present).
    *   Standard Policies attached.
    *   Bucket preserved (implicitly).

This design satisfies all constraints: it recovers access, rotates keys, enforcing the standard tier, and ensures 100% data safety.
