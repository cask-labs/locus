# Onboarding (The Bootstrap)

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
