# Attach IAM Policies Directly to Users for Per-Device Isolation

**Date:** 2025-12-22

## Context

The Locus architecture implements a **per-device isolation model** where each device installation generates:
- A unique device ID (UUID) to prevent "split brain" data collisions
- A dedicated AWS IAM User with restricted runtime credentials
- Credentials stored in encrypted local storage on the device

During infrastructure validation via Checkov, the template triggers a security finding:
- **CKV_AWS_40**: "Ensure IAM policies are attached only to groups or roles"

This rule reflects a traditional access management best practice: use **groups** to manage policies for multiple users, or **roles** for service-to-service delegation and temporary credentials.

**The Question:** Does our architecture conflict with this best practice, or is there a way to satisfy both?

## Decision

We will **attach IAM policies directly to IAM Users** (one per device) rather than indirectly through groups or roles.

This is the correct approach for our architecture and is an **acknowledged exception** to Checkov's generic best-practice rule.

## Justification

### Why Not Roles?
- **Roles** are designed for **temporary credentials** via STS AssumeRole (session tokens)
- Locus requires **persistent AccessKeys** (Access Key ID + Secret Access Key) stored in encrypted local storage
- Roles cannot produce long-term credentials; they only generate temporary session tokens
- **Verdict**: Architecturally incompatible with our credential model

### Why Not Groups?
- **Groups** are a management abstraction for organizing multiple users with shared policies
- Our design is intentionally **one user per device** for isolation (not multiple users sharing permissions)
- Adding a group with a single user is semantically incorrect; it suggests future multi-user scenarios that contradict the per-device isolation model
- It adds a no-op CloudFormation resource with zero functional benefit
- **Verdict**: Violates the principle of not adding infrastructure for tooling compliance

### Why Users (Direct Policy Attachment)?

The design intent is:
1. **One IAM User per device installation** - Each device gets its own isolated user
2. **Scoped runtime credentials** - User has only `s3:PutObject`, `s3:GetObject`, `s3:ListBucket` on the specific bucket
3. **No cross-device access** - Users are completely isolated from each other (different AWS accounts per user, different buckets per device)
4. **Direct policy attachment** - Policy attached directly to the user is the clearest, simplest representation of the 1:1 relationship

**Verdict**: Direct user policy attachment is the correct and most explicit way to represent this architecture.

## Why Checkov's Rule Doesn't Apply Here

**CKV_AWS_40 assumes**:
- Multiple users exist in an account
- Users should be organized into groups to simplify permission management
- This reduces "access management complexity" and "reduces opportunity for privilege escalation"

**Locus architecture is different**:
- Each user is in a separate AWS account (user's own account)
- Each user is completely isolated (one per device, different buckets)
- No shared permission groups across devices (contradicts isolation model)
- Policy complexity is **minimal** (3 actions, 1 bucket)

**Conclusion**: Checkov's rule optimizes for multi-user account management; we optimize for per-device isolation. These are orthogonal concerns.

## Consequences

### Positive
- **Architecturally correct** - Direct representation of the 1:1 device-to-user relationship
- **Simple and explicit** - No indirection through groups; easier to understand the data flow
- **Minimal infrastructure** - No unnecessary CloudFormation resources
- **Clear intent** - Future maintainers immediately see that policy is tied to this specific user, not a shared group

### Negative
- **Checkov compliance failure** - The linter will report CKV_AWS_40; requires documentation and acceptance
- **Generic tool assumption** - Requires developers to understand why a generic security rule doesn't apply here

## Alternatives Considered

### Option 1: Add IAM Group (Rejected)
```yaml
LocusGroup:
  Type: AWS::IAM::Group

LocusGroupMembership:
  Type: AWS::IAM::UserToGroupAddition
  Properties:
    GroupName: !Ref LocusGroup
    Users:
      - !Ref LocusUser

LocusPolicy:
  Type: AWS::IAM::Policy
  Properties:
    Groups:  # Instead of Users
      - !Ref LocusGroup
```

**Why rejected**:
- Satisfies Checkov but adds a meaningless abstraction
- A group with one user contradicts the purpose of groups
- Suggests future multi-user scenarios that don't fit the design
- Increases template complexity without functional benefit

### Option 2: Use IAM Role with AssumeRole (Rejected)
Not viable—roles produce temporary credentials, not persistent AccessKeys needed for local storage.

## Related Decisions

- **Infrastructure Design** - See `/docs/technical_discovery/infrastructure.md` for the full per-device isolation model
- **Behavioral Spec: Onboarding & Identity** - See `/docs/behavioral_specs/01_onboarding_identity.md` for credential lifecycle and key swap pattern
- **Bootstrap vs. Runtime Keys** - Bootstrap keys have CloudFormation privileges; runtime keys are least-privileged and device-isolated

## Future Considerations

- If Checkov updates CKV_AWS_40 to account for single-user isolation patterns, this exception can be removed
- If the architecture evolves to multi-device management in a single account, this decision may need revisiting (at that point, groups become appropriate)

### Admin Access & ABAC
While we reject **IAM Groups** for device isolation, we adopt **Resource Tagging** to support future "Admin" or "Supervisor" personas.
- **Problem**: An Admin needs access to *all* Locus device buckets, but not unrelated buckets in the user's account.
- **Solution**: We apply the tag `LocusRole: DeviceBucket` to all standard stacks.
- **Mechanism**: The Admin Policy uses Attribute-Based Access Control (ABAC) via `Condition: { StringEquals: { s3:ResourceTag/LocusRole: DeviceBucket } }`.
- **Benefit**: This allows the Admin to "see all devices" dynamically without requiring a central "Admin Group" resource.

## Sign-Off

**Decision Maker:** Project Team (informed by architecture requirements and Checkov best-practice assessment)

**Approved:** Yes

**Rationale Summary:** Direct IAM policy attachment to users is the correct implementation of per-device isolation. Checkov's CKV_AWS_40 rule is designed for traditional multi-user account management and does not apply to our single-user-per-device architecture. Adding a group for compliance satisfaction would violate the principle of not adding infrastructure purely for tooling agreement.
