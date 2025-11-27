# User Flows

This document defines the core user journeys for the Locus application, mapping the interaction from initialization to daily usage and data visualization.

## 1. Onboarding (The Bootstrap)
**Goal:** Transition from a fresh install to a fully provisioned, cloud-connected system.

*   **Prerequisite:** The user has manually created an IAM User in their AWS Console and attached the `iam-bootstrap-policy.json`.
*   **Step 1: Welcome & Permissions:**
    *   The app explains the need for "Always Allow" location access.
    *   The user grants location, notification, and battery optimization exemptions.
*   **Step 2: Credential Entry:**
    *   The user enters their AWS Access Key ID and Secret Access Key.
    *   *Constraint:* The app validates the format of the keys before proceeding.
*   **Step 3: Stack Configuration:**
    *   The user specifies a unique name for their S3 bucket (or accepts a generated default).
*   **Step 4: Provisioning:**
    *   The app triggers the CloudFormation deployment.
    *   A progress indicator shows the creation of resources (Bucket, Policies).
*   **Outcome:** The system confirms "Setup Complete" and immediately begins the background tracking service.

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
