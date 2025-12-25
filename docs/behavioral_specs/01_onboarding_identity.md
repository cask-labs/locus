# Behavioral Specification: Onboarding & Identity

**Bounded Context:** This specification governs the lifecycle of User Identity, from initial credential validation and infrastructure provisioning (Bootstrap) to System Recovery and the transition to operational status (Runtime).

**Prerequisite:** This is the foundational specification. The system cannot perform any other function (Tracking, Sync, Visualization) until these conditions are met.

---

## Pre-Setup Guidance
*   **R1.050** **When** the application launches for the first time, the system **shall** display a disclaimer regarding potential cloud provider costs.
*   **R1.060** **When** the user requires assistance with credentials, the system **shall** provide an in-app guide for generating keys to maintain user context.

## Credential Validation
*   **R1.100** **When** the user provides cloud credentials (Access Key ID, Secret Access Key, Session Token), the system **shall** validate them by performing a "Dry Run" check against the identity and storage services.
*   **R1.150** **When** the user provides credentials, the system **shall** support parsing a structured JSON object (e.g., from CLI output) to automatically populate the required fields.
*   **R1.160** **If** JSON parsing fails (e.g., malformed JSON, missing fields), **then** the system **shall** surface a non-fatal validation error, **shall not** overwrite existing fields, and **shall not** attempt the "Dry Run" check.
*   **R1.200** **If** the "Dry Run" check fails (e.g., Invalid Signature, Permission Denied), **then** the system **shall** display a specific error message describing the failure reason and **shall not** proceed to the Choice screen.
*   **R1.300** **When** validating credentials, the system **shall** require the presence of a Session Token and treat the credentials as temporary.

## Infrastructure Provisioning (New Device)
*   **R1.400** **When** the user selects "Setup New Device", the system **shall** request a unique Device Name, pre-filling it with the system default model name.
*   **R1.500** **If** the user provides a Device Name that already corresponds to an existing storage resource in the account, **then** the system **shall** refuse the name and prompt for a unique alternative.
*   **R1.600** **When** the user initiates deployment, the system **shall** execute the provisioning process as a visible background task to ensure resilience against termination.
*   **R1.700** **When** provisioning infrastructure, the system **shall** use the provided "Bootstrap Keys" to create the required cloud resources.
*   **R1.800** **When** the resources are created, the system **shall** generate a new, restricted user identity (Runtime User) specifically for this device installation.
*   **R1.900** **When** the Runtime User is created, the system **shall** securely store the new Runtime Keys and **shall** permanently discard the Bootstrap Keys.
*   **R1.1000** **If** the provisioning process fails, **then** the system **shall** redirect the user back to the input fields and **shall not** attempt to automatically delete the resources.

## System Recovery (Link Existing Store)
*   **R1.1100** **When** the user selects "Link Existing Store", the system **shall** list all available storage buckets with names starting with the project prefix `locus-`.
*   **R1.1150** **The** system **shall** asynchronously validate the candidate buckets by verifying the presence of the `LocusRole: DeviceBucket` tag, distinguishing between "Validating", "Available", and "Invalid" states.
*   **R1.1160** **The** system **shall** prevent the selection of buckets marked as "Invalid".
*   **R1.1200** **If** no matching stores are found, **then** the system **shall** display a "No Locus stores found" message.
*   **R1.1300** **When** the user selects an existing store to link, the system **shall** create a **new** unique user identity (Runtime User) for this installation, distinct from any previous users associated with that store.
*   **R1.1350** **When** deploying access infrastructure for recovery, the system **shall** execute the process as a visible background task to ensure resilience against termination.
*   **R1.1400** **When** linking to an existing store, the system **shall** generate a new, unique `device_id` (UUID) for the current installation to prevent "Split Brain" data collisions with previous installations.
*   **R1.1500** **When** recovery is complete, the system **shall** perform a "Lazy Sync" (inventory scan) to populate the local history index without downloading bulk data.

## Permissions
*   **R1.1550** **When** requesting location access, the system **shall** request permissions in two distinct stages (Foreground then Background) if required by the underlying platform constraints.
*   **R1.1555** **If** the user denies the Foreground permission, **then** the system **shall not** request Background permission and **shall** inform the user that location functionality is unavailable until access is granted.
*   **R1.1560** **If** the user exits the application during the permission phase, **then** the system **shall** force the user back to the permission request screen upon the next launch ("Permission Trap").

## Onboarding Completion
*   **R1.1600** **When** the provisioning or recovery process completes successfully, the system **shall** display a Success Screen requiring manual confirmation (e.g., "Go to Dashboard").
*   **R1.1700** **When** the user confirms the Success Screen, the system **shall** clear the entire Onboarding navigation stack and transition to the Dashboard.
*   **R1.1800** **When** the transition to the Dashboard occurs, the system **shall** immediately begin the Tracking and Watchdog processes.
*   **R1.1900** **If** the user relaunches the app before completing the flow, **then** the system **shall** restore the last known provisioning state or return to the relevant step ("Setup Trap").

## Admin Upgrade
*   **R1.2000** **When** an existing user wishes to upgrade to "Admin" status, they **shall** initiate the flow via the Settings screen.
*   **R1.2100** **When** upgrading, the system **shall** request a new set of temporary "Bootstrap Keys" capable of creating admin resources.
*   **R1.2200** **When** provisioning the Admin identity, the system **shall** use a specialized Admin Template (`locus-admin.yaml`).
*   **R1.2300** **When** the Admin Runtime User is created, the system **shall** replace the existing Runtime Keys with the new Admin Keys and force an application restart/refresh.
*   **R1.2400** **The** Admin Identity **shall** possess a "Hybrid" role:
    *   **Write Access:** To the device's own S3 bucket (for self-tracking).
    *   **Read Access:** Broad `ListBucket` and `GetObject` permissions strictly scoped to resources matching the Locus Project Tag (`LocusRole: DeviceBucket`).
