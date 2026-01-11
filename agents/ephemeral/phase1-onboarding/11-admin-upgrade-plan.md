# Implementation Plan - Task 11: Admin Upgrade Flow (Revised Strategy)

## Context & Findings
This plan implements the "Admin Upgrade" feature defined in `docs/behavioral_specs/01_onboarding_identity.md` (R1.2000 - R1.2400).
Analysis has identified critical requirements to support this flow:
- **Single Conditional Template:** To prevent data loss (S3 Bucket deletion) caused by Logical ID mismatches, we must use a single `locus-stack.yaml` with an `IsAdmin` parameter, rather than a separate admin template.
- **In-Place Update:** The upgrade must perform a CloudFormation `UpdateStack` operation on the existing `locus-user-<deviceName>` stack.
- **Persistence:** The authoritative CloudFormation `StackName` must be persisted in `RuntimeCredentials` to allow robust targeting for updates. Since it was not persisted in earlier tasks, it must be **discovered** at runtime during the upgrade.
- **Discovery:** Admin users require `tag:GetResources` permission to discover other device buckets.

## 1. Documentation Updates
*Ensure specifications match the architectural reality.*

1.  **Update Behavioral Specification**
    - **File:** `docs/behavioral_specs/01_onboarding_identity.md`
    - **Action:** Update **R1.2200** to reflect the "Single Template" strategy (using `IsAdmin` parameter) instead of requiring a separate `locus-admin.yaml`.
    - **Verification:** Read the file to confirm the text change.

## 2. Data Layer & Assets
*Establish the infrastructure and data foundations for the Admin identity.*

2.  **Modify CloudFormation Template**
    - **File:** `core/data/src/main/assets/locus-stack.yaml`
    - **Action:** Modify the existing template to support Admin capabilities via parameters.
    - **Details:**
        - Add Parameter: `IsAdmin` (Type: String, Default: "false", AllowedValues: ["true", "false"]).
        - Add Condition: `AdminEnabled` equals `true`.
        - Modify `LocusPolicy`:
            - Use `Fn::If` in the `Statement` list to conditionally include the Admin permissions block.
            - **Important:** If `AdminEnabled` is false, use `Ref: AWS::NoValue` to remove the block entirely. This ensures the Logical ID of the Policy resource remains stable, preventing replacement.
            - Admin permissions:
                - `s3:ListBucket` on resources tagged with `LocusRole: DeviceBucket`.
                - `tag:GetResources` (Resource Groups Tagging API).
    - **Goal:** Guarantees `LocusDataBucket` Logical ID remains identical, preserving user data, while enabling Admin discovery.
    - **Verification:** Run `cfn-lint core/data/src/main/assets/locus-stack.yaml` immediately to verify the conditional syntax is valid.

3.  **Update Runtime Credentials Schema**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/RuntimeCredentials.kt`
    - **Action:** Add `val stackName: String` and `val isAdmin: Boolean = false`.
        - **Note:** This schema change is additive. Existing persisted data won't have `stackName` until the upgrade flow discovers and saves it.
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/model/RuntimeCredentialsDto.kt`
    - **Action:** Add fields to DTO and update mappers.
    - **Verification:** Read `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/RuntimeCredentials.kt` to confirm the new properties were added.

4.  **Enhance CloudFormation Client**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/CloudFormationClient.kt`
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/infrastructure/CloudFormationClientImpl.kt`
    - **Action:** Add `updateStack` method.
    - **Signature:** `suspend fun updateStack(stackName: String, templateBody: String, parameters: Map<String, String>)`
    - **Logic:** Must handle "No updates are to be performed" (ValidationError) as a successful state.
    - **Verification:** Read `core/data/src/main/kotlin/com/locus/core/data/infrastructure/CloudFormationClientImpl.kt` to confirm the `updateStack` method was added correctly.

5.  **Verify Resource Provider**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/ResourceProvider.kt`
    - **Action:** Ensure existing `getStackTemplate()` is available (no new method needed).
    - **Verification:** Read the file to confirm availability.

## 3. Domain Logic & Background Processing
*Enable the provisioning logic to handle the Admin upgrade.*

6.  **Enhance Stack Provisioning Service**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/StackProvisioningService.kt`
    - **Action:** Add `updateAndPollStack(stackName: String, parameters: Map<String, String>)`.
    - **Logic:** Calls `client.updateStack` and reuses the existing polling mechanism.
    - **Verification:** Read the file to confirm the method addition.

7.  **Create Upgrade Account Use Case**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/UpgradeAccountUseCase.kt` (New)
    - **Action:** Implement the upgrade logic with **Stack Name Discovery**:
        1.  Retrieve existing `RuntimeCredentials` (fail if missing).
        2.  **Discovery Step:**
            - Create a temporary `S3Client` using the provided **Bootstrap Keys** (Admin privileges).
            - Call `getBucketTags(currentCreds.bucketName)`.
            - Extract the `aws:cloudformation:stack-name` tag value.
            - Throw an error if tag is missing (critical dependency).
        3.  Load `locus-stack.yaml` (standard template).
        4.  Call `stackProvisioningService.updateAndPollStack` with parameters:
            - `IsAdmin="true"`
            - `StackName=<DiscoveredStackName>`
        5.  Verify outputs.
        6.  **State Update:**
            - Construct a *new* `RuntimeCredentials` object copying existing values but adding the discovered `stackName` and setting `isAdmin=true`.
            - Call `authRepository.saveCredentials(...)` to persist this complete state.
    - **Verification:** List the file `core/domain/src/main/kotlin/com/locus/core/domain/usecase/UpgradeAccountUseCase.kt` to confirm it was created successfully.

8.  **Update Provisioning Worker**
    - **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorker.kt`
    - **Action:**
        - Add `MODE_ADMIN_UPGRADE`.
        - **Security:** Do NOT pass credentials via `InputData`. Instead, read `BootstrapCredentials` from `SecureStorage` (set by ViewModel).
        - Inject `UpgradeAccountUseCase`.
        - In `doWork`, handle the new mode.
        - On completion, clear `BootstrapCredentials` from `SecureStorage`.
    - **Verification:** Read the file to confirm logic updates.

## 4. UI Implementation
*Create the Settings and Upgrade screens.*

9.  **Create Settings Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/SettingsScreen.kt` (New)
    - **Action:** Scaffold screen with "Admin Upgrade" button (visible if `!isAdmin`).
    - **Verification:** List the file to confirm creation.

10. **Create Admin Upgrade Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeScreen.kt` (New)
    - **Action:** UI for entering Bootstrap Keys.
    - **Verification:** List the file to confirm creation.

11. **Create Admin Upgrade ViewModel**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeViewModel.kt` (New)
    - **Action:**
        - Validate keys.
        - Save keys to `SecureStorage` as `BootstrapCredentials`.
        - Dispatch `ProvisioningWorker` (Upgrade Mode).
    - **Verification:** List the file to confirm creation.

12. **Integrate Dashboard Entry Point**
    - **File:** `app/src/main/kotlin/com/locus/android/features/dashboard/DashboardScreen.kt`
    - **Action:** Add Settings icon.
    - **Verification:** Read the file to confirm the icon was added.

## 5. Reactive UX
*Handle the critical post-upgrade lifecycle.*

13. **Implement Reactive State Updates**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/SettingsScreen.kt`
    - **Action:**
        - Observe `AuthRepository.authState`.
        - When state becomes `Authenticated(isAdmin=true)`, automatically update the UI (e.g., hide Upgrade button, show Admin tools).
        - **Do not force a restart.** Let the reactive UI handle the transition.
    - **Verification:** Read the file to confirm the observation logic.

## 6. Verification
*Ensure the feature works as expected.*

14. **Unit Tests**
    - **Action:** Test `UpgradeAccountUseCase` discovers stack name via tags.
    - **Action:** Test `CloudFormationClient` update logic handles parameters and "No updates" error.
    - **Action:** Test `AuthRepository` correctly updates state after upgrade without rotating keys.
    - **Verification:** Run `scripts/run_local_validation.sh` to execute the tests.

15. **Manual Verification**
    - **Action:** Provision new user (Standard).
    - **Action:** Upgrade to Admin.
    - **Action:** Verify S3 bucket is preserved (same name/contents).
    - **Action:** Verify new permissions (ListBucket allowed).
    - **Verification:** Document manual verification results in a memory recording.

## 7. Pre-commit & Submit
- [ ] Run `scripts/run_local_validation.sh`.
- [ ] Submit changes.
