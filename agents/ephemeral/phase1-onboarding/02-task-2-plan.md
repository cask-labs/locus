# 02-task-2-plan.md

## Prerequisites: Human Action Steps

- None. The project structure is already scaffolded.

## Implementation Steps

### Step 1: Verify Package Structure
- **Action:** List files in `core/domain/src/main/kotlin/com/locus/core/domain` to confirm package structure.
- **Verification:** Confirm output shows `repository` and `usecase` directories.

### Step 2: Refactor Domain Result Types
- **Action:**
  - Move `core/domain/src/main/kotlin/com/locus/core/domain/DomainException.kt` to `core/domain/src/main/kotlin/com/locus/core/domain/result/DomainException.kt`.
  - Move `core/domain/src/main/kotlin/com/locus/core/domain/LocusResult.kt` to `core/domain/src/main/kotlin/com/locus/core/domain/result/LocusResult.kt`.
  - Refactor `DomainException.kt` to be a sealed hierarchy (or abstract base with sealed subclasses) defining:
    - `NetworkError` (Offline, Timeout, Server, Generic)
    - `AuthError` (InvalidCredentials, Expired, AccessDenied, Generic)
    - `S3Error` (BucketNotFound, Generic)
    - `BatteryCriticalException`
    - `ProvisioningError` (StackExists, Permissions, Quota, DeploymentFailed, Wait)
  - Refactor `LocusResult.kt` to match the package change.
- **Verification:** Read the modified files to confirm they match the spec and package structure.

### Step 3: Create Domain Models
- **Action:**
  - Create `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/BootstrapCredentials.kt`:
    - `accessKeyId: String`
    - `secretAccessKey: String`
    - `sessionToken: String` (Non-nullable, strictly required for Bootstrap)
  - Create `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/RuntimeCredentials.kt`:
    - `accessKeyId: String`
    - `secretAccessKey: String`
    - `sessionToken: String? = null` (Optional/Null for IAM User credentials)
  - Create `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/AuthState.kt`:
    - `Uninitialized`
    - `SetupPending`
    - `Authenticated`
  - Create `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/ProvisioningState.kt` using the following state definition as Source of Truth:
    - `Idle`
    - `ValidatingInput`
    - `VerifyingBootstrapKeys`
    - `DeployingStack(stackName)`
    - `WaitingForCompletion(stackName, status)`
    - `FinalizingSetup`
    - `Success`
    - `Failure(error: DomainException)`
  - Create `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/BucketValidationStatus.kt`:
    - `Validating`
    - `Available`
    - `Invalid`
- **Verification:** Read at least one created model file to verify correctness.

### Step 4: Create Repository Interfaces
- **Action:**
  - Create `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt` with methods:
    - `getAuthState(): Flow<AuthState>`
    - `getProvisioningState(): Flow<ProvisioningState>`
    - `updateProvisioningState(state: ProvisioningState)`
    - `validateCredentials(creds: BootstrapCredentials): LocusResult<Unit>`
    - `validateBucket(bucketName: String): LocusResult<BucketValidationStatus>`
    - `saveBootstrapCredentials(creds: BootstrapCredentials): LocusResult<Unit>`
    - `promoteToRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit>`
    - `replaceWithAdminCredentials(creds: RuntimeCredentials): LocusResult<Unit>`
    - `clearBootstrapCredentials(): LocusResult<Unit>`
    - `getRuntimeCredentials(): LocusResult<RuntimeCredentials>`
    - `recoverAccount(bucketName: String, deviceName: String): LocusResult<RuntimeCredentials>`
- **Verification:** Read the created `AuthRepository.kt` to verify the interface matches the requirements.

### Step 5: Verification (Unit Tests)
- **Action:**
  - Create `core/domain/src/test/kotlin/com/locus/core/domain/result/LocusResultTest.kt`.
  - Add tests to verify `LocusResult` instantiation and type safety.
  - Run `./scripts/run_local_validation.sh` to ensure no linting errors and that tests pass.
- **Completion Criteria:** All tests pass and lint checks succeed.
