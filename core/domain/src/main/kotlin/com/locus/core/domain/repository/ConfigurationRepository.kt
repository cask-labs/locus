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
     *
     * @return The device ID if it has been initialized, or `null` if no device identity has been
     *   configured or is otherwise unavailable.
     */
    suspend fun getDeviceId(): String?

    /**
     * Retrieves the telemetry salt.
     *
     * @return The telemetry salt if it has been initialized, or `null` if no identity has been
     *   configured.
     */
    suspend fun getTelemetrySalt(): String?
}
