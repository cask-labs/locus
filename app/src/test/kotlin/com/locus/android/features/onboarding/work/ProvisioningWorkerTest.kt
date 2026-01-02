package com.locus.android.features.onboarding.work

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.ProvisioningUseCase
import com.locus.core.domain.usecase.RecoverAccountUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import org.robolectric.annotation.Config
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule

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
        context = androidx.test.core.app.ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `doWork returns success when provisioning succeeds`() = runTest {
        // Arrange
        val creds = BootstrapCredentials("ak", "sk", "st", "us-east-1")
        coEvery { authRepository.getBootstrapCredentials() } returns LocusResult.Success(creds)
        coEvery { provisioningUseCase(any(), any()) } returns LocusResult.Success(Unit)

        val worker = TestListenableWorkerBuilder<ProvisioningWorker>(context)
            .setInputData(workDataOf(
                ProvisioningWorker.KEY_MODE to ProvisioningWorker.MODE_PROVISION,
                ProvisioningWorker.KEY_DEVICE_NAME to "device-1"
            ))
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters
                ): androidx.work.ListenableWorker {
                    return ProvisioningWorker(
                        appContext,
                        workerParameters,
                        authRepository,
                        provisioningUseCase,
                        recoverAccountUseCase
                    )
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(Result.success())
        coVerify { provisioningUseCase(creds, "device-1") }
    }

    @Test
    fun `doWork returns retry on transient network error`() = runTest {
        // Arrange
        val creds = BootstrapCredentials("ak", "sk", "st", "us-east-1")
        coEvery { authRepository.getBootstrapCredentials() } returns LocusResult.Success(creds)
        coEvery { provisioningUseCase(any(), any()) } returns LocusResult.Failure(DomainException.NetworkError.Offline)

        val worker = TestListenableWorkerBuilder<ProvisioningWorker>(context)
            .setInputData(workDataOf(
                ProvisioningWorker.KEY_MODE to ProvisioningWorker.MODE_PROVISION,
                ProvisioningWorker.KEY_DEVICE_NAME to "device-1"
            ))
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                 override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters
                ): androidx.work.ListenableWorker {
                    return ProvisioningWorker(
                        appContext,
                        workerParameters,
                        authRepository,
                        provisioningUseCase,
                        recoverAccountUseCase
                    )
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(Result.retry())
    }

    @Test
    fun `doWork returns failure and updates state on fatal error`() = runTest {
        // Arrange
        val creds = BootstrapCredentials("ak", "sk", "st", "us-east-1")
        coEvery { authRepository.getBootstrapCredentials() } returns LocusResult.Success(creds)
        coEvery { provisioningUseCase(any(), any()) } returns LocusResult.Failure(DomainException.ProvisioningError.Permissions("Permission Denied"))

        val worker = TestListenableWorkerBuilder<ProvisioningWorker>(context)
            .setInputData(workDataOf(
                ProvisioningWorker.KEY_MODE to ProvisioningWorker.MODE_PROVISION,
                ProvisioningWorker.KEY_DEVICE_NAME to "device-1"
            ))
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                 override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters
                ): androidx.work.ListenableWorker {
                    return ProvisioningWorker(
                        appContext,
                        workerParameters,
                        authRepository,
                        provisioningUseCase,
                        recoverAccountUseCase
                    )
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(Result.failure())
        coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
    }

    @Test
    fun `doWork returns failure when bootstrap credentials missing`() = runTest {
        // Arrange
        coEvery { authRepository.getBootstrapCredentials() } returns LocusResult.Failure(DomainException.AuthError.InvalidCredentials)

        val worker = TestListenableWorkerBuilder<ProvisioningWorker>(context)
             .setInputData(workDataOf(
                ProvisioningWorker.KEY_MODE to ProvisioningWorker.MODE_PROVISION,
                ProvisioningWorker.KEY_DEVICE_NAME to "device-1"
            ))
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                 override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters
                ): androidx.work.ListenableWorker {
                    return ProvisioningWorker(
                        appContext,
                        workerParameters,
                        authRepository,
                        provisioningUseCase,
                        recoverAccountUseCase
                    )
                }
            })
            .build()

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(Result.failure())
        coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
    }
}
