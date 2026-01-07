# Implementation Plan - Task 11: Admin Upgrade Flow

## Context & Findings
This plan implements the "Admin Upgrade" feature defined in `docs/behavioral_specs/01_onboarding_identity.md` (R1.2000 - R1.2400).
The following architectural decisions have been confirmed:
- **Hybrid IAM Policy (`locus-admin.yaml`):** The Admin identity combines standard "Write-Own" permissions with broader "Read-All" permissions scoped to the `LocusRole: DeviceBucket` tag (R1.2400).
- **Settings Screen Entry:** The upgrade flow is initiated from a dedicated `SettingsScreen` (R1.2000).
- **Soft Restart:** A "Soft Restart" strategy (clearing the task stack and restarting `MainActivity`) is required to apply the new identity context (R1.2300).

## 1. Data Layer & Assets
*Establish the infrastructure and data foundations for the Admin identity.*

1.  **Create Admin CloudFormation Template**
    - **File:** `core/data/src/main/assets/locus-admin.yaml`
    - **Action:** Create the CloudFormation template implementing the Hybrid IAM Policy.
    - **Details:** Must include `Statement` allowing `s3:ListBucket` and `s3:GetObject` on resources tagged with `LocusRole: DeviceBucket`.

2.  **Update Domain Models**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/RuntimeCredentials.kt`
    - **Action:** Add `val isAdmin: Boolean = false` property.
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/model/RuntimeCredentialsDto.kt`
    - **Action:** Add `isAdmin` field to DTO and update mappers in `AuthRepositoryImpl`.

3.  **Update Resource Provider**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/ResourceProvider.kt` (Interface)
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/infrastructure/ResourceProviderImpl.kt` (Impl)
    - **Action:** Add `getAdminStackTemplate(): String` method to expose the new asset.

4.  **Enhance AuthRepository**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt`
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
    - **Action:**
        - Add `replaceRuntimeCredentials(creds: RuntimeCredentials)` method to support the upgrade.
        - Ensure `SecureStorageDataSource` correctly serializes/deserializes the new `isAdmin` flag.

## 2. Domain Logic & Background Processing
*Enable the provisioning logic to handle the Admin upgrade.*

5.  **Update Provisioning Use Case**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/ProvisioningUseCase.kt`
    - **Action:**
        - Update `invoke` to accept `isAdmin: Boolean = false`.
        - Logic: If `isAdmin` is true, use `resourceProvider.getAdminStackTemplate()` instead of the standard template.
        - Logic: Ensure the resulting `RuntimeCredentials` has `isAdmin = true`.

6.  **Update Provisioning Worker**
    - **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorker.kt`
    - **Action:**
        - Add input constant `MODE_ADMIN_UPGRADE`.
        - In `doWork`, read this mode and call `provisioningUseCase` with `isAdmin = true`.

## 3. UI Implementation
*Create the Settings and Upgrade screens.*

7.  **Create Settings Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/SettingsScreen.kt` (New)
    - **Action:** Create a scaffold screen with an "Admin Upgrade" button.
    - **Logic:** The button should only be visible if the current user is NOT an Admin (`!isAdmin`).
    - **Navigation:** Add `OnboardingDestinations.SETTINGS` (or similar) to navigation graph.

8.  **Create Admin Upgrade Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeScreen.kt` (New)
    - **Action:** Implement UI for entering Bootstrap Keys (reuse components from `CredentialEntryScreen`).
    - **Logic:** "Start Upgrade" button triggers `AdminUpgradeViewModel`.

9.  **Create Admin Upgrade ViewModel**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeViewModel.kt` (New)
    - **Action:**
        - Validate keys ("Dry Run").
        - Dispatch `ProvisioningWorker` with `MODE_ADMIN_UPGRADE`.
        - Observe `WorkInfo` state.

10. **Integrate Dashboard Entry Point**
    - **File:** `app/src/main/kotlin/com/locus/android/features/dashboard/DashboardScreen.kt`
    - **Action:** Add a "Settings" icon/action to the Top App Bar that navigates to `SettingsScreen`.

## 4. Restart Logic
*Handle the critical post-upgrade lifecycle.*

11. **Implement Soft Restart**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeScreen.kt`
    - **Action:** On `ProvisioningState.Success`, execute the Soft Restart to refresh the app state:
      ```kotlin
      val intent = Intent(context, MainActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      context.startActivity(intent)
      ```

## 5. Verification
*Ensure the feature works as expected.*

12. **Unit Tests**
    - **Action:** Test `ProvisioningUseCase` selects the correct template based on the `isAdmin` flag.
    - **Action:** Test `RuntimeCredentials` serialization includes the `isAdmin` state.

13. **Manual Verification**
    - **Action:** Deploy app, navigate to Settings -> Admin Upgrade.
    - **Action:** Enter keys, verify `locus-user-admin` stack creation in AWS console.
    - **Action:** Verify app restarts and user remains logged in with Admin privileges.

## 6. Pre-commit & Submit
- [ ] Run `scripts/run_local_validation.sh`.
- [ ] Submit changes.
