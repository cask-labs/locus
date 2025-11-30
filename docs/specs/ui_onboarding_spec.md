# Onboarding UI Specification

**Related Requirements:** [Setup & Onboarding](../requirements/setup_onboarding.md), [UI Presentation Spec](ui_presentation_spec.md)

This document details the user interface for the Onboarding flow, covering the initial launch, credential entry, and the divergence between "New Device" and "Recovery" paths.

## 1. Flow Overview

The onboarding process guides the user from a fresh install to a fully provisioned, tracking-ready state.

```mermaid
graph TD
    Start((Launch)) --> Welcome[Welcome Screen]
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
+--------------------------------------------------+
| [?] How to generate AWS Keys                     |
|                                                  |
|           [   GET STARTED   ]                    |
+--------------------------------------------------+
```

### 2.2. Credential Entry
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

### 2.3. Choice Screen
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

### 2.4. Path A: New Device Setup
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

### 2.5. Path B: Recovery (Link Store)
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

### 2.6. Provisioning (Progress)
**Purpose:** Visual feedback during long-running CloudFormation tasks.

**Components:**
*   **Progress Indicator:** Linear or Circular progress.
*   **Status Text:** "Provisioning Resources...", "Creating IAM User...", "Success!".

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|                                                  |
|                                                  |
|            ( Cloud Processing Icon )             |
|                                                  |
|           Provisioning Infrastructure...         |
|           [===========       ] 60%               |
|                                                  |
|           Creating IAM User...                   |
|                                                  |
+--------------------------------------------------+
```
