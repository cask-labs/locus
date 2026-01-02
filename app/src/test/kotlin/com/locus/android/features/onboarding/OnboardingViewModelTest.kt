package com.locus.android.features.onboarding

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { authRepository.getAuthState() } returns flowOf(AuthState.Uninitialized)
        viewModel = OnboardingViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkAuthState updates startDestination`() = runTest {
        assertThat(viewModel.uiState.value.startDestination).isEqualTo("onboarding")
    }

    @Test
    fun `pasteJson parses valid aws json`() {
        val json = """
            {
                "Credentials": {
                    "AccessKeyId": "ASIA...",
                    "SecretAccessKey": "secret...",
                    "SessionToken": "token..."
                }
            }
        """.trimIndent()

        viewModel.pasteJson(json)

        val creds = viewModel.uiState.value.credentials
        assertThat(creds.accessKeyId).isEqualTo("ASIA...")
        assertThat(creds.secretAccessKey).isEqualTo("secret...")
        assertThat(creds.sessionToken).isEqualTo("token...")
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `pasteJson sets error on invalid json`() {
        viewModel.pasteJson("invalid json")
        assertThat(viewModel.uiState.value.error).isNotNull()
    }

    @Test
    fun `validateCredentials success updates state`() = runTest {
        val creds = BootstrapCredentials("key", "secret", "token", "us-east-1")
        viewModel.updateAccessKeyId("key")
        viewModel.updateSecretAccessKey("secret")
        viewModel.updateSessionToken("token")

        coEvery { authRepository.validateCredentials(any()) } returns LocusResult.Success(Unit)
        coEvery { authRepository.saveBootstrapCredentials(any()) } returns LocusResult.Success(Unit)

        viewModel.validateCredentials()

        assertThat(viewModel.uiState.value.isCredentialsValid).isTrue()
        coVerify { authRepository.saveBootstrapCredentials(match { it.accessKeyId == "key" }) }
    }

    @Test
    fun `validateCredentials failure sets error`() = runTest {
         viewModel.updateAccessKeyId("key")
        viewModel.updateSecretAccessKey("secret")
        viewModel.updateSessionToken("token")

        coEvery { authRepository.validateCredentials(any()) } returns LocusResult.Failure(Exception("Invalid"))

        viewModel.validateCredentials()

        assertThat(viewModel.uiState.value.isCredentialsValid).isFalse()
        assertThat(viewModel.uiState.value.error).contains("Invalid")
    }
}
