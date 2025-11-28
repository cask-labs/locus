# Onboarding (The Bootstrap)

**Goal:** Transition from a fresh install to a fully provisioned, cloud-connected system.

## Step 0: The Choice
Upon first launch, the user is presented with two primary paths:
1.  **New Setup:** "I want to start tracking this device." (Proceeds with this flow).
2.  **Recover / Link Existing:** "I want to restore history or link to an existing store." (Proceeds to [System Recovery & Reconnection](recovery_and_reconnection.md)).

---

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
    *   The user enters a **Device Name**.
    *   **Default:** The field is pre-filled with the device model (e.g., `Pixel 7`) to save time.
    *   *Logic:* This name is used to generate the CloudFormation Stack name (`Locus-Pixel7`) and ensures uniqueness within your account. See [Naming Strategy](naming_strategy.md).
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
