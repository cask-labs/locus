package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class OnboardingUiState(
    val credentials: BootstrapCredentials = BootstrapCredentials("", "", "", "us-east-1"),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCredentialsValid: Boolean = false,
    val startDestination: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            authRepository.getAuthState().collect { authState ->
                val destination = when (authState) {
                    AuthState.Authenticated -> "dashboard" // Placeholder route name
                    AuthState.SetupPending -> "provisioning" // Placeholder route name
                    AuthState.Uninitialized -> "onboarding" // Placeholder route name
                }
                _uiState.value = _uiState.value.copy(startDestination = destination)
            }
        }
    }

    fun updateAccessKeyId(value: String) {
        _uiState.value = _uiState.value.copy(
            credentials = _uiState.value.credentials.copy(accessKeyId = value),
            error = null
        )
    }

    fun updateSecretAccessKey(value: String) {
        _uiState.value = _uiState.value.copy(
            credentials = _uiState.value.credentials.copy(secretAccessKey = value),
            error = null
        )
    }

    fun updateSessionToken(value: String) {
        _uiState.value = _uiState.value.copy(
            credentials = _uiState.value.credentials.copy(sessionToken = value),
            error = null
        )
    }

    fun pasteJson(jsonString: String) {
        try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonObject = json.decodeFromString<JsonObject>(jsonString)

            // Try to find the keys. AWS CLI usually outputs AccessKeyId, SecretAccessKey, SessionToken
            // We'll look for common variations or nested structures if needed, but simple first.
            // AWS CLI output often wraps in "Credentials": { ... }

            val credentialsObject = jsonObject["Credentials"] as? JsonObject ?: jsonObject

            val accessKeyId = credentialsObject["AccessKeyId"]?.jsonPrimitive?.contentOrNull
            val secretAccessKey = credentialsObject["SecretAccessKey"]?.jsonPrimitive?.contentOrNull
            val sessionToken = credentialsObject["SessionToken"]?.jsonPrimitive?.contentOrNull

            if (accessKeyId != null && secretAccessKey != null && sessionToken != null) {
                _uiState.value = _uiState.value.copy(
                    credentials = BootstrapCredentials(
                        accessKeyId = accessKeyId,
                        secretAccessKey = secretAccessKey,
                        sessionToken = sessionToken,
                        region = "us-east-1" // Default region for bootstrap
                    ),
                    error = null
                )
            } else {
                 _uiState.value = _uiState.value.copy(error = "Invalid JSON format: Missing keys")
            }

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to parse JSON")
        }
    }

    fun validateCredentials() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val creds = _uiState.value.credentials

            if (creds.accessKeyId.isBlank() || creds.secretAccessKey.isBlank() || creds.sessionToken.isBlank()) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "All fields are required")
                return@launch
            }

            val result = authRepository.validateCredentials(creds)
            when (result) {
                is LocusResult.Success -> {
                    // Save validated credentials
                    val saveResult = authRepository.saveBootstrapCredentials(creds)
                    if (saveResult is LocusResult.Success) {
                        _uiState.value = _uiState.value.copy(isLoading = false, isCredentialsValid = true)
                    } else {
                         _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to save credentials")
                    }
                }
                is LocusResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error.message ?: "Validation failed")
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
