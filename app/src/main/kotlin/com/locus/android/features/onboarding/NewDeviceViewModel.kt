package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.ProvisioningUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewDeviceUiState(
    val deviceName: String = "",
    val isNameValid: Boolean = false,
    val error: String? = null,
    val isChecking: Boolean = false,
    val availabilityMessage: String? = null,
)

sealed class NewDeviceEvent {
    data object NavigateToProvisioning : NewDeviceEvent()
}

@HiltViewModel
class NewDeviceViewModel
    @Inject
    constructor(
        private val provisioningUseCase: ProvisioningUseCase,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NewDeviceUiState())
        val uiState: StateFlow<NewDeviceUiState> = _uiState.asStateFlow()

        private val _event = Channel<NewDeviceEvent>(Channel.BUFFERED)
        val event = _event.receiveAsFlow()

        private val deviceNameRegex = Regex("^[a-z0-9-]+$")

        fun onDeviceNameChanged(name: String) {
            val isValid = name.isNotBlank() && deviceNameRegex.matches(name)
            _uiState.update {
                it.copy(
                    deviceName = name,
                    isNameValid = isValid,
                    error =
                        if (!isValid && name.isNotEmpty()) {
                            "Only lowercase letters, numbers, and hyphens allowed"
                        } else {
                            null
                        },
                )
            }
        }

        fun checkAvailability() {
            // Mock/Stub implementation for now as per plan
            val name = _uiState.value.deviceName
            if (!deviceNameRegex.matches(name)) return

            viewModelScope.launch {
                _uiState.update { it.copy(isChecking = true) }
                // Simulate network delay
                delay(SIMULATED_DELAY_MS)

                // Basic mock logic: reject if "existing" is in the name for testing
                if (name.contains("existing")) {
                    _uiState.update { it.copy(isChecking = false, error = "Device name unavailable") }
                } else {
                    _uiState.update { it.copy(isChecking = false, availabilityMessage = "Available!") }
                }
            }
        }

        fun onDeploy() {
            viewModelScope.launch {
                // In real implementation this triggers background worker or use case
                // For now, we manually kick off the use case or just navigate to provisioning to simulate
                // The provisioning screen observes state.

                // We need bootstrap creds.
                val credsResult = authRepository.getBootstrapCredentials()
                if (credsResult is LocusResult.Success) {
                    // We launch the UseCase in a separate scope or WorkManager in real life.
                    // For this synchronous/fake version, we launch here but don't block navigation.
                    // Ideally we use WorkManager.

                    launch {
                        // This is blocking/suspend, so it runs in background
                        provisioningUseCase(credsResult.data, _uiState.value.deviceName)
                    }

                    _event.send(NewDeviceEvent.NavigateToProvisioning)
                } else {
                    _uiState.update { it.copy(error = "Missing bootstrap credentials") }
                }
            }
        }

        companion object {
            private const val SIMULATED_DELAY_MS = 500L
        }
    }
