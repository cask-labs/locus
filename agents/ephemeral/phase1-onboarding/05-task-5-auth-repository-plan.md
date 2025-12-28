# Task 5: AuthRepository Implementation Plan

## Goal
Implement the `AuthRepository` in the Data Layer to serve as the central broker for authentication, provisioning state, and credential management. This implementation bridges the Domain Layer (pure business logic) with the Infrastructure Layer (AWS Clients, Secure Storage).

## Prerequisites
- [x] Task 2 (Domain Models) completed.
- [x] Task 3 (Secure Storage) completed (assumed available).
- [x] Task 4 (AWS Clients) completed (assumed available).
- [ ] **Action:** Update `docs/technical_discovery/specs/domain_layer_spec.md` to include `scanForRecoveryBuckets` and refined `recoverAccount` logic.

## Implementation Steps

### Step 1: Update Domain Interface
**File:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt`
- Add `suspend fun scanForRecoveryBuckets(): LocusResult<List<String>>` to the interface.
- Ensure `replaceRuntimeCredentials` is present and documented for Admin Upgrade/Rotation.

### Step 2: Create AuthRepositoryImpl
**File:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
- **Class Structure:**
  - `class AuthRepositoryImpl @Inject constructor(...) : AuthRepository`
  - Injects: `S3Client`, `CloudFormationClient`, `SecureStorageDataSource`, `CoroutineScope` (Application Scope), `DispatcherProvider`.
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
    *   *Note:* This is for Admin Upgrade/Manual Rotation, not automated rotation.
  - `saveBootstrapCredentials`: Save Bootstrap, Emit `SetupPending`.
  - `clearBootstrapCredentials`: Delete Bootstrap.
- **Validation Logic:**
  - `validateBucket`: Call `S3Client.listBuckets()` (check existence) -> `S3Client.getBucketTags()` (check `LocusRole: DeviceBucket`). Return `Available`, `Validating`, or `Invalid`.
- **Recovery Logic:**
  - `scanForRecoveryBuckets`: Call `S3Client.listBuckets()`, filter by `locus-` prefix.
  - `recoverAccount(bucketName, deviceName)`:
    1. `S3Client.getBucketTags(bucketName)` -> Extract `aws:cloudformation:stack-name`.
    2. `CloudFormationClient.describeStack(stackName)` -> Parse Outputs for `AccessKeyId`, `SecretAccessKey`.
    3. Return `RuntimeCredentials`.

### Step 3: Configure Dependency Injection
**File:** `core/data/src/main/kotlin/com/locus/core/data/di/DataModule.kt` (or `AuthModule`)
- Bind `AuthRepositoryImpl` to `AuthRepository` using `@Binds`.
- Ensure it is scoped as `@Singleton`.

### Step 4: Unit Testing
**File:** `core/data/src/test/kotlin/com/locus/core/data/repository/AuthRepositoryImplTest.kt`
- **Test Cases:**
  - `init loads Authenticated state when Runtime credentials exist`
  - `promoteToRuntimeCredentials deletes bootstrap and updates state`
  - `recoverAccount parses stack outputs correctly` (Mock S3 Tags -> Stack Name -> Stack Outputs)
  - `scanForRecoveryBuckets filters correctly`
  - `validateBucket returns Invalid if tag is missing`

## Validation Checklist
- [ ] `AuthRepository` interface matches the updated Spec.
- [ ] `AuthRepositoryImpl` compiles and binds via Hilt.
- [ ] Unit tests pass covering all major flows (Provisioning, Recovery, State Init).
- [ ] `scanForRecoveryBuckets` successfully returns a list of candidate buckets (mocked).
