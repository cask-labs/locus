package com.locus.core.data.repository

import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import aws.sdk.kotlin.services.s3.listBuckets
import com.locus.core.data.source.local.SecureStorageDataSource
import com.locus.core.data.source.remote.aws.AwsClientFactory
import com.locus.core.data.util.await
import com.locus.core.data.worker.ProvisioningWorker
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.DomainException.AuthError
import com.locus.core.domain.result.DomainException.ProvisioningError
import com.locus.core.domain.result.LocusResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val awsClientFactory: AwsClientFactory,
        private val secureStorage: SecureStorageDataSource,
        private val applicationScope: CoroutineScope,
        private val workManager: WorkManager,
    ) : AuthRepository {
        private val mutableAuthState = MutableStateFlow<AuthState>(AuthState.Uninitialized)
        private val mutableProvisioningState = MutableStateFlow<ProvisioningState>(ProvisioningState.Idle)

        override suspend fun initialize() {
            loadInitialState()
            checkProvisioningWorkerStatus()
        }

        private suspend fun checkProvisioningWorkerStatus() {
            try {
                val workInfos = workManager.getWorkInfosForUniqueWork(AuthRepository.PROVISIONING_WORK_NAME).await()
                if (workInfos.isNotEmpty()) {
                    val info = workInfos.first()
                    when (info.state) {
                        WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                            mutableProvisioningState.value = ProvisioningState.Working("Resuming setup...")
                        }
                        WorkInfo.State.FAILED -> {
                            val errorMessage = info.outputData.getString("error_message") ?: "Setup failed in background"
                            mutableProvisioningState.value =
                                ProvisioningState.Failure(
                                    ProvisioningError.DeploymentFailed(errorMessage),
                                )
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            // If succeeded, we expect AuthState to be updated.
                            // But process death might mean we need to reload from storage.
                            loadInitialState()
                            if (mutableAuthState.value == AuthState.Authenticated) {
                                mutableProvisioningState.value = ProvisioningState.Success
                            }
                        }
                        else -> {
                            // Cancelled or Blocked
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check provisioning worker status", e)
            }
        }

        private suspend fun loadInitialState() {
            val runtimeResult = secureStorage.getRuntimeCredentials()
            if (runtimeResult is LocusResult.Success && runtimeResult.data != null) {
                mutableAuthState.value = AuthState.Authenticated
                return
            }

            val bootstrapResult = secureStorage.getBootstrapCredentials()
            if (bootstrapResult is LocusResult.Success && bootstrapResult.data != null) {
                mutableAuthState.value = AuthState.SetupPending
                return
            }

            mutableAuthState.value = AuthState.Uninitialized
        }

        override fun getAuthState(): Flow<AuthState> = mutableAuthState.asStateFlow()

        override fun getProvisioningState(): Flow<ProvisioningState> = mutableProvisioningState.asStateFlow()

        override suspend fun updateProvisioningState(state: ProvisioningState) {
            mutableProvisioningState.value = state
        }

        override suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials> {
            return try {
                val result = secureStorage.getBootstrapCredentials()
                when (result) {
                    is LocusResult.Success -> {
                        val creds = result.data
                        if (creds != null) {
                            LocusResult.Success(creds)
                        } else {
                            LocusResult.Failure(AuthError.InvalidCredentials)
                        }
                    }
                    is LocusResult.Failure -> LocusResult.Failure(result.error)
                }
            } catch (e: Exception) {
                LocusResult.Failure(AuthError.Generic(e))
            }
        }

        override suspend fun validateCredentials(creds: BootstrapCredentials): LocusResult<Unit> {
            return try {
                val client = awsClientFactory.createBootstrapS3Client(creds)
                client.use { s3 ->
                    s3.listBuckets()
                }
                LocusResult.Success(Unit)
            } catch (e: Exception) {
                LocusResult.Failure(AuthError.InvalidCredentials)
            }
        }

        override suspend fun saveBootstrapCredentials(creds: BootstrapCredentials): LocusResult<Unit> {
            val result = secureStorage.saveBootstrapCredentials(creds)
            if (result is LocusResult.Success) {
                mutableAuthState.value = AuthState.SetupPending
            }
            return result
        }

        override suspend fun promoteToRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit> {
            val saveResult = secureStorage.saveRuntimeCredentials(creds)
            if (saveResult is LocusResult.Failure) return saveResult

            val clearResult = secureStorage.clearBootstrapCredentials()
            if (clearResult is LocusResult.Failure) {
                return clearResult
            }

            mutableAuthState.value = AuthState.Authenticated
            mutableProvisioningState.value = ProvisioningState.Success
            return LocusResult.Success(Unit)
        }

        override suspend fun replaceRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit> {
            val result = secureStorage.saveRuntimeCredentials(creds)
            if (result is LocusResult.Success) {
                mutableAuthState.value = AuthState.Authenticated
            }
            return result
        }

        override suspend fun clearBootstrapCredentials(): LocusResult<Unit> {
            return secureStorage.clearBootstrapCredentials()
        }

        override suspend fun getRuntimeCredentials(): LocusResult<RuntimeCredentials> {
            val result = secureStorage.getRuntimeCredentials()
            return when (result) {
                is LocusResult.Success -> {
                    val data =
                        result.data ?: return LocusResult.Failure(
                            AuthError.InvalidCredentials,
                        )
                    LocusResult.Success(data)
                }
                is LocusResult.Failure -> result
            }
        }

        override suspend fun getOnboardingStage(): com.locus.core.domain.model.auth.OnboardingStage {
            return try {
                val stageStr = secureStorage.getOnboardingStage()
                if (stageStr != null) {
                    com.locus.core.domain.model.auth.OnboardingStage.valueOf(stageStr)
                } else {
                    com.locus.core.domain.model.auth.OnboardingStage.IDLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load onboarding stage", e)
                if (mutableAuthState.value == AuthState.Authenticated) {
                    com.locus.core.domain.model.auth.OnboardingStage.PERMISSIONS_PENDING
                } else {
                    com.locus.core.domain.model.auth.OnboardingStage.IDLE
                }
            }
        }

        override suspend fun setOnboardingStage(stage: com.locus.core.domain.model.auth.OnboardingStage) {
            try {
                secureStorage.saveOnboardingStage(stage.name)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save onboarding stage", e)
            }
        }

        override suspend fun startProvisioning(
            mode: String,
            param: String,
        ): LocusResult<Unit> {
            return try {
                val credsResult = getBootstrapCredentials()
                if (credsResult is LocusResult.Failure) return credsResult

                val workRequest =
                    OneTimeWorkRequestBuilder<ProvisioningWorker>()
                        .setInputData(
                            workDataOf(
                                ProvisioningWorker.KEY_MODE to mode,
                                ProvisioningWorker.KEY_PARAM to param,
                            ),
                        )
                        .build()

                workManager.enqueueUniqueWork(
                    AuthRepository.PROVISIONING_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest,
                )
                // Immediately set state to Working
                mutableProvisioningState.value = ProvisioningState.Working("Starting background setup...")
                LocusResult.Success(Unit)
            } catch (e: Exception) {
                LocusResult.Failure(ProvisioningError.DeploymentFailed(e.message ?: "Failed to start worker"))
            }
        }

        companion object {
            private const val TAG = "AuthRepositoryImpl"
        }
    }
