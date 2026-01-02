package com.locus.android.features.onboarding.work

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.locus.android.R
import com.locus.android.util.NotificationConstants
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.ProvisioningUseCase
import com.locus.core.domain.usecase.RecoverAccountUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ProvisioningWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val authRepository: AuthRepository,
        private val provisioningUseCase: ProvisioningUseCase,
        private val recoverAccountUseCase: RecoverAccountUseCase,
    ) : CoroutineWorker(appContext, workerParams) {
        companion object {
            const val WORK_NAME = AuthRepository.PROVISIONING_WORK_NAME
            const val KEY_MODE = "mode"
            const val KEY_DEVICE_NAME = "device_name"
            const val KEY_BUCKET_NAME = "bucket_name"

            const val MODE_PROVISION = "PROVISION"
            const val MODE_RECOVER = "RECOVER"
            const val NOTIFICATION_ID = 1001
        }

        override suspend fun getForegroundInfo(): ForegroundInfo {
            val notification = createNotification(applicationContext.getString(R.string.notification_setup_starting))
            // For Android 14+ we need to specify the type
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                ForegroundInfo(NOTIFICATION_ID, notification)
            }
        }

        @Suppress("TooGenericExceptionCaught")
        override suspend fun doWork(): Result {
            // 1. Notification - Immediately set foreground
            setForeground(getForegroundInfo())

            // 2. Credentials
            val credsResult = authRepository.getBootstrapCredentials()
            if (credsResult is LocusResult.Failure) {
                handleFatalError("Bootstrap credentials missing")
                return Result.failure()
            }
            val creds = (credsResult as LocusResult.Success).data

            // 3. Dispatch & Execution
            val mode = inputData.getString(KEY_MODE)
            val deviceName = inputData.getString(KEY_DEVICE_NAME)
            val bucketName = inputData.getString(KEY_BUCKET_NAME)

            return try {
                val result =
                    when (mode) {
                        MODE_PROVISION -> {
                            require(!deviceName.isNullOrBlank()) { "Device name required" }
                            provisioningUseCase(creds, deviceName)
                        }
                        MODE_RECOVER -> {
                            require(!bucketName.isNullOrBlank()) { "Bucket name required" }
                            recoverAccountUseCase(creds, bucketName)
                        }
                        else -> throw IllegalArgumentException("Invalid mode: $mode")
                    }

                when (result) {
                    is LocusResult.Success -> Result.success()
                    is LocusResult.Failure -> handleError(result.error)
                }
            } catch (e: Exception) {
                // Handle unchecked exceptions as Fatal for now, or retry if network related
                handleFatalError(e.message ?: "Unknown error")
                Result.failure()
            }
        }

        private suspend fun handleError(error: Any): Result {
            return if (error is DomainException) {
                when (error) {
                    // Transient Errors - Retry
                    is DomainException.NetworkError,
                    is DomainException.ProvisioningError.Wait,
                    -> {
                        Result.retry()
                    }
                    // Fatal Errors - Fail
                    else -> {
                        handleFatalError(
                            error.message
                                ?: applicationContext.getString(R.string.notification_setup_fallback_message),
                        )
                        Result.failure()
                    }
                }
            } else {
                handleFatalError(applicationContext.getString(R.string.notification_setup_unknown_error))
                Result.failure()
            }
        }

        private suspend fun handleFatalError(message: String) {
            authRepository.updateProvisioningState(
                ProvisioningState.Failure(
                    DomainException.ProvisioningError.DeploymentFailed(message),
                ),
            )
        }

        private fun createNotification(message: String): Notification {
            return NotificationCompat.Builder(applicationContext, NotificationConstants.CHANNEL_ID_SETUP)
                .setContentTitle(applicationContext.getString(R.string.notification_setup_title))
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher) // Fallback icon
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }
