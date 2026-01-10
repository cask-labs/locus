# Implementation Plan - Task 11: Admin Upgrade Flow (Revised Strategy)

## Context & Findings
This plan implements the "Admin Upgrade" feature defined in `docs/behavioral_specs/01_onboarding_identity.md` (R1.2000 - R1.2400).
Analysis has identified critical requirements to support this flow:
- **Single Conditional Template:** To prevent data loss (S3 Bucket deletion) caused by Logical ID mismatches, we must use a single `locus-stack.yaml` with an `IsAdmin` parameter, rather than a separate admin template.
- **In-Place Update:** The upgrade must perform a CloudFormation `UpdateStack` operation on the existing `locus-user-<deviceName>` stack.
- **Persistence:** The `deviceName` must be persisted in `RuntimeCredentials` to allow reconstructing the stack name for updates.
- **Discovery:** Admin users require `tag:GetResources` permission to discover other device buckets.

## 1. Documentation Updates
*Ensure specifications match the architectural reality.*

1.  **Update Behavioral Specification**
    - **File:** `docs/behavioral_specs/01_onboarding_identity.md`
    - **Action:** Update **R1.2200** to reflect the "Single Template" strategy (using `IsAdmin` parameter) instead of requiring a separate `locus-admin.yaml`.

## 2. Data Layer & Assets
*Establish the infrastructure and data foundations for the Admin identity.*

2.  **Modify CloudFormation Template**
    - **File:** `core/data/src/main/assets/locus-stack.yaml`
    - **Action:** Modify the existing template to support Admin capabilities via parameters.
    - **Details:**
        - Add Parameter: `IsAdmin` (Type: String, Default: "false", AllowedValues: ["true", "false"]).
        - Add Condition: `AdminEnabled` equals `true`.
        - Modify `LocusPolicy`: Add a `Statement` that is conditional on `AdminEnabled`, allowing:
            - `s3:ListBucket` and `s3:GetObject` on resources tagged with `LocusRole: DeviceBucket`.
            - `tag:GetResources` (Resource Groups Tagging API) to allow discovering buckets.
    - **Goal:** Guarantees `LocusDataBucket` Logical ID remains identical, preserving user data, while enabling Admin discovery.

3.  **Update Runtime Credentials Schema**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/RuntimeCredentials.kt`
    - **Action:** Add `val deviceName: String` and `val isAdmin: Boolean = false`.
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/model/RuntimeCredentialsDto.kt`
    - **Action:** Add fields to DTO and update mappers.

4.  **Enhance CloudFormation Client**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/CloudFormationClient.kt`
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/infrastructure/CloudFormationClientImpl.kt`
    - **Action:** Add `updateStack` method.
    - **Signature:** `suspend fun updateStack(stackName: String, templateBody: String, parameters: Map<String, String>)`
    - **Logic:** Must handle "No updates are to be performed" (ValidationError) as a successful state.

5.  **Verify Resource Provider**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/ResourceProvider.kt`
    - **Action:** Ensure existing `getStackTemplate()` is available (no new method needed).

## 3. Domain Logic & Background Processing
*Enable the provisioning logic to handle the Admin upgrade.*

6.  **Enhance Stack Provisioning Service**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/StackProvisioningService.kt`
    - **Action:** Add `updateAndPollStack(stackName: String, parameters: Map<String, String>)`.
    - **Logic:** Calls `client.updateStack` and reuses the existing polling mechanism.

7.  **Create Upgrade Account Use Case**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/UpgradeAccountUseCase.kt` (New)
    - **Action:** Implement the upgrade logic:
        1.  Retrieve existing `RuntimeCredentials` (fail if missing).
        2.  Load `locus-stack.yaml` (standard template).
        3.  Construct stack name: `locus-user-${creds.deviceName}`.
        4.  Call `stackProvisioningService.updateAndPollStack` with parameter `IsAdmin="true"`.
        5.  Verify outputs.
        6.  **Critical:** Call `authRepository.promoteToRuntimeCredentials` (or similar) to save the new keys AND explicitly refresh the in-memory `AuthState`.

8.  **Update Provisioning Worker**
    - **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorker.kt`
    - **Action:**
        - Add `MODE_ADMIN_UPGRADE`.
        - Inject `UpgradeAccountUseCase`.
        - In `doWork`, handle the new mode.

## 4. UI Implementation
*Create the Settings and Upgrade screens.*

9.  **Create Settings Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/SettingsScreen.kt` (New)
    - **Action:** Scaffold screen with "Admin Upgrade" button (visible if `!isAdmin`).

10. **Create Admin Upgrade Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeScreen.kt` (New)
    - **Action:** UI for entering Bootstrap Keys.

11. **Create Admin Upgrade ViewModel**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeViewModel.kt` (New)
    - **Action:** Validate keys -> Dispatch `ProvisioningWorker` (Upgrade Mode).

12. **Integrate Dashboard Entry Point**
    - **File:** `app/src/main/kotlin/com/locus/android/features/dashboard/DashboardScreen.kt`
    - **Action:** Add Settings icon.

## 5. Restart Logic
*Handle the critical post-upgrade lifecycle.*

13. **Implement Soft Restart**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeScreen.kt`
    - **Action:**
        - Observe the worker success state.
        - Ensure the UI waits for the `AuthState` to reflect `Authenticated(isAdmin=true)` (via repository observation).
        - Once confirmed, clear task stack and restart `MainActivity` to reset the UI hierarchy.

## 6. Verification
*Ensure the feature works as expected.*

14. **Template Validation**
    - **Action:** Run `cfn-lint core/data/src/main/assets/locus-stack.yaml` to verify the conditional syntax.

15. **Unit Tests**
    - **Action:** Test `UpgradeAccountUseCase` passes `IsAdmin="true"` parameter.
    - **Action:** Test `CloudFormationClient` update logic handles parameters and "No updates" error.
    - **Action:** Test `AuthRepository` correctly updates state after upgrade.

16. **Manual Verification**
    - **Action:** Provision new user (Standard).
    - **Action:** Upgrade to Admin.
    - **Action:** Verify S3 bucket is preserved (same name/contents).
    - **Action:** Verify new permissions (ListBucket allowed).

## 7. Pre-commit & Submit
- [ ] Run `scripts/run_local_validation.sh`.
- [ ] Submit changes.
