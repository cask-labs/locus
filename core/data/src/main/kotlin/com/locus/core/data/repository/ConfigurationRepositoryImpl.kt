package com.locus.core.data.repository

import android.content.SharedPreferences
import com.locus.core.data.source.local.SecureStorageDataSource
import com.locus.core.domain.repository.ConfigurationRepository
import com.locus.core.domain.result.LocusResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurationRepositoryImpl
    @Inject
    constructor(
        private val secureStorage: SecureStorageDataSource,
        private val prefs: SharedPreferences,
    ) : ConfigurationRepository {
        override suspend fun initializeIdentity(
            deviceId: String,
            salt: String,
        ): LocusResult<Unit> {
            try {
                // Store Device ID in plain prefs
                // Persist the telemetry salt. While runtime credentials also manage the salt,
                // we store it here as a fallback/redundancy for scenarios where we might
                // need it independent of the full credential set or for early initialization.
                prefs.edit()
                    .putString(KEY_DEVICE_ID, deviceId)
                    .putString(SecureStorageDataSource.KEY_SALT, salt)
                    .apply()

                return LocusResult.Success(Unit)
            } catch (e: Exception) {
                return LocusResult.Failure(e)
            }
        }

        override suspend fun getDeviceId(): String? {
            return prefs.getString(KEY_DEVICE_ID, null)
        }

        override suspend fun getTelemetrySalt(): String? {
            return secureStorage.getTelemetrySalt() ?: prefs.getString(SecureStorageDataSource.KEY_SALT, null)
        }

        companion object {
            private const val KEY_DEVICE_ID = "device_id"
        }
    }
