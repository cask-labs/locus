# User Flows

This document defines the core user journeys for the Locus application, mapping the interaction from initialization to daily usage and data visualization.

## 1. Onboarding (The Bootstrap)
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

## 2. Daily Operation (Passive)
**Goal:** Continuous, reliable data collection with minimal user intervention.

*   **Status Indication:**
    *   A persistent notification indicates the service is running ("Locus is tracking").
    *   The notification updates to show the latest sync status or error state.
*   **Battery Management:**
    *   **Low Battery (<10%):** The app pauses uploads and reduces tracking frequency. The user receives a notification about the conservation mode.
    *   **Critical Battery (<3%):** Tracking stops completely to preserve the phone's remaining life.
    *   **Recovery:** When charged >15%, the app automatically resumes full-fidelity tracking and syncing.
*   **User Action:**
    *   The user sees the persistent notification as assurance of operation. No active interaction is required unless an error occurs.

## 3. Visualization (History View)
**Goal:** Verify and explore historical movements.

*   **Step 1: Access:**
    *   The user opens the main application UI.
*   **Step 2: Date Selection:**
    *   The user selects a specific date from a calendar interface.
*   **Step 3: Data Retrieval:**
    *   The app queries the S3 bucket for all track segments matching that date.
    *   The app downloads and decompresses the relevant Gzip files.
*   **Step 4: Rendering:**
    *   The map displays the day's route.
    *   The interface shows summary statistics (Total Distance, Duration).
    *   Gaps in data (e.g., dead battery) are visually distinct from active tracking.

## 4. Manual Sync & Status
**Goal:** Immediate verification of data safety.

*   **Step 1: Status Check:**
    *   The main screen displays the "Last Successful Sync" timestamp.
    *   It shows the current size of the local buffer (number of points waiting to upload).
*   **Step 2: Forced Sync:**
    *   The user taps a "Sync Now" button.
    *   The app immediately packages the local buffer, compresses it, and attempts an upload.
*   **Step 3: Feedback:**
    *   **Success:** The local buffer count drops to zero; the "Last Sync" time updates to "Just now".
    *   **Failure:** An error message explains the issue (e.g., "No Network", "AWS Error").

## 5. System Recovery (Re-provisioning)
**Goal:** Restore access to existing data on a new device.

*   **Scenario:** A user installs the app on a new phone but wants to keep using their existing S3 bucket.
*   **Step 1: Credential Entry:**
    *   The user enters their AWS keys.
*   **Step 2: Bucket Discovery:**
    *   The app checks for existing Locus stacks/buckets associated with these keys.
    *   The user selects the existing bucket.
*   **Outcome:** The app links to the existing bucket without attempting to create a new CloudFormation stack. History becomes immediately available.
