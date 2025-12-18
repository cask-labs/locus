# Data Persistence Specification

**Status:** Draft
**Related Requirements:** [Data Storage](../requirements/data_storage.md), [Data Strategy](../data_strategy.md)

This document defines the technical implementation for the Local Data Persistence layer, utilizing the Android Room Persistence Library (SQLite).

## 1. Database Configuration

*   **Database Name:** `locus_db`
*   **Type:** Room Database (SQLite)
*   **Version:** `1`
*   **Encryption:** None (Relies on Android OS-level filesystem encryption).
*   **Concurrency:** Write-Ahead Logging (WAL) enabled for non-blocking reads/writes.

## 2. Entity Definitions

### 2.1. LocationEntity (`locations`)
This entity stores the high-frequency track data buffer waiting for upload.

| Field | Column Name | Type (Kotlin) | Type (SQLite) | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `id` | `Long` | `INTEGER` | **PK**, AutoGen | Internal sequence ID. |
| `t` | `time` | `Instant` | `INTEGER` | **Index**, NotNull | Unix Epoch Seconds (Data Timestamp). |
| `lat` | `lat` | `Double` | `REAL` | NotNull | Latitude (WGS84). |
| `lon` | `lon` | `Double` | `REAL` | NotNull | Longitude (WGS84). |
| `acc` | `acc` | `Float` | `REAL` | NotNull | Horizontal Accuracy (m). |
| `alt` | `alt` | `Double` | `REAL` | NotNull | Altitude (m). |
| `spd` | `spd` | `Float` | `REAL` | NotNull | Speed (m/s). |
| `bat` | `bat` | `Int` | `INTEGER` | NotNull | Battery Level (0-100). |
| `cs` | `cell_signal` | `Int?` | `INTEGER` | Nullable | Cell Signal Level (0-4). |
| `cd` | `cell_dbm` | `Int?` | `INTEGER` | Nullable | Cell Signal dBm. |
| `ws` | `wifi_signal` | `Int?` | `INTEGER` | Nullable | WiFi Signal Level (0-4). |
| `wd` | `wifi_dbm` | `Int?` | `INTEGER` | Nullable | WiFi Signal dBm. |
| `ax` | `acc_x` | `Float?` | `REAL` | Nullable | Accelerometer X. |
| `ay` | `acc_y` | `Float?` | `REAL` | Nullable | Accelerometer Y. |
| `az` | `acc_z` | `Float?` | `REAL` | Nullable | Accelerometer Z. |
| `mx` | `mag_x` | `Float?` | `REAL` | Nullable | Magnetometer X. |
| `my` | `mag_y` | `Float?` | `REAL` | Nullable | Magnetometer Y. |
| `mz` | `mag_z` | `Float?` | `REAL` | Nullable | Magnetometer Z. |
| `bar` | `pressure` | `Float?` | `REAL` | Nullable | Barometer (hPa). |

**Indices:**
*   `index_locations_time` on `time` (ASC) - Used for range queries during Sync.

### 2.2. LogEntity (`logs`)
This entity stores diagnostic logs and telemetry in a Circular Buffer.
**Note:** This schema uses a flat structure to optimize SQLite indexing and filtering (e.g., filtering by `level` or `tag`). The Network Layer is responsible for mapping this flat structure into the nested JSON Wire Format defined in `telemetry_spec.md`.

| Field | Column Name | Type (Kotlin) | Type (SQLite) | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `id` | `Long` | `INTEGER` | **PK**, AutoGen | Internal sequence ID. |
| `t` | `time` | `Instant` | `INTEGER` | **Index**, NotNull | Unix Epoch Seconds (Log Timestamp). |
| `lvl` | `level` | `Int` | `INTEGER` | NotNull | Log Level (DEBUG=3, ERROR=6, etc). |
| `tag` | `tag` | `String` | `TEXT` | NotNull | Log Tag. |
| `msg` | `message` | `String` | `TEXT` | NotNull | Log Message. |
| `bat` | `battery_level` | `Int` | `INTEGER` | NotNull | Battery % at log time. |
| `chg` | `is_charging` | `Boolean` | `INTEGER` | NotNull | Charging State (0/1). |
| `net` | `network_state` | `String` | `TEXT` | NotNull | Network State (e.g., "WIFI", "NONE"). |
| `perm`| `permissions` | `Boolean` | `INTEGER` | NotNull | Location Permission Granted (0/1). |
| `svc` | `service_running`| `Boolean` | `INTEGER` | NotNull | Tracker Service Active (0/1). |
| `meta`| `metadata` | `String` | `TEXT` | Nullable | JSON Blob for extra context. |

**Indices:**
*   `index_logs_time` on `time` (ASC) - Used for range queries during Sync.

### 2.3. CursorEntity (`cursors`)
This entity stores the independent upload progress for logs.

| Field | Column Name | Type (Kotlin) | Type (SQLite) | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `id` | `String` | `TEXT` | **PK** | Cursor Key (e.g., "log_s3", "log_community"). |
| `last` | `last_processed_id` | `Long` | `INTEGER` | NotNull | The ID of the last successfully uploaded item. |

### 2.4. TrackMetadataEntity (`track_metadata`)
This entity acts as an index for the File-Based Historical Track Cache.

| Field | Column Name | Type (Kotlin) | Type (SQLite) | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `key` | `file_key` | `String` | `TEXT` | **PK** | S3 Key / Local File Path (e.g., "tracks/2023/10/27..."). |
| `date` | `date_str` | `String` | `TEXT` | **Index**, NotNull | Date string "YYYY-MM-DD" for calendar queries. |
| `dev` | `device_id` | `String` | `TEXT` | **Index**, NotNull | Device ID extracted from filename. |
| `sz` | `size_bytes` | `Long` | `INTEGER` | NotNull | File size in bytes (for eviction). |
| `ts` | `last_accessed` | `Instant` | `INTEGER` | NotNull | Timestamp of last read (for LRU eviction). |

**Indices:**
*   `index_track_date` on `date_str` (ASC) - Used for populating Calendar dots.
*   `index_track_device` on `device_id` (ASC) - Used for filtering by device.

## 3. Data Access Objects (DAOs)

### 3.1. LocationDao

```kotlin
@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(location: LocationEntity): Long

    // Stream for Upload: Get batch of oldest points
    // Limit is typically calculated based on target file size or chunk count
    @Query("SELECT * FROM locations ORDER BY time ASC LIMIT :limit")
    suspend fun getOldestBatch(limit: Int): List<LocationEntity>

    // Cleanup: Delete points confirmed uploaded (by range)
    @Query("DELETE FROM locations WHERE time <= :timestamp")
    suspend fun deleteBefore(timestamp: Long)

    // Monitoring: Check buffer usage
    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getCount(): Long

    // FIFO Eviction: Delete oldest points if buffer full
    @Query("DELETE FROM locations WHERE id IN (SELECT id FROM locations ORDER BY time ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int)
}
```

### 3.2. LogDao

```kotlin
@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: LogEntity)

    // UI Query: Rich filtering
    // returns Flow to observe changes
    @Query("SELECT * FROM logs WHERE level IN (:levels) AND (tag LIKE '%' || :search || '%' OR message LIKE '%' || :search || '%') ORDER BY time DESC LIMIT :limit")
    fun getLogs(levels: List<Int>, search: String, limit: Int = 1000): Flow<List<LogEntity>>

    // Stream for Upload: Get batch of items strictly AFTER the cursor
    @Query("SELECT * FROM logs WHERE id > :cursorId ORDER BY id ASC LIMIT :limit")
    suspend fun getBatchAfter(cursorId: Long, limit: Int): List<LogEntity>

    // FIFO Eviction: Delete oldest points if buffer full
    // This is the ONLY mechanism for log deletion. Logs are never deleted based on upload status.
    @Query("DELETE FROM logs WHERE id IN (SELECT id FROM logs ORDER BY time ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int)

    @Query("SELECT COUNT(*) FROM logs")
    suspend fun getCount(): Long
}
```

### 3.3. CursorDao

```kotlin
@Dao
interface CursorDao {
    @Query("SELECT * FROM cursors WHERE id = :key")
    suspend fun getCursor(key: String): CursorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCursor(cursor: CursorEntity)
}
```

### 3.4. TrackMetadataDao

```kotlin
@Dao
interface TrackMetadataDao {
    // Used for Calendar Dots (Any device)
    @Query("SELECT * FROM track_metadata WHERE date_str BETWEEN :startDate AND :endDate")
    fun getTracksForRange(startDate: String, endDate: String): Flow<List<TrackMetadataEntity>>

    // Used for "Source Device" filter list
    @Query("SELECT DISTINCT device_id FROM track_metadata WHERE date_str = :date")
    suspend fun getDevicesForDate(date: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: TrackMetadataEntity)

    @Query("UPDATE track_metadata SET last_accessed = :now WHERE file_key = :key")
    suspend fun updateAccessTime(key: String, now: Long)

    @Query("SELECT SUM(size_bytes) FROM track_metadata")
    suspend fun getTotalCacheSize(): Long

    // LRU Eviction Candidates
    @Query("SELECT * FROM track_metadata ORDER BY last_accessed ASC LIMIT :limit")
    suspend fun getOldestTracks(limit: Int): List<TrackMetadataEntity>

    @Delete
    suspend fun delete(track: TrackMetadataEntity)
}
```

## 4. Type Converters

To map complex Kotlin types to SQLite primitives:

*   **InstantConverter:**
    *   `Instant` -> `Long` (Epoch Seconds)
    *   `Long` -> `Instant`
*   **UUIDConverter:** (If needed for IDs in future, currently using Long)
    *   `UUID` -> `String`
    *   `String` -> `UUID`

## 5. Buffer Management Logic

### 5.1. The "Soft Limit" Proxy
To enforce the **500MB Soft Limit** for the Location Buffer without expensive file size checks, we use a **Row Count Proxy**.
*   **Assumption:** Average `LocationEntity` size on disk (WAL + Indices) ≈ 200 bytes.
*   **Limit:** 500,000,000 bytes / 200 bytes/row = **2,500,000 Rows**.
*   **Enforcement:** The `SyncWorker` checks `LocationDao.getCount()` before insertion. If `count > 2,500,000`, it triggers `deleteOldest(chunk_size)`.

### 5.2. Log Buffer Logic (Circular)
*   **Circular Buffer:** `LogEntity` acts as a circular buffer.
*   **Retention:** Data is **NEVER** deleted based on upload status. It is only deleted when the buffer exceeds its capacity (e.g., 5MB limit).
*   **Upload Progress:** Tracked via `CursorEntity`.
*   **Fail-Open:** If `cursor_community` fails to advance, `cursor_s3` can still proceed.

## 6. Historical Track Cache (File-Based)

To support visualization of historical data without constant S3 downloads, the system employs a **Local File Cache**.

### 6.1. Strategy
*   **Files:** Gzipped NDJSON files are stored directly on the internal filesystem, mirroring the S3 Key structure (e.g., `files/tracks/2023/10/27/device_123.json.gz`).
*   **Immutability:** Historical files are treated as read-only.
*   **Index:** `TrackMetadataEntity` tracks the existence and metadata of these files to allow the UI (Calendar) to query availability without filesystem I/O.

### 6.2. LRU Eviction Policy
*   **Limit:** **500MB** Hard Limit for the cache directory.
*   **Trigger:** Checked after every new file download.
*   **Logic:**
    1.  Get `TotalCacheSize` from DAO.
    2.  If `TotalCacheSize > 500MB`:
    3.  Query `getOldestTracks(10)`.
    4.  Delete the physical files.
    5.  Delete the rows from `TrackMetadataDao`.
    6.  Repeat until under limit.

## 7. Migration Strategy
*   **Version 1:** Initial Schema.
*   **Future:** `fallbackToDestructiveMigration()` is authorized for initial development.
