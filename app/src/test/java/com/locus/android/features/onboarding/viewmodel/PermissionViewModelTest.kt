package com.locus.android.features.onboarding.viewmodel

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.repository.AuthRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class PermissionViewModelTest {
    private lateinit var viewModel: PermissionViewModel
    private lateinit var authRepository: AuthRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        viewModel = PermissionViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `returns coarse error when fine location denied and coarse granted`() =
        runTest {
            // Given
            val fine = false
            val coarse = true
            val background = false

            // When
            viewModel.updatePermissions(fine, coarse, background)

            // Then
            assertThat(viewModel.uiState.value).isEqualTo(PermissionUiState.CoarseLocationError)
        }

    @Test
    fun `shows foreground pending when nothing granted`() =
        runTest {
            // Given
            val fine = false
            val coarse = false
            val background = false

            // When
            viewModel.updatePermissions(fine, coarse, background)

            // Then
            assertThat(viewModel.uiState.value).isEqualTo(PermissionUiState.ForegroundPending)
        }

    @Test
    fun `shows background pending when fine granted but background denied`() =
        runTest {
            // Given
            val fine = true
            val coarse = true
            val background = false

            // When
            viewModel.updatePermissions(fine, coarse, background)

            // Then
            assertThat(viewModel.uiState.value).isEqualTo(PermissionUiState.BackgroundPending)
        }

    @Test
    fun `shows granted when fine and background granted`() =
        runTest {
            // Given
            val fine = true
            val coarse = true
            val background = true

            // When
            viewModel.updatePermissions(fine, coarse, background)

            // Then
            assertThat(viewModel.uiState.value).isEqualTo(PermissionUiState.Granted)
        }

    @Test
    fun `shows background pending even if notifications denied`() =
        runTest {
            // Given
            val fine = true
            val coarse = true
            val background = false

            // When
            viewModel.updatePermissions(fine, coarse, background)

            // Then
            assertThat(viewModel.uiState.value).isEqualTo(PermissionUiState.BackgroundPending)
        }

    @Test
    fun `completeOnboarding calls repository`() =
        runTest {
            // When
            viewModel.completeOnboarding()

            // Then
            coVerify { authRepository.setOnboardingStage(OnboardingStage.COMPLETE) }
        }
}
