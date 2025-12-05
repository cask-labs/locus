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

## 2. Technical Implementation Notes (State Persistence)

To ensure robustness against process death (OS killing the app) or user backgrounding, the flow relies on a "State Machine Persister" using `EncryptedSharedPreferences`.

*   **Provisioning Persistence:**
    *   When the user begins **Provisioning**, a state flag `PROVISIONING_STARTED` is saved to disk along with the `stack_name`.
    *   If the app is killed and relaunched, it detects this flag and **immediately restores the Provisioning Progress screen**, checking `WorkManager` status or polling CloudFormation directly.
*   **The "Setup Trap" (Permissions):**
    *   Upon successful provisioning, the state updates to `PERMISSIONS_PENDING`.
    *   If the user closes the app before granting permissions, future launches **bypass** Welcome/Dashboard and land directly on **Permission Step 1**. The user is "trapped" in the flow until completion.

## 3. Screen Specifications

### 3.1. Welcome Screen
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

### 3.2. Key Generation Guide (Bottom Sheet)
**Trigger:** Tapping "How to generate AWS Keys" on the Welcome Screen.
**Purpose:** Step-by-step instructions for users to generate temporary keys via CloudShell.

**Components:**
*   **Step 1:** "Log in to AWS Console."
*   **Step 2:** "Open CloudShell (Terminal Icon)."
*   **Step 3:** "Paste this command:"
    *   **Visual:** A **Code Block** (Surface Variant background, Monospace font) containing the command.
    *   **Actions:**
        *   **Copy Button:** Icon Button aligned to the right, inside the block.
        *   **Share Button:** Icon Button next to Copy. Invokes the system Share Sheet with the command text (useful for sending to another device).
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
|  +--------------------------------------------+  |
|  | aws sts get-session-token ...              |  |
|  |                           [Share] [Copy]   |  | <-- Code Block with Actions
|  +--------------------------------------------+  |
|  3. Copy the output values (JSON).               |
|                                                  |
|           [ CLOSE ]                              |
+--------------------------------------------------+
```

### 3.3. Credential Entry
**Purpose:** Collect AWS credentials to bootstrap the connection.

**Components:**
*   **Convenience Action:** **"Paste JSON"** (Text Button or Icon).
    *   *Behavior:* Parses clipboard content for a `Credentials` object containing `AccessKeyId`, `SecretAccessKey`, and `SessionToken`. Automatically fills fields. Errors if invalid JSON.
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
|  [ Paste JSON ]                                  | <--- Auto-fill Action
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

### 3.4. Choice Screen
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

### 3.5. Path A: New Device Setup
**Purpose:** Define the device identity and deploy infrastructure.

**Components:**
*   **Input:** Device Name (default: System Model, e.g., "pixel-7").
*   **Validation:**
    *   **Strict Formatting:** Lowercase, alphanumeric, hyphens only (auto-formatted).
    *   **Debounced Availability Check:** Triggers 500ms after typing stops.
    *   **States:**
        *   *Idle:* No indicator.
        *   *Checking:* Small inline spinner/text "Checking availability..."
        *   *Taken:* Red error text "Name already in use. Please choose another."
        *   *Available:* Green checkmark / "Available".
*   **Action:** "Deploy Infrastructure" (Disabled if invalid or name taken).

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
|  (O) Checking availability...                    | <--- Debounced State
|                                                  |
+--------------------------------------------------+
|         [ DEPLOY INFRASTRUCTURE ]                |
+--------------------------------------------------+
```

### 3.6. Path B: Recovery (Link Store)
**Purpose:** Select an existing Locus store to link.

**States:**
1.  **Loading:** Initial state while fetching buckets via `s3:ListBuckets`.
2.  **Error:** Network failure or Access Denied. Requires "Retry" button.
3.  **Content (Populated):** List of buckets matching `locus-` prefix.
    *   *Data:* Bucket Name only. (Date is omitted for performance).
4.  **Empty:** No buckets found.

**Components:**
*   **Action:** Tap a list item to select.

**ASCII Wireframe (Loading/Error):**
```text
+--------------------------------------------------+
|  < Back                                          |
+--------------------------------------------------+
|  Select Existing Store                           |
|                                                  |
|      ( Circular Progress Indicator )             | <--- Loading State
|              Finding stores...                   |
|                                                  |
|  [ OR ]                                          |
|                                                  |
|      ( Icon: cloud_off )                         | <--- Error State
|      Failed to load stores.                      |
|           [ RETRY ]                              |
+--------------------------------------------------+
```

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
|                                                  |
|  [ (Bucket Icon) Locus-iPhone                 ]  |
|                                                  |
+--------------------------------------------------+
```

### 3.7. Provisioning (Progress)
**Purpose:** Visual feedback during long-running CloudFormation tasks.

**Behaviors:**
*   **Back Button:** Minimizes the app (Backgrounds it). Does **not** cancel the process.
*   **App Launch:** Always restores this screen if provisioning is active (via State Persistence).

**Displayed Steps:**
1.  **"Validating CloudFormation Template..."**
2.  **"Creating Storage Stack..."**
3.  **"Provisioning Resources..."**
4.  **"Generating Runtime Keys..."**
5.  **"Finalizing Setup..."**

**States:**
*   **In Progress:** Progress bar + Step.
*   **Success:** Checkmark + "Setup Complete". Auto-transitions to Permissions.
*   **Hard Failure (e.g., Rollback):**
    *   Shows Error Icon & "Deployment Failed".
    *   **Action:** "Back to Setup" (Redirects to New Device Screen).
    *   *Note:* The app does **not** attempt to delete the failed stack.

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

### 3.8. Permission Step 1: Foreground
**Purpose:** Request the initial "While Using" location permission.
**Constraint:** If the user exits here, relaunching the app returns *immediately* to this screen ("Setup Trap").

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

### 3.9. Permission Step 2: Background
**Purpose:** Explain the necessity of "Always Allow" location permissions.

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

### 3.10. Permission Denied (Blocking)
**Purpose:** Blocks the user from proceeding if permissions are missing.

**Behavior:**
*   **Automatic Re-Check:** Re-checks permissions in `onResume` (returning from Settings).
*   **Permanent Denial:** If the user has selected "Don't ask again" (system dialog blocked), the "Open Settings" button must Intent specifically to the **App Info** screen (`ACTION_APPLICATION_DETAILS_SETTINGS`) rather than general Location Settings, to allow manual toggle.

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
|                                                  |
|               (Check Again)                      |
+--------------------------------------------------+
```

### 3.11. Success (Completion)
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
