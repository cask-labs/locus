# Report: CloudFormation Strategy Alternatives for Admin Upgrade

**Analysis Date:** 2024-05-21
**Context:** Evaluation of architectural strategies for implementing the "Admin Upgrade" use case in a User-Owned Data model.

## Objective
Enable a user to elevate their existing "Standard" installation (S3-only access) to "Admin" status (Cross-device discovery/audit) by modifying their CloudFormation stack, while strictly preserving their existing data.

---

## Alternatives Analysis

### Strategy A: Single Conditional Template (Parameter-Based)
**Description:**
Maintain a single `locus-stack.yaml` file. Introduce a parameter `IsAdmin` (AllowedValues: "true", "false"). Use CloudFormation `Conditions` to toggle between resources or property values.
- **Upgrade Mechanism:** `UpdateStack` call changing `IsAdmin` from `false` to `true`.
- **Implementation Detail:** The template defines a condition `AdminEnabled: !Equals [!Ref IsAdmin, "true"]`.

**Pros:**
- **Data Safety (Highest):** The `AWS::S3::Bucket` resource is defined once. Its Logical ID never changes. CloudFormation guarantees preservation of the resource during updates.
- **Maintainability:** Single source of truth. Changes to shared infrastructure (e.g., S3 Lifecycle Rules, Encryption settings) are applied to all users, Standard and Admin alike.
- **Simplicity:** The Android client only needs to manage one template file.

**Cons:**
- **Template Complexity:** YAML can become verbose with `Fn::If` intrinsics or multiple conditional resources.

### Strategy B: Dual Template Swapping
**Description:**
Maintain two separate files: `locus-stack-standard.yaml` and `locus-stack-admin.yaml`. The Admin template is a superset (or different version) of the Standard template.
- **Upgrade Mechanism:** `UpdateStack` call providing the `locus-stack-admin.yaml` body instead of the original standard one.

**Pros:**
- **Readability:** Each file clearly shows the permissions for that role without conditional logic clutter.
- **Isolation:** "Standard" users are not carrying hidden "Admin" code in their stack definition.

**Cons:**
- **Data Safety Risk (Critical):** If the `AWS::S3::Bucket` resource in the Admin template has a different **Logical ID** or different immutable properties than the Standard template, CloudFormation may decide to **Replace** (Delete + Create) the bucket. This is a catastrophic "foot-gun".
- **Drift/Maintenance:** Engineers must remember to apply bug fixes (e.g., S3 security patches) to *both* files. Divergence is inevitable.

### Strategy C: Nested Stacks / Sidecar Stack
**Description:**
The primary "Standard" stack remains untouched. An "Admin" stack is created as a separate stack (or nested stack) that references the Standard stack's resources (Bucket ARN, User ARN) and attaches additional policies.
- **Upgrade Mechanism:** `CreateStack` (AdminStack).

**Pros:**
- **Modularity:** Strictly adheres to the Open/Closed principle. The Core stack is closed for modification.
- **Safety:** The Core stack (holding the data) is never touched during upgrade.

**Cons:**
- **Client Complexity:** The Android app must now manage the lifecycle of *two* stacks. It needs to know which one to delete during "Reset App".
- **IAM Limitations:** Attaching policies to an IAM User defined in another stack requires exporting/importing values, which introduces dependency locking (cannot delete Core without deleting Admin first).
- **Deployment:** Nested stacks often require uploading templates to S3 first, which complicates the "Bootstrap" flow where the user might not have a bucket yet.

---

## Recommendation

### **Selected Strategy: Strategy A (Single Conditional Template)**

**Justification:**
1.  **Safety First:** In a user-owned data model, **Data Loss Prevention** is the paramount non-functional requirement. Strategy A provides the strongest guarantee that the S3 Bucket resource remains stable because its definition is physically identical and bound to the same Logical ID in the template. Strategy B introduces human error risks (copy-paste errors changing Logical IDs) that could wipe data.
2.  **Operational Simplicity:** The Android client is a "Thick Client" managing infrastructure. Minimizing the state it needs to track (just "One Stack Name") reduces the surface area for bugs in the Recovery/Sync logic. Strategy C doubles the state management burden.
3.  **Maintainability:** We expect the infrastructure best practices (Encryption, Lifecycle rules) to evolve. Managing these updates in one place (Strategy A) is superior to keeping multiple files in sync (Strategy B).

### Refined Design Pattern for Strategy A

To mitigate the "Template Complexity" con, I recommend using **Conditional Policy Resources** instead of inline `Fn::If` statements.

**Bad (Inline Complexity):**
```yaml
LocusPolicy:
  Type: AWS::IAM::Policy
  Properties:
    PolicyDocument:
      Statement:
        - Effect: Allow
          Action:
            - s3:PutObject
            - Fn::If: [AdminEnabled, "tag:GetResources", Ref: AWS::NoValue] # Hard to read/validate
```

**Good (Resource Swapping):**
```yaml
Conditions:
  IsAdmin: !Equals [!Ref IsAdmin, "true"]
  IsStandard: !Not [!Condition IsAdmin]

LocusUser:
  Type: AWS::IAM::User
  Properties:
    # ...

StandardPolicy:
  Type: AWS::IAM::Policy
  Condition: IsStandard
  Properties:
    Users: [!Ref LocusUser]
    # Simple, read-only S3 permissions

AdminPolicy:
  Type: AWS::IAM::Policy
  Condition: IsAdmin
  Properties:
    Users: [!Ref LocusUser]
    # Superset of permissions: S3 + Tagging + Listing
```

**Why this is better:**
- **Auditable:** You can look at `AdminPolicy` in isolation and see exactly what permissions are granted.
- **Clean Upgrade:** When `IsAdmin` flips to True, CloudFormation creates `AdminPolicy` and deletes `StandardPolicy`. The `LocusUser` and `LocusDataBucket` remain untouched.

## Summary of Changes Required
1.  **Template:** Refactor `locus-stack.yaml` to use the **Conditional Policy Resource** pattern proposed above.
2.  **Parameters:** Add `IsAdmin` (Default: false) and `KeySerial` (for rotation).
3.  **Removal:** Remove `BucketName` (Safety).
