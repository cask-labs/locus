package com.locus.core.data.source.local

import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.DataStore
import com.locus.core.data.model.BootstrapCredentialsDto
import com.locus.core.data.model.RuntimeCredentialsDto
import com.locus.core.data.model.toDomain
import com.locus.core.data.model.toDto
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.result.LocusResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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
    ) {
        companion object {
            const val KEY_SALT = "telemetry_salt"
            const val KEY_ONBOARDING_STAGE = "onboarding_stage"
            private const val TAG = "SecureStorageDataSource"
        }

        // --- Onboarding Stage (Plain Prefs) ---

        suspend fun saveOnboardingStage(stage: OnboardingStage): LocusResult<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    plainPrefs.edit().putString(KEY_ONBOARDING_STAGE, stage.name).commit()
                    LocusResult.Success(Unit)
                } catch (e: Exception) {
                    LocusResult.Failure(e)
                }
            }
        }

        suspend fun getOnboardingStage(): LocusResult<OnboardingStage?> {
            return withContext(Dispatchers.IO) {
                try {
                    val stageName = plainPrefs.getString(KEY_ONBOARDING_STAGE, null)
                    val stage =
                        stageName?.let {
                            try {
                                OnboardingStage.valueOf(it)
                            } catch (e: IllegalArgumentException) {
                                OnboardingStage.IDLE
                            }
                        }
                    LocusResult.Success(stage)
                } catch (e: Exception) {
                    LocusResult.Failure(e)
                }
            }
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
    }
