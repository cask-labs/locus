package com.locus.core.domain.usecase

import com.locus.core.domain.repository.TrackingManager
import javax.inject.Inject

class StartTrackingUseCase
    @Inject
    constructor(
        private val trackingManager: TrackingManager,
    ) {
        operator fun invoke() {
            trackingManager.startTracking()
        }
    }
