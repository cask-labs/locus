# Settings

**Purpose:** Manage configuration, identity, and application behavior.

## 1. Layout Behavior
*   **Grouped List:** Settings are organized into distinct categories (Identity, General, Data) with headers.
*   **Standard List Items:** Uses standard Material Design list items with switches or chevrons.
*   **Tablet Layout (>600dp):** Two-pane layout.
    *   **Pane 1 (Left):** **Navigation Rail** (Persistent).
    *   **Pane 2 (Right):** Content is centered with a **max-width of 600dp**.

## 2. Components
*   **Icon:** `settings`
*   **Identity:** Display current "Device ID" and "AWS Stack Name".
    *   **Tap to Copy:** Tapping the row copies the value (Device ID or Stack Name) to the clipboard and displays a transient **Snackbar** ("Copied to clipboard").
    *   **Loading State:** If these values are being fetched asynchronously (e.g., from DataStore/SharedPreferences), display a small **Inline Progress Spinner** next to the label until the data is available.
*   **Preferences (General):**
    *   "Theme": Tapping opens a Dialog to select [System Default | Light | Dark].
    *   "Unit System": Toggle (Metric/Imperial). Changes must reflect immediately across the app.
    *   "Share Anonymous Stats": Toggle (Opt-in). Help improve Locus by sharing crash reports and basic health stats.
*   **Danger Zone:**
    *   "Clear Local Track Buffer" (Red Text). *Warning:* Tapping this triggers a confirmation dialog. If confirmed, the button enters a **Loading/Disabled State** (Indeterminate Spinner) while the **Track** database deletion occurs. This action does not affect the Diagnostic Log buffer.
        *   *Dialog Wireframe:*
            ```text
            +--------------------------------------------------+
            |  Delete Unsynced Track Data?                     |
            |                                                  |
            |  You are about to delete 1,240 points from       |
            |  the local track buffer.                         |
            |                                                  |
            |  This data has NOT been uploaded to S3 yet.      |
            |  This action cannot be undone.                   |
            |                                                  |
            |      [ CANCEL ]       [ DELETE PERMANENTLY ]     |
            +--------------------------------------------------+
            ```
        *   *Feedback:* Upon successful completion, display a **Snackbar** ("Local track buffer cleared") and revert the button to its enabled state.
    *   "Reset App" (Red Text). *Warning:* Wipes all keys (Runtime Keys in `EncryptedSharedPreferences`), databases, and preferences. Returns app to "Fresh Install" state (Onboarding).
        *   *Feedback:* This action triggers a **Blocking Progress Dialog** ("Resetting Application...") that prevents interaction/exit until the cleanup is complete and the app restarts.
    *   "Destroy Cloud Resources" (Red Text). *Warning:* Initiates the [Offboarding Workflow](offboarding.md) to permanently delete CloudFormation stacks and S3 buckets before resetting the application.
        *   *Behavior:* Navigates to the Offboarding Credential Input screen.
*   **About (Section):** Displayed as a grouped section at the bottom of the main settings list.
    *   *Structure:* Header ("About") followed by standard list items.
    *   *Items:* "Version" (e.g., 1.0.0 (12)), "Source Code" (Link).
    *   *Behavior:* External links (e.g., Source Code) must open in the system default **External Browser** (e.g., Chrome Custom Tab), not a WebView.

## 3. Wireframes

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|  Identity                                        |
|  Device: Pixel7 (Locus-Pixel7)                   |
|  ----------------------------------------------  |
|  General                                         |
|  Theme: System Default                           |
|  [ ] Metric Units (km)                           |
|  [ ] Share Anonymous Stats                       |
|  ----------------------------------------------  |
|  Danger Zone                                     |
|  [ Clear Local Track Buffer (!) ]                | <--- Triggers Confirmation Dialog
|  [ Reset Application (!)      ]                  | <--- Triggers Confirmation Dialog
|  [ Destroy Cloud Resources (!) ]                 | <--- Starts Offboarding Flow
|  ----------------------------------------------  |
|  About                                           |
|  Version 1.0.0 (12)                              |
|  Source Code                                 >   |
+--------------------------------------------------+
```
