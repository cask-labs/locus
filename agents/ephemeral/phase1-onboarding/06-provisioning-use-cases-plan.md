# Implementation Plan - Provisioning Use Cases (Task 6)

**Goal:** Implement the Provisioning and Account Recovery business logic, adhering to the pure domain architecture by abstracting infrastructure behind new Domain Interfaces and orchestrating workflows via Use Cases.

## Prerequisites: Human Action Steps
*   None required. The agent will handle the necessary refactoring as part of the implementation.

## Phase 1: Domain & Infrastructure Definition

### Step 1: Refactor `AuthRepository`
**Action:** Remove orchestration responsibilities from the repository to enforce strict State Management.
*   **Target:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt`
*   **Target:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
*   **Changes:**
    *   Remove `recoverAccount(bucketName, deviceName)`
    *   Remove `scanForRecoveryBuckets()`
    *   Remove `validateBucket(bucketName)`
*   **Verification:** Ensure compilation succeeds (logic moved to Use Cases in subsequent steps).

### Step 2: Define Domain Infrastructure Interfaces
**Action:** Create pure Kotlin interfaces for AWS services in the Domain Layer.
*   **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/CloudFormationClient.kt`
    *   `suspend fun createStack(template: String, parameters: Map<String, String>): LocusResult<String>` (Returns Stack ID)
    *   `suspend fun describeStack(stackName: String): LocusResult<StackStatus>`
    *   *Note on `template`:* See Steps 8 and 9 for details on how this template string is loaded.
*   **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/S3Client.kt`
    *   `suspend fun listBuckets(): LocusResult<List<String>>`
    *   `suspend fun getBucketTags(bucketName: String): LocusResult<Map<String, String>>`
*   **Verification:** Verify files exist and contain correct signatures.

### Step 3: Define Configuration Repository
**Action:** Define the missing repository for Identity Management.
*   **File:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/ConfigurationRepository.kt`
    *   `suspend fun initializeIdentity(deviceId: String, salt: String): LocusResult<Unit>`
*   **Verification:** Verify interface definition.

### Step 4: Implement Infrastructure Clients
**Action:** Implement the interfaces in the Data Layer using `AwsClientFactory`.
*   **File:** `core/data/src/main/kotlin/com/locus/core/data/infrastructure/CloudFormationClientImpl.kt`
*   **File:** `core/data/src/main/kotlin/com/locus/core/data/infrastructure/S3ClientImpl.kt`
*   **Verification:** Verify implementations use `AwsClientFactory` correctly.

### Step 5: Implement Configuration Repository
**Action:** Implement a basic version of the Configuration Repository.
*   **File:** `core/data/src/main/kotlin/com/locus/core/data/repository/ConfigurationRepositoryImpl.kt`
*   **Details:** Can use SharedPreferences or DataStore to persist `deviceId` and `salt`.
*   **Verification:** Verify implementation.

### Step 6: Update Dependency Injection
**Action:** Bind the new interfaces in Hilt.
*   **Target:** `core/data/src/main/kotlin/com/locus/core/data/di/DataModule.kt`
*   **Changes:** Add `@Binds` for `CloudFormationClient`, `S3Client`, and `ConfigurationRepository`.
*   **Verification:** Check module for new bindings.

## Phase 2: Use Case Implementation

### Step 7: Implement `ScanBucketsUseCase`
**Action:** Create the discovery logic.
*   **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/ScanBucketsUseCase.kt`
*   **Logic:**
    1.  Call `S3Client.listBuckets`.
    2.  Filter for prefix `locus-`.
    3.  For each bucket, call `S3Client.getBucketTags`.
    4.  Validate `LocusRole: DeviceBucket` tag.
    5.  Return `List<BucketValidationStatus>` (Available/Invalid).
*   **Verification:** Unit Test `ScanBucketsUseCaseTest`.

### Step 8: Implement `ProvisioningUseCase`
**Action:** Create the new device setup logic.
*   **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/ProvisioningUseCase.kt`
*   **Shared Constants:** Define private constants `POLL_INTERVAL = 5_000L` and `POLL_TIMEOUT = 600_000L` (10 mins) to ensure consistency.
*   **Logic:**
    1.  Validate Device Name input.
    2.  Load Template: `locus-stack.yaml` from application resources.
        *   *Implementation Note:* Create a simple private helper `loadResource(resId: Int): String` (or similar injection) to avoid complex abstraction (YAGNI).
    3.  Call `CloudFormationClient.createStack` with parameters.
    4.  **Polling Strategy:**
        *   Interval: **5 seconds** fixed delay.
        *   Timeout: **10 minutes** hard stop.
        *   Loop `CloudFormationClient.describeStack`.
    5.  Call `AuthRepository.updateProvisioningState` with progress.
    6.  On Success:
        *   Parse Stack Outputs (Access Key, Secret, Bucket).
        *   Generate new UUID for `device_id`.
        *   Generate Salt: **SecureRandom 32-byte Hex String**.
        *   Call `ConfigurationRepository.initializeIdentity`.
        *   Call `AuthRepository.promoteToRuntimeCredentials`.
*   **Verification:** Unit Test `ProvisioningUseCaseTest` (Mocking clients).

### Step 9: Implement `RecoverAccountUseCase`
**Action:** Create the account linking logic.
*   **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/RecoverAccountUseCase.kt`
*   **Shared Constants:** Use the same `POLL_INTERVAL` and `POLL_TIMEOUT` values.
*   **Logic:**
    1.  Load Template: Reuse `locus-stack.yaml` from application resources (using same helper approach).
    2.  Call `CloudFormationClient.createStack` with parameters (Existing Bucket).
    3.  **Polling Strategy:**
        *   Interval: **5 seconds** fixed delay.
        *   Timeout: **10 minutes** hard stop.
        *   Loop `CloudFormationClient.describeStack`.
    4.  Call `AuthRepository.updateProvisioningState` with progress.
    5.  On Success:
        *   Parse Stack Outputs.
        *   **Identity Generation:** Explicitly generate a **new UUID** for `device_id` and a **new Salt**.
            *   *Reference:* `R1.1400` mandates a new device ID to prevent "Split Brain" data collisions with previous installations. **Do not reuse existing IDs.**
        *   Call `ConfigurationRepository.initializeIdentity`.
        *   Call `AuthRepository.promoteToRuntimeCredentials`.
*   **Verification:** Unit Test `RecoverAccountUseCaseTest`.

## Phase 3: Documentation & Verification

### Step 10: Update Domain Specification
**Action:** Ensure the spec reflects the architecture.
*   **Target:** `docs/technical_discovery/specs/domain_layer_spec.md`
*   **Changes:** Add `ProvisioningUseCase`, `RecoverAccountUseCase`, `ScanBucketsUseCase`. Define Client Interfaces. Remove old Repo methods.

### Step 11: Final Validation
**Action:** Run local validation.
*   **Command:** `./scripts/run_local_validation.sh`
*   **Goal:** Pass compilation, linting, and all unit tests.
