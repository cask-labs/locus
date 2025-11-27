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
{"type": "header", "version": 1, "device_id": "Pixel7_a8f3", "start_time": 1698300000}
```

**Lines 2..N (Data):**
The schema uses short keys to minimize file size.
```json
{
  "t": 1698300001,      // Timestamp (Epoch)
  "lat": 37.7749,       // Latitude
  "lon": -122.4194,     // Longitude
  "acc": 4.5,           // Accuracy (m)
  "alt": 120,           // Altitude (m)
  "spd": 1.2,           // Speed (m/s)
  "bear": 180.0,        // Bearing (0-360)
  "sat": 12,            // Satellite Count
  "bat": 85,            // Battery Level (%)
  "chg": false,         // Charging Status
  "net_typ": "cell",    // Network Type (wifi, cell, none)
  "net_lvl": 3,         // Signal Level (0-4)
  "net_dbm": -85        // Signal Strength (dBm)
}
```

### Conditional Sensor Data
To conserve battery, high-frequency environmental sensors are only recorded when speed exceeds **4.5 m/s** (approx 16 km/h).
When `spd > 4.5`, the following fields are added:

```json
{
  ...
  "acc_x": 0.01, "acc_y": 0.05, "acc_z": 9.81,  // Accelerometer (m/s^2)
  "mag_x": 12.4, "mag_y": -5.1, "mag_z": 0.2,   // Magnetometer (uT)
  "pres": 1013.2                                // Pressure (hPa)
}
```
