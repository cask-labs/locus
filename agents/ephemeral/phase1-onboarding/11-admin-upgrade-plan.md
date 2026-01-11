# Implementation Plan - Task 11: Admin Upgrade Flow (Revised Strategy)

## Context & Findings
This plan implements the "Admin Upgrade" feature defined in `docs/behavioral_specs/01_onboarding_identity.md` (R1.2000 - R1.2400).
Analysis has identified critical requirements to support this flow:
- **Single Conditional Template:** To prevent data loss (S3 Bucket deletion) caused by Logical ID mismatches, we must use a single `locus-stack.yaml` with an `IsAdmin` parameter, rather than a separate admin template.
- **In-Place Update:** The upgrade must perform a CloudFormation `UpdateStack` operation on the existing `locus-user-<deviceName>` stack.
- **Persistence:** The authoritative CloudFormation `StackName` must be persisted in `RuntimeCredentials` to allow robust targeting for updates.
    - **All Users:** Since there is no existing installed user base, we can mandate that `stackName` is persisted immediately upon provisioning for all users.
    - **Recovered Users:** Since the app doesn't know the stack name after a reinstall, we must persist it on the S3 bucket itself via Tags (`LocusStackName`) so it can be recovered during the scan process.
- **Discovery:** Admin users require `tag:GetResources` permission to discover other device buckets.
- **Infrastructure Fixes:** Critical mismatches between Kotlin constants and CloudFormation outputs must be resolved before implementing the upgrade.

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
    - **Action:** Modify the existing template to support Admin capabilities, Recovery, and Fix existing gaps.
    - **Details:**
        - **Recovery Support (Bucket Linking):**
            - Add Parameter: `BucketName` (Type: String, Default: "", Description: "Existing bucket to link").
            - Add Condition: `CreateNewBucket` (True if `BucketName` parameter is empty).
            - Condition `LocusDataBucket` resource on `CreateNewBucket`.
            - Update `LocusPolicy` and `Outputs` to reference `!If [CreateNewBucket, !Ref LocusDataBucket, !Ref BucketName]`.
        - **Admin Support:**
            - Add Parameter: `IsAdmin` (Type: String, Default: "false", AllowedValues: ["true", "false"]).
            - Add Condition: `AdminEnabled` equals `true`.
            - Recovery Tag: Add a Tag to `LocusDataBucket` -> Key: `LocusStackName`, Value: `!Ref "AWS::StackName"`.
            - Modify `LocusPolicy`: Use `Fn::If` to conditionally include `s3:ListBucket` and `tag:GetResources` if `AdminEnabled`.
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
        - Must handle "No updates are to be performed" as success.
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
    - **Logic:** Calls `client.updateStack` and reuses the existing polling mechanism.
    - **Verification:** Read the file to confirm the method addition.

8.  **Update Provisioning Use Cases**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/ProvisioningUseCase.kt`
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/RecoverAccountUseCase.kt`
    - **Action:** Update logic to ensure `StackName` is always populated in `RuntimeCredentials`.
        - **Provisioning:** Extract `StackName` from the CloudFormation inputs/outputs.
        - **Recovery:** Update `ScanBucketsUseCase` to read the `LocusStackName` tag from the discovered bucket. `RecoverAccountUseCase` must use this value.
    - **Verification:** Verify that `ConfigurationRepository.initializeIdentity` or `AuthRepository` receives the `stackName`.

9.  **Create Upgrade Account Use Case**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/UpgradeAccountUseCase.kt` (New)
    - **Action:** Implement the upgrade logic:
        1.  Retrieve existing `RuntimeCredentials`.
        2.  **Resolve Stack Name:** Use `creds.stackName` directly.
        3.  Load `locus-stack.yaml`.
        4.  Call `stackProvisioningService.updateAndPollStack` with parameters:
            - `IsAdmin="true"`
            - `StackName=creds.stackName`
            - `BucketName=creds.bucketName` (Pass existing bucket name to ensure it is linked, not recreated).
        5.  **State Update:**
            - Construct `RuntimeCredentials` with `stackName`, `isAdmin=true`.
            - Call `authRepository.saveCredentials(...)`.
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
    - **Action:** Test `UpgradeAccountUseCase` passes correct stack name and bucket name.
    - **Action:** Test `CloudFormationClient` update logic.
    - **Action:** Test `AuthRepository` updates state after upgrade.
    - **Verification:** Run `scripts/run_local_validation.sh`.

17. **Manual Verification**
    - **Action:** Provision new user (Standard).
    - **Action:** Upgrade to Admin.
    - **Action:** Verify S3 bucket is preserved.
    - **Action:** Verify new permissions.
    - **Verification:** Document manual verification results in a memory recording.

## 7. Pre-commit & Submit
- [ ] Run `scripts/run_local_validation.sh`.
- [ ] Submit changes.
