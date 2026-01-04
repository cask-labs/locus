package com.locus.android.features.onboarding

import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.BucketValidationStatus
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.ScanBucketsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RecoveryViewModelTest {
    private lateinit var viewModel: RecoveryViewModel
    private val authRepository: AuthRepository = mockk()
    private val workManager: WorkManager = mockk(relaxed = true)
    private val scanBucketsUseCase: ScanBucketsUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RecoveryViewModel(authRepository, workManager, scanBucketsUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadBuckets updates state correctly`() =
        runTest {
            // Mock dependencies
            val creds =
                BootstrapCredentials(
                    accessKeyId = "test-key",
                    secretAccessKey = "test-secret",
                    sessionToken = "test-token",
                    region = "us-east-1",
                )
            coEvery { authRepository.getBootstrapCredentials() } returns LocusResult.Success(creds)
            coEvery { scanBucketsUseCase(any()) } returns
                LocusResult.Success(
                    listOf("bucket1" to BucketValidationStatus.Available),
                )

            viewModel.loadBuckets()

            // Run tasks
            testDispatcher.scheduler.advanceUntilIdle()

            // Finished loading
            val finalState = viewModel.uiState.value
            assertThat(finalState.isLoading).isFalse()
            assertThat(finalState.error).isNull()
            assertThat(finalState.buckets).containsExactly("bucket1")
        }
}
