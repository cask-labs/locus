package com.locus.core.domain.repository

import com.locus.core.domain.result.LocusResult

interface ConfigurationRepository {
    /**
     * Initializes the identity for the device.
     * @param deviceId The unique device ID.
     * @param salt The telemetry salt.
     */
    suspend fun initializeIdentity(
        deviceId: String,
        salt: String,
    ): LocusResult<Unit>

    /**
     * Retrieves the device ID.
     */
    suspend fun getDeviceId(): String?

    /**
     * Retrieves the telemetry salt.
     */
    suspend fun getTelemetrySalt(): String?
}
