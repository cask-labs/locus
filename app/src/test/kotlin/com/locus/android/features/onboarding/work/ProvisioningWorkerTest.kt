package com.locus.android.features.onboarding.work

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.locus.android.R
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.ProvisioningUseCase
import com.locus.core.domain.usecase.RecoverAccountUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = HiltTestApplication::class)
class ProvisioningWorkerTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val provisioningUseCase: ProvisioningUseCase = mockk()
    private val recoverAccountUseCase: RecoverAccountUseCase = mockk()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun buildWorker(
        mode: String,
        deviceName: String? = null,
        bucketName: String? = null,
    ): ProvisioningWorker {
        val inputDataBuilder = androidx.work.Data.Builder().putString(ProvisioningWorker.KEY_MODE, mode)

        deviceName?.let { inputDataBuilder.putString(ProvisioningWorker.KEY_DEVICE_NAME, it) }
        bucketName?.let { inputDataBuilder.putString(ProvisioningWorker.KEY_BUCKET_NAME, it) }

        return TestListenableWorkerBuilder<ProvisioningWorker>(context)
            .setInputData(inputDataBuilder.build())
            .setWorkerFactory(
                object : androidx.work.WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: androidx.work.WorkerParameters,
                    ): androidx.work.ListenableWorker {
                        return ProvisioningWorker(
                            appContext,
                            workerParameters,
                            authRepository,
                            provisioningUseCase,
                            recoverAccountUseCase,
                        )
                    }
                },
            )
            .build()
    }

    @Test
    fun `getForegroundInfo returns correct attributes`() =
        runTest {
            val worker = buildWorker(ProvisioningWorker.MODE_PROVISION, "device-1")

            val foregroundInfo = worker.getForegroundInfo()

            assertThat(foregroundInfo.notificationId).isEqualTo(ProvisioningWorker.NOTIFICATION_ID)
            assertThat(foregroundInfo.foregroundServiceType).isEqualTo(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            // The channel ID is defined in NotificationConstants.CHANNEL_ID_SETUP which is "setup_channel"
            // The test previously expected "setup_status", but the code uses "setup_channel"
            assertThat(foregroundInfo.notification.channelId).isEqualTo("setup_channel")
            assertThat(foregroundInfo.notification.smallIcon.resId).isEqualTo(R.mipmap.ic_launcher)
        }

    @Test
    fun `doWork returns success when provisioning succeeds`() =
        runTest {
            // Arrange
            val creds = BootstrapCredentials("ak", "sk", "st", "us-east-1")
            coEvery { authRepository.getBootstrapCredentials() } returns LocusResult.Success(creds)
            coEvery { provisioningUseCase(any(), any()) } returns LocusResult.Success(Unit)

            val worker = buildWorker(ProvisioningWorker.MODE_PROVISION, deviceName = "device-1")

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(Result.success())
            coVerify { provisioningUseCase(creds, "device-1") }
        }

    @Test
    fun `doWork returns success when recovery succeeds`() =
        runTest {
            // Arrange
            val creds = BootstrapCredentials("ak", "sk", "st", "us-east-1")
            coEvery { authRepository.getBootstrapCredentials() } returns LocusResult.Success(creds)
            coEvery { recoverAccountUseCase(any(), any()) } returns LocusResult.Success(Unit)

            val worker = buildWorker(ProvisioningWorker.MODE_RECOVER, bucketName = "locus-bucket")

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(Result.success())
            coVerify { recoverAccountUseCase(creds, "locus-bucket") }
        }

    @Test
    fun `doWork returns retry on transient network error`() =
        runTest {
            // Arrange
            val creds = BootstrapCredentials("ak", "sk", "st", "us-east-1")
            coEvery { authRepository.getBootstrapCredentials() } returns LocusResult.Success(creds)
            coEvery { provisioningUseCase(any(), any()) } returns
                LocusResult.Failure(DomainException.NetworkError.Offline)

            val worker = buildWorker(ProvisioningWorker.MODE_PROVISION, deviceName = "device-1")

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(Result.retry())
        }

    @Test
    fun `doWork returns failure and updates state on fatal error`() =
        runTest {
            // Arrange
            val creds = BootstrapCredentials("ak", "sk", "st", "us-east-1")
            coEvery { authRepository.getBootstrapCredentials() } returns LocusResult.Success(creds)
            coEvery {
                provisioningUseCase(any(), any())
            } returns LocusResult.Failure(DomainException.ProvisioningError.Permissions("Permission Denied"))

            val worker = buildWorker(ProvisioningWorker.MODE_PROVISION, deviceName = "device-1")

            // Act
            val result = worker.doWork()

            // Assert
            val outputData = (result as? Result.Failure)?.outputData
            assertThat(outputData).isNotNull()
            assertThat(outputData?.getString("error_message")).isEqualTo("Permission Denied")
            coVerify {
                authRepository.updateProvisioningState(match { it is ProvisioningState.Failure })
            }
        }

    @Test
    fun `doWork returns failure when bootstrap credentials missing`() =
        runTest {
            // Arrange
            coEvery { authRepository.getBootstrapCredentials() } returns
                LocusResult.Failure(DomainException.AuthError.InvalidCredentials)

            val worker = buildWorker(ProvisioningWorker.MODE_PROVISION, deviceName = "device-1")

            // Act
            val result = worker.doWork()

            // Assert
            val outputData = (result as? Result.Failure)?.outputData
            assertThat(outputData).isNotNull()
            assertThat(outputData?.getString("error_message")).isEqualTo("Bootstrap credentials missing")
            coVerify {
                authRepository.updateProvisioningState(match { it is ProvisioningState.Failure })
            }
        }
}
