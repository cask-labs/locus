# Materials & Inventory: Phase 1 - Onboarding & Identity

## Design Pattern
**Path B: Domain State Machine + Foreground Service**

## Connection Points

**Incoming:**
- **Trigger:** User clicks "Start Setup" or "Recover Account" on the Onboarding UI.
- **Input:**
    - `AccessKeyId` (String)
    - `SecretAccessKey` (String)
    - `SessionToken` (String)
    - `DeviceName` (String) - *For New Setup*
    - `BucketName` (String) - *For Recovery*

**Outgoing:**
- **Destination:** AWS CloudFormation (CreateStack), AWS S3 (ListBuckets), AWS IAM (CreateUser).
- **Interface:** AWS SDK for Kotlin (`CloudFormationClient`, `S3Client`, `IamClient`).
- **Persistence:** Local Disk (`EncryptedSharedPreferences` for keys, `File` for provisioning logs).

## Module Breakdown

- `:core:domain` - **Business Logic**
    - `AuthRepository` (Interface): State holder.
    - `ProvisioningUseCase`: Orchestrates the step-by-step logic.
    - `CredentialValidator`: Performs the "Dry Run".
    - `OnboardingState`: Sealed class hierarchy (`Idle`, `Validating`, `Provisioning`, `Success`, `Error`).

- `:core:data` - **Implementation**
    - `RealAuthRepository`: Implements persistence and state management.
    - `EncryptedSharedPreferencesDataSource`: Secure key storage.
    - `CloudFormationDataSource`: Wraps AWS CloudFormation interactions.
    - `ProvisioningService`: **Foreground Service** that hosts the Use Case execution scope.

- `:app` - **UI Layer**
    - `OnboardingViewModel`: Observes `AuthRepository`, exposes state to UI.
    - `OnboardingScreen`: Composable UI (Input, Progress Log, Success).

## Cross-Cutting Patterns

**Handling Duplicates:**
- **Idempotency:** The "Setup Trap" checks `AuthRepository.isProvisioned()` at app launch. If `true`, redirects to Dashboard. If `Provisioning`, resumes the log view.

**Handling Trouble:**
- **Validation:** "Dry Run" (`sts:GetCallerIdentity`) prevents starting a heavy stack deployment with bad keys.
- **Rollback:** If CloudFormation fails, we do *not* auto-delete (R1.1000). We leave the stack for debugging but reset the local app state to "Input" so the user can try again (potentially with a different name).

**Protecting Information:**
- **Bootstrap Keys:** Held in memory *only* during the provisioning session. overwritten/nulled after success.
- **Runtime Keys:** Stored in `EncryptedSharedPreferences`.
- **Secret Display:** Secret Access Key is masked in the UI.

**System Fit (Android):**
- **Foreground Service:** Uses a `Notification` ("Locus Setup: Provisioning resources...") to prevent OS killing.
- **Back Stack:** Onboarding Activity is `finish()`ed upon transition to Dashboard to prevent "Back" navigation.

## Tuning Options
- `locus-stack.yaml` - The CloudFormation template itself.
- `OnboardingState.LogEntry` - Granularity of user-facing logs.

## Dependencies
- `aws.sdk.kotlin:cloudformation`
- `aws.sdk.kotlin:iam`
- `aws.sdk.kotlin:sts`
- `aws.sdk.kotlin:s3`
- `androidx.security:security-crypto` (EncryptedSharedPreferences)
