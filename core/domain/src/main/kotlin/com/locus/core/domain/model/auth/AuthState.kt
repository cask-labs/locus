package com.locus.core.domain.model.auth

/**
 * Represents the high-level authentication state of the application.
 */
sealed class AuthState {
    /**
     * Initial state. No credentials present, setup required.
     */
    data object Uninitialized : AuthState()

    /**
     * Setup is in progress (e.g., provisioning CloudFormation).
     * Credentials might exist but are temporary or not yet verified as runtime.
     */
    data object SetupPending : AuthState()

    /**
     * Application is fully authenticated with valid Runtime Credentials.
     */
    data object Authenticated : AuthState()
}
