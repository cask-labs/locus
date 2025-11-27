# Onboarding (The Bootstrap)

**Goal:** Transition from a fresh install to a fully provisioned, cloud-connected system.

*   **Prerequisite:** The user must have an IAM User with the `iam-bootstrap-policy.json` attached.
    *   *UI Support:* The first screen contains a "Help me set up AWS" link pointing to the [Infrastructure Documentation](infrastructure.md).
*   **Step 1: Permissions (The Two-Step Dance):**
    *   **Phase A (Foreground):** The user taps "Enable Location". The system dialog appears; the user selects "While using the app".
    *   **Phase B (Background):** The app explains that "Allow all the time" is required for background tracking. A button opens the System Settings page for the app.
    *   **Phase C (Confirmation):** The user manually selects "Allow all the time" in Settings and returns to the app. The app verifies the permission is granted.
    *   *Additional:* User grants Notification (runtime) and Battery Optimization (whitelist) permissions.
*   **Step 2: Credential Entry:**
    *   The user enters their AWS Access Key ID and Secret Access Key.
    *   *UI Requirement:* Fields are optimized for clipboard pasting. The Secret Key field has a "Show/Hide" toggle (masked by default).
    *   *Constraint:* The app validates the format of the keys (regex check) before allowing the user to proceed.
*   **Step 3: Device Identity:**
    *   The user enters a **Device Name** (e.g., "Pixel7").
    *   *Logic:* This name is used to generate the CloudFormation Stack name (`Locus-Pixel7`) and ensures uniqueness. See [Naming Strategy](naming_strategy.md).
*   **Step 4: Provisioning:**
    *   The user taps "Deploy Infrastructure".
    *   **Process:** The app starts a **Foreground Service** (shown as a persistent notification: "Locus Setup: Provisioning...").
    *   **UI Feedback:**
        *   **In-App:** A log console displays real-time steps (e.g., "Creating Stack...", "Waiting for Bucket...", "Applying Policies...").
        *   **Background:** The user can leave the app; the notification remains active.
    *   **Outcome:**
        *   **Success:** Notification updates to "Setup Complete". App transitions to the Dashboard.
        *   **Failure:** The log displays the raw AWS error message with a "Retry" button.
*   **Outcome:** The system is fully provisioned, the bucket name is discovered and saved, and background tracking begins immediately.
