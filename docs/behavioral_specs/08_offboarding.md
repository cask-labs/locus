# Behavioral Specification: Offboarding & Resource Cleanup

**Bounded Context:** This specification governs the decommissioning of cloud resources (CloudFormation Stacks, S3 Buckets) and the local application reset. This is a high-risk, "Danger Zone" workflow.

**Prerequisite:** The device must have an active network connection to perform cloud cleanup operations.

---

## 1. Initiation & Authorization
*   **R8.100** **When** the user initiates the "Destroy Cloud Resources" action from Settings, the system **shall** present a specific credential entry screen explaining the need for temporary Admin privileges.
*   **R8.200** **When** the user provides the "Offboarding Credentials" (Access Key, Secret Key, Session Token), the system **shall** validate them by attempting to list CloudFormation stacks.
*   **R8.300** **If** the credentials lack the necessary permissions (e.g., `cloudformation:ListStacks`), **then** the system **shall** deny access and display a specific error.

## 2. Resource Discovery
*   **R8.400** **When** authorization succeeds, the system **shall** scan the AWS account for all CloudFormation stacks that match the Locus Project Prefix (`locus-`).
*   **R8.500** **When** displaying the found stacks, the system **shall** validate the presence of the Project Tag (`project=locus`) and **shall** only allow selection of stacks that possess this valid tag.
*   **R8.600** **If** no matching stacks are found, **then** the system **shall** inform the user and offer to proceed directly to the Local App Reset.
*   **R8.700** **When** displaying the list, the system **shall** pre-select the stack associated with the current device (if applicable) but allow the user to modify the selection.

## 3. Confirmation (High Friction)
*   **R8.800** **When** the user confirms the selection, the system **shall** display the **First Warning Dialog** explicitly stating the number of stacks and the permanent loss of history.
*   **R8.900** **When** the user acknowledges the first warning, the system **shall** display the **Final Confirmation Dialog** requiring the user to type a specific confirmation phrase (e.g., "DESTROY") to enable the execution button.

## 4. Execution & Resilience (The Trap)
*   **R8.1000** **When** the execution begins, the system **shall** enter a persistent "Cleanup Mode" (Trap) that prevents navigation away from the progress screen.
*   **R8.1100** **If** the application is terminated or crashes during cleanup, **then** the system **shall** automatically resume the "Cleanup Mode" immediately upon the next launch.
*   **R8.1200** **When** deleting a stack, the system **shall** first iterate through all associated S3 Buckets and **shall** delete all objects (including versions and delete markers) to ensure the bucket is empty.
*   **R8.1300** **When** the buckets are empty, the system **shall** issuance the `DeleteStack` command to CloudFormation.
*   **R8.1400** **If** a specific resource deletion fails (e.g., Network Timeout, API Throttling), **then** the system **shall** display the error, pause the process, and provide a "Retry" action.
*   **R8.1500** **When** in the "Cleanup Mode", the system **shall** provide a "Force Reset" escape hatch (e.g., "Ignore Cloud Errors & Reset App") to allow the user to wipe the local device even if cloud cleanup is impossible.
*   **R8.1550** **When** the "Force Reset" option is presented, the system **shall** explicitly warn the user that skipping cleanup may leave orphaned resources in their AWS account that will continue to accrue costs.

## 5. Local Termination
*   **R8.1600** **When** the cloud cleanup is complete (or skipped via escape hatch), the system **shall** perform a "Factory Reset" of the local application.
*   **R8.1700** **When** performing the Factory Reset, the system **shall** delete all Databases, Preferences, Keystores (including EncryptedSharedPreferences), and temporary files.
*   **R8.1800** **When** the Factory Reset is complete, the system **shall** automatically restart the application process, returning the user to the Onboarding Welcome Screen.
