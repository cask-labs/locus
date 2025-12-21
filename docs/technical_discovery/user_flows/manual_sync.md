# Manual Sync & Status

**Goal:** Immediate verification of data safety and system health.

*   **Step 1: Dashboard Status:**
    *   The dashboard displays:
        *   **Local Buffer:** Number of data points waiting on the device.
        *   **Last Sync:** Relative time (e.g., "5 minutes ago").
        *   **Connection State:** Current network eligibility (e.g., "Ready", "No Internet").
*   **Step 2: User Action (Force Sync):**
    *   The user taps **"Sync Now"** on the Dashboard.
    *   *Note:* This is the **exclusive** method for triggering a manual sync; no other settings or menus expose this function.
    *   **Transformation:** The button immediately transforms into a **Linear Progress Indicator** within the button's footprint, displaying text like "Preparing...".
    *   **Override:** This action bypasses low-battery (<10%) restrictions.
    *   **Debounce:** If a sync is already in progress, the UI attaches to the existing event.
*   **Step 3: Execution:**
    *   **Scenario A (Data Exists):** The app packages buffered points into batches and uploads them.
        *   *Feedback:* The button's progress indicator updates dynamically (e.g., "[=== 33% ===] Batch 1 of 3").
    *   **Scenario B (Buffer Empty):** The button should be disabled or the action should provide immediate feedback that there is no data to sync. No network request is performed.
*   **Step 4: Feedback:**
    *   **Success:** Buffer count clears to 0. "Last Sync" updates to "Just now". A "Success" toast appears.
    *   **Failure:** A specific, actionable error message is displayed (e.g., "Upload failed: Check Internet Connection"). No automatic retry is attempted for manual triggers, putting the user in control of the next step.
