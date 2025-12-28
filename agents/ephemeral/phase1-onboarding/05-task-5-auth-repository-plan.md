# Task 5: AuthRepository Implementation Plan

## Goal
Implement the `AuthRepository` in the Data Layer to serve as the central broker for authentication, provisioning state, and credential management. This implementation bridges the Domain Layer (pure business logic) with the Infrastructure Layer (AWS Clients, Secure Storage).

## Prerequisites
- [x] Task 2 (Domain Models) completed.
- [x] Task 3 (Secure Storage) completed (assumed available).
- [x] Task 4 (AWS Clients) completed (assumed available).
- [ ] **Action:** Update `docs/technical_discovery/specs/domain_layer_spec.md` to include `scanForRecoveryBuckets` and refined `recoverAccount` logic.

## Implementation Steps

### Step 1: Update Domain Layer
**File:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt`
- Add `suspend fun scanForRecoveryBuckets(): LocusResult<List<String>>` to the interface.
- Ensure `replaceRuntimeCredentials` is present and documented for Admin Upgrade/Rotation.

**File:** `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/ProvisioningState.kt`
- Add `data object ValidatingBucket : ProvisioningState()` to the sealed class.
- **Purpose:** Represents the specific state where the system is checking bucket existence and tags via the network, distinct from local input validation (`ValidatingInput`).

**File:** `core/domain/src/main/kotlin/com/locus/core/domain/result/DomainException.kt`
- Add `sealed class RecoveryError(message: String) : DomainException(message)` to the file.
- Add subclasses:
  - `data object MissingStackTag : RecoveryError("Bucket missing stack name tag")`
  - `data object InvalidStackOutputs : RecoveryError("Stack outputs missing required credentials")`

### Step 2: Create AwsClientFactory
**File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/AwsClientFactory.kt`
- **Purpose:** Provide a mechanism to create ephemeral AWS clients (S3, CloudFormation) on-demand using temporary `BootstrapCredentials`. This avoids injecting `ViewModelScoped` or mismatched clients into the Singleton Repository.
- **Class Structure:**
  - `@Singleton class AwsClientFactory @Inject constructor()`
- **Methods:**
  - `fun createBootstrapS3Client(creds: BootstrapCredentials): S3Client`
  - `fun createBootstrapCloudFormationClient(creds: BootstrapCredentials): CloudFormationClient`
- **Configuration:**
  - Must apply the standard configurations defined in `Network_Infrastructure_Spec` (Timeouts, User-Agent, Region = `us-east-1` for bootstrap).
  - Use `StaticCredentialsProvider` wrapping the passed credentials.

### Step 3: Create AuthRepositoryImpl
**File:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
- **Class Structure:**
  - `class AuthRepositoryImpl @Inject constructor(...) : AuthRepository`
  - Injects: `AwsClientFactory`, `SecureStorageDataSource`, `CoroutineScope` (Application Scope), `DispatcherProvider`.
  - **Crucial:** Do **NOT** inject `S3Client` or `CloudFormationClient` directly (avoid scope mismatch).
- **State Management:**
  - Define `private val _authState = MutableStateFlow<AuthState>(AuthState.Uninitialized)`
  - Define `private val _provisioningState = MutableStateFlow<ProvisioningState>(ProvisioningState.Idle)`
  - **Init Block:** Launch coroutine to load initial state from `SecureStorageDataSource` asynchronously.
    - If `SecureStorage` has Runtime Keys -> Emit `Authenticated`.
    - Else If `SecureStorage` has Bootstrap Keys -> Emit `SetupPending`.
    - Else -> Emit `Uninitialized`.
- **Credential Logic:**
  - `promoteToRuntimeCredentials(creds)`:
    1. Save `RuntimeCredentials` to Secure Storage.
    2. **Delete** `BootstrapCredentials` from Secure Storage (Critical Step).
    3. Emit `AuthState.Authenticated`.
  - `replaceRuntimeCredentials(creds)`:
    1. Save `RuntimeCredentials` (overwriting old ones).
    2. Emit `AuthState.Authenticated` (refreshing state).
  - `saveBootstrapCredentials`: Save Bootstrap, Emit `SetupPending`.
  - `clearBootstrapCredentials`: Delete Bootstrap.
- **Validation Logic (Ephemeral):**
  - `validateBucket(bucketName)`:
    1. Get Bootstrap Creds from Storage. If missing, return `Invalid` immediately.
    2. Emit `ProvisioningState.ValidatingBucket` state to `_provisioningState` to indicate network check is in progress.
    3. `clientFactory.createBootstrapS3Client(creds).use { client -> ... }`
    4. **Bucket Existence:** Call `client.listBuckets()`.
       - If bucket is not found or access is denied, return `Invalid`.
    5. **Tag Validation:** Call `client.getBucketTagging(bucketName)`.
       - Check for `LocusRole: DeviceBucket` tag.
       - If tag set is missing, malformed, or tag is absent, return `Invalid`.
    6. **Outcome:**
       - If all checks pass, return `Available`.
       - For any other error (network, parsing, etc.), return `Invalid`.
- **Recovery Logic (Ephemeral):**
  - `scanForRecoveryBuckets()`:
    1. Get Bootstrap Creds.
    2. `clientFactory.createBootstrapS3Client(creds).use { client -> ... }`
    3. `client.listBuckets()` -> Filter list client-side for bucket names where `bucket.name.startsWith("locus-")`.
    4. Return `LocusResult<List<String>>` containing candidate bucket names as plain strings.
    5. **Note:** Perform no additional validation (e.g., tags or stack metadata) at this stage to avoid N+1 network calls. Deeper validation is delegated to `validateBucket` or `recoverAccount`.
  - `recoverAccount(bucketName, deviceName)`:
    1. Get Bootstrap Creds.
    2. `clientFactory.createBootstrapS3Client(creds).use { client -> ... }`
    3. **Stack Name Extraction:** Call `client.getBucketTagging(bucketName)`.
       - Extract `aws:cloudformation:stack-name`.
       - If tag is missing/empty or call fails (e.g., `NoSuchTagSet`), return `LocusResult.Failure(RecoveryError.MissingStackTag)`.
    4. `clientFactory.createBootstrapCloudFormationClient(creds).use { cfClient -> ... }`
    5. **Stack Output Parsing:** Call `cfClient.describeStacks(stackName)`.
       - Parse Outputs for `AccessKeyId` and `SecretAccessKey`.
       - If `describeStacks` fails or outputs are missing required keys, return `LocusResult.Failure(RecoveryError.InvalidStackOutputs)`.
    6. Return `RuntimeCredentials`.

### Step 4: Configure Dependency Injection
**File:** `core/data/src/main/kotlin/com/locus/core/data/di/DataModule.kt`
- Bind `AuthRepositoryImpl` to `AuthRepository` using `@Binds`.
- Ensure it is scoped as `@Singleton`.
- `AwsClientFactory` will be provided automatically via `@Inject` constructor + `@Singleton` annotation.

### Step 5: Unit Testing
**File:** `core/data/src/test/kotlin/com/locus/core/data/repository/AuthRepositoryImplTest.kt`
- **Mocks:** Mock `AwsClientFactory` to return Mock S3/CF clients.
- **Test Cases:**
  - `init loads Authenticated state when Runtime credentials exist`
  - `promoteToRuntimeCredentials deletes bootstrap and updates state`
  - `recoverAccount uses ephemeral client and parses stack outputs correctly`
  - `scanForRecoveryBuckets filters correctly` (Verify client-side logic)
  - `validateBucket returns Invalid if tag is missing`

## Validation Checklist
- [ ] `AuthRepository` interface matches the updated Spec.
- [ ] `ProvisioningState` and `DomainException` updated with new types.
- [ ] `AwsClientFactory` implemented with correct timeouts and user-agent.
- [ ] `AuthRepositoryImpl` compiles and binds via Hilt.
- [ ] Unit tests pass covering all major flows (Provisioning, Recovery, State Init).
- [ ] `scanForRecoveryBuckets` successfully returns a list of candidate buckets (mocked).
- [ ] **Run `scripts/run_local_validation.sh` and ensure all checks pass.**

## Reference: Domain Model Updates

### ProvisioningState
Added `ValidatingBucket` to support detailed UI feedback during `validateBucket` operations.
```kotlin
sealed class ProvisioningState {
    // ... existing states
    data object ValidatingBucket : ProvisioningState() // Added
}
```

### RecoveryError
Added strict error types for recovery failures to enable precise error handling.
```kotlin
sealed class RecoveryError(message: String) : DomainException(message) {
    // The bucket exists but lacks the link to the CloudFormation stack
    data object MissingStackTag : RecoveryError("Bucket missing stack name tag")

    // The CloudFormation stack exists but doesn't have the required Output keys
    data object InvalidStackOutputs : RecoveryError("Stack outputs missing required credentials")
}
```
