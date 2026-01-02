package com.locus.android.features.onboarding

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class OnboardingViewModelTest {
    private lateinit var viewModel: OnboardingViewModel
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.getAuthState() } returns MutableStateFlow(AuthState.Uninitialized)
        viewModel = OnboardingViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pasteJson parses valid json correctly`() {
        val validJson =
            """
            {
                "AccessKeyId": "testKey",
                "SecretAccessKey": "testSecret",
                "SessionToken": "testToken"
            }
            """.trimIndent()

        viewModel.pasteJson(validJson)

        val state = viewModel.uiState.value
        assertThat(state.accessKeyId).isEqualTo("testKey")
        assertThat(state.secretAccessKey).isEqualTo("testSecret")
        assertThat(state.sessionToken).isEqualTo("testToken")
        assertThat(state.error).isNull()
    }

    @Test
    fun `pasteJson handles invalid json`() {
        val invalidJson = "invalid"

        viewModel.pasteJson(invalidJson)

        val state = viewModel.uiState.value
        assertThat(state.error).contains("Failed to parse JSON")
    }

    @Test
    fun `pasteJson handles missing keys`() {
        val incompleteJson =
            """
            {
                "AccessKeyId": "testKey"
            }
            """.trimIndent()

        viewModel.pasteJson(incompleteJson)

        val state = viewModel.uiState.value
        assertThat(state.error).contains("Invalid JSON format")
    }

    @Test
    fun `validateCredentials calls repository and updates state on success`() =
        runTest {
            viewModel.onAccessKeyIdChanged(TEST_KEY)
            viewModel.onSecretAccessKeyChanged(TEST_SECRET)
            viewModel.onSessionTokenChanged(TEST_TOKEN)

            coEvery { authRepository.validateCredentials(any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.saveBootstrapCredentials(any()) } returns LocusResult.Success(Unit)

            viewModel.validateCredentials()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { authRepository.validateCredentials(any()) }
            coVerify { authRepository.saveBootstrapCredentials(any()) }
            // We would also check for the event emission if we could easily collect it in this test setup
        }

    @Test
    fun `validateCredentials sets error on failure`() =
        runTest {
            viewModel.onAccessKeyIdChanged(TEST_KEY)
            viewModel.onSecretAccessKeyChanged(TEST_SECRET)
            viewModel.onSessionTokenChanged(TEST_TOKEN)

            coEvery { authRepository.validateCredentials(any()) } returns LocusResult.Failure(Exception("Fail"))

            viewModel.validateCredentials()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value.error).contains("Invalid credentials")
        }

    companion object {
        private const val TEST_KEY = "key"
        private const val TEST_SECRET = "secret"
        private const val TEST_TOKEN = "token"
    }
}
