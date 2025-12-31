# Code Review Findings: Provisioning & Recovery Logic

**Date:** 2025-12-31 14:02:49
**Branch:** `jules-1838885202015464227-9537db40`

### **1. Executive Summary**
The recent changes implement the "Provisioning" and "Account Recovery" use cases in the Domain layer (`ProvisioningUseCase`, `RecoverAccountUseCase`) along with extensive unit tests. While the functional logic addresses the requirements and test coverage is high, the implementation suffers from significant code duplication, "magic string" usage, and a potentially fragile polling mechanism.

### **2. Detailed Analysis**

#### **2.1. Code Duplication (DRY Violation)**
*   **Problem:** `ProvisioningUseCase` and `RecoverAccountUseCase` share a substantial amount of identical or near-identical logic.
*   **Specifics:**
    *   **Stack Creation:** Both prepare parameters and call `cloudFormationClient.createStack`.
    *   **Polling Loop:** Both implement an identical `while` loop structure:
        *   Checks `timeProvider.currentTimeMillis()`.
        *   Calls `describeStack`.
        *   Parses `StackDetails` status.
        *   Handles `CREATE_COMPLETE`, `CREATE_FAILED`, `ROLLBACK_...` identically.
        *   Updates `ProvisioningState.WaitingForCompletion`.
    *   **Output Parsing:** Both duplicate the logic to validation and extraction of `RuntimeAccessKeyId`, `RuntimeSecretAccessKey`, `BucketName` (or pass-through), and `AccountId`.
    *   **Success Handling:** Both promote credentials to `RuntimeCredentials` and initialize identity using identical steps.
*   **Impact:** This duplication increases the maintenance burden. A change to the polling strategy (e.g., adding exponential backoff) or error handling must be applied in two places, increasing the risk of divergence and bugs.

#### **2.2. Polling Loop Robustness (Fail Fast)**
*   **Problem:** The polling loop in both use cases indiscriminately retries *all* errors returned by `describeStack` until the 10-minute timeout expires.
*   **Specifics:** The code catches failures from `cloudFormationClient.describeStack` (implied by the `LocusResult` check) but does not distinguish between them.
*   **Deep Dive:** If `describeStack` returns a permanent error (e.g., `AccessDenied` due to revoked bootstrap keys, or `ThrottlingException` without backoff), the current implementation effectively "swallows" the error for the duration of the loop, waiting `POLL_INTERVAL` to retry. This results in a poor user experience where the user waits 10 minutes for a failure that was known immediately.
*   **Justification:** The system must distinguish between *Transient* errors (Network, Timeout) which should be retried, and *Permanent* errors (Auth, Validation) which should cause an immediate failure.

#### **2.3. Hardcoded Values & Constants**
*   **Problem:** Constants are scattered and duplicated across files.
*   **Specifics:**
    *   `POLL_INTERVAL` (5000ms) and `POLL_TIMEOUT` (600,000ms) are defined in companion objects of both Use Cases.
    *   Output keys: `"RuntimeAccessKeyId"`, `"RuntimeSecretAccessKey"`, `"BucketName"`.
    *   Tag keys: `"LocusRole"`, `"DeviceBucket"`, `"aws:cloudformation:stack-name"`.
*   **Impact:** "Magic Strings" make the code brittle and harder to refactor. A typo in one file could break the contract with the CloudFormation template.

#### **2.4. Test Coverage**
*   **Observation:** Test coverage is generally excellent, covering happy paths, timeouts, stack failures, and credential promotion failures.
*   **Minor Note:** `RecoverAccountUseCaseTest` verifies failure paths for `ProvisioningState.ValidatingBucket` but does not explicitly verify that this state is emitted during the *successful* flow (though it likely is).

### **3. Proposed Fixes**

#### **3.1. Extract Shared Logic: `StackProvisioningService`**
Create a Domain Service (`StackProvisioningService`) to encapsulate the CloudFormation lifecycle. This service will handle:
*   **Input:** Stack Name, Template, Parameters, Bootstrap Credentials.
*   **Action:** Create Stack -> Poll for Completion -> Parse Outputs.
*   **Output:** `LocusResult<StackOutputs>` (where `StackOutputs` is a strong type containing Access Keys, Bucket, etc.).

**Benefit:** `ProvisioningUseCase` and `RecoverAccountUseCase` become lightweight orchestrators that simply define *what* stack to build (parameters), delegating the *how* to the service.

#### **3.2. Centralize Constants**
Create `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/InfrastructureConstants.kt`.
*   Move `POLL_INTERVAL`, `POLL_TIMEOUT` here.
*   Create objects for `StackOutputs` (keys) and `StackTags`.

#### **3.3. Refine Polling Logic**
Update the extracted polling mechanism to implement "Fail Fast":
*   Retry only on `DomainException.NetworkError` or `DomainException.Timeout`.
*   Immediately return `Failure` on `DomainException.AuthError`, `ProvisioningError`, or generic exceptions.

#### **3.4. Update Tests**
*   Refactor tests to verify the integration with the new Service or mock it.
*   Ensure that the "Fail Fast" logic is covered by new test cases in the `StackProvisioningServiceTest`.
