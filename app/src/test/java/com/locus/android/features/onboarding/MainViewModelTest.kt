package com.locus.android.features.onboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.locus.android.MainViewModel
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { authRepository.getAuthState() } returns MutableStateFlow(AuthState.Uninitialized)
        coEvery { authRepository.getOnboardingStage() } returns OnboardingStage.IDLE

        viewModel = MainViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial stage is loaded from repository`() =
        runTest {
            coVerify { authRepository.getOnboardingStage() }
            assertThat(viewModel.onboardingStage.value).isEqualTo(OnboardingStage.IDLE)
        }

    @Test
    fun `completeOnboarding updates repository and state`() =
        runTest {
            viewModel.completeOnboarding()

            coVerify { authRepository.setOnboardingStage(OnboardingStage.COMPLETE) }
            assertThat(viewModel.onboardingStage.value).isEqualTo(OnboardingStage.COMPLETE)
        }
}
