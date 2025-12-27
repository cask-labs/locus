# 03-task-3-secure-storage-plan.md

## Purpose
Implement secure storage for authentication credentials using `EncryptedSharedPreferences`, with a fallback mechanism for non-critical data.

## Prerequisites
- **Human Action:** None.
- **Dependency:** Add `androidx.security:security-crypto` to `libs.versions.toml`.

## Classification
- **New Capability:** `SecureStorageDataSource` implementation.
- **New Capability:** `BootstrapCredentialsDto`, `RuntimeCredentialsDto`.
- **Enhancement:** Update `libs.versions.toml`.
- **Documentation:** Update `data_persistence_spec.md`.

## Spec Alignment (Phase 3)
| Requirement | Implementation Component | Behavior |
| :--- | :--- | :--- |
| **Secure Credential Storage** | `SecureStorageDataSource` | Wraps `EncryptedSharedPreferences` to store sensitive keys at rest. |
| **Fail Hard Policy** | `SecureStorageDataSource` | Auth methods (save/get credentials) catch initialization/IO errors and return `LocusResult.Failure`, triggering a fatal error in UI. |
| **Fallback Policy** | `SecureStorageDataSource` | Config methods (salt) attempt encrypted storage but catch errors and fall back to `Context.MODE_PRIVATE`. |
| **Data Integrity** | `CredentialsDto` (JSON) | Credentials are stored as atomic JSON blobs to ensure all keys (Access+Secret) are updated together. |
| **Domain Purity** | `core/data` DTOs | Domain models remain pure; serialization logic is isolated in Data layer DTOs. |

## Implementation Plan

### 1. Update Documentation
- **File:** `docs/technical_discovery/specs/data_persistence_spec.md`
- **Action:** Update the "Authentication" section to reflect JSON storage strategy (instead of individual keys) and add "Telemetry Salt" with fallback strategy.

### 2. Add Dependencies
- **Action:** Update `gradle/libs.versions.toml` to include `androidx.security:security-crypto:1.1.0-alpha06`.
- **Verification:** Run `./gradlew build` to ensure dependency resolution works.

### 3. Create Data Transfer Objects (DTOs)
- **File:** `core/data/src/main/kotlin/com/locus/core/data/model/BootstrapCredentialsDto.kt`
- **File:** `core/data/src/main/kotlin/com/locus/core/data/model/RuntimeCredentialsDto.kt`
- **Details:**
  - Create data classes annotated with `@Serializable`.
  - Add mapping extension functions (`toDomain`, `toDto`).

### 4. Implement SecureStorageDataSource
- **File:** `core/data/src/main/kotlin/com/locus/core/data/source/local/SecureStorageDataSource.kt`
- **Logic:**
  - **Init:** Lazy initialization of `EncryptedSharedPreferences`.
  - **Auth (Fail Hard):**
    ```kotlin
    fun getBootstrapCredentials(): LocusResult<BootstrapCredentials> {
        return try {
            val json = encryptedPrefs.getString(KEY_BOOTSTRAP, null)
            // parse and map
        } catch (e: Exception) {
            LocusResult.Failure(SecurityException("Secure storage unavailable", e))
        }
    }
    ```
  - **Config (Fallback):**
    ```kotlin
    fun getTelemetrySalt(): String? {
        return try {
            encryptedPrefs.getString(KEY_SALT, null)
        } catch (e: Exception) {
            plainPrefs.getString(KEY_SALT, null)
        }
    }
    ```

### 5. Setup Dependency Injection
- **File:** `core/data/src/main/kotlin/com/locus/core/data/di/DataModule.kt`
- **Action:** Add `@Provides` or `@Binds` for `SecureStorageDataSource`.
- **Scope:** `@Singleton` to ensure only one instance of SharedPreferences is held.

### 6. Verification Strategy
- **Type:** Android Instrumented Tests (`androidTest`)
- **Location:** `core/data/src/androidTest/java/com/locus/core/data/source/local/SecureStorageDataSourceTest.kt`
- **Test Cases:**
  1.  **Persistence:** Save `BootstrapCredentials`, recreate DataSource, assert `getBootstrapCredentials` returns correct data.
  2.  **Atomicity:** Verify JSON structure allows saving/retrieving full object.
  3.  **Clearing:** Save, then `clearBootstrapCredentials`, assert `get` returns null/empty.
  4.  **Fallback (Simulated):** (Optional/Advanced) Mock internal Prefs to throw, verify fallback logic for Salt. *Note: Mocking SharedPreferences inside ESP is hard; integration test on device might just test the "Happy Path" for Salt.*

## Completion Criteria
- [ ] `data_persistence_spec.md` is updated.
- [ ] `SecureStorageDataSource` is implemented and compiles.
- [ ] DTOs are created and isolated in Data layer.
- [ ] Dependency `androidx.security` is added.
- [ ] `connectedAndroidTest` passes on emulator/device.
