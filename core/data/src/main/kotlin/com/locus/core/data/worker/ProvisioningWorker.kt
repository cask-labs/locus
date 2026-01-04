package com.locus.core.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.ProvisioningUseCase
import com.locus.core.domain.usecase.RecoverAccountUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json

@HiltWorker
class ProvisioningWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val provisioningUseCase: ProvisioningUseCase,
        private val recoverAccountUseCase: RecoverAccountUseCase,
        private val authRepository: AuthRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val mode = inputData.getString(KEY_MODE) ?: return Result.failure()
            val param = inputData.getString(KEY_PARAM) ?: return Result.failure()

            val credsResult = authRepository.getBootstrapCredentials()
            val creds =
                if (credsResult is LocusResult.Success && credsResult.data != null) {
                    credsResult.data!!
                } else {
                    Log.e(TAG, "Failed to retrieve bootstrap credentials from storage")
                    // If credentials are gone, we can't proceed.
                    // This is a fatal error for the worker.
                    return Result.failure()
                }

            val result =
                when (mode) {
                    MODE_NEW_DEVICE -> provisioningUseCase(creds, param)
                    MODE_RECOVERY -> recoverAccountUseCase(creds, param)
                    else -> LocusResult.Failure(DomainException.AuthError.InvalidCredentials) // Should not happen
                }

            return when (result) {
                is LocusResult.Success -> Result.success()
                is LocusResult.Failure -> {
                    // Domain UseCase already updates AuthRepository state to Failure.
                    // But we should verify.
                    // If the Worker fails, WorkManager might retry depending on policy,
                    // but here we want to surface the error to the UI and stop.
                    // Returning failure stops the chain.
                    Result.failure()
                }
            }
        }

        companion object {
            private const val TAG = "ProvisioningWorker"
            const val KEY_MODE = "mode"
            const val KEY_PARAM = "param" // Device Name or Bucket Name

            const val MODE_NEW_DEVICE = "new_device"
            const val MODE_RECOVERY = "recovery"
        }
    }
