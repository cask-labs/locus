package com.locus.core.testing.repository

import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class FakeAuthRepository @Inject constructor() : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Uninitialized)
    private val _provisioningState = MutableStateFlow<ProvisioningState>(ProvisioningState.Idle)

    private var storedBootstrap: BootstrapCredentials? = null
    private var storedRuntime: RuntimeCredentials? = null
    var shouldFailValidation: Boolean = false

    override suspend fun initialize() {
        if (storedRuntime != null) {
            _authState.value = AuthState.Authenticated
        } else if (storedBootstrap != null) {
            _authState.value = AuthState.SetupPending
        } else {
            _authState.value = AuthState.Uninitialized
        }
    }

    override fun getAuthState(): Flow<AuthState> = _authState.asStateFlow()

    override fun getProvisioningState(): Flow<ProvisioningState> = _provisioningState.asStateFlow()

    override suspend fun updateProvisioningState(state: ProvisioningState) {
        _provisioningState.value = state
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
        _authState.value = AuthState.SetupPending
        return LocusResult.Success(Unit)
    }

    override suspend fun promoteToRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit> {
        storedRuntime = creds
        storedBootstrap = null
        _authState.value = AuthState.Authenticated
        _provisioningState.value = ProvisioningState.Success
        return LocusResult.Success(Unit)
    }

    override suspend fun replaceRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit> {
        storedRuntime = creds
        _authState.value = AuthState.Authenticated
        return LocusResult.Success(Unit)
    }

    override suspend fun clearBootstrapCredentials(): LocusResult<Unit> {
        storedBootstrap = null
        if (_authState.value == AuthState.SetupPending) {
            _authState.value = AuthState.Uninitialized
        }
        return LocusResult.Success(Unit)
    }

    override suspend fun getRuntimeCredentials(): LocusResult<RuntimeCredentials> {
        return storedRuntime?.let { LocusResult.Success(it) }
            ?: LocusResult.Failure(Exception("No runtime credentials"))
    }
}
