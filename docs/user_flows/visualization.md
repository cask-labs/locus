# Visualization (History View)

**Goal:** Verify, explore, and analyze historical movements using S3 as the source of truth.

*   **Step 1: Access & Discovery:**
    *   The user opens the history tab.
    *   A calendar view appears. Days containing historical data are visually highlighted, based on a locally cached index of the S3 bucket.
*   **Step 2: Date Selection:**
    *   The user selects a highlighted date.
    *   The view displays the Day's Summary Statistics: Total Distance (km), Total Duration (hrs), and Average Speed (km/h).
*   **Step 3: Smart Retrieval (Cache-First):**
    *   **Local Check:** The app checks internal private storage for a cached track file for that date.
    *   **Remote Fetch:** If not cached, the app queries S3 for that date prefix and downloads the relevant `.gz` segments.
    *   **Processing:** The app stitches segments together, merging tracks from multiple devices if necessary, standardizes the time series, and updates the local cache.
    *   **Write-Through Indexing:** Successful downloads (or uploads during daily operation) immediately update the local "History Index", ensuring the calendar view remains current without requiring expensive `s3:ListObjects` calls.
*   **Step 4: Rendering:**
    *   **Source Verification:** The map renders *only* data confirmed to be in S3. Data currently in the local upload buffer is explicitly excluded to strictly verify remote storage.
    *   **Route:** The path is drawn on the offline-capable map.
    *   **Visual Discontinuity:** If a time gap greater than 5 minutes exists between two points, no line is drawn connecting them.
    *   **Rapid Acceleration:** Events categorized as rapid acceleration or hard braking are marked with distinct icons on the route.
*   **Step 5: Signal Quality Heat Map:**
    *   The user can toggle a "Signal Quality" overlay.
    *   The track is colored to represent signal strength.
    *   The view visually differentiates between **WiFi** sources and **Cellular** sources (e.g., via distinct color palettes or stroke styles).
*   **Step 6: Detailed Inspection:**
    *   The user taps any point on the route.
    *   A detail panel displays the precise Timestamp, Speed, Battery Level, and Network Signal Strength (dBm).
