# Behavioral Specification: Historical Visualization

**Bounded Context:** This specification governs the consumption side of the system: retrieving, caching, and rendering the data that has been successfully collected and synced.

**Prerequisite:** Depends on **[Cloud Synchronization](03_cloud_synchronization.md)** (data availability).
**Downstream:** None (End of chain).

---

## Data Retrieval & Caching
*   **R6.100** **When** the user opens the History/Map view, the system **shall** query the local File-Based Cache for track data.
*   **R6.200** **When** viewing a specific month, the system **shall** check the "Staleness" of the local index for that month.
*   **R6.300** **If** the target month is the "Current Month", **then** the system **shall** always query the remote storage to update the index.
*   **R6.400** **If** the target month is a "Past Month" AND the local index is missing or stale (cached before month end), **then** the system **shall** query the remote storage to update the index.
*   **R6.500** **If** the target month is a "Past Month" AND the local index is fresh, **then** the system **shall** use the local index without querying the remote storage.
*   **R6.600** **When** fetching track data, the system **shall** download the data files from the remote storage and store them in the local File-Based Cache.

## Visualization Logic
*   **R6.700** **When** rendering tracks, the system **shall** strictly display only data that has been successfully synced to the remote storage (or cached from it), excluding data currently in the local upload buffer.
*   **R6.800** **When** multiple devices have uploaded data for the same date, the system **shall not** merge these distinct track segments into a unified view.
*   **R6.900** **When** viewing history, the system **shall** provide a "Source Device" filter allowing the user to view tracks from one device at a time, defaulting to the current device.
*   **R6.1000** **When** rendering a track, the system **shall** apply a downsampling algorithm to reduce point count while preserving geometry.
*   **R6.1100** **If** the time difference between two sequential points exceeds 5 minutes, **then** the system **shall** render a "Clean Break" (gap) in the line to indicate discontinuity.

## Map Interaction & Overlays
*   **R6.1200** **When** the map is displayed, the system **shall** render Bitmap Tiles using the user's preferred theme via a Color Filter.
*   **R6.1300** **When** the user toggles the "Signal Quality" layer, the system **shall** overlay a heatmap visualizing network strength, distinguishing between WiFi and Cellular sources.
*   **R6.1400** **When** the user selects a specific point on the track, the system **shall** display a Detail Sheet with raw data (Speed, Altitude, Coordinates).
*   **R6.1500** **If** the device is offline and cached tiles are unavailable, **then** the system **shall** display a "Map Offline" warning but allow interaction with the cached track geometry.
