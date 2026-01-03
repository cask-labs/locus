package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@HiltViewModel
class NewDeviceViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NewDeviceUiState())
        val uiState: StateFlow<NewDeviceUiState> = _uiState.asStateFlow()

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
            private const val SIM_STEP_DELAY_1 = 1000L
            private const val SIM_STEP_DELAY_2 = 1500L
            private const val SIM_STEP_DELAY_3 = 2000L
        }

        fun deploy() {
            // NOTE: Temporary simulation for UI verification. Task 10 will replace this with actual Service start.
            viewModelScope.launch {
                authRepository.setOnboardingStage(com.locus.core.domain.model.auth.OnboardingStage.PROVISIONING)
                // Simulate Provisioning Steps
                authRepository.updateProvisioningState(ProvisioningState.Working("Validating input..."))
                delay(SIM_STEP_DELAY_1)
                authRepository.updateProvisioningState(ProvisioningState.Working("Creating CloudFormation Stack..."))
                delay(SIM_STEP_DELAY_2)
                authRepository.updateProvisioningState(ProvisioningState.Working("Deploying resources..."))
                delay(SIM_STEP_DELAY_3)
                authRepository.updateProvisioningState(ProvisioningState.Working("Verifying outputs..."))
                delay(SIM_STEP_DELAY_1)
                authRepository.updateProvisioningState(ProvisioningState.Success)
            }
        }
    }
