# ADR: Use Satellite Stacks for Secondary Identities

## Status
Accepted

## Context
The Locus application supports three primary identity personas:
1.  **Standard User (Owner):** Provisions the infrastructure (S3 Bucket) and tracks data.
2.  **Recovery User:** Links a new device to an existing S3 Bucket (e.g., after phone replacement).
3.  **Admin User:** Links a device with privileged access (e.g., viewing other buckets) to an existing home bucket.

A recent assessment (`template_strategy_comparison.md`) proposed implementing the "Admin Upgrade" by modifying the existing CloudFormation stack (using `UpdateStack`) with a conditional template. This proposal was driven by a fear that using a separate template (`locus-admin.yaml`) to update the stack would cause Data Loss (Bucket deletion) because the admin template lacks the Bucket resource.

However, the application architecture imposes a critical constraint: **The Application does not persist the CloudFormation Stack Name.**
After initial provisioning, the app stores only the `RuntimeCredentials` (Access Keys, Bucket Name). It discards the Stack Name. Consequently, the Application *cannot* programmatically perform an `UpdateStack` operation because it does not know which stack to update.

## Decision
We will implement Secondary Identities (Recovery and Admin) using **Satellite Stacks**.

A **Satellite Stack** is a distinct, independent CloudFormation stack that provisions only Identity resources (IAM User, Access Key, Policy) and links them to an *existing* Bucket managed by a separate Data Stack.

### Architecture
*   **Data Stack (Primary):** Created via `locus-stack.yaml`. Manages the S3 Bucket and the initial Owner Identity.
*   **Satellite Stack (Secondary):** Created via `locus-user.yaml` (Recovery) or `locus-admin.yaml` (Admin). Manages a *new* IAM User and Policy.

## Rationale

1.  **Feasibility (The Stack Name Constraint):**
    Since the App does not know the primary stack's name, it cannot update it. However, creating a *new* stack is always possible because the App can simply generate a new name (e.g., `locus-admin-<UUID>`). This aligns with the "Stateless Provisioning" philosophy.

2.  **Safety (Zero Data Loss Risk):**
    The "Data Loss" risk identified in the assessment is predicated on the "Template Swap" strategy (updating the Data Stack with an Admin Template). By strictly using Satellite Stacks, the Data Stack (containing the Bucket) is **never touched**. Its lifecycle is completely decoupled from the secondary identities. Even if the Satellite Stack is deleted, the Bucket remains.

3.  **Simplicity:**
    This approach avoids complex conditional logic (`Fn::If`) in the primary `locus-stack.yaml`. The primary template remains focused on "Infrastructure + Owner", while secondary templates focus purely on "Access".

## Consequences

### Positive
*   **Guaranteed Data Safety:** Structural prevention of accidental bucket deletion.
*   **Decoupled Lifecycle:** Admin access can be revoked by deleting the Admin Stack without affecting the User's data or primary access.
*   **No "Stack Discovery" Required:** The app does not need to implement complex logic to "find" the user's stack ARN.

### Negative
*   **Multiple Credentials:** The application architecture conceptually supports one set of `RuntimeCredentials`. When "Upgrading to Admin", the app must strictly **replace** the Standard Credentials with the Admin Credentials (which must be a superset of permissions).
*   **Template Maintenance:** We must maintain multiple templates (`locus-stack.yaml`, `locus-admin.yaml`).

### Compliance Requirements
To make this strategy viable, the **Admin Template (`locus-admin.yaml`) must be fixed**.
Currently, it provides Read-Only access. To serve as a functional replacement for the Standard Credentials (allowing the Admin device to also track data), it must explicitly include `s3:PutObject` permissions for the specific Target Bucket.

## References
*   `docs/technical_discovery/specs/domain_layer_spec.md` (Provisioning State Machine)
*   `agents/ephemeral/phase1-deep-dive/recovery-discovery-strategy.md` (Defining the templates)
