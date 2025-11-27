# Android Client Architecture

## A. Service Layer (The Engine)
*   **Component:** `ForegroundService`.
*   **Behavior:**
    *   Acquires `PARTIAL_WAKE_LOCK`.
    *   Requests Location Updates (GPS Provider, minTime=1000ms).
    *   **Battery Safety:** Automatically stops tracking if battery drops below **10%**.

## B. Visualization
*   **Provider:** **OpenStreetMap (via `osmdroid`)**.
*   **Benefits:** No API keys required, open source, offline caching capability.

## C. Persistence Layer (The Buffer)
*   **Local DB:** Room (SQLite).
*   **Purge Strategy:** Rows are deleted only after successful S3 upload confirmation.

## D. Sync Worker
*   **Trigger:** Periodic (e.g., every 15 mins).
*   **Action:**
    1.  Query oldest points.
    2.  Gzip compress.
    3.  Upload to S3.
    4.  On Success -> Delete points from DB.
