package com.locus.core.domain.model.auth

/**
 * Represents the high-level stage of the onboarding process.
 * Used to persist the user's progress through the "Setup Trap".
 */
enum class OnboardingStage {
    IDLE,
    PROVISIONING,
    PERMISSIONS_PENDING,
    COMPLETE,
}
