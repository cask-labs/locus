# Manual Sync & Status

**Goal:** Immediate verification of data safety.

*   **Step 1: Status Check:**
    *   The main screen displays the "Last Successful Sync" timestamp.
    *   It shows the current size of the local buffer (number of points waiting to upload).
*   **Step 2: Forced Sync:**
    *   The user taps a "Sync Now" button.
    *   The app immediately packages the local buffer, compresses it, and attempts an upload.
*   **Step 3: Feedback:**
    *   **Success:** The local buffer count drops to zero; the "Last Sync" time updates to "Just now".
    *   **Failure:** An error message explains the issue (e.g., "No Network", "AWS Error").
