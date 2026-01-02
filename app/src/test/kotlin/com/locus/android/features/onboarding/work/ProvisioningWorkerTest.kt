package com.locus.android.features.onboarding.work

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.locus.android.features.onboarding.work.ProvisioningWorker.Companion.KEY_BUCKET_NAME
import com.locus.android.features.onboarding.work.ProvisioningWorker.Companion.KEY_DEVICE_NAME
import com.locus.android.features.onboarding.work.ProvisioningWorker.Companion.KEY_MODE
import com.locus.android.features.onboarding.work.ProvisioningWorker.Companion.MODE_PROVISION
import com.locus.android.features.onboarding.work.ProvisioningWorker.Companion.MODE_RECOVER
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ProvisioningWorkerTest {
    private lateinit var context: Context
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val provisioningUseCase: ProvisioningUseCase = mockk()
    private val recoverAccountUseCase: RecoverAccountUseCase = mockk()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Default happy path for credentials
        coEvery { authRepository.getBootstrapCredentials() } returns
            LocusResult.Success(
                BootstrapCredentials("ak", "sk", "st", "reg"),
            )
    }

    @Test
    fun `doWork returns success when provisioning succeeds`() =
        runTest {
            coEvery { provisioningUseCase(any(), any()) } returns LocusResult.Success(Unit)

            val worker =
                TestListenableWorkerBuilder<ProvisioningWorker>(context)
                    .setInputData(
                        workDataOf(
                            KEY_MODE to MODE_PROVISION,
                            KEY_DEVICE_NAME to "test-device",
                        ),
                    )
                    .setWorkerFactory(
                        object : androidx.work.WorkerFactory() {
                            override fun createWorker(
                                appContext: Context,
                                workerClassName: String,
                                workerParameters: WorkerParameters,
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

            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
            coVerify { provisioningUseCase(any(), "test-device") }
        }

    @Test
    fun `doWork returns retry on transient network error`() =
        runTest {
            coEvery { provisioningUseCase(any(), any()) } returns
                LocusResult.Failure(
                    DomainException.NetworkError.Offline,
                )

            val worker =
                TestListenableWorkerBuilder<ProvisioningWorker>(context)
                    .setInputData(
                        workDataOf(
                            KEY_MODE to MODE_PROVISION,
                            KEY_DEVICE_NAME to "test-device",
                        ),
                    )
                    .setWorkerFactory(
                        object : androidx.work.WorkerFactory() {
                            override fun createWorker(
                                appContext: Context,
                                workerClassName: String,
                                workerParameters: WorkerParameters,
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

            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.retry())
        }

    @Test
    fun `doWork returns failure and updates state on fatal error`() =
        runTest {
            coEvery { provisioningUseCase(any(), any()) } returns
                LocusResult.Failure(
                    DomainException.AuthError.InvalidCredentials,
                )

            val worker =
                TestListenableWorkerBuilder<ProvisioningWorker>(context)
                    .setInputData(
                        workDataOf(
                            KEY_MODE to MODE_PROVISION,
                            KEY_DEVICE_NAME to "test-device",
                        ),
                    )
                    .setWorkerFactory(
                        object : androidx.work.WorkerFactory() {
                            override fun createWorker(
                                appContext: Context,
                                workerClassName: String,
                                workerParameters: WorkerParameters,
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

            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.failure())
            coVerify {
                authRepository.updateProvisioningState(match { it is ProvisioningState.Failure })
            }
        }

    @Test
    fun `doWork returns success when recovery succeeds`() =
        runTest {
            coEvery { recoverAccountUseCase(any(), any()) } returns LocusResult.Success(Unit)

            val worker =
                TestListenableWorkerBuilder<ProvisioningWorker>(context)
                    .setInputData(
                        workDataOf(
                            KEY_MODE to MODE_RECOVER,
                            KEY_BUCKET_NAME to "locus-bucket",
                        ),
                    )
                    .setWorkerFactory(
                        object : androidx.work.WorkerFactory() {
                            override fun createWorker(
                                appContext: Context,
                                workerClassName: String,
                                workerParameters: WorkerParameters,
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

            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
            coVerify { recoverAccountUseCase(any(), "locus-bucket") }
        }
}
