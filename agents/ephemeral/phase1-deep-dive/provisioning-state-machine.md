# Technical Deep Dive: Provisioning State Machine

## Overview
The provisioning process is the most critical and fragile part of the Onboarding phase. It involves asynchronous network operations (CloudFormation) that can take minutes to complete. The state must be preserved across application restarts (process death) and provide granular feedback to the user.

## State Machine Definition

The `ProvisioningState` is a sealed class hierarchy that represents the detailed progress of the provisioning workflow. This is distinct from the high-level `AuthState`.

```kotlin
sealed class ProvisioningState {
    /**
     * The initial state. No provisioning is in progress.
     */
    object Idle : ProvisioningState()

    /**
     * Step 1: Validating user input (Regex, Empty checks) locally.
     */
    object ValidatingInput : ProvisioningState()

    /**
     * Step 2: "Dry Run" / Pre-flight check.
     * The system uses the provided "Bootstrap Keys" to call STS GetCallerIdentity.
     * This ensures the keys are valid before attempting complex CFN operations.
     */
    object VerifyingBootstrapKeys : ProvisioningState()

    /**
     * Step 3: Stack Deployment Initiated.
     * The `CreateStack` (or `UpdateStack` for Admin) call has been made.
     * @param stackName The user-defined device name.
     */
    data class DeployingStack(
        val stackName: String
    ) : ProvisioningState()

    /**
     * Step 4: Polling for Completion.
     * The stack ID has been received, and we are polling `DescribeStacks`.
     * @param stackName The user-defined device name.
     * @param status The last known CFN status (e.g., "CREATE_IN_PROGRESS").
     */
    data class WaitingForCompletion(
        val stackName: String,
        val status: String
    ) : ProvisioningState()

    /**
     * Step 5: Finalizing.
     * Stack is CREATE_COMPLETE. The app is extracting Outputs and saving Runtime keys.
     * This is the "Atomic Promotion" phase.
     */
    object FinalizingSetup : ProvisioningState()

    /**
     * Terminal State: Success.
     * The user has been successfully promoted to Runtime.
     */
    object Success : ProvisioningState()

    /**
     * Terminal State: Failure.
     * Something went wrong. The user must take action.
     * @param error The domain error detailing the cause.
     */
    data class Failure(val error: AppError) : ProvisioningState()
}
```

## Resilience & Persistence Strategy

The `ProvisioningService` (Foreground Service) is the "Keeper of the State". To survive process death (e.g., low memory kill during the 2-minute stack creation), we use a lightweight persistence mechanism:

1.  **Persistence Target:** `SharedPreferences` (distinct from EncryptedSharedPreferences).
2.  **Persisted Field:** `provisioning_active_stack_name` (String?).
3.  **Logic:**
    *   **On `DeployingStack` entry:** Write `stackName` to prefs.
    *   **On Service Start:** Check if `provisioning_active_stack_name` exists.
        *   **If Yes:** Immediate transition to `WaitingForCompletion(stackName, "UNKNOWN")` and resume polling.
        *   **If No:** Remain `Idle`.
    *   **On `Success` or `Failure`:** Remove `stackName` from prefs.

## Admin Upgrade Flow

The Admin Upgrade uses the **exact same state machine**, but with a different internal execution path (deploying `locus-admin.yaml`).

*   **State:** `DeployingStack` (conceptually "Deploying Upgrade").
*   **Context:** The `ProvisioningUseCase` knows if it is running in "Standard" or "Admin" mode based on the function called (`provisionDevice` vs `upgradeToAdmin`).

## UI Interface

The UI (ViewModel) observes `Flow<ProvisioningState>` from the Repository/Service.

*   **Progress Indicator:** Visible in `Verifying`, `Deploying`, `Waiting`, `Finalizing`.
*   **Status Text:**
    *   `Verifying` -> "Checking credentials..."
    *   `Deploying` -> "Initiating deployment..."
    *   `Waiting` -> "Provisioning cloud resources (Status: $status)..."
    *   `Finalizing` -> "Securing identity..."
