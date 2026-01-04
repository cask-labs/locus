package com.locus.android.features.onboarding

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.usecase.RecoverAccountUseCase
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
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val recoverAccountUseCase: RecoverAccountUseCase = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RecoveryViewModel(authRepository, recoverAccountUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadBuckets updates state correctly`() =
        runTest {
            viewModel.loadBuckets()

            // Initially loading - run current tasks to let the launch block start
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.uiState.value.isLoading).isTrue()

            testDispatcher.scheduler.advanceUntilIdle()

            // Finished loading
            val finalState = viewModel.uiState.value
            assertThat(finalState.isLoading).isFalse()
            assertThat(finalState.buckets).isNotEmpty()
        }
}
