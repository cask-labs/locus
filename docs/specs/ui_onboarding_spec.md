# Onboarding UI Specification

**Related Requirements:** [Setup & Onboarding](../requirements/setup_onboarding.md), [UI Presentation Spec](ui_presentation_spec.md)

This document details the user interface for the Onboarding flow, covering the initial launch, credential entry, and the divergence between "New Device" and "Recovery" paths.

## 1. Flow Overview

The onboarding process guides the user from a fresh install to a fully provisioned, tracking-ready state.

```mermaid
graph TD
    Start((Launch)) --> Welcome[Welcome Screen]
    Welcome --> Guide[Key Gen Guide]
    Guide --> Welcome
    Welcome --> Creds[Credential Entry]
    Creds --> Valid{Credentials Valid?}
    Valid -- No --> Creds
    Valid -- Yes --> Choice[Choice Screen]

    Choice --> |Path A| NewDevice[New Device Setup]
    Choice --> |Path B| Recovery[Link Existing Store]

    NewDevice --> Provision[Provisioning Progress]
    Recovery --> Discovery[Bucket Discovery]
    Discovery --> Provision

    Provision --> PermsA[Permission A: Foreground]
    PermsA --> PermsB[Permission B: Background]
    PermsB --> Success((Dashboard))
```

*   **Layout Constraint (All Screens):**
    *   **Landscape/Tablet:** Use a **Scrollable Column** layout centered on the screen with a maximum width (e.g., `600dp`). Do not use split-pane layouts for Onboarding to keep the flow linear and simple.

## 2. Screen Specifications

### 2.1. Welcome Screen
**Purpose:** Introduce the "Bring Your Own Cloud" concept and provide guidance.

**Components:**
*   **Hero Image/Icon:** Locus Logo.
*   **Title/Body:** "Welcome to Locus. Your Data, Your Cloud."
*   **Cost Disclaimer:** "Standard AWS S3 usage rates apply. Estimated cost: <$0.10/month for standard usage."
*   **Action:** "Get Started" button.
*   **Help:** "Guide: How to generate AWS Keys" (Opens In-App Bottom Sheet Guide).

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|                                                  |
|                  ( Locus Logo )                  |
|                                                  |
|               Bring Your Own Cloud               |
|                                                  |
|     Locus stores your location history in your   |
|     private AWS S3 bucket. You own the data.     |
|                                                  |
|     (i) Standard AWS S3 rates apply.             |
|         Est: <$0.10/month                        |
|                                                  |
+--------------------------------------------------+
| [?] How to generate AWS Keys                     |
|                                                  |
|           [   GET STARTED   ]                    |
+--------------------------------------------------+
```

### 2.2. Key Generation Guide (Bottom Sheet)
**Trigger:** Tapping "How to generate AWS Keys" on the Welcome Screen.
**Purpose:** Step-by-step instructions for users to generate temporary keys via CloudShell.

**Components:**
*   **Step 1:** "Log in to AWS Console."
*   **Step 2:** "Open CloudShell (Terminal Icon)."
*   **Step 3:** "Paste this command:" (Copy Button).
    *   `aws sts get-session-token --duration-seconds 3600`
*   **Step 4:** "Copy the output JSON."
*   **Security Note:** "These keys expire in 1 hour. For maximum security, ensure your Console User uses the **Locus Bootstrap Policy**." (Link to Policy JSON).

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|  How to generate keys                            |
|                                                  |
|  1. Open AWS CloudShell.                         |
|  2. Run this command:                            |
|     [ aws sts get-session-token ... ] (Copy)     |
|  3. Copy the output values.                      |
|                                                  |
|           [ CLOSE ]                              |
+--------------------------------------------------+
```

### 2.3. Credential Entry
**Purpose:** Collect AWS credentials to bootstrap the connection.

**Components:**
*   **Inputs:** Access Key ID, Secret Access Key (masked), Session Token (**Required**).
*   **Validation:** "Validate Credentials" button (performs Dry Run).
*   **Feedback:**
    *   **Invalid:** Inline error messages (e.g., "Invalid Signature").
    *   **Valid:** A transient **Snackbar** displays "Credentials Verified" before automatically transitioning to the Choice Screen.

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|  < Back                                          |
+--------------------------------------------------+
|  Connect AWS Account                             |
|                                                  |
|  Enter your temporary 'Bootstrap' credentials.   |
|                                                  |
|  Access Key ID                                   |
|  [ AKIAIOSFODNN7EXAMPLE        ]                 |
|                                                  |
|  Secret Access Key                               |
|  [ *************************** ] (Show)          |
|                                                  |
|  Session Token (Required)                        |
|  [                             ]                 |
|                                                  |
+--------------------------------------------------+
|           [ VALIDATE CREDENTIALS ]               |
+--------------------------------------------------+
| (Snackbar: Credentials Verified)                 | <--- Transient Success State
+--------------------------------------------------+
```

### 2.4. Choice Screen
**Purpose:** Determine if this is a new installation or a recovery of an existing one.

**Components:**
*   **Header:** Standard "Setup Options" title.
*   **Option A (New Device):** "Set up as New Device" (Primary Action).
*   **Option B (Recovery):** "Link Existing Store" (Secondary/Outlined Action).

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|  < Back                                          |
+--------------------------------------------------+
|  Setup Options                                   |
|                                                  |
|  How would you like to proceed?                  |
|                                                  |
|  +--------------------------------------------+  |
|  |  New Device Setup                          |  |
|  |  Create a new store for this phone.        |  |
|  |  [ SET UP AS NEW ]                         |  |
|  +--------------------------------------------+  |
|                                                  |
|  OR                                              |
|                                                  |
|  +--------------------------------------------+  |
|  |  Recovery                                  |  |
|  |  Link to an existing Locus bucket.         |  |
|  |  [ LINK EXISTING STORE ]                   |  |
|  +--------------------------------------------+  |
+--------------------------------------------------+
```

### 2.5. Path A: New Device Setup
**Purpose:** Define the device identity and deploy infrastructure.

**Components:**
*   **Input:** Device Name (default: System Model, e.g., "pixel-7").
*   **Validation:**
    *   **Strict Formatting:** The UI must enforce AWS-compatible naming conventions (lowercase, alphanumeric, hyphens only).
    *   **Auto-Format:** Spaces are automatically converted to hyphens; uppercase characters are converted to lowercase as the user types.
    *   **Availability:** Check for name collisions against existing stacks.
*   **Action:** "Deploy Infrastructure".

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|  < Back                                          |
+--------------------------------------------------+
|  New Device                                      |
|                                                  |
|  Give this device a unique name.                 |
|                                                  |
|  Device Name                                     |
|  [ pixel-7                     ]                 |
|  (Checked: Available)                            |
|                                                  |
+--------------------------------------------------+
|         [ DEPLOY INFRASTRUCTURE ]                |
+--------------------------------------------------+
```

### 2.6. Path B: Recovery (Link Store)
**Purpose:** Select an existing Locus store to link.

**Components:**
*   **List:** List of detected buckets/stores (e.g., "Locus-Pixel6", "Locus-iPhone").
    *   *Empty State:* If `s3:ListBuckets` returns no valid `locus-` buckets, display "No Locus stores found."
*   **Action:** Tap a list item to select.

**ASCII Wireframe (List Populated):**
```text
+--------------------------------------------------+
|  < Back                                          |
+--------------------------------------------------+
|  Select Existing Store                           |
|                                                  |
|  Found 2 Locus stores:                           |
|                                                  |
|  [ (Bucket Icon) Locus-Pixel6                 ]  |
|    Last active: 2 days ago                       |
|                                                  |
|  [ (Bucket Icon) Locus-iPhone                 ]  |
|    Last active: 1 month ago                      |
|                                                  |
+--------------------------------------------------+
```

**ASCII Wireframe (Empty State):**
```text
+--------------------------------------------------+
|  < Back                                          |
+--------------------------------------------------+
|  Select Existing Store                           |
|                                                  |
|  ( Icon: search_off )                            |
|                                                  |
|  No Locus stores found.                          |
|                                                  |
+--------------------------------------------------+
```

### 2.7. Provisioning (Progress)
**Purpose:** Visual feedback during long-running CloudFormation tasks.

**Displayed Steps:**
*   *Note: These steps correspond to the resources defined in `locus-stack.yaml`.*
1.  **"Validating CloudFormation Template..."** (Client-side validation & S3 Upload)
2.  **"Creating Storage Stack..."** (Initiate CloudFormation Stack Creation)
3.  **"Provisioning Resources..."** (AWS creating S3 Buckets, IAM User, & Policies)
4.  **"Generating Runtime Keys..."** (Retrieving `AccessKey` from Stack Outputs)
5.  **"Finalizing Setup..."** (Saving Runtime Keys to Keystore, Deleting Bootstrap Keys)

**States:**
*   **In Progress:** Shows progress bar and current step.
*   **Failure:** Shows Error Icon, Error Message, and "Retry" or "View Logs" button.
*   **Success:** Shows Checkmark, "Setup Complete", and transitions automatically or via button to Permission Flow.

**ASCII Wireframe (In Progress):**
```text
+--------------------------------------------------+
|                                                  |
|            ( Cloud Processing Icon )             |
|                                                  |
|           Configuring IAM User...                |
|           [==================  ] 4/6             |
|                                                  |
|           (v) Storage Stack Created              |
|           (v) Validated Template                 |
|                                                  |
+--------------------------------------------------+
```

### 2.8. Permission Step 1: Foreground (Rationale)
**Purpose:** Request the initial "While Using" location permission. This is the prerequisite for background access on Android 11+.

**Components:**
*   **Icon:** Standard Material Symbol: `location_on`.
*   **Title:** "Step 1: Enable Tracking"
*   **Body:** "Locus needs to access your location to record your journey."
*   **Action:** "Continue" (Triggers System Dialog: "While Using").

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|                                                  |
|          ( Icon: location_on )                   |
|                                                  |
|            Step 1: Enable Tracking               |
|                                                  |
|  Locus needs to access your location to record   |
|  your journey.                                   |
|                                                  |
|  Please select "While using the app" in the      |
|  next dialog.                                    |
|                                                  |
+--------------------------------------------------+
|                                                  |
|           [      CONTINUE      ]                 |
+--------------------------------------------------+
```

### 2.9. Permission Step 2: Background (Rationale)
**Purpose:** Explain the necessity of "Always Allow" location permissions before redirecting to settings (Android 11+).

**Components:**
*   **Icon:** Standard Material Symbol: `location_on` (or a variation indicating background).
*   **Title:** "Step 2: Enable Background"
*   **Body:** "To prevent gaps in your history when the screen is off, Locus needs 'Always Allow' access."
*   **Action:** "Open Settings" (Triggers System Settings).

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|                                                  |
|          ( Icon: location_on + badge )           |
|                                                  |
|            Step 2: Enable Background             |
|                                                  |
|  To record while the screen is off, Locus        |
|  needs 'Always Allow' access.                    |
|                                                  |
|  We will open Settings. Please select            |
|  "Allow all the time".                           |
|                                                  |
+--------------------------------------------------+
|                                                  |
|           [    OPEN SETTINGS    ]                |
+--------------------------------------------------+
```

### 2.10. Permission Denied (Blocking)
**Purpose:** Blocks the user from proceeding if they deny the required permissions during the flow. This ensures no user enters the Dashboard in a broken state.

**Behavior:**
*   **Trigger:** User denies permissions or returns from settings without granting "Always Allow".
*   **Immediacy:** This blocking state triggers *immediately* upon returning to the app if the required permission is not granted.
*   **Re-Check Loop:** When the app resumes (`onResume`) from the System Settings, it **automatically** re-checks the permission status.
    *   *If Granted:* The app automatically advances to the Success Screen (no user action needed).
    *   *If Still Denied:* The app remains on this blocking screen.
*   **State:** The screen transitions to this blocking state.
*   **Action:** "Open Settings" is the *only* available action.

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|                                                  |
|          ( Icon: error / warning )               |
|                                                  |
|             Permission Required                  |
|                                                  |
|  Locus cannot function without Location access.  |
|                                                  |
|  You must enable "Allow all the time" in         |
|  Settings to proceed.                            |
|                                                  |
+--------------------------------------------------+
|                                                  |
|           [    OPEN SETTINGS    ]                |
+--------------------------------------------------+
```

### 2.11. Success (Completion)
**Purpose:** Final confirmation before entering the dashboard.

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|                                                  |
|            ( Icon: check_circle )                |
|                                                  |
|               You're all set!                    |
|                                                  |
|       Infrastructure deployed & Permissions      |
|       granted.                                   |
|                                                  |
+--------------------------------------------------+
|         [ GO TO DASHBOARD ]                      |
|    (Action clears back stack)                    |
+--------------------------------------------------+
```
