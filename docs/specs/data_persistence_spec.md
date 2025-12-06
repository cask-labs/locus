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
This entity stores the high-frequency track data.

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
This entity stores diagnostic logs and telemetry.

| Field | Column Name | Type (Kotlin) | Type (SQLite) | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `id` | `Long` | `INTEGER` | **PK**, AutoGen | Internal sequence ID. |
| `t` | `time` | `Instant` | `INTEGER` | **Index**, NotNull | Unix Epoch Seconds (Log Timestamp). |
| `lvl` | `level` | `Int` | `INTEGER` | NotNull | Log Level (DEBUG=3, ERROR=6, etc). |
| `tag` | `tag` | `String` | `TEXT` | NotNull | Log Tag. |
| `msg` | `message` | `String` | `TEXT` | NotNull | Log Message. |
| `bat` | `battery_level` | `Int` | `INTEGER` | NotNull | Battery % at log time. |
| `net` | `network_state` | `String` | `TEXT` | NotNull | Network State (e.g., "WIFI", "NONE"). |
| `meta`| `metadata` | `String` | `TEXT` | Nullable | JSON Blob for extra context. |

**Indices:**
*   `index_logs_time` on `time` (ASC) - Used for range queries during Sync.

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

    @Query("SELECT * FROM logs ORDER BY time ASC LIMIT :limit")
    suspend fun getOldestBatch(limit: Int): List<LogEntity>

    @Query("DELETE FROM logs WHERE time <= :timestamp")
    suspend fun deleteBefore(timestamp: Long)

    @Query("SELECT COUNT(*) FROM logs")
    suspend fun getCount(): Long

    @Query("DELETE FROM logs WHERE id IN (SELECT id FROM logs ORDER BY time ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int)
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
To enforce the **500MB Soft Limit** without expensive file size checks, we use a **Row Count Proxy**.
*   **Assumption:** Average `LocationEntity` size on disk (WAL + Indices) ≈ 200 bytes.
*   **Limit:** 500,000,000 bytes / 200 bytes/row = **2,500,000 Rows**.
*   **Enforcement:** The `SyncWorker` checks `LocationDao.getCount()` before insertion or after upload. If `count > 2,500,000`, it triggers `deleteOldest(chunk_size)`.

### 5.2. Sync Deletion Logic
*   **S3 is Truth:** Data is only deleted from `LocationDao` when the S3 Upload Request returns `200 OK`.
*   **Community is Optional:** The `LogDao` deletion logic depends *only* on the S3 upload status. Failure to upload to the Community endpoint does *not* prevent local log deletion.

## 6. Migration Strategy
*   **Version 1:** Initial Schema.
*   **Future:** `fallbackToDestructiveMigration()` is authorized for initial development.
