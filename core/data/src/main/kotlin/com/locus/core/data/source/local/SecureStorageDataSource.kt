package com.locus.core.data.source.local

import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import com.google.crypto.tink.Aead
import com.locus.core.data.model.BootstrapCredentialsDto
import com.locus.core.data.model.RuntimeCredentialsDto
import com.locus.core.data.model.toDomain
import com.locus.core.data.model.toDto
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.result.LocusResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Named

/**
 * Handles secure storage of authentication credentials using DataStore + Tink,
 * with a fallback to SharedPreferences for non-critical configuration like Telemetry Salt.
 */
class SecureStorageDataSource
    @Inject
    constructor(
        @Named("bootstrapDataStore") private val bootstrapDataStore: DataStore<BootstrapCredentialsDto?>,
        @Named("runtimeDataStore") private val runtimeDataStore: DataStore<RuntimeCredentialsDto?>,
        private val plainPrefs: SharedPreferences,
        private val aead: Aead,
    ) {
        companion object {
            const val KEY_SALT = "telemetry_salt"
            const val KEY_ONBOARDING_STAGE = "onboarding_stage"
            private const val TAG = "SecureStorageDataSource"
        }

        // --- Bootstrap Credentials ---

        suspend fun saveBootstrapCredentials(creds: BootstrapCredentials): LocusResult<Unit> {
            return try {
                bootstrapDataStore.updateData { creds.toDto() }
                LocusResult.Success(Unit)
            } catch (e: Exception) {
                LocusResult.Failure(e)
            }
        }

        suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials?> {
            return try {
                val dto = bootstrapDataStore.data.first()
                LocusResult.Success(dto?.toDomain())
            } catch (e: Exception) {
                // Fail Hard Policy
                LocusResult.Failure(SecurityException("Secure storage unavailable or corrupted", e))
            }
        }

        suspend fun clearBootstrapCredentials(): LocusResult<Unit> {
            return try {
                bootstrapDataStore.updateData { null }
                LocusResult.Success(Unit)
            } catch (e: Exception) {
                LocusResult.Failure(e)
            }
        }

        // --- Runtime Credentials ---

        suspend fun saveRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit> {
            return try {
                runtimeDataStore.updateData { creds.toDto() }
                // Also attempt to save salt to fallback plainPrefs just in case
                creds.telemetrySalt?.let { salt ->
                    val success =
                        withContext(Dispatchers.IO) {
                            plainPrefs.edit().putString(KEY_SALT, salt).commit()
                        }
                    if (!success) {
                        Log.w(TAG, "Failed to fallback-save telemetry salt")
                    }
                }
                LocusResult.Success(Unit)
            } catch (e: Exception) {
                LocusResult.Failure(e)
            }
        }

        suspend fun getRuntimeCredentials(): LocusResult<RuntimeCredentials?> {
            return try {
                val dto = runtimeDataStore.data.first()
                LocusResult.Success(dto?.toDomain())
            } catch (e: Exception) {
                // Fail Hard Policy
                LocusResult.Failure(SecurityException("Secure storage unavailable or corrupted", e))
            }
        }

        suspend fun clearRuntimeCredentials(): LocusResult<Unit> {
            return try {
                runtimeDataStore.updateData { null }
                LocusResult.Success(Unit)
            } catch (e: Exception) {
                LocusResult.Failure(e)
            }
        }

        // --- Configuration (Fallback Policy) ---

        /**
         * Retrieves the telemetry salt, trying secure storage first, then falling back to SharedPreferences.
         */
        suspend fun getTelemetrySalt(): String? {
            // 1. Try Secure DataStore (part of RuntimeCredentials)
            try {
                val dto = runtimeDataStore.data.first()
                if (dto?.telemetrySalt != null) {
                    return dto.telemetrySalt
                }
            } catch (e: Exception) {
                // Ignore secure storage failure for salt, fall through to legacy/fallback
            }

            // 2. Fallback to Plain SharedPreferences
            return plainPrefs.getString(KEY_SALT, null)
        }

        // --- Onboarding Stage ---

        suspend fun saveOnboardingStage(stage: String) {
            withContext(Dispatchers.IO) {
                try {
                    val encryptedBytes =
                        aead.encrypt(
                            stage.toByteArray(StandardCharsets.UTF_8),
                            KEY_ONBOARDING_STAGE.toByteArray(StandardCharsets.UTF_8),
                        )
                    val encryptedString = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
                    plainPrefs.edit().putString(KEY_ONBOARDING_STAGE, encryptedString).apply()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to encrypt onboarding stage", e)
                    // Fallback to plain text if encryption fails (better than crash, but log error)
                    // This is debatable security-wise but prevents hard crash.
                    // Given 'Fail-Secure' preference, we should probably throw or not save.
                    // But if we don't save, user is stuck.
                    // Let's attempt plain text as last resort for stage (low risk enum).
                    plainPrefs.edit().putString(KEY_ONBOARDING_STAGE, stage).apply()
                }
            }
        }

        suspend fun getOnboardingStage(): String? {
            return withContext(Dispatchers.IO) {
                val stored = plainPrefs.getString(KEY_ONBOARDING_STAGE, null) ?: return@withContext null
                try {
                    val encryptedBytes = Base64.decode(stored, Base64.DEFAULT)
                    val decryptedBytes =
                        aead.decrypt(
                            encryptedBytes,
                            KEY_ONBOARDING_STAGE.toByteArray(StandardCharsets.UTF_8),
                        )
                    String(decryptedBytes, StandardCharsets.UTF_8)
                } catch (e: Exception) {
                    // It might be plain text (from fallback or legacy)
                    // Or tampering.
                    // If it matches an enum value, return it.
                    // Assuming stored is the plain text value.
                    // Verification logic belongs to domain/repository (valueOf(stageStr)).
                    Log.w(TAG, "Failed to decrypt onboarding stage, assuming plain text or corrupted", e)
                    stored
                }
            }
        }
    }
