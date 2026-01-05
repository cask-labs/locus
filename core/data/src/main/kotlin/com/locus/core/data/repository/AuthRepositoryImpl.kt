package com.locus.core.data.repository

import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import aws.sdk.kotlin.services.s3.listBuckets
import com.locus.core.data.source.local.SecureStorageDataSource
import com.locus.core.data.source.remote.aws.AwsClientFactory
import com.locus.core.data.util.await
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.OnboardingStage
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
import kotlinx.coroutines.flow.update
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
        private val mutableOnboardingStage = MutableStateFlow<OnboardingStage>(OnboardingStage.IDLE)

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
                        else -> {
                            // Succeeded or Cancelled - standard flow should have updated state,
                            // or it is old work. If SUCCEEDED, we might want to check auth state?
                            // But initialize() already loaded auth state.
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check provisioning worker status", e)
            }
        }

        private suspend fun loadInitialState() {
            // Load Onboarding Stage
            val stageResult = secureStorage.getOnboardingStage()
            if (stageResult is LocusResult.Success) {
                mutableOnboardingStage.value = stageResult.data
            }

            val runtimeResult = secureStorage.getRuntimeCredentials()
            if (runtimeResult is LocusResult.Success && runtimeResult.data != null) {
                mutableAuthState.value = AuthState.Authenticated

                // Fail-Secure: If we are authenticated but failed to read the stage,
                // default to PERMISSIONS_PENDING (safe trap) instead of IDLE.
                // This ensures we verify permissions and don't dump the user to the Welcome screen.
                if (stageResult is LocusResult.Failure) {
                    mutableOnboardingStage.value = OnboardingStage.PERMISSIONS_PENDING
                } else if (stageResult is LocusResult.Success) {
                    // Self-Healing: If we are authenticated, we must have passed provisioning.
                    // If the stage is not COMPLETE, we should trap the user in the permissions flow
                    // to ensure they finish setup. This catches cases where the user
                    // force-quit during permissions or if state was partially lost.
                    if (stageResult.data != OnboardingStage.COMPLETE) {
                        mutableOnboardingStage.value = OnboardingStage.PERMISSIONS_PENDING
                    }
                }
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

        override fun getOnboardingStage(): Flow<OnboardingStage> = mutableOnboardingStage.asStateFlow()

        override suspend fun setOnboardingStage(stage: OnboardingStage) {
            try {
                val result = secureStorage.saveOnboardingStage(stage)
                if (result is LocusResult.Success) {
                    mutableOnboardingStage.value = stage
                } else {
                    Log.e(TAG, "Failed to persist onboarding stage: $stage")
                    // Fail-open / optimistic update:
                    // We still advance the in-memory onboarding stage so that the user can
                    // continue through the flow even if persistence fails (e.g. due to an
                    // intermittent storage or encryption error). This means the in-memory
                    // value may temporarily diverge from what is stored on disk.
                    //
                    // Known edge cases:
                    // - If the process dies before a subsequent successful save, the
                    // persisted stage will still reflect the older value. On next app
                    // start, loadInitialState() will restore from storage and the user
                    // may see an earlier onboarding stage than the one they had reached
                    // in-memory.
                    // - If persistence keeps failing, we will only ever have the
                    // in-memory progression; callers relying on durable storage must
                    // account for this and handle retries or error reporting at a higher level.
                    //
                    // This tradeoff is intentional in favor of UX continuity. Revisit this
                    // behavior if stronger guarantees about persisted onboarding progress
                    // are required.
                    mutableOnboardingStage.value = stage
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception saving onboarding stage", e)
                // Fail-open behavior also applies when an unexpected exception occurs.
                // We still advance the in-memory stage so the UI and user flow are not
                // blocked on storage reliability. The same caveats as above apply:
                // process death before a successful write will cause the stage derived
                // from storage on next startup to lag behind what the user last saw.
                mutableOnboardingStage.value = stage
            }
        }

        override fun getProvisioningState(): Flow<ProvisioningState> = mutableProvisioningState.asStateFlow()

        override suspend fun updateProvisioningState(state: ProvisioningState) {
            mutableProvisioningState.update { currentState ->
                when {
                    state is ProvisioningState.Working && currentState is ProvisioningState.Working -> {
                        val newHistory = (currentState.history + currentState.currentStep).takeLast(ProvisioningState.MAX_HISTORY_SIZE)
                        state.copy(history = newHistory)
                    }
                    state is ProvisioningState.Failure && currentState is ProvisioningState.Working -> {
                        // Capture the step that was running when we failed
                        state.copy(
                            failedStep = currentState.currentStep,
                            history = currentState.history,
                        )
                    }
                    else -> state
                }
            }
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
                // Use 'use' to auto-close, but we perform operation inside explicitly
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
            // Note: We intentionally do not update OnboardingStage here.
            // The caller or surrounding flow is responsible for setting it explicitly
            // (for example to PERMISSIONS_PENDING after promotion, and later to COMPLETE).
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

        companion object {
            private const val TAG = "AuthRepositoryImpl"
        }
    }
