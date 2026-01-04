package com.locus.core.testing.repository

import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class FakeAuthRepository
    @Inject
    constructor() : AuthRepository {
        private val mutableAuthState = MutableStateFlow<AuthState>(AuthState.Uninitialized)
        private val mutableProvisioningState = MutableStateFlow<ProvisioningState>(ProvisioningState.Idle)

        private var storedBootstrap: BootstrapCredentials? = null
        private var storedRuntime: RuntimeCredentials? = null
        private var storedOnboardingStage: OnboardingStage = OnboardingStage.IDLE
        var shouldFailValidation: Boolean = false
        var shouldFailProvisioningStart: Boolean = false

        override suspend fun initialize() {
            if (storedRuntime != null) {
                mutableAuthState.value = AuthState.Authenticated
            } else if (storedBootstrap != null) {
                mutableAuthState.value = AuthState.SetupPending
            } else {
                mutableAuthState.value = AuthState.Uninitialized
            }
        }

        override fun getAuthState(): Flow<AuthState> = mutableAuthState.asStateFlow()

        override fun getProvisioningState(): Flow<ProvisioningState> = mutableProvisioningState.asStateFlow()

        override suspend fun updateProvisioningState(state: ProvisioningState) {
            mutableProvisioningState.value = state
        }

        override suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials> {
            return storedBootstrap?.let { LocusResult.Success(it) }
                ?: LocusResult.Failure(Exception("No bootstrap credentials"))
        }

        override suspend fun validateCredentials(creds: BootstrapCredentials): LocusResult<Unit> {
            return if (shouldFailValidation) {
                LocusResult.Failure(Exception("Invalid credentials"))
            } else {
                LocusResult.Success(Unit)
            }
        }

        override suspend fun saveBootstrapCredentials(creds: BootstrapCredentials): LocusResult<Unit> {
            storedBootstrap = creds
            mutableAuthState.value = AuthState.SetupPending
            return LocusResult.Success(Unit)
        }

        override suspend fun promoteToRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit> {
            storedRuntime = creds
            storedBootstrap = null
            mutableAuthState.value = AuthState.Authenticated
            mutableProvisioningState.value = ProvisioningState.Success
            return LocusResult.Success(Unit)
        }

        override suspend fun replaceRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit> {
            storedRuntime = creds
            mutableAuthState.value = AuthState.Authenticated
            return LocusResult.Success(Unit)
        }

        override suspend fun clearBootstrapCredentials(): LocusResult<Unit> {
            storedBootstrap = null
            if (mutableAuthState.value == AuthState.SetupPending) {
                mutableAuthState.value = AuthState.Uninitialized
            }
            return LocusResult.Success(Unit)
        }

        override suspend fun getRuntimeCredentials(): LocusResult<RuntimeCredentials> {
            return storedRuntime?.let { LocusResult.Success(it) }
                ?: LocusResult.Failure(Exception("No runtime credentials"))
        }

        override suspend fun getOnboardingStage(): OnboardingStage {
            return storedOnboardingStage
        }

        override suspend fun setOnboardingStage(stage: OnboardingStage) {
            storedOnboardingStage = stage
        }

        override suspend fun startProvisioning(
            mode: String,
            param: String,
        ): LocusResult<Unit> {
            return if (shouldFailProvisioningStart) {
                LocusResult.Failure(Exception("Failed to start provisioning"))
            } else {
                mutableProvisioningState.value = ProvisioningState.Working("Fake Provisioning Started")
                LocusResult.Success(Unit)
            }
        }
    }
