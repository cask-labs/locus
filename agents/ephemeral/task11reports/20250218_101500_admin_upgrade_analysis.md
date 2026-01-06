# Analysis Report: Admin Upgrade Plan (Task 11)

## 1. Critical: Potential Data Loss via CloudFormation Stack Update

### Analysis
The plan instructs the creation of a separate `locus-admin.yaml` template and modifying `ProvisioningUseCase` to use this template when `isAdmin` is true. The provisioning logic constructs the stack name using the device name (`locus-user-<device>`).
- If the user upgrades, CloudFormation interprets this as a **Stack Update** (same stack name, new template).
- **Risk:** If `locus-admin.yaml` does not contain the `AWS::S3::Bucket` resource with the **exact same Logical ID** (e.g., `LocusDataBucket`) as `locus-stack.yaml`, CloudFormation will **delete the existing S3 bucket** and all user data during the update.
- **Risk:** Even if the bucket resource exists, if properties change significantly without a `Retain` deletion policy (though `Retain` is for stack deletion, not resource removal), it might trigger replacement.

### Resolution
The plan must explicitly mandate that `locus-admin.yaml` be a **strict superset** of `locus-stack.yaml`.
- It **MUST** include the `AWS::S3::Bucket` resource.
- The **Logical ID** of the bucket must match exactly.
- **Verification Step:** Add a verification step to ensure the S3 Bucket is preserved (check creation date) after upgrade.

## 2. Critical: Unintended Identity Reset (Device ID & Salt)

### Analysis
The current `ProvisioningUseCase` logic (Step 4 in code) unconditionally generates a new identity:
```kotlin
val newDeviceId = UUID.randomUUID().toString()
val newSalt = AuthUtils.generateSalt()
configRepository.initializeIdentity(newDeviceId, newSalt)
```
- **Problem:** When performing an **Admin Upgrade**, the user expects to retain their current history and identity. Generating a new `device_id` effectively "factory resets" the history view (as the app filters tracks by device ID), even though the data remains in the bucket.
- **Gap:** The plan does not instruct modifying this logic to preserve identity during an upgrade.

### Resolution
Update **Step 5 (Update Provisioning Use Case)** to include logic for identity preservation:
- If `isAdmin` is true:
    - **Skip** generating `newDeviceId` and `newSalt`.
    - **Retrieve** the existing `deviceId` and `telemetrySalt` from `ConfigurationRepository` (or pass them in).
    - Ensure the new `RuntimeCredentials` object uses the **existing** salt.

## 3. Missing Input Wiring: Device Name

### Analysis
The `ProvisioningUseCase` requires `deviceName` as an input to derive the stack name.
- **Gap:** The plan for **Step 9 (Create Admin Upgrade ViewModel)** does not specify how the ViewModel obtains the device name.
- Unlike the initial onboarding (where the user types a name), the upgrade flow must target the **existing** stack.
- The user should not be prompted to re-enter their device name (risk of typo -> creating new stack vs updating).

### Resolution
Update **Step 9** to explicitly state that `AdminUpgradeViewModel` must inject `ConfigurationRepository` to fetch the current `deviceName` and pass it to the `ProvisioningWorker`.

## 4. UI/UX: Settings Screen Availability

### Analysis
The plan instructs creating a "New" Settings Screen (Step 7).
- **Context:** If this is strictly Phase 1, the full Settings screen might not be scoped yet.
- **Verification:** Ensure the scope includes a functional "Settings" destination in the navigation graph, even if it only contains the "Admin Upgrade" button for now.

## Summary of Required Changes
1.  **Modify Step 1:** Explicitly require `locus-admin.yaml` to include the `AWS::S3::Bucket` resource with the matching Logical ID to prevent data deletion.
2.  **Modify Step 5:** Add logic to `ProvisioningUseCase` to **preserve existing Device ID and Salt** when `isAdmin = true`.
3.  **Modify Step 9:** wiring `AdminUpgradeViewModel` to fetch the existing device name automatically.
