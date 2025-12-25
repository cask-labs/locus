# Offboarding UI

**Purpose:** Guide the user through the destructive process of removing Locus resources from their AWS account and resetting the application.

## 1. Flow Overview
1.  **Entry:** Settings > Danger Zone > "Destroy Cloud Resources".
2.  **Auth:** "Verify Authority" (Credential Input).
3.  **Discovery:** "Scanning AWS..." (Loading State).
4.  **Selection:** "Select Resources to Destroy" (Multi-select List).
5.  **Confirmation:** Two-step Verification (Dialogs).
6.  **Execution:** "Cleanup in Progress" (Blocking Console).
7.  **Exit:** App restart to Onboarding.

## 2. Screens

### 2.1 Verify Authority (Credential Input)
*   **Context:** Similar to Onboarding, but framed as "Granting Permission to Destroy".
*   **Elements:**
    *   **Headline:** "Admin Access Required"
    *   **Body:** "To delete CloudFormation stacks and S3 buckets, Locus needs temporary Admin credentials. These will be discarded immediately after the cleanup."
    *   **Inputs:** Access Key ID, Secret Access Key, Session Token (Mandatory).
    *   **Action:** "VERIFY & SCAN" (Primary Button).
    *   **Help:** "How to generate keys" (Bottom Sheet trigger).

### 2.2 Select Resources
*   **Context:** User selects which stacks to delete.
*   **Elements:**
    *   **Headline:** "Select Stacks to Destroy"
    *   **List:** Checkbox list of found stacks (e.g., `locus-user-pixel7`, `locus-user-oldphone`).
        *   *Label:* Stack Name + Creation Date.
        *   *Tag Validation:*
            *   Show **all** stacks matching the `locus-` prefix.
            *   Check for `project=locus` tag.
            *   **If Valid:** Checkbox is enabled and selectable.
            *   **If Invalid:** Display "Invalid Project Tag" badge; Item is disabled (unselectable).
    *   **Selection Logic:**
        *   "Select All" / "Deselect All" toggle (affects only valid stacks).
        *   Current device's stack is pre-selected.
    *   **Action:** "DESTROY [N] STACKS" (Red, Filled Button).

### 2.3 Confirmation Dialogs

**Dialog 1: The Warning**
```text
+--------------------------------------------------+
|  Permanently Delete Data?                        |
|                                                  |
|  You are about to delete 2 Stacks and all        |
|  data associated with them.                      |
|                                                  |
|  This action CANNOT be undone. Data will be      |
|  lost forever.                                   |
|                                                  |
|      [ CANCEL ]             [ I UNDERSTAND ]     |
+--------------------------------------------------+
```

**Dialog 2: The Final Check**
```text
+--------------------------------------------------+
|  Confirm Destruction                             |
|                                                  |
|  Type "DESTROY" to confirm.                      |
|                                                  |
|  [ DESTROY                    ]                  |
|                                                  |
|      [ CANCEL ]         [ DELETE EVERYTHING ]    | <--- Disabled until match
+--------------------------------------------------+
```

### 2.4 Cleanup Execution (The Trap)
*   **Context:** Blocking screen. Cannot navigate back. Resumes on app restart.
*   **Layout:**
    *   **Status Icon:** Large Spinner or processing graphic.
    *   **Headline:** "Cleaning up..."
    *   **Console Log:** A scrolling list of actions.
        *   `[OK] Emptying bucket locus-data-xyz...`
        *   `[OK] Deleting stack locus-user-pixel7...`
        *   `[..] Deleting stack locus-user-oldphone...`
    *   **Escape Hatch:** A "Trouble?" text button at the bottom.
        *   Opens Dialog: "Force Local Reset? If cloud deletion is failing, you can skip it and just reset this app. Orphaned cloud resources will remain in your AWS account." -> [ FORCE RESET ]

## 3. Wireframes

**Execution Screen (ASCII):**
```text
+--------------------------------------------------+
|                                                  |
|                ( Large Spinner )                 |
|                                                  |
|           Destroying Cloud Resources             |
|                                                  |
|  +--------------------------------------------+  |
|  | > Found 2 stacks                           |  |
|  | > Connected to us-east-1                   |  |
|  | > Emptying bucket locus-store-a8f... (Done)|  |
|  | > Deleting stack locus-user-pixel7...      |  |
|  |   ... Waiting for completion               |  |
|  |                                            |  |
|  +--------------------------------------------+  |
|                                                  |
|                                                  |
|                                                  |
|            [ Trouble? Skip to Reset ]            |
+--------------------------------------------------+
```
