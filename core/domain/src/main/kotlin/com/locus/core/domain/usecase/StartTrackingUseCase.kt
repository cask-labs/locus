package com.locus.core.domain.usecase

import javax.inject.Inject

/**
 * UseCase responsible for starting all necessary background tracking services.
 * In a real implementation, this would interact with a repository or a service controller abstraction
 * to avoid direct dependency on Android framework classes (like Service).
 *
 * For now, since we are calling this from MainActivity (UI Layer) or similar, we might need an interface.
 * However, the plan says "Create StartTrackingUseCase".
 * Ideally, the Domain layer shouldn't know about `TrackerService` (which is Android specific).
 * But for Phase 1, we might need a bridge.
 *
 * A better approach is to have a `TrackingController` interface in Domain, implemented in App/Data,
 * and injected here.
 *
 * But given the simplicity of the plan "Create StartTrackingUseCase... Invoke TrackerService.start()",
 * and since `TrackerService` is in `:app`, checking dependency rules:
 * `:core:domain` cannot depend on `:app`.
 *
 * So `StartTrackingUseCase` in `:core:domain` CANNOT call `TrackerService.start()` directly if `TrackerService` is in `:app`.
 *
 * I must check the project structure.
 * `:app` -> `:core:domain`
 * `:core:domain` -> (Pure Kotlin)
 *
 * So `StartTrackingUseCase` cannot reference `TrackerService`.
 *
 * I need to define an interface `TrackingServiceController` in Domain,
 * and implement it in `:app` (or `:core:data` if the service was there, but the service is in `:app`).
 *
 * Let's see. The plan says: "Create StartTrackingUseCase (core/domain/usecase)... Invoke TrackerService.start()".
 * This implies a violation of dependency rules if not careful.
 *
 * I will define an interface `TrackingManager` in Domain.
 * And I will implement it in `app` module, or use a lambda/callback, or inject it.
 * But UseCases are usually in Domain.
 *
 * Wait, maybe `TrackerService` should be in `:core:data`?
 * The plan said "Create TrackerService.kt (Stub if missing) ... in com.locus.android.services".
 * And "Update MainActivity".
 *
 * If `StartTrackingUseCase` is in Domain, it can't see `TrackerService`.
 *
 * Options:
 * 1. Define `TrackingManager` interface in Domain. `StartTrackingUseCase` calls `trackingManager.startTracking()`.
 *    Implementation of `TrackingManager` is in `:app` (which sees Domain and Service) and bound via Hilt.
 *    But Hilt in `:app` can provide the implementation.
 *
 * I will go with Option 1.
 */

interface TrackingManager {
    fun startTracking()
}

class StartTrackingUseCase @Inject constructor(
    private val trackingManager: TrackingManager
) {
    operator fun invoke() {
        trackingManager.startTracking()
    }
}
