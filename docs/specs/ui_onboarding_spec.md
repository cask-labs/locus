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

    Provision --> Success((Dashboard))
```

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
*   **Feedback:** Inline error messages for invalid keys.

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
```

### 2.4. Choice Screen
**Purpose:** Determine if this is a new installation or a recovery of an existing one.

**Components:**
*   **Option A (New Device):** "Set up as New Device" (Primary Action).
*   **Option B (Recovery):** "Link Existing Store" (Secondary/Outlined Action).

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|  < Back                                          |
+--------------------------------------------------+
|  Setup Options                                   |
|                                                  |
|  Credentials Validated!                          |
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
*   **Input:** Device Name (default: System Model, e.g., "Pixel 7").
*   **Validation:** Check for name collisions.
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
|  [ Pixel 7                     ]                 |
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
*   **Action:** Tap a list item to select.

**ASCII Wireframe:**
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
*   **Success:** Shows Checkmark, "You're all set!", and "Go to Dashboard" button.

**Components:**
*   **Progress Indicator:** Linear progress bar (determinate based on step count).
*   **Current Step:** Text description of the active task.
*   **Log Window (Optional/Expandable):** Detailed output for debugging.

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

**ASCII Wireframe (Success):**
```text
+--------------------------------------------------+
|                                                  |
|            ( Checkmark Icon )                    |
|                                                  |
|               You're all set!                    |
|                                                  |
|       Infrastructure deployed successfully.      |
|                                                  |
+--------------------------------------------------+
|         [ GO TO DASHBOARD ]                      |
|    (Action clears back stack)                    |
+--------------------------------------------------+
```

**ASCII Wireframe (Failure):**
```text
+--------------------------------------------------+
|                                                  |
|            ( Error / Alert Icon )                |
|                                                  |
|               Provisioning Failed                |
|                                                  |
|       Error: CloudFormation Stack Creation       |
|       failed. (Rollback Complete)                |
|                                                  |
|       [ View Error Logs ]                        |
|                                                  |
+--------------------------------------------------+
|             [ RETRY ]                            |
+--------------------------------------------------+
```

### 2.8. Permission Rationale
**Purpose:** Explain the necessity of "Always Allow" location permissions before triggering the system dialogs. This improves approval rates and clarifies intent.

**Components:**
*   **Icon:** Location/Map icon.
*   **Title:** "Enable Location Tracking"
*   **Body:** "To build your history, Locus needs to access your location in the background, even when the app is closed. This data is stored only on your phone and your private S3 bucket."
*   **Action:** "Continue" (Triggers System Dialogs).

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|                                                  |
|                ( Location Icon )                 |
|                                                  |
|            Enable Location Tracking              |
|                                                  |
|  Locus runs in the background to record your     |
|  journey.                                        |
|                                                  |
|  We need you to select "Allow all the time"      |
|  in the next step to ensure gaps don't appear    |
|  in your history.                                |
|                                                  |
+--------------------------------------------------+
|                                                  |
|           [      CONTINUE      ]                 |
+--------------------------------------------------+
```
