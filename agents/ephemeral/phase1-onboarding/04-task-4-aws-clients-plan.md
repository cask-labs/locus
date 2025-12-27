# Implementation Plan - Task 4: AWS Infrastructure Clients

## Prerequisites: Human Action Steps

Execute these refactoring operations using your IDE or terminal before implementation begins:

### Step 1: Move CloudFormation Template

*   **File:** `docs/technical_discovery/locus-stack.yaml`
*   **Action:** Move to Assets Directory
*   **New Path:** `core/data/src/main/assets/locus-stack.yaml`
*   **Note:** Create the `assets` directory if it does not exist.

## Implementation Steps

### Step 1: Add Dependencies
**Goal:** Enable AWS SDK for Kotlin in the Data module.

*   **File:** `core/data/build.gradle.kts`
*   **Action:** Add dependencies:
    *   `implementation(libs.aws.sdk.s3)`
    *   `implementation(libs.aws.sdk.cloudformation)`
    *   `implementation(libs.aws.sdk.sts)`
*   **Verification:** `./gradlew :core:data:dependencies`

### Step 2: Define Data Source Interfaces
**Goal:** Decouple Repositories from concrete AWS Clients.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/InfrastructureProvisioner.kt`
    *   Interface: `createStack`, `describeStack`, `getBucketTags`.
*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/RemoteStorageInterface.kt`
    *   Interface: `listBuckets`. (Note: `uploadTrack` is deferred to Phase 3/Task).

### Step 3: Implement Dynamic Credentials Provider
**Goal:** Allow S3Client to use keys stored in `SecureStorageDataSource`.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/LocusCredentialsProvider.kt`
*   **Logic:**
    *   Implements `aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider`.
    *   Injects `SecureStorageDataSource`.
    *   `resolve()` reads `RuntimeCredentials` from storage.
    *   Maps to `Credentials(accessKeyId, secretAccessKey, sessionToken)`.

### Step 4: Implement Bootstrap Client (CloudFormation)
**Goal:** Implement `InfrastructureProvisioner` using AWS SDK.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/CloudFormationClient.kt`
*   **Logic:**
    *   Accepts `BootstrapCredentials` in constructor (or method).
    *   Uses `StaticCredentialsProvider`.
    *   Region: Hardcoded `us-east-1`.
    *   Reads `locus-stack.yaml` from `assets`.
    *   Implements `createStack` (with `CAPABILITY_NAMED_IAM`), `describeStack` (polling logic), `getBucketTags`.

### Step 5: Implement Runtime Client (S3)
**Goal:** Implement `RemoteStorageInterface` using AWS SDK.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/S3Client.kt`
*   **Logic:**
    *   Injects `LocusCredentialsProvider`.
    *   Region: Configurable (passed in methods or config).
    *   Implements `listBuckets` (for Account Recovery).

### Step 6: Dependency Injection
**Goal:** Provide clients via Hilt.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/di/NetworkModule.kt`
*   **Logic:**
    *   `@Provides` `InfrastructureProvisioner`.
    *   `@Provides` `RemoteStorageInterface`.
    *   `@Provides` `LocusCredentialsProvider`.

### Step 7: Testing
**Goal:** Verify behavior with Mockk.

*   **File:** `core/data/src/test/kotlin/com/locus/core/data/source/remote/aws/CloudFormationClientTest.kt`
*   **File:** `core/data/src/test/kotlin/com/locus/core/data/source/remote/aws/S3ClientTest.kt`
*   **Logic:**
    *   Mock the underlying AWS SDK clients directly with Mockk (they are interfaces), or abstract the logic enough to test the flow.
    *   **Note:** AWS SDK for Kotlin v2 clients are interface-based, so they can be mocked directly with Mockk without special dynamic mocking; focus tests on the surrounding logic and interactions with these mocked clients.

## Validation Strategy
*   **Compilation:** Ensure `:core:data` compiles with new dependencies.
*   **Unit Tests:** Run `./gradlew :core:data:testDebugUnitTest`.
*   **Asset Check:** Verify `locus-stack.yaml` is readable via `InstrumentationRegistry` or ClassLoader in tests.
