package com.locus.core.domain.repository

/**
 * Interface to manage the tracking system.
 * Implemented by the App layer (Service) or Data layer.
 */
interface TrackingManager {
    fun startTracking()
}
