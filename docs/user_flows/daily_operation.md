# Daily Operation (Passive)

**Goal:** Continuous, reliable data collection with minimal user intervention.

*   **Status Indication:**
    *   A persistent notification indicates the service is running ("Locus is tracking").
    *   The notification updates to show the latest sync status or error state.
*   **Battery Management:**
    *   **Low Battery (<10%):** The app pauses uploads and reduces tracking frequency. The user receives a notification about the conservation mode.
    *   **Critical Battery (<3%):** Tracking stops completely to preserve the phone's remaining life.
    *   **Recovery:** When charged >15%, the app automatically resumes full-fidelity tracking and syncing.
*   **User Action:**
    *   The user sees the persistent notification as assurance of operation. No active interaction is required unless an error occurs.
