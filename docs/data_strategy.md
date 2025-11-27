# Data Strategy

## A. Storage Format (S3)
*   **Format:** NDJSON (Newline Delimited JSON), Gzipped.
*   **Compression:** `.gz` (Expected ~90% size reduction).
*   **Path Structure:**
    `s3://<bucket_name>/tracks/YYYY/MM/DD/<device_id>_<start_timestamp>_v<version>.json.gz`

## B. Schema Versioning
*   **Current Version:** `v1`
*   **Strategy:** The filename includes the version (`_v1`). The JSON payload also includes a header object.

## C. JSON Schema (v1)
Each file is a Gzipped text file.
**Line 1 (Header):**
```json
{"type": "header", "version": 1, "device_id": "Pixel7_a8f3", "start_time": 1698300000}
```
**Lines 2..N (Data):**
```json
{"t": 1698300001, "lat": 37.7749, "lon": -122.4194, "acc": 4.5, "alt": 120, "spd": 1.2}
```
