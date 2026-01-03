package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class OnboardingUiState(
    val accessKeyId: String = "",
    val secretAccessKey: String = "",
    val sessionToken: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class OnboardingEvent {
    data object CredentialsValidated : OnboardingEvent()

    data class NavigateTo(val route: String) : OnboardingEvent()
}

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private companion object {
            const val BOOTSTRAP_REGION = "us-east-1"
            const val STATE_TIMEOUT_MILLIS = 5000L
        }

        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        private val _event = Channel<OnboardingEvent>()
        val event = _event.receiveAsFlow()

        val provisioningState: StateFlow<ProvisioningState> =
            authRepository.getProvisioningState()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                    initialValue = ProvisioningState.Idle,
                )

        init {
            checkAuthState()
        }

        private fun checkAuthState() {
            viewModelScope.launch {
                authRepository.getAuthState().collect { state ->
                    // Initial check logic could go here, but navigation is mostly handled by MainActivity/NavGraph
                    // watching AuthRepository state.
                }
            }
        }

        fun acknowledgeSuccess() {
            viewModelScope.launch {
                authRepository.setOnboardingStage(OnboardingStage.PERMISSIONS_PENDING)
            }
        }

        fun completeOnboarding() {
            viewModelScope.launch {
                authRepository.setOnboardingStage(OnboardingStage.COMPLETE)
            }
        }

        fun onAccessKeyIdChanged(value: String) {
            _uiState.update { it.copy(accessKeyId = value, error = null) }
        }

        fun onSecretAccessKeyChanged(value: String) {
            _uiState.update { it.copy(secretAccessKey = value, error = null) }
        }

        fun onSessionTokenChanged(value: String) {
            _uiState.update { it.copy(sessionToken = value, error = null) }
        }

        fun pasteJson(json: String) {
            try {
                val jsonObject = JSONObject(json)
                // Retrieve keys case-insensitively or try standard AWS CLI keys
                val accessKeyId =
                    jsonObject.optString("AccessKeyId").ifEmpty {
                        jsonObject.optString("accessKeyId")
                    }
                val secretAccessKey =
                    jsonObject.optString("SecretAccessKey").ifEmpty {
                        jsonObject.optString("secretAccessKey")
                    }
                val sessionToken =
                    jsonObject.optString("SessionToken").ifEmpty {
                        jsonObject.optString("sessionToken")
                    }

                if (accessKeyId.isNotBlank() && secretAccessKey.isNotBlank() && sessionToken.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            accessKeyId = accessKeyId,
                            secretAccessKey = secretAccessKey,
                            sessionToken = sessionToken,
                            error = null,
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Invalid JSON format: Missing required keys") }
                }
            } catch (
                @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
            ) {
                _uiState.update { it.copy(error = "Failed to parse JSON: ${e.message}") }
            }
        }

        fun validateCredentials() {
            val currentState = _uiState.value
            if (currentState.accessKeyId.isBlank() ||
                currentState.secretAccessKey.isBlank() ||
                currentState.sessionToken.isBlank()
            ) {
                _uiState.update { it.copy(error = "All fields are required") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                // Note: Region hardcoded to us-east-1 for Bootstrap as per Network Infrastructure Spec (Section 2.1)
                val creds =
                    BootstrapCredentials(
                        accessKeyId = currentState.accessKeyId,
                        secretAccessKey = currentState.secretAccessKey,
                        sessionToken = currentState.sessionToken,
                        region = BOOTSTRAP_REGION,
                    )

                // Validate against AWS
                val validationResult = authRepository.validateCredentials(creds)

                if (validationResult is LocusResult.Success) {
                    // Save credentials to secure storage
                    val saveResult = authRepository.saveBootstrapCredentials(creds)
                    if (saveResult is LocusResult.Success) {
                        _event.send(OnboardingEvent.CredentialsValidated)
                    } else {
                        _uiState.update { it.copy(error = "Failed to save credentials") }
                    }
                } else {
                    val errorMsg =
                        when ((validationResult as LocusResult.Failure).error) {
                            is DomainException.AuthError -> "Invalid credentials provided"
                            is DomainException.NetworkError -> "Network connection error"
                            else -> "Invalid credentials or network error"
                        }
                    _uiState.update { it.copy(error = errorMsg) }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
