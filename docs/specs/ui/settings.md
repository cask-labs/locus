# Settings

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

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
    *   **Loading State:** If these values are being fetched asynchronously (e.g., from DataStore/SharedPreferences), display a small **Inline Progress Spinner** next to the label until the data is available.
*   **Preferences (General):**
    *   "Theme": Tapping opens a Dialog to select [System Default | Light | Dark].
    *   "Unit System": Toggle (Metric/Imperial). Changes must reflect immediately across the app.
    *   "Share Anonymous Stats": Toggle (Opt-in). Help improve Locus by sharing crash reports and basic health stats.
*   **Danger Zone:**
    *   "Clear Local Buffer" (Red Text). *Warning:* Tapping this triggers a confirmation dialog. If confirmed, the button enters a **Loading/Disabled State** (Indeterminate Spinner) while the database deletion occurs.
        *   *Feedback:* Upon successful completion, display a **Snackbar** ("Local buffer cleared") and revert the button to its enabled state.
    *   "Reset App" (Red Text). *Warning:* Wipes all keys (Runtime Keys in `EncryptedSharedPreferences`), databases, and preferences. Returns app to "Fresh Install" state (Onboarding).
        *   *Feedback:* This action triggers a **Blocking Progress Dialog** ("Resetting Application...") that prevents interaction/exit until the cleanup is complete and the app restarts.
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
|  [ Clear Local Cache (!)      ]                  | <--- Triggers Confirmation Dialog
|  [ Reset Application (!)      ]                  | <--- Triggers Confirmation Dialog
|  ----------------------------------------------  |
|  About                                           |
|  Version 1.0.0 (12)                              |
|  Source Code                                 >   |
+--------------------------------------------------+
```
