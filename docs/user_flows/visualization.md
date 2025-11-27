# Visualization (History View)

**Goal:** Verify and explore historical movements.

*   **Step 1: Access:**
    *   The user opens the main application UI.
*   **Step 2: Date Selection:**
    *   The user selects a specific date from a calendar interface.
*   **Step 3: Data Retrieval:**
    *   The app queries the S3 bucket for all track segments matching that date.
    *   The app downloads and decompresses the relevant Gzip files.
*   **Step 4: Rendering:**
    *   The map displays the day's route.
    *   The interface shows summary statistics (Total Distance, Duration).
    *   Gaps in data (e.g., dead battery) are visually distinct from active tracking.
