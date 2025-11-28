# Data Strategy

## Storage Format (S3)
*   **Format:** NDJSON (Newline Delimited JSON), Gzipped.
*   **Compression:** `.gz` (Expected ~90% size reduction).
*   **Path Structure:**
    `s3://<bucket_name>/tracks/YYYY/MM/DD/<device_id>_<start_timestamp>_v<version>.json.gz`

## Schema Versioning
*   **Current Version:** `v1`
*   **Strategy:** The filename includes the version (`_v1`). The JSON payload also includes a header object.

## JSON Schema (v1)
Each file is a Gzipped text file.

**Line 1 (Header):**
```json
{
  "type": "header",
  "version": 1,
  "device_id": "Pixel7_a8f3",
  "start_time": 1698300000
}
```

**Lines 2..N (Data):**
```json
{
  "t": 1698300001,
  "lat": 37.7749,
  "lon": -122.4194,
  "acc": 4.5,
  "alt": 120,
  "spd": 1.2
}
```

## Synchronization Strategy: Lazy Loading

To respect user data plans and battery life, Locus does not "sync" the entire history.

### 1. Inventory First
When connecting to an existing store, the app performs a lightweight scan of the S3 directory structure (`tracks/`).
*   **Goal:** Build a local index of *which days* have data.
*   **Mechanism:** Uses `ListObjects` with delimiters to identify Year/Month/Day prefixes.
*   **Result:** The History Calendar is populated with indicators. No track data is downloaded.

### 2. On-Demand Fetch
Full track data is only downloaded when the user explicitly interacts with a specific date.

## Identity & Write Patterns

### The "New Identity" Rule
Every fresh installation of Locus generates a **Unique Device ID** (e.g., `Pixel7_<RandomSuffix>`).
*   **Why:** To prevent "Split Brain" or overwrites. If a user factory resets their phone and reinstalls, the new installation acts as a distinct "writer" to the same "book" (Bucket).
*   **Merging:** The Visualization engine is responsible for merging tracks from multiple Device IDs that occur on the same day. The user sees a unified history.
