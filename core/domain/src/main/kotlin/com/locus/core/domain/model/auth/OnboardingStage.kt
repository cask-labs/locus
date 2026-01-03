package com.locus.core.domain.model.auth

/**
 * Represents the high-level stage of the onboarding process.
 * Used to persist state across process death ("Setup Trap") to ensure users
 * complete mandatory steps (like permissions) before accessing the dashboard.
 */
enum class OnboardingStage {
    /**
     * Fresh install or reset state. No provisioning started.
     */
    IDLE,

    /**
     * Provisioning (CloudFormation deployment) is in progress or failed.
     * The user should be returned to the provisioning screen.
     */
    PROVISIONING,

    /**
     * Provisioning succeeded, but mandatory permissions (Location) have not been granted.
     * The user is "trapped" in the permission flow until granted.
     */
    PERMISSIONS_PENDING,

    /**
     * Onboarding is fully complete. User can access the Dashboard.
     */
    COMPLETE,
}
