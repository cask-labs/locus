package com.locus.core.domain.model.auth

/**
 * Represents the high-level stage of the onboarding process, persisted to survive app restarts.
 * Used to implement the "Setup Trap".
 */
enum class OnboardingStage {
    IDLE,
    PROVISIONING,
    PERMISSIONS_PENDING,
    COMPLETE,
}
