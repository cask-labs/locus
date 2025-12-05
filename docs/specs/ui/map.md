# Map (Visualization)

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

**Purpose:** Verify and explore historical movement data.

## 1. Layout Behavior
*   **Full Screen:** The map view occupies the entire screen behind transparent system bars.
*   **Overlays:** Controls and Action Buttons are anchored to the edges (safe area insets).
*   **Bottom Sheet (General):**
    *   **Tablet Constraint:** On large screens (>600dp width), **all** Bottom Sheets (Persistent History, Layer Switcher, Calendar) must have a maximum width of **600dp** and be horizontally centered.
*   **Persistent Sheet:** Peaks at the bottom to show the Day Summary.
    *   *Interaction:* This sheet acts as the anchor for the map's visible area padding.

## 2. Components
*   **Map View:** Full-screen `osmdroid` view.
    *   *Theme:* **Dark Mode Support:** The map tiles themselves must visually adapt to Dark Mode using a **Color Filter** (e.g., inversion or dimming matrix) applied to the MapView canvas when the system theme is Dark.
        *   *Exception:* This Color Filter must be **disabled** when the user selects "Satellite" mode, as satellite imagery should not be inverted.
    *   *Performance:* **Downsampling:** The rendered path is visually simplified (e.g., Ramer-Douglas-Peucker) for performance; zooming in reveals more detail.
    *   *Auto-Fit:* When a day with a valid track is loaded, the map must automatically zoom and pan to fit the track's bounding box (with ~50dp padding).
    *   *Offline State:* If the map is viewed offline and **any** tiles are not cached (showing an empty grid), a transient **Snackbar** ("Map Offline") must appear to explain the missing visual context.
    *   *Permission Denied:* If the user has denied Location permissions, the map remains functional for viewing history. The "My Location" FAB is displayed but **disabled** (or triggers a permission request explanation dialog when tapped).
*   **Controls:**
    *   **Layer Switcher:** Floating button anchored to the **Top Right**, stacked vertically below the Share/Snapshot button.
    *   **Share/Snapshot:** Floating button anchored to the **Top Right** (topmost control).
        *   *Behavior:* Generates a static image (JPEG/PNG) of the current map viewport (including the visible track) and invokes the system Share Sheet.
    *   **Zoom Buttons (+/-):** Floating buttons anchored to the **Bottom Right**.
        *   *Position:* Anchored just above the **Mode A (Peek)** height.
        *   *Behavior:* When the Bottom Sheet transitions to **Mode B (Detail)** or if the sheet were to expand, these buttons must **fade out** to prevent occlusion.
*   **Layer Switcher (Modal Bottom Sheet):**
    *   *Trigger:* Top Right Overlay Button.
    *   *Behavior:* Opens as a **Modal** Bottom Sheet.
    *   *Content:*
        *   **Map Type:** Radio selection (Standard, Satellite).
        *   **Signal Overlay:** Radio selection (None, Signal: Cellular, Signal: WiFi). Mutually exclusive.
*   **Calendar Picker (Modal Bottom Sheet):**
    *   *Trigger:* Tapping the Date text in the Persistent Sheet.
    *   *Range:* Bounded from the **First Logged Date** (from local DB) to **Today**.
    *   *Features:*
        *   **Data Indicators:** Dots on days with verified synced data.
        *   **Offline State:** If the S3 Index cannot be fetched, the picker still opens but displays a **Warning Banner** ("Offline: Data availability unknown") and hides all data dots. Selection is still allowed.
*   **Bottom Sheet (Multi-Mode):**
    *   **Mode A (Day Summary):** The default state.
        *   *State:* **Non-Expandable** (Fixed Peek Height).
        *   *Content:* Date, Total Distance, Total Duration, Average Speed.
        *   *Loading:* Displays an indeterminate **Linear Progress Indicator** at the top edge while fetching track data.
    *   **Mode B (Point Detail):** Active when a track point is tapped.
        *   *State:* **Fixed Height** (Content wrap). Typically taller than Mode A.
        *   *Transition:* Smooth animation from Mode A. Tapping another point updates Mode B instantly.
        *   *Dismissal:* Tapping the Map (background), swiping down, or tapping "Close" returns to Mode A.
        *   *Content:* Time, Speed, Battery, Altitude, Signal. Missing fields are hidden (layout collapses).

## 3. Error Handling
*   **S3 Index Failure (Network Error):**
    *   *Context:* Failed to load the list of available days.
    *   *Visual:* Bottom Sheet displays "Offline: Cannot fetch history index." with a "Retry" button.
    *   *Impact:* Calendar Picker enters "Offline State".
*   **Track Download Failure:**
    *   *Context:* User selected a day, but the specific track file download failed.
    *   *Visual:* **Snackbar** "Failed to load track."
    *   *Sheet:* Remains in Mode A (Summary), potentially showing "--" for stats.
*   **Empty State (No History):**
    *   *Context:* Selected day has no data.
    *   *Visual:* Map centers on user (or default). Bottom Sheet displays "No data recorded today."

## 4. Map Overlays
*   **Visual Discontinuity:** Track lines must break if the time gap > 5 minutes.
*   **Signal Quality:** When a signal layer is active, the track line is colored to represent signal strength.
    *   **No Data:** Areas with *no* signal data (e.g., visual discontinuity gaps) are not drawn.
*   **Markers:**
    *   **Rapid Acceleration/Braking:** 24dp Icon Marker (`speed`), Color: `Error` (Red). Clickable.

## 5. Wireframes

**ASCII Wireframe (Calendar Picker - Offline):**
```text
+--------------------------------------------------+
|  Select Date                                     |
|  [!] Offline: Data availability unknown          |
|                                                  |
|  <             October 2023             >        |
|  Su Mo Tu We Th Fr Sa                            |
|      1  2  3  4  5  6                            |
|                  (No Dots)                       |
|   7  8  9 10 11 12 13                            |
|                                                  |
+--------------------------------------------------+
```

**ASCII Wireframe (Day Summary - Mode A):**
```text
+--------------------------------------------------+
|               [Share]                            |  <-- Top Right (1)
|               [Layers]                           |  <-- Top Right (2)
|               ( Map Area )                       |
|         . . . . . . . . . . .                    |
|         .    (Track Line)   .           [ + ]    |  <-- Zoom (Fade out if Mode B)
|         . . . . . . . . . . .           [ - ]    |
+--------------------------------------------------+
|  [=== Loading... (Linear Progress) ========]     |
|  [ October 4, 2023 (v) ]                         |  <-- Tapping opens Calendar
|  12.4 km  •  4h 20m  •  24 km/h avg              |
+--------------------------------------------------+
```

**ASCII Wireframe (Point Detail - Mode B):**
*   *Note:* Zoom buttons are hidden.

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
