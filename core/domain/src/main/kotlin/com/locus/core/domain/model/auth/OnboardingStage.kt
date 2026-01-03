package com.locus.core.domain.model.auth

/**
 * Represents the high-level stage of the onboarding process.
 * Used for the "Setup Trap" to ensure users complete permissions.
 */
enum class OnboardingStage {
    IDLE,
    PROVISIONING,
    PERMISSIONS_PENDING,
    COMPLETE,
}
