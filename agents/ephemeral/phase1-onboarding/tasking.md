# Phase 1: Onboarding & Identity - Task Breakdown

**Feature:** Onboarding, Identity Management, and Cloud Provisioning.
**Specs:**
- `docs/behavioral_specs/01_onboarding_identity.md` (Primary)
- `docs/technical_discovery/specs/background_processing_spec.md` (Provisioning Worker)
- `docs/technical_discovery/specs/domain_layer_spec.md` (AuthRepository)

---

## Task 1: Documentation Cleanup & UI Foundation
**Title:** Align Documentation and Establish Design System
**Purpose:** Fix known inconsistencies in documentation regarding Foreground Services and establish the missing Material Design 3 foundation (Theme, Color, Type) required for UI development.
**Behaviors:**
- `docs/requirements/ui_feedback.md` is updated to refer to "High Priority Background Task" or "WorkManager" instead of "Foreground Service" for provisioning.
- `docs/technical_discovery/specs/background_processing_spec.md` is verified to align with WorkManager usage.
- A basic Material Design 3 Theme (`LocusTheme`) is created in `:app`.
- Type (`Typography`) and Color (`ColorScheme`) definitions are added.
**Verification:**
- `grep "Foreground Service" docs/requirements/ui_feedback.md` does not return the conflicting line.
- Inspect `app/src/main/java/com/locus/android/ui/theme/Theme.kt` exists.

## Task 2: Domain Layer Modeling
**Title:** Define Authentication Domain Models and Interfaces
**Purpose:** Establish the core types and contracts for authentication, ensuring the Domain layer is pure Kotlin.
**Behaviors:**
- Define `LocusResult<T>`, `AppError` (Auth, Network, Provisioning variants).
- Define `AuthState` (Uninitialized, SetupPending, Authenticated).
- Define `ProvisioningState` (Idle, Validating, Deploying, Success, Failure).
- Define `AuthRepository` interface (as per `domain_layer_spec.md`).
- Define `BootstrapCredentials` and `RuntimeCredentials` value objects.
**Verification:**
- `AuthRepository.kt` exists in `:core:domain`.
- Unit tests verify `LocusResult` encapsulation.

## Task 3: Secure Storage Implementation
**Title:** Implement Secure Credential Storage
**Purpose:** Provide a safe mechanism to store sensitive AWS keys.
**Behaviors:**
- Implement `SecureStorageDataSource` in `:core:data`.
- Wraps `EncryptedSharedPreferences`.
- Supports saving/retrieving/clearing `BootstrapCredentials` and `RuntimeCredentials`.
- Handles `SharedPreferences` fallbacks if encryption fails (for non-sensitive data, or fails hard for keys).
**Verification:**
- `androidTest` verifies data is persisted and retrievable.
- `androidTest` verifies data persists across app restarts.

## Task 4: AWS Infrastructure Clients
**Title:** Implement CloudFormation and S3 Clients
**Purpose:** Enable the application to talk to AWS for provisioning and discovery.
**Behaviors:**
- Implement `CloudFormationClient`:
  - `createStack(template: String, params: Map<String, String>)`
  - `describeStack(stackName: String)`
  - `deleteStack(stackName: String)`
- Implement `S3Client`:
  - `listBuckets()`
  - `getBucketTags(bucketName: String)` (for `LocusRole: DeviceBucket` validation)
- Integrate `locus-stack.yaml` and `locus-admin.yaml` as raw resources or string constants.
**Verification:**
- Unit tests using `Mockk` for AWS SDK wrappers.
- `CloudFormationClient` correctly loads the YAML template.

## Task 5: AuthRepository Implementation
**Title:** Implement AuthRepository Logic
**Purpose:** connect the Domain layer to Data and Infrastructure layers.
**Behaviors:**
- Implement `AuthRepositoryImpl` in `:core:data`.
- Implements `validateBucket` logic (List + Tag Check).
- Implements `recoverAccount` logic.
- Implements `promoteToRuntimeCredentials` (swap keys).
- Manages internal `StateFlow` for `AuthState` and `ProvisioningState`.
**Verification:**
- Unit tests using `Mockk` verify the coordination between Storage and AWS Clients.
- `promoteToRuntimeCredentials` verifies old keys are cleared and new keys are saved.

## Task 6: Provisioning Use Cases
**Title:** Implement Provisioning Business Logic
**Purpose:** Encapsulate the complex rules for setting up a new device or recovering an account.
**Behaviors:**
- Implement `ProvisioningUseCase`:
  - Validates inputs.
  - Calls `CloudFormationClient.createStack`.
  - Polls for completion.
  - Returns `LocusResult`.
- Implement `RecoverAccountUseCase`:
  - Scans for buckets.
  - Validates tags.
  - Configures runtime.
**Verification:**
- Unit tests verify success/failure paths and error mapping.

## Task 7: Provisioning Worker
**Title:** Implement Background Provisioning Worker
**Purpose:** Ensure provisioning survives app backgrounding/process death using WorkManager.
**Behaviors:**
- Implement `ProvisioningWorker` in `:app` (or specific feature module).
- configured as `Expedited` / Long-Running.
- Invokes `ProvisioningUseCase` or `RecoverAccountUseCase`.
- Updates a persistent Notification ("Locus Setup: Provisioning...").
**Verification:**
- Unit tests using `WorkManagerTestInitHelper`.
- Verify Worker result states.

## Task 8: Onboarding UI - Part 1 (Credentials)
**Title:** Implement Welcome and Credential Entry Screens
**Purpose:** Allow the user to input their AWS keys.
**Behaviors:**
- Implement `WelcomeScreen` (Cost disclaimer).
- Implement `CredentialEntryScreen` (Access Key, Secret Key, Session Token).
- Implement "Paste JSON" feature (Regex parsing of AWS CLI output).
- Implement "Dry Run" validation logic (calling `AuthRepository.validateCredentials` - effectively a list/get call).
**Verification:**
- Compose Previews.
- Interactive test: Pasting valid JSON populates fields.

## Task 9: Onboarding UI - Part 2 (Execution)
**Title:** Implement Provisioning and Success Screens
**Purpose:** Visualize the background provisioning process and confirm success.
**Behaviors:**
- Implement `ProvisioningScreen`:
  - Observes `AuthRepository.provisioningState` (or WorkerInfo).
  - Shows progress indicator and status text.
- Implement `SuccessScreen`:
  - "Go to Dashboard" button.
- Integrate the `ProvisioningWorker` trigger.
**Verification:**
- Compose Previews.
- Test: "Go to Dashboard" clears the back stack.

## Task 10: Permissions & Setup Trap
**Title:** Implement Permissions Flow and Application Trap
**Purpose:** Ensure the user cannot use the app without completing setup and granting permissions.
**Behaviors:**
- Implement `PermissionRequestScreen`:
  - Stage 1: Foreground Location (System Dialog).
  - Stage 2: Background Location (Deep Link to Settings).
- Implement `MainActivity` logic ("The Trap"):
  - On Launch, check `AuthState`.
  - If `SetupPending` or `Uninitialized`, route to Onboarding.
  - If `Authenticated` but permissions missing, route to Permissions.
**Verification:**
- Robolectric test verifying `MainActivity` routing logic based on mock Repo state.

## Task 11: Admin Upgrade Flow
**Title:** Implement Admin Upgrade in Settings
**Purpose:** Allow power users to upgrade to Admin status (R1.2000+).
**Behaviors:**
- Add "Admin Upgrade" entry point in `SettingsScreen`.
- Implement `AdminUpgradeScreen` (similar to Credential Entry).
- Re-use `ProvisioningWorker` with `Admin` mode/flag.
- Logic to replace Runtime keys with Admin keys.
**Verification:**
- Unit tests for `AdminUpgradeUseCase`.
- Verify Admin keys replace Runtime keys in Secure Storage.
