package com.locus.core.domain.repository

import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.result.LocusResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Authentication and Provisioning state.
 */
interface AuthRepository {
    companion object {
        const val PROVISIONING_WORK_NAME = "provisioning"
    }

    /**
     * Initializes the repository, loading the initial state.
     */
    suspend fun initialize()

    /**
     * Observes the current high-level authentication state.
     */
    fun getAuthState(): Flow<AuthState>

    /**
     * Observes the detailed provisioning state during setup.
     */
    fun getProvisioningState(): Flow<ProvisioningState>

    /**
     * Updates the provisioning state (typically called by the Provisioning Service/Worker).
     */
    suspend fun updateProvisioningState(state: ProvisioningState)

    /**
     * Observes the current onboarding stage.
     */
    fun getOnboardingStage(): Flow<OnboardingStage>

    /**
     * Updates the current onboarding stage.
     */
    suspend fun setOnboardingStage(stage: OnboardingStage): LocusResult<Unit>

    /**
     * Validates that the provided Bootstrap Credentials are valid AWS Temporary Credentials.
     */
    suspend fun validateCredentials(creds: BootstrapCredentials): LocusResult<Unit>

    /**
     * Retrieves the stored Bootstrap Credentials securely.
     */
    suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>

    /**
     * Persists the Bootstrap Credentials securely (RAM/EncryptedSharedPrefs).
     */
    suspend fun saveBootstrapCredentials(creds: BootstrapCredentials): LocusResult<Unit>

    /**
     * Saves the Runtime Credentials and transitions the AuthState to Authenticated.
     * This effectively "logs the user in".
     */
    suspend fun promoteToRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit>

    /**
     * Replaces existing runtime credentials (e.g., key rotation).
     */
    suspend fun replaceRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit>

    /**
     * Clears any stored Bootstrap Credentials (security cleanup).
     */
    suspend fun clearBootstrapCredentials(): LocusResult<Unit>

    /**
     * Retrieves the currently active Runtime Credentials.
     */
    suspend fun getRuntimeCredentials(): LocusResult<RuntimeCredentials>
}
