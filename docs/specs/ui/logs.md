# Logs (Diagnostics)

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

**Purpose:** Provide deep technical insight into the system's operation. While essential for verification during the "Implementation Definition" phase, this screen also serves as a critical diagnostic tool for users to verify system health in production.

## 1. Layout Behavior
*   **Sticky Header:** The Filter Chips row remains pinned to the top while the list scrolls.
*   **Reverse Layout (StackFromBottom):** The `RecyclerView` starts from the bottom (newest items) by default. New entries are appended to the bottom. If the user is at the bottom, it auto-scrolls; if the user has scrolled up, it maintains position.
*   **Tablet Constraint:** Content restricted to a max-width (e.g., 800dp) and centered.

## 2. Components
*   **Filter Chips:** Multi-select Checkboxes (not Radio buttons) to filter by tag/level.
    *   *Logic:* Filters function as a **Union (OR)** operation. Selecting "Error" and "Warn" displays entries that are *either* Errors *or* Warnings.
    *   *Design:* Must be distinctively color-coded (e.g., Error=Red, Warn=Yellow, Net=Blue) to match the corresponding log lines.
    *   *Accessibility:* Colors must meet contrast requirements.
*   **Log List:** Scrollable list of log entries. Lines are color-coded to match their severity/category.
    *   *Empty State:* If no logs exist (or all are filtered out), display "No logs recorded yet."
*   **Jump to Bottom FAB:** A small Floating Action Button (e.g., "Arrow Down" icon).
    *   *Visibility:* Appears **only** when the user has scrolled up away from the bottom (stopping auto-scroll). Disappears when the user is at the bottom.
    *   *Action:* Tapping smoothly scrolls the list back to the most recent entry and resumes auto-scrolling.
*   **Export/Share:** Action to save logs to a file via the System Share Sheet.
    *   *Note:* **No "Copy to Clipboard"** functionality is provided to avoid performance issues with large buffers.
    *   *Behavior:* Tapping "Share" exports the **entire raw log buffer** (all lines, unfiltered) as a `.txt` file attachment to ensure full context for debugging.
    *   *Feedback:* When tapped, the button becomes **Disabled** and transforms into a **Circular Progress Spinner** while the file is generated.

## 3. Wireframes

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|                                          [Share] |
+--------------------------------------------------+
|  [x] Error   [x] Warn   [ ] Net   [ ] Auth       |  <-- Multi-select Chips (Union/OR, Colored)
+--------------------------------------------------+
| 14:02:10 [Loc] RecordPoint: Acc=12m              |
| 14:02:05 [Net] Upload: Success (200 OK)          |
| 14:01:55 [S3]  ListObjects: tracks/2023/10       |
| 14:01:40 [Wtch] Heartbeat: OK                    |
| 14:00:00 [Bat] Level: 84% (Discharging)          |
|                                        [ v ]     |  <-- Jump to Bottom FAB (Visible on scroll)
+--------------------------------------------------+
| [Dashboard]    Map      [Logs]     Settings      |
+--------------------------------------------------+
```
