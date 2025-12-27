# 03-task-3-secure-storage-plan.md

## Purpose
Implement secure storage for authentication credentials and configuration using **Jetpack DataStore** and **Google Tink**, replacing the legacy `EncryptedSharedPreferences`.

## Prerequisites
- **Human Action:** None.
- **Dependency:** Add `androidx.datastore:datastore`, `org.jetbrains.kotlinx:kotlinx-serialization-json`, and `com.google.crypto.tink:tink-android` to `libs.versions.toml`.

## Classification
- **New Capability:** `SecureStorageDataSource` implementation using DataStore.
- **New Capability:** `EncryptedDataStoreSerializer` implementation using Tink.
- **New Capability:** `BootstrapCredentialsDto`, `RuntimeCredentialsDto`.
- **Enhancement:** Update `libs.versions.toml`.
- **Documentation:** Update `data_persistence_spec.md` to reflect DataStore + Tink architecture.

## Spec Alignment (Phase 3)
| Requirement | Implementation Component | Behavior |
| :--- | :--- | :--- |
| **Secure Credential Storage** | `SecureStorageDataSource` | Uses `DataStore` with `Aead` (Tink) encryption to store sensitive keys at rest. |
| **Fail Hard Policy** | `SecureStorageDataSource` | Auth methods catch DataStore/Tink errors (e.g., corruption) and return `LocusResult.Failure`, triggering a fatal error in UI. |
| **Fallback Policy** | `SecureStorageDataSource` | Config methods (salt) attempt encrypted storage but catch errors and fall back to standard `SharedPreferences`. |
| **Data Integrity** | `CredentialsDto` (JSON) | Credentials are stored as atomic JSON blobs to ensure all keys (Access+Secret) are updated together. |
| **Modern Architecture** | `DataStore` | Fully asynchronous API (`Flow`/`suspend`) prevents main-thread I/O blocking, adhering to modern Android standards. |
| **Domain Purity** | `core/data` DTOs | Domain models remain pure; serialization logic is isolated in Data layer DTOs. |

## Implementation Plan

### 1. Update Documentation
- **File:** `docs/technical_discovery/specs/data_persistence_spec.md`
- **Action:**
  - Replace Section 2 "Key-Value Storage (Preferences)" entirely.
  - Define "Secure DataStore" utilizing Google Tink (`Aead`) for encryption.
  - Remove references to `EncryptedSharedPreferences`.
  - Specify the usage of Proto DataStore (with Kotlin Serialization) for type safety.

### 2. Add Dependencies
- **Action:** Update `gradle/libs.versions.toml` to include:
  - `androidx.datastore:datastore:1.0.0` (or latest stable).
  - `com.google.crypto.tink:tink-android:1.8.0` (or latest stable).
- **Verification:** Run `./gradlew build` to ensure dependency resolution works.

### 3. Create Data Transfer Objects (DTOs)
- **Directory:** `core/data/src/main/kotlin/com/locus/core/data/model/`
- **File:** `BootstrapCredentialsDto.kt`
- **File:** `RuntimeCredentialsDto.kt`
- **Details:**
  - Create data classes annotated with `@Serializable`.
  - Add mapping extension functions (`toDomain`, `toDto`).

### 4. Implement Encrypted Serializer
- **File:** `core/data/src/main/kotlin/com/locus/core/data/source/local/EncryptedDataStoreSerializer.kt`
- **Logic:**
  - Implement `Serializer<T>`.
  - Inject Tink `Aead` primitive.
  - **Read:** Read bytes -> Decrypt (Tink) -> Deserialize (JSON).
  - **Write:** Serialize (JSON) -> Encrypt (Tink) -> Write bytes.
  - Handle `GeneralSecurityException` by throwing `CorruptionException`.

### 5. Implement SecureStorageDataSource
- **File:** `core/data/src/main/kotlin/com/locus/core/data/source/local/SecureStorageDataSource.kt`
- **Logic:**
  - **Init:** Inject `DataStore<BootstrapCredentialsDto>`, `DataStore<RuntimeCredentialsDto>`, and `SharedPreferences` (fallback).
  - **Auth (Fail Hard):**
    ```kotlin
    suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials> {
        return try {
            val dto = bootstrapDataStore.data.first()
            LocusResult.Success(dto.toDomain())
        } catch (e: Exception) {
            LocusResult.Failure(SecurityException("Secure storage unavailable", e))
        }
    }
    ```
  - **Config (Fallback):**
    ```kotlin
    suspend fun getTelemetrySalt(): String? {
        return try {
            // Try Encrypted DataStore first
            encryptedDataStore.data.first().salt
        } catch (e: Exception) {
            // Fallback to plain SharedPreferences
            plainPrefs.getString(KEY_SALT, null)
        }
    }
    ```

### 6. Setup Dependency Injection
- **File:** `core/data/src/main/kotlin/com/locus/core/data/di/DataModule.kt`
- **Action:**
  - Provide `Aead` instance using `AndroidKeysetManager`.
  - Provide `DataStore` instances (Singleton, scoped to `@ApplicationContext`).
  - Bind `SecureStorageDataSource`.

### 7. Verification Strategy
- **Type:** Android Instrumented Tests (`androidTest`)
- **Location:** `core/data/src/androidTest/java/com/locus/core/data/source/local/SecureStorageDataSourceTest.kt`
- **Test Cases:**
  1.  **Persistence:** Save `BootstrapCredentials`, recreate DataSource, assert `get` returns correct data.
  2.  **Atomicity:** Verify JSON structure allows saving/retrieving full object.
  3.  **Clearing:** Save, then clear, assert `get` returns empty/null.
  4.  **Fallback:** Verify logic catches DataStore errors and reads from SharedPreferences for Salt.
  5.  **Integration:** Verify Tink KeySet generation works on emulator/device.

## Completion Criteria
- [ ] `data_persistence_spec.md` is updated.
- [ ] `SecureStorageDataSource` is implemented using DataStore + Tink.
- [ ] Dependencies (`datastore`, `tink`) are added.
- [ ] `connectedAndroidTest` passes on emulator/device.
