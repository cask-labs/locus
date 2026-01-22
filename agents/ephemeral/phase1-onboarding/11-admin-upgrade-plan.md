# Implementation Plan - Task 11: Admin Upgrade Flow (Revised Strategy)

## Context & Findings
This plan implements the "Admin Upgrade" feature defined in `docs/behavioral_specs/01_onboarding_identity.md` (R1.2000 - R1.2400) and refines the Recovery strategy.

**Strategic Pivot:**
- **Recovery Strategy:** We are shifting from a "Linker" model (create new stack) to a **"Takeover" model** (update existing stack). This simplifies permission management and ensures strict resource ownership.
- **Key Rotation:** To secure the account during Takeover, we introduce a `KeySerial` parameter. Changing this value forces CloudFormation to replace the IAM User and keys, invalidating lost credentials.
- **Data Safety:** We explicitly **REMOVE** the `BucketName` parameter from the template. The stack *owns* the bucket; preventing the template from conditionally removing the bucket resource is paramount for data safety.

## 1. Documentation Updates
*Ensure specifications match the architectural reality.*

1.  **Update Behavioral Specification**
    - **File:** `docs/behavioral_specs/01_onboarding_identity.md`
    - **Action:** Update **R1.2200** to reflect the "Single Template" strategy (using `IsAdmin` parameter) instead of requiring a separate `locus-admin.yaml`.
    - **Verification:** Read the file to confirm the text change.

## 2. Data Layer & Assets
*Establish the infrastructure and data foundations for the Admin identity.*

2.  **Fix Infrastructure Constants Mismatch**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/InfrastructureConstants.kt`
    - **Action:** Update `OUT_RUNTIME_ACCESS_KEY` to `"AccessKeyId"` and `OUT_RUNTIME_SECRET_KEY` to `"SecretAccessKey"` to match the CloudFormation template outputs exactly.
    - **Verification:** Read the file to confirm the constants match `locus-stack.yaml` outputs.

3.  **Modify CloudFormation Template**
    - **File:** `core/data/src/main/assets/locus-stack.yaml`
    - **Action:** Modify the existing template to support Admin capabilities, Key Rotation, and "Takeover" safety.
    - **Details:**
        - **Parameters:**
            - **REMOVE:** `BucketName` (Critical Safety Fix).
            - **ADD:** `IsAdmin` (Type: String, Default: "false", AllowedValues: ["true", "false"]).
            - **ADD:** `KeySerial` (Type: String, Default: "1", Description: "Change to force Key Rotation").
        - **Resources:**
            - `LocusUser`: Update `UserName` to `!Sub "locus-user-${StackName}-${KeySerial}"`.
            - `LocusDataBucket`: Remove `CreateNewBucket` condition. Bucket is now unconditional.
            - `StandardPolicy` vs `AdminPolicy`: Use Conditional Resources (toggled by `IsAdmin`) instead of complex inline `Fn::If` blocks for readability.
        - **Outputs:**
            - **ADD:** `IsAdmin` (Value: !Ref IsAdmin). Essential for state persistence.
    - **Verification:** Run `cfn-lint core/data/src/main/assets/locus-stack.yaml` to verify conditional syntax.

4.  **Update Runtime Credentials Schema**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/RuntimeCredentials.kt`
    - **Action:** Add `val stackName: String` (Non-nullable, mandatory) and `val isAdmin: Boolean = false`.
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/model/RuntimeCredentialsDto.kt`
    - **Action:** Add fields to DTO and update mappers.
    - **Verification:** Read `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/RuntimeCredentials.kt` to confirm the new properties were added.

5.  **Enhance CloudFormation Client**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/CloudFormationClient.kt`
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/infrastructure/CloudFormationClientImpl.kt`
    - **Action:** Add `updateStack` method.
    - **Signature:** `suspend fun updateStack(stackName: String, templateBody: String, parameters: Map<String, String>)`
    - **Details:**
        - Must include `capabilities = listOf(Capability.CapabilityNamedIam)`.
        - Must handle "No updates are to be performed" as success (return existing outputs).
        - Must return `StackProvisioningResult` (StackId + **New Outputs**).
    - **Verification:** Read `core/data/src/main/kotlin/com/locus/core/data/infrastructure/CloudFormationClientImpl.kt` to confirm the `updateStack` method was added correctly.

6.  **Verify Resource Provider**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/ResourceProvider.kt`
    - **Action:** Ensure existing `getStackTemplate()` is available.
    - **Verification:** Read the file to confirm availability.

## 3. Domain Logic & Background Processing
*Enable the provisioning logic to handle the Admin upgrade and ensure future persistence.*

7.  **Enhance Stack Provisioning Service**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/StackProvisioningService.kt`
    - **Action:** Add `updateAndPollStack(stackName: String, parameters: Map<String, String>)`.
    - **Logic:** Calls `client.updateStack` and polls for `UPDATE_COMPLETE` or `UPDATE_COMPLETE_CLEANUP_IN_PROGRESS`.
    - **Verification:** Read the file to confirm the method addition.

8.  **Refactor Recovery Use Case (The "Takeover")**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/RecoverAccountUseCase.kt`
    - **Action:** Rewrite to use `UpdateStack` instead of `CreateStack`.
        1.  **Discovery:** Identify target `StackName` from S3 tags.
        2.  **Rotation:** Generate a new UUID for `KeySerial`.
        3.  **Execution:** Call `stackProvisioningService.updateAndPollStack` with:
            - `StackName`: `<DiscoveredStackName>`
            - `KeySerial`: `<NewUUID>` (Forces Key Rotation)
            - `IsAdmin`: `"false"` (Forces Downgrade to Standard - Enforces "No Admin Recovery")
        4.  **Result:** Populate `RuntimeCredentials` from new outputs.
    - **Verification:** Verify logic implements the Takeover strategy.

9.  **Create Upgrade Account Use Case**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/UpgradeAccountUseCase.kt` (New)
    - **Action:** Implement the upgrade logic:
        1.  Retrieve existing `RuntimeCredentials`.
        2.  **State Check:** Call `client.describeStack` to get the *current* `KeySerial` parameter. (Crucial: Do NOT rotate keys during upgrade).
        3.  **Execution:** Call `stackProvisioningService.updateAndPollStack` with parameters:
            - `IsAdmin`: `"true"`
            - `StackName`: `creds.stackName`
            - `KeySerial`: `<CurrentKeySerial>` (Preserved)
        4.  **State Update:**
            - Update `RuntimeCredentials` with `isAdmin=true`.
            - Persist.
    - **Verification:** List the file `core/domain/src/main/kotlin/com/locus/core/domain/usecase/UpgradeAccountUseCase.kt` to confirm creation.

10. **Update Provisioning Worker**
    - **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorker.kt`
    - **Action:**
        - Add `MODE_ADMIN_UPGRADE`.
        - **Security:** Read `BootstrapCredentials` from `SecureStorage`.
        - Inject `UpgradeAccountUseCase`.
        - In `doWork`, handle the new mode.
        - On completion, clear `BootstrapCredentials`.
    - **Verification:** Read the file to confirm logic updates.

## 4. UI Implementation
*Create the Settings and Upgrade screens.*

11. **Create Settings Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/SettingsScreen.kt` (New)
    - **Action:** Scaffold screen with "Admin Upgrade" button (visible if `!isAdmin`).
    - **Verification:** List the file to confirm creation.

12. **Create Admin Upgrade Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeScreen.kt` (New)
    - **Action:** UI for entering Bootstrap Keys.
    - **Verification:** List the file to confirm creation.

13. **Create Admin Upgrade ViewModel**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeViewModel.kt` (New)
    - **Action:**
        - Validate keys.
        - Save keys to `SecureStorage`.
        - Dispatch `ProvisioningWorker` (Upgrade Mode).
        - **Error Handling:** Observe the `WorkInfo` of the dispatched worker.
        - If `WorkInfo.State` is `FAILED`, extract error message (from output data) and expose it to UI (e.g. `_errorEvent`).
    - **Verification:** List the file to confirm creation.

14. **Integrate Dashboard Entry Point**
    - **File:** `app/src/main/kotlin/com/locus/android/features/dashboard/DashboardScreen.kt`
    - **Action:** Add Settings icon to TopBar or FAB (as temporary entry point).
    - **Verification:** Read the file to confirm the icon was added.

## 5. Reactive UX
*Handle the critical post-upgrade lifecycle.*

15. **Implement Reactive State Updates**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/SettingsScreen.kt`
    - **Action:**
        - Observe `AuthRepository.authState` for success (Admin granted).
        - Observe `AdminUpgradeViewModel` error events for failures (Worker failed).
        - Display Snackbar or Dialog on failure.
    - **Verification:** Read the file to confirm the observation logic.

## 6. Verification
*Ensure the feature works as expected.*

16. **Unit Tests**
    - **Action:** Test `UpgradeAccountUseCase` preserves `KeySerial`.
    - **Action:** Test `RecoverAccountUseCase` rotates `KeySerial`.
    - **Action:** Test `CloudFormationClient` update logic.
    - **Verification:** Run `scripts/run_local_validation.sh`.

17. **Manual Verification**
    - **Action:** Provision new user (Standard).
    - **Action:** Upgrade to Admin.
    - **Action:** Perform Recovery (Takeover).
    - **Action:** Verify Keys Rotated & Admin Downgraded.
    - **Verification:** Document manual verification results.

## 7. Pre-commit & Submit
- [ ] Run `scripts/run_local_validation.sh`.
- [ ] Submit changes.
