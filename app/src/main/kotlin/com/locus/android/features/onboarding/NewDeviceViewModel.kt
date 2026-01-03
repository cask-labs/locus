package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
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
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NewDeviceUiState())
        val uiState: StateFlow<NewDeviceUiState> = _uiState.asStateFlow()

        private val _event = Channel<NewDeviceEvent>()
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

        companion object {
            private const val SIMULATED_DELAY_MS = 500L
        }

        fun onDeploy() {
            viewModelScope.launch {
                // Update stage to provisioning to trigger "The Trap" if app is killed
                val result = authRepository.setOnboardingStage(OnboardingStage.PROVISIONING)
                if (result is LocusResult.Success) {
                    _event.send(NewDeviceEvent.NavigateToProvisioning)
                    // TODO: Actually start the WorkManager job here in the next task
                } else {
                    _uiState.update { it.copy(error = "Failed to start provisioning") }
                }
            }
        }
    }
