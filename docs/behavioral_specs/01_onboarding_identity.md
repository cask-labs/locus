# Behavioral Specification: Onboarding & Identity

**Bounded Context:** This specification governs the lifecycle of User Identity, from initial credential validation and infrastructure provisioning (Bootstrap) to System Recovery and the transition to operational status (Runtime).

**Prerequisite:** This is the foundational specification. The system cannot perform any other function (Tracking, Sync, Visualization) until these conditions are met.

---

## Credential Validation
*   **R1.100** **When** the user provides cloud credentials (Access Key ID, Secret Access Key, Session Token), the system **shall** validate them by performing a "Dry Run" check against the identity and storage services.
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
*   **R1.1100** **When** the user selects "Link Existing Store", the system **shall** list all available storage buckets matching the project prefix.
*   **R1.1200** **If** no matching stores are found, **then** the system **shall** display a "No Locus stores found" message.
*   **R1.1300** **When** the user selects an existing store to link, the system **shall** create a **new** unique user identity (Runtime User) for this installation, distinct from any previous users associated with that store.
*   **R1.1400** **When** linking to an existing store, the system **shall** generate a new, unique `device_id` (UUID) for the current installation to prevent "Split Brain" data collisions with previous installations.
*   **R1.1500** **When** recovery is complete, the system **shall** perform a "Lazy Sync" (inventory scan) to populate the local history index without downloading bulk data.

## Onboarding Completion
*   **R1.1600** **When** the provisioning or recovery process completes successfully, the system **shall** display a Success Screen requiring manual confirmation (e.g., "Go to Dashboard").
*   **R1.1700** **When** the user confirms the Success Screen, the system **shall** clear the entire Onboarding navigation stack and transition to the Dashboard.
*   **R1.1800** **When** the transition to the Dashboard occurs, the system **shall** immediately begin the Tracking and Watchdog processes.
*   **R1.1900** **If** the user relaunches the app before completing the flow, **then** the system **shall** restore the last known provisioning state or return to the relevant step ("Setup Trap").
