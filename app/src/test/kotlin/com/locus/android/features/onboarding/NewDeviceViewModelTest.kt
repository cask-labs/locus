package com.locus.android.features.onboarding

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.repository.AuthRepository
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
@Config(sdk = [34])
class NewDeviceViewModelTest {
    private lateinit var viewModel: NewDeviceViewModel
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val workManager: WorkManager = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val application: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = NewDeviceViewModel(authRepository, workManager, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onDeviceNameChanged updates state correctly for valid input`() {
        val validName = "valid-name-123"
        viewModel.onDeviceNameChanged(validName)

        val state = viewModel.uiState.value
        assertThat(state.deviceName).isEqualTo(validName)
        assertThat(state.isNameValid).isTrue()
        assertThat(state.error).isNull()
    }

    @Test
    fun `onDeviceNameChanged updates state correctly for invalid input`() {
        val invalidName = "Invalid Name!"
        viewModel.onDeviceNameChanged(invalidName)

        val state = viewModel.uiState.value
        assertThat(state.deviceName).isEqualTo(invalidName)
        assertThat(state.isNameValid).isFalse()
        assertThat(state.error).isNotNull()
    }

    @Test
    fun `checkAvailability updates loading state`() =
        runTest {
            viewModel.onDeviceNameChanged("valid-name")
            viewModel.checkAvailability()

            // Initially checking - need to trigger coroutine execution for the first state update
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.uiState.value.isChecking).isTrue()

            testDispatcher.scheduler.advanceUntilIdle()

            // Finished checking
            val finalState = viewModel.uiState.value
            assertThat(finalState.isChecking).isFalse()
            assertThat(finalState.availabilityMessage).isEqualTo("Available!")
        }
}
