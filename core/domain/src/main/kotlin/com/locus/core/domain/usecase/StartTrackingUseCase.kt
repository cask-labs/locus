package com.locus.core.domain.usecase

import javax.inject.Inject

/**
 * UseCase responsible for starting all necessary background tracking services.
 * This interacts with a TrackingManager abstraction to avoid direct dependency
 * on Android framework classes (like Service) in the Domain layer.
 */

interface TrackingManager {
    fun startTracking()

    fun scheduleWatchdog()
}

class StartTrackingUseCase
    @Inject
    constructor(
        private val trackingManager: TrackingManager,
    ) {
        operator fun invoke() {
            trackingManager.startTracking()
            trackingManager.scheduleWatchdog()
        }
    }
