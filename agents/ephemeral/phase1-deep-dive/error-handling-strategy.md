# Technical Deep Dive: Error Handling Strategy

## Overview
CloudFormation (CFN) and S3 errors are often cryptic. This strategy maps technical AWS error codes to actionable, user-friendly `AppError` types and UI messages.

## Error Taxonomy

### 1. Pre-Flight Validation (Fast Fail)
Occurs during `ProvisioningState.VerifyingBootstrapKeys`.

| Source | Error Code | AppError | UI Message | Action |
|--------|------------|----------|------------|--------|
| STS | `InvalidClientTokenId` | `AuthError.InvalidCredentials` | "The Access Key ID is invalid." | Check input. |
| STS | `SignatureDoesNotMatch` | `AuthError.InvalidCredentials` | "The Secret Key is incorrect." | Check input. |
| STS | `ExpiredToken` | `AuthError.Expired` | "Your Session Token has expired." | Generate new keys. |
| Network | `UnknownHostException` | `NetworkError.Offline` | "No internet connection." | Check WiFi/Cell. |

### 2. CloudFormation Deployment (Slow Fail)
Occurs during `ProvisioningState.DeployingStack` or `WaitingForCompletion`.

| Source | Error Code / Status | AppError | UI Message | Action |
|--------|---------------------|----------|------------|--------|
| CFN | `AlreadyExistsException` | `ProvisioningError.StackExists` | "A device with this name ({name}) is already registered." | User must change the Device Name. |
| CFN | `InsufficientCapabilitiesException` | `ProvisioningError.Permissions` | "Your AWS keys lack permission to create IAM Users." | User needs to check their Policy. |
| CFN | `LimitExceededException` | `ProvisioningError.Quota` | "AWS Account limit reached (Stacks or Users)." | User needs to clean up AWS account. |
| CFN | `ROLLBACK_COMPLETE` (Status) | `ProvisioningError.DeploymentFailed` | "Deployment failed. The system rolled back changes." | Hard fail. Suggest manual cleanup or retry. |
| CFN | `ROLLBACK_IN_PROGRESS` (Status) | `ProvisioningError.Wait` | "Deployment failed. Cleaning up..." | UI shows blocking spinner until complete. |

### 3. S3 Interaction (Runtime)
Occurs during `ProvisioningState.FinalizingSetup` (Validation) or general use.

| Source | Error Code | AppError | UI Message | Action |
|--------|------------|----------|------------|--------|
| S3 | `404 Not Found` | `S3Error.BucketNotFound` | "The S3 bucket could not be found." | Critical. Check Console. |
| S3 | `403 Forbidden` | `AuthError.AccessDenied` | "Access denied to S3." | Runtime keys might be revoked. |

## Recovery Workflows

### Scenario: "Stack Name Taken"
1.  **State:** `Failure(StackExists)`.
2.  **UI:** Displays error "Name 'Pixel7' is taken."
3.  **Action:** User edits name input -> "Pixel7-Work".
4.  **Transition:** `Idle` -> `Verifying` -> `Deploying`.

### Scenario: "Rollback"
1.  **State:** `WaitingForCompletion` detects `ROLLBACK_IN_PROGRESS`.
2.  **UI:** Updates status to "Deployment failed, cleaning up...".
3.  **Logic:** Service *continues polling* until `ROLLBACK_COMPLETE` or `DELETE_COMPLETE`.
4.  **State:** Transitions to `Failure(DeploymentFailed)`.
5.  **UI:** "Setup failed. Please check AWS Console for details." (We cannot easily automate the "Why" of a rollback without deep log parsing).

### Scenario: "Network Drop during Polling"
1.  **State:** `WaitingForCompletion`.
2.  **Event:** `DescribeStacks` throws `IOException`.
3.  **Logic:**
    *   **Do NOT fail.**
    *   Exponential Backoff (Retry in 5s, 10s...).
    *   Keep state as `WaitingForCompletion`.
    *   **Reason:** CFN continues running on the server. We just need to reconnect to see the result.
