# Architecture Selection: Phase 1

## Selected Path: Path C (WorkManager + Domain State)

We will proceed with **Path C**. While Path B (Foreground Service) was initially considered for its real-time feedback capabilities, **WorkManager** is the correct, modern Android architecture for guaranteeing the completion of long-running operations like CloudFormation deployment, especially under Android 14+ restrictions.

## Key Components

### 1. `AuthRepository` (The Brain)
*   **Role:** Single Source of Truth for Identity State.
*   **Responsibility:**
    *   Manages the State Machine (`Uninitialized` -> `Provisioning` -> `Authenticated`).
    *   Persists the *State* (e.g., "Step 3/5: Creating Bucket") to disk immediately.
    *   Holds the "Bootstrap Keys" (Memory) and manages the swap to "Runtime Keys" (EncryptedStorage).
    *   Exposes `StateFlow<AuthState>` for the UI.

### 2. `ProvisioningWorker` (The Engine)
*   **Type:** `CoroutineWorker`.
*   **Configuration:** `Expedited` + `ListenableWorker.setForeground()`.
*   **Responsibility:**
    *   Executes the CloudFormation deployment reliably.
    *   Updates a high-priority Notification ("Setting up Locus... 40%").
    *   Calls the underlying Domain Use Cases.
    *   **Security:** Does **not** accept keys as input data. It reads them from the secure memory/disk cache managed by `AuthRepository`.

### 3. `ProvisioningIdentityUseCase` (The Logic)
*   **Role:** Domain Logic encapsulation.
*   **Responsibility:**
    *   Orchestrates the sequence: Validate -> Create Stack -> Poll -> Parse Outputs.
    *   Contains the business rules for "Validation" and "Recovery Discovery".

### 4. `CloudFormationClient` / `S3Client` (The Tools)
*   **Role:** Data Layer.
*   **Responsibility:**
    *   Wrap the AWS SDK.
    *   Handle the raw API calls.

## Data Flow (Happy Path)

1.  **UI:** User enters Creds -> `AuthViewModel`.
2.  **VM:** Calls `AuthRepository.validate(creds)`.
3.  **Repo:** Checks creds (Dry Run). If OK, stores them in Memory (and optionally EncryptedPrefs for Worker access).
4.  **UI:** User clicks "Start Setup".
5.  **VM:** Enqueues `ProvisioningWorker` (Expedited).
6.  **Worker:** Starts. Calls `AuthRepository.startProvisioning(config)`.
7.  **Repo:** Updates State to `Provisioning`. Saves to Disk.
8.  **Worker:** Calls `ProvisioningIdentityUseCase.execute()`.
9.  **UseCase:**
    *   Creates Stack (via Client).
    *   Polls status loop...
    *   Updates `Repo` with "Logs" (e.g., "Bucket Created...").
10. **Repo:** Emits new State -> UI updates "Console View".
11. **UseCase:** Stack Complete. Returns Outputs.
12. **Repo:** Performs "Atomic Promotion" (Save Runtime Keys, Delete Bootstrap Keys). Updates State to `Authenticated`.
13. **Worker:** Returns `Result.success()`.
14. **UI:** Detects `Authenticated` -> Navigates to Dashboard.

## Failure Handling (The "Trap")

If the app crashes at Step 9:
1.  **Relaunch:** `MainActivity` starts.
2.  **Check:** `AuthRepository` initializes. Reads State from Disk ("Provisioning").
3.  **Action:**
    *   If "Provisioning" + "Has Stack ID" -> Check Status.
        *   If Stack `CREATE_COMPLETE` -> Finish Promotion.
        *   If Stack `ROLLBACK_COMPLETE` -> Move to `Failure`.
        *   If Stack `CREATE_IN_PROGRESS` -> WorkManager will likely restart the Worker automatically (Resilience).
    *   If "Provisioning" + "No Stack ID" -> Move to `Failure` ("Setup Interrupted").
