# Map (Visualization)

**Purpose:** Verify and explore historical movement data.

## 1. Layout Behavior
*   **Full Screen:** The map view occupies the entire screen behind transparent system bars.
*   **Overlays:** Controls and Action Buttons are anchored to the edges (safe area insets).
*   **Bottom Sheet:** A persistent sheet that peaks at the bottom (minimized height) and expands on drag or tap. It does *not* cover the whole map when minimized, only showing essential text.
*   **Back Button Behavior:**
    *   **Hierarchy:** Close Point Detail -> Collapse Bottom Sheet -> **Return to Dashboard**.
    *   *Rationale:* The Dashboard is the "Start Destination". Pressing Back from the Map should follow standard navigation patterns and return the user to the home screen, not exit the application immediately.
*   **Tablet Layout (>600dp):** Two-pane layout.
    *   **Pane 1 (Left):** **Navigation Rail** (Persistent).
    *   **Pane 2 (Right):** Full-screen **Map View**.
    *   *Constraint:* The **Persistent Bottom Sheet** is constrained to a **max-width of 600dp** and is **centered specifically within Pane 2** (the Map area), not the whole screen.

## 2. Components
*   **Icon:** `map`
*   **Map View:** Full-screen `osmdroid` view.
    *   *Theme:* **Dark Mode Support:** The map tiles themselves must visually adapt to Dark Mode using a **Color Filter** (e.g., inversion or dimming matrix) applied to the MapView canvas when the system theme is Dark.
        *   *Exception:* This Color Filter must be **disabled** when the user selects "Satellite" mode, as satellite imagery should not be inverted.
    *   *Performance:* **Downsampling:** The rendered path is visually simplified (e.g., Ramer-Douglas-Peucker) for performance; zooming in reveals more detail.
    *   *Offline State:* If the map is viewed offline and **any** tiles are not cached (showing an empty grid), a transient **Snackbar** ("Map Offline") must appear to explain the missing visual context.
*   **Controls:**
    *   **Zoom Buttons (+/-):** Floating buttons anchored to the **Bottom Right**, just above the Bottom Sheet peek height.
        *   *Behavior:* These buttons must **fade out/hide** when the interface enters **Mode B (Point Detail)** to prevent visual clutter and accidental touches.
    *   **Share/Snapshot:** Floating button anchored to the **Top Right**.
        *   *Behavior:* Generates a static image (JPEG/PNG) of the current map viewport (including the visible track) and invokes the system Share Sheet.
*   **Layer Switcher (Modal Bottom Sheet):**
    *   *Trigger:* FAB or Overlay Button.
    *   *Behavior:* Opens as a **Modal** Bottom Sheet (distinct from the persistent history sheet).
    *   *Tablet Constraints:* Max-width **600dp**, centered within the Map Pane.
    *   *Content:*
        *   **Map Type:** Radio selection (Standard, Satellite).
        *   **Signal Overlay:** Radio selection (None, Signal: Cellular, Signal: WiFi). These are mutually exclusive to prevent visual clutter.
        *   **Source Device:**
            *   *Default:* **Current Device** (Strict Default). If no data exists for the current device on the selected day, the map is empty.
            *   *Interaction:* **Single Select** via Radio Buttons.
            *   *State:*
                *   If only 1 device found: Display name as Read-Only Text.
                *   If >1 device found: Display Radio Group.
                *   If Loading: Display Shimmer effect.
*   **Empty State (No History):**
    *   If no data is recorded/selected, Map centers on user location. Bottom Sheet displays "No data recorded today."
*   **Empty State (Network Error):**
    *   If S3 Index cannot be fetched: Map centers on user. Bottom Sheet displays "Offline: Cannot fetch history." with a "Retry" text button.
*   **Bottom Sheet (Multi-Mode):**
    *   **Mode A (Day Summary):** Persistent summary of the selected day.
    *   **Loading State:** When fetching data, the top of the Bottom Sheet displays an indeterminate **Linear Progress Indicator**.
    *   **Mode B (Point Detail):** Displays details when a track point is tapped.
        *   *Internal State:* This mode is an internal state of the Map Screen composable, not a separate Navigation Destination.
        *   *Dynamic Content:* Fields with missing data (e.g., no Altitude or Signal info) must be **hidden completely** (layout collapses) rather than displaying "N/A" or empty values.
    *   **Dismissal:** Users can return to Mode A by tapping the map area, swiping the sheet down, or tapping the Close button.
    *   **Date Interaction:** The Date text is a clickable touch target that opens a **Custom Calendar Picker** (Modal Bottom Sheet).
        *   *Feature:* The Calendar must display **Data Indicators** (dots) on days that have **verified (synced)** historical data available in the local cache or S3 index. Unsynced local buffer data is not indicated here.
        *   *Loading State:* While fetching the "data dots" from the local database:
            *   Display an **Indeterminate Progress Indicator** (Spinner/Bar) over the calendar grid.
            *   **Disable** the "Previous Month" and "Next Month" controls to prevent rapid navigation/race conditions.
            *   Disable interaction with individual dates.
    *   **Accessibility:** Must have a clear Content Description (e.g., "Change Date, current is Oct 4").

## 3. Map Overlays
*   **Visual Discontinuity:** Track lines must break if the time gap > 5 minutes.
*   **Signal Quality:** When a signal layer (Cellular or WiFi) is active, the track line is colored to represent signal strength.
    *   **Logic:** See [Heatmap Logic Specification](../logic_heatmap.md).
    *   **No Data:** Areas with *no* signal data (e.g., visual discontinuity gaps) are simply not drawn.
*   **Rapid Acceleration/Braking Markers:**
    *   **Trigger:** Events flagged as rapid acceleration or hard braking.
    *   **Visual:** A **24dp** Icon Marker using the Material Symbol **`speed`**.
    *   **Color:** **Error/Red** (`MaterialTheme.colorScheme.error`).
    *   **Interaction:** Clickable; opens Point Detail mode.

## 4. Wireframes

**ASCII Wireframe (Calendar Picker):**
```text
+--------------------------------------------------+
|  Select Date                                     |
|  ( Indeterminate Progress Bar if Loading... )    |
|                                                  |
|  < (Disabled)  October 2023  (Disabled) >        |
|  Su Mo Tu We Th Fr Sa                            |
|      1  2  3  4  5  6                            |
|                  .                               |
|   7  8  9 10 11 12 13                            |
|      .     .                                     |
|  ....................                            |
+--------------------------------------------------+
```

**ASCII Wireframe (Day Summary):**
```text
+--------------------------------------------------+
|                                [Share]  [Layers] |  <-- Action Overlays (Top Right)
|               ( Map Area )                       |
|         . . . . . . . . . . .                    |
|         .                   .                    |
|         .    (Track Line)   .                    |
|         .                   .                    |
|         . . . . . . . . . . .           [ + ]    |  <-- Zoom Buttons (Bottom Right)
|                                         [ - ]    |
+--------------------------------------------------+
|  [=== Loading... (Progress Indicator) ===]       |  <-- Linear Progress (if loading)
|  [ October 4, 2023 (v) ]                         |  <-- Clickable (Opens Data-Dot Calendar)
|  12.4 km  •  4h 20m  •  24 km/h avg              |
+--------------------------------------------------+
| [Dashboard]   [Map]      Logs      Settings      |
+--------------------------------------------------+
```

**ASCII Wireframe (Point Detail):**
*   *Note:* Fields with missing data (e.g., no Altitude or Signal info) must be **hidden completely** rather than displaying "N/A" or empty values.

```text
+--------------------------------------------------+
|               ( Map Area )                       |
|             (Selected Point O)                   |
+--------------------------------------------------+
|  [ X ] Close Detail                              |
|  14:02:15  •  35 km/h  •  Bat: 84%               |
|  Signal: WiFi (Level 3, -65 dBm)                 |
|  Altitude: 450m                                  |
+--------------------------------------------------+
```

**ASCII Wireframe (Network Error):**
```text
+--------------------------------------------------+
|                                                  |
|               ( Map Area )                       |
|                                                  |
+--------------------------------------------------+
|  Offline: Cannot fetch history index.            |
|               [ RETRY ]                          |
+--------------------------------------------------+
```
