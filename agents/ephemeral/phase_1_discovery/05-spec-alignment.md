# Specification Alignment: Phase 1

This document traces the implementation plan back to the core behavioral specification `01_onboarding_identity.md`.

| Spec ID | Requirement | Plan Artifact | Coverage Strategy |
|---------|-------------|---------------|-------------------|
| **R1.050** | Cost Disclaimer | `WelcomeScreen.kt` | UI Text |
| **R1.060** | Key Gen Guide | `WelcomeScreen.kt` (Bottom Sheet) | UI Component |
| **R1.100** | Dry Run Validation | `ValidateCredentialsUseCase.kt` | Unit Test (FakeClient) |
| **R1.150** | JSON Parsing | `CredentialParser.kt` (Helper) | Unit Test |
| **R1.200** | Dry Run Fail | `AuthRepository` (State: Error) | UI State Test |
| **R1.300** | Session Token Mandatory | `ValidateCredentialsUseCase.kt` | Input Validation |
| **R1.400** | Default Device Name | `SetupChoiceScreen.kt` | ViewModel Logic |
| **R1.500** | Unique Name Check | `ProvisionIdentityUseCase.kt` | Integration Test (Fail Simulation) |
| **R1.600** | Visible Background Task | `ProvisioningWorker.kt` | Manual Verification (Background App) |
| **R1.700** | Bootstrap Keys Usage | `CloudFormationClient.kt` | Code Review |
| **R1.800** | Runtime User Gen | `locus-stack.yaml` (CFN) | Infrastructure Test |
| **R1.900** | Secure Storage & Discard | `AuthRepository.promoteToRuntime()` | Security Audit / Unit Test |
| **R1.1000**| Prov Failure Handling | `AuthRepository` (State: Failure) | Test Case: Rollback |
| **R1.1100**| List Locus Stores | `RecoverIdentityUseCase.kt` | Unit Test (Filtering) |
| **R1.1150**| Validate Buckets (Tags) | `S3Client.validateBucket()` | Integration Test |
| **R1.1300**| New Identity on Link | `locus-access-stack.yaml` | Infrastructure Test |
| **R1.1350**| Recovery Background Task | `ProvisioningWorker.kt` | Reused Logic |
| **R1.1400**| Unique Device ID | `DeviceIdGenerator.kt` | Unit Test |
| **R1.1500**| Lazy Sync | `SyncRepository` (Later Phase) | **Deferred** (Note: Spec says "When recovery is complete", we will just initialize empty index) |
| **R1.1550**| Permission 2-Step | `PermissionManager.kt` | UI Automator |
| **R1.1900**| Setup Trap | `MainActivity` / `AuthRepository` | Manual Verification (Kill App) |
| **R1.2200**| Admin Upgrade | `locus-admin.yaml` | Infrastructure Test |

## Gaps & Deviations
1.  **Lazy Sync (R1.1500):**
    *   The "Lazy Sync" is a Phase 3 (Cloud Sync) feature. In Phase 1, we simply mark the state as "Authenticated" and perhaps fetch the *metadata* of the bucket, but we do not fully implement the Sync Engine yet.
    *   *Alignment:* The Onboarding flow will just ensure the credentials work. The Dashboard will handle the empty state.
