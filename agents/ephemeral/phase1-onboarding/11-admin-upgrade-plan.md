# Implementation Plan - Task 11: Admin Upgrade Flow (Revised)

## Context & Findings
This plan implements the "Admin Upgrade" feature defined in `docs/behavioral_specs/01_onboarding_identity.md` (R1.2000 - R1.2400).
Analysis has identified critical requirements to support this flow:
- **In-Place Update:** The upgrade must perform a CloudFormation `UpdateStack` operation on the existing `locus-user-<deviceName>` stack to preserve the S3 bucket and data history.
- **Persistence:** The `deviceName` must be persisted in `RuntimeCredentials` to allow reconstructing the stack name for updates.
- **Dedicated Logic:** A dedicated `UpgradeAccountUseCase` is required to handle the upgrade lifecycle, distinct from new device provisioning.

## 1. Data Layer & Assets
*Establish the infrastructure and data foundations for the Admin identity.*

1.  **Create Admin CloudFormation Template**
    - **File:** `core/data/src/main/assets/locus-admin.yaml`
    - **Action:** Create the CloudFormation template implementing the Hybrid IAM Policy.
    - **Details:** Must include `Statement` allowing `s3:ListBucket` and `s3:GetObject` on resources tagged with `LocusRole: DeviceBucket`.
    - **Constraint:** Must use the same Logical IDs for persistent resources (e.g., S3 Bucket) to ensure preservation during `UpdateStack`.

2.  **Update Runtime Credentials Schema**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/RuntimeCredentials.kt`
    - **Action:** Add `val deviceName: String` and `val isAdmin: Boolean = false`.
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/model/RuntimeCredentialsDto.kt`
    - **Action:** Add fields to DTO and update mappers.
    - **Note:** Requires a migration strategy or simple default for existing local data (though Phase 0 implies clean state is acceptable).

3.  **Enhance CloudFormation Client**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/CloudFormationClient.kt`
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/infrastructure/CloudFormationClientImpl.kt`
    - **Action:** Add `updateStack` method supporting `UpdateStackRequest`.
    - **Logic:** Must handle "No updates are to be performed" as a successful state.

4.  **Update Resource Provider**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/ResourceProvider.kt`
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/infrastructure/ResourceProviderImpl.kt`
    - **Action:** Add `getAdminStackTemplate(): String`.

## 2. Domain Logic & Background Processing
*Enable the provisioning logic to handle the Admin upgrade.*

5.  **Enhance Stack Provisioning Service**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/StackProvisioningService.kt`
    - **Action:** Add `updateAndPollStack` method.
    - **Logic:** Calls `client.updateStack` and reuses the existing polling mechanism to wait for `UPDATE_COMPLETE`.

6.  **Create Upgrade Account Use Case**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/UpgradeAccountUseCase.kt` (New)
    - **Action:** Implement the upgrade logic:
        1.  Retrieve existing `RuntimeCredentials` (fail if missing).
        2.  Load `locus-admin.yaml`.
        3.  Construct stack name: `locus-user-${creds.deviceName}`.
        4.  Call `stackProvisioningService.updateAndPollStack`.
        5.  Verify outputs and promote new credentials (preserving `deviceId` and `telemetrySalt`, setting `isAdmin=true`).

7.  **Update Provisioning Worker**
    - **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorker.kt`
    - **Action:**
        - Add `MODE_ADMIN_UPGRADE`.
        - Inject `UpgradeAccountUseCase`.
        - In `doWork`, handle the new mode.

## 3. UI Implementation
*Create the Settings and Upgrade screens.*

8.  **Create Settings Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/SettingsScreen.kt` (New)
    - **Action:** Scaffold screen with "Admin Upgrade" button (visible if `!isAdmin`).

9.  **Create Admin Upgrade Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeScreen.kt` (New)
    - **Action:** UI for entering Bootstrap Keys.

10. **Create Admin Upgrade ViewModel**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeViewModel.kt` (New)
    - **Action:** Validate keys -> Dispatch `ProvisioningWorker` (Upgrade Mode).

11. **Integrate Dashboard Entry Point**
    - **File:** `app/src/main/kotlin/com/locus/android/features/dashboard/DashboardScreen.kt`
    - **Action:** Add Settings icon.

## 4. Restart Logic
*Handle the critical post-upgrade lifecycle.*

12. **Implement Soft Restart**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeScreen.kt`
    - **Action:** On success, clear task stack and restart `MainActivity`.

## 5. Verification
*Ensure the feature works as expected.*

13. **Unit Tests**
    - **Action:** Test `UpgradeAccountUseCase` preserves `deviceId`/`salt` and sets `isAdmin`.
    - **Action:** Test `CloudFormationClient` update logic.

14. **Manual Verification**
    - **Action:** Provision new user.
    - **Action:** Upgrade to Admin.
    - **Action:** Verify S3 bucket is preserved (same name/contents).
    - **Action:** Verify new permissions (ListBucket allowed).

## 6. Pre-commit & Submit
- [ ] Run `scripts/run_local_validation.sh`.
- [ ] Submit changes.
