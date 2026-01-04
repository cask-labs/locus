package com.locus.android.features.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.locus.android.R
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.ProvisioningUseCase
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
        private val provisioningUseCase: ProvisioningUseCase,
        application: Application,
    ) : AndroidViewModel(application) {
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
                            getApplication<Application>().getString(R.string.onboarding_new_device_name_invalid)
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
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            error =
                                getApplication<Application>()
                                    .getString(R.string.onboarding_new_device_name_unavailable),
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            availabilityMessage =
                                getApplication<Application>()
                                    .getString(R.string.onboarding_new_device_name_available),
                        )
                    }
                }
            }
        }

        companion object {
            private const val SIMULATED_DELAY_MS = 500L
        }

        fun deploy() {
            val deviceName = _uiState.value.deviceName
            if (!deviceNameRegex.matches(deviceName)) return

            viewModelScope.launch {
                val bootstrapResult = authRepository.getBootstrapCredentials()
                if (bootstrapResult is LocusResult.Failure) {
                    // This should theoretically not happen if we are here,
                    // but we should handle it or log it.
                    // For now, we assume credentials exist.
                    return@launch
                }
                val creds = (bootstrapResult as LocusResult.Success).data

                authRepository.setOnboardingStage(com.locus.core.domain.model.auth.OnboardingStage.PROVISIONING)

                // Trigger the use case
                provisioningUseCase(creds, deviceName)

                // Note: The UseCase updates ProvisioningState in the repository,
                // which the UI (ProvisioningScreen) observes.
            }
        }
    }
