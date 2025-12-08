# Logs (Diagnostics)

**Purpose:** Provide deep technical insight into the system's operation. While essential for verification, this screen also serves as a critical diagnostic tool for users to verify system health in production.

## 1. Layout Behavior
*   **Sticky Header:** The Top App Bar (containing the Title/Search) and the Filter Chips row remain pinned to the top.
*   **Reverse Layout (StackFromBottom):** The `RecyclerView` starts from the bottom (newest items) by default. New entries are appended to the bottom.
*   **Auto-Scroll Logic:**
    *   **Default:** The list auto-scrolls to stay at the newest entry as logs arrive.
    *   **Paused:** Auto-scroll is **disabled** if:
        1.  The user scrolls up away from the bottom.
        2.  A specific **Filter** (other than "All") is active.
        3.  The **Search** input is active.
    *   **Resumed:** Tapping the "Jump to Bottom" FAB re-enables auto-scroll.
*   **Tablet Layout (>600dp):** Two-pane layout.
    *   **Pane 1 (Left):** **Navigation Rail** (Persistent).
    *   **Pane 2 (Right):** Content (Logs List) is centered horizontally with a **max-width of 800dp** to ensure readability on large devices.

## 2. Components
*   **Icon:** `terminal`
*   **Data Source:** **Local Buffer Only.**
    *   The screen displays *strictly* the contents of the local circular buffer (approx. 5MB of recent logs).
    *   **No Remote Fetch:** There is no "Infinite Scroll" to download older history from S3. Deep historical analysis must be performed by downloading the `.ndjson.gz` files from the S3 bucket externally.

### 2.1. Top App Bar & Search
*   **Search Action:** A "Magnifying Glass" icon in the top-right.
    *   *Interaction:* Tapping expands a text input field, temporarily replacing the screen title.
    *   *Logic:* Search acts as an **AND** filter combined with active chips (e.g., `(Error OR Warn) AND "Network"`).
    *   *Matching:* Case-insensitive substring match against Tag and Message.
*   **Context Menu:**
    *   **Export Logs:**
        *   *Format:* **Plain Text (`.txt`)**. This ensures the file is easily readable on any device without specialized tools.
        *   *Content:* The complete current contents of the local buffer, formatted line-by-line.

### 2.2. Filter Chips
*   **Type:** Multi-select Choice Chips.
*   **Logic:** Functions as a **Union (OR)** operation. Selecting "Error" and "Warn" displays entries that are *either* Errors *or* Warnings.
*   **Levels:** strictly adheres to conventional log levels:
    *   **Error** (Red)
    *   **Warn** (Yellow)
    *   **Info** (Blue)
    *   **Debug** (Gray)
*   **Persistence:** Filters **reset to default (Show All)** when leaving the screen.

### 2.3. Log List
*   **Row Layout:** Single-line (ellipsize at end) to maximize density.
    *   **Icon:** Small, colored status icon at the start (e.g., ❌ Red, ⚠️ Yellow, ℹ️ Blue).
    *   **Timestamp:**
        *   *Today:* `HH:mm:ss`
        *   *Older:* `MMM dd HH:mm`
    *   **Tag:** `[Tag]` in bold/colored text.
    *   **Message:** The log content.
*   **Interaction:** Tapping any row opens the **Log Detail Bottom Sheet**.

### 2.4. Floating Controls
*   **Jump to Bottom FAB:** A small Floating Action Button (e.g., "Arrow Down" icon).
    *   *Visibility:* Appears **only** when auto-scroll is paused (scrolled up, searching, or filtering). Disappears when at the bottom.
    *   *Action:* Smoothly scrolls to the most recent entry and resumes auto-scroll.

### 2.5. Log Detail Bottom Sheet (Modal)
*   **Trigger:** Tap on any log row.
*   **State:** Displays the **static** state of that specific entry (does not update live).
*   **Header:**
    *   Large Level Icon & Label (e.g., "ERROR").
    *   Full Timestamp: `MM-dd HH:mm:ss.SSS`.
    *   Tag Name.
*   **Body:**
    *   **Monospace Font:** Preserves formatting for stack traces and JSON.
    *   **Selectable Text:** User can long-press to select/copy specific substrings.
    *   **Scrollable:** Vertical scrolling for long messages.
*   **Footer Actions:**
    *   **Copy Full Entry:** Copies the formatted string to clipboard.
    *   **Share Entry:** System Share Sheet for just this log line.
    *   **Navigation:** `< Previous` and `Next >` buttons to step through logs without closing the sheet.
*   **Visuals:** Background dims behind the sheet.

## 3. States

### 3.1. Loading States
*   **Initial Load:** A **Centered Circular Progress Indicator** must appear immediately upon opening the screen while the initial database fetch is performed, before the list content becomes visible.
*   **Exporting:** When "Share/Export" is tapped:
    *   The menu item becomes **Disabled**.
    *   A **Circular Progress Spinner** replaces the icon.

### 3.2. Empty States
*   **No Logs Recorded:** If the database is truly empty.
    *   *Message:* "No logs recorded yet."
*   **No Matches:** If data exists but filters/search hide it.
    *   *Message:* "No logs match current criteria."

### 3.3. Error States
*   **Export Failure:** If file generation fails (e.g., storage full).
    *   *Action:* Display a **Transient Snackbar** with the error message (e.g., "Failed to export logs").
    *   *Reset:* The Export button returns to its enabled state.

## 4. Wireframes

**ASCII Wireframe (List View):**
```text
+--------------------------------------------------+
| Logs                        [Search] [Export/Menu]|
+--------------------------------------------------+
|  [x] Error   [x] Warn   [ ] Info   [ ] Debug     |  <-- Sticky Header (Standard Levels)
+--------------------------------------------------+
|                                                  |
| ❌ 14:02:10 [Loc] RecordPoint: Acc=12m           |
| ℹ️ 14:02:05 [Net] Upload: Success (200 OK)       |
| ℹ️ 14:01:55 [S3]  ListObjects: tracks/2023/10    |
| ⚠️ 14:01:40 [Wtch] Heartbeat: OK                 |
| ℹ️ Oct 26 14:00 [Bat] Level: 84% (Discharging)   |
|                                        [ v ]     |  <-- Jump to Bottom FAB
+--------------------------------------------------+
| [Dashboard]    Map      [Logs]     Settings      |
+--------------------------------------------------+
```

**ASCII Wireframe (Detail Bottom Sheet):**
```text
+--------------------------------------------------+
| (Dimmed Background)                              |
|                                                  |
| +----------------------------------------------+ |
| | ❌ ERROR                    2023-10-27...    | |
| | Tag: [LocationManager]                       | |
| |----------------------------------------------| |
| | java.lang.IllegalStateException: Location    | |
| | provider not ready.                          | |
| |   at com.locus.service.LocService...         | |
| |   at android.os.Looper...                  | |
| |                                              | |
| | (Scrollable Monospace Text)                  | |
| |                                              | |
| |----------------------------------------------| |
| | [ < Prev ]   [ Copy ] [ Share ]   [ Next > ] | |
| +----------------------------------------------+ |
+--------------------------------------------------+
```
