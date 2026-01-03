package com.locus.android

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `advanceToPermissions updates repository and local state`() =
        runTest(testDispatcher) {
            // Given
            coEvery { authRepository.setOnboardingStage(any()) } returns Unit

            // When
            viewModel.advanceToPermissions()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { authRepository.setOnboardingStage(OnboardingStage.PERMISSIONS_PENDING) }
            assertThat(viewModel.onboardingStage.value).isEqualTo(OnboardingStage.PERMISSIONS_PENDING)
        }
}
