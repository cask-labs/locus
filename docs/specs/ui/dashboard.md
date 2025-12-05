# Dashboard (Home)

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

**Purpose:** Provide an "at-a-glance" view of system health and allow manual overrides.

## 1. Layout Behavior
*   **Phone (Portrait):** Scrollable Column. The **Status Card** and **Action Button** scroll with the content (not pinned). The "Recent Activity" list appears at the bottom.
*   **Phone (Landscape):** Scrollable Column (Standard).
*   **Tablet/Large Screen (Landscape > 600dp):** Three-pane layout.
    *   **Pane 1 (Left, Fixed):** **Navigation Rail** (Persistent).
    *   **Pane 2 (Middle, Fixed):** **Status Card** and **"Sync Now" Action Button**. This acts as the control panel.
    *   **Pane 3 (Right, Scrollable):** **Stats Grid** and **Recent Activity** history. The Stats Grid scrolls **with** the content (not pinned).

## 2. Components
*   **Icon:** `dashboard`
*   **Skeleton Loader (Initial State):**
    *   When the Dashboard first loads (before local DB query completes), all text values in the Status Card and Stats Grid must display a **Shimmer/Skeleton** placeholder effect to indicate loading.
    *   **Initialization:** If the Foreground Service status is not yet known (e.g., app just launched), the Status Card displays an "Acquiring..." or "Initializing..." state after the skeleton load finishes, distinct from an error state, until the first valid status emission is received.
*   **Status Card:** A prominent card mirroring the Persistent Notification state. Handles "Active", "Error", "Paused", "User Stopped", and "Initializing" states.
*   **Stats Grid:** "Local Buffer" count, "Last Sync" time, "Next Sync" estimate.
    *   *Reactivity:* Toggling the "Unit System" in Settings must immediately reflect in these values (e.g., switching distance units if displayed here) without requiring a reload.
*   **Actions:** "Sync Now" button.
    *   *Placement:* On phones, this button is placed **below** the Stats Grid (scrolling). On tablets, it is fixed in Pane 2 (Middle).
    *   *Behavior:* When tapped, transforms into a **Linear Progress Indicator** showing "Uploading batch X of Y..." until completion.
    *   *Empty Buffer Behavior:* If the local buffer is empty (0 points), the button remains **enabled**. Tapping it triggers a transient **Snackbar** ("Buffer is empty") to provide immediate system feedback that the command was received but no work is needed.
    *   *Offline Behavior:* If the device is offline, the button remains enabled, but tapping it triggers a "Fail Fast" behavior: a **Snackbar** appears immediately ("No Internet Connection"), and no network request is attempted.
    *   *Error Handling:* Transient failures (e.g., "Network Error" during upload) must revert the button state and appear as a **Snackbar** anchored above the bottom navigation.
*   **Sensor Status:** Small indicators for GPS, Network, and Battery state.
    *   *Design:* These must use an **Icon + Short Value** format (e.g., [Icon] High, [Icon] 85%) and leverage dynamic **color and icon changes** (e.g., Green Check, Red Alert, Grey Slash) to indicate state.
*   **Stop Tracking Action:** A secondary action to manually stop the tracking service.
    *   *Placement:* Located **inside the Status Card** (top right icon button) to keep the context tight and the layout clean.
    *   *Interaction:* Tapping triggers a **Confirmation Dialog** to prevent accidental data gaps.
*   **Recent Activity:** A simple list showing the last few days of tracking summary (e.g., "Yesterday: 14km").
    *   *Limit:* The list displays a maximum of **5 items** (e.g., the last 5 days with activity).
    *   *Interaction:* Tapping an item in this list navigates the user to the **Map Tab**, pre-loading the selected date.
    *   *Empty State:* If no activity is found (0 records), display a centered "No recent activity recorded" message with a generic illustration/icon.

## 3. Wireframes

**ASCII Wireframe (Active - Phone Portrait):**
```text
+--------------------------------------------------+
|  [ STATUS CARD ]                                 |
|  Status: Recording (High Accuracy)               |
|  State:  Synced                                  |
|                                                  |
|           [ STOP TRACKING ] (Outlined)           | <-- Manual Stop Action
|  ----------------------------------------------  |
|  [ (Sat) High ]  [ (Bat) 85% ]  [ (Wifi) On ]    | <-- Icon + Text, Colored by State
+--------------------------------------------------+
|                                                  |
|   +----------------+      +----------------+     |
|   |  1,240         |      |  5 mins ago    |     |
|   |  Buffered Pts  |      |  Next: ~10m    |     |
|   +----------------+      +----------------+     |
|                                                  |
+--------------------------------------------------+
|                                                  |
|           [  SYNC NOW (Cloud Icon)  ]            |  <-- Primary Action (Filled Tonal)
|      (Becomes: [=== 50% ===] Batch 1/2)          |
|                                                  |
+--------------------------------------------------+
|  Recent Activity                                 |
|  - Yesterday: 14km                               |
|  - Oct 4: 12km                                   |
|                                                  |
|  (Scrolls...)                                    |
+--------------------------------------------------+
| [Dashboard]    Map       Logs      Settings      |  <-- Bottom Nav
+--------------------------------------------------+
```

**ASCII Wireframe (Active - Tablet Landscape):**
```text
+---+------------------------------------+------------------------------------+
| N |  [ STATUS CARD ]                   |  Buffered: 1,240                   |
| A |  Status: Recording                 |  Last Sync: 5 mins ago             |
| V |  State:  Synced                    |                                    |
|   |  --------------------------------  |                                    |
| R |  [GPS] [Bat] [Wifi]                |  Recent Activity                   |
| A |  --------------------------------  |  - Yesterday: 14km                 |
| I |  [ SYNC NOW (Cloud Icon) ]         |  - Oct 4: 12km                     |
| L |                                    |                                    |
|   |  (Pane 2: Fixed height/width)      |  (Pane 3: Scrollable Stats)        |
|   |                                    |                                    |
+---+------------------------------------+------------------------------------+
```

**Status Card (Paused - Tier 2 Environmental):**
```text
+--------------------------------------------------+
|  [ STATUS CARD ] (Yellow/Warning Background)     |
|  Status: Paused (Low Battery)                    |
|  State:  Idle                                    |
|  ----------------------------------------------  |
|  Recording paused to save battery (<15%).        |
|  Will resume automatically when charged.         |
|                                                  |
|  (No Action Button - Passive State)              |
+--------------------------------------------------+
```

**Status Card (User Stopped):**
```text
+--------------------------------------------------+
|  [ STATUS CARD ] (Yellow/Grey Background)        |
|  Status: Stopped by User                         |
|  State:  Idle                                    |
|  ----------------------------------------------  |
|  Tracking paused. Tap to resume.                 |
|                                                  |
|           [ RESUME TRACKING ]                    |
+--------------------------------------------------+
```

**Status Card (Error State - Tier 3 Fatal):**
```text
+--------------------------------------------------+
|  [ STATUS CARD ] (Red Background)                |
|  Status: Service Halted                          |
|  Error:  Permission Revoked                      |
|  ----------------------------------------------  |
|  Locus requires "Always Allow" location access   |
|  to function.                                    |
|                                                  |
|  [ FIX ISSUE (Opens Settings) ]                  |
+--------------------------------------------------+
```
