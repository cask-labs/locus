package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.locus.android.features.onboarding.work.ProvisioningWorker
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
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
        private val authRepository: AuthRepository,
        private val workManager: WorkManager,
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
                // Simulate network delay for now until Bucket API is ready
                delay(SIMULATED_DELAY_MS)

                // Basic validation for now
                if (name.contains("existing")) {
                    _uiState.update { it.copy(isChecking = false, error = "Device name unavailable") }
                } else {
                    _uiState.update { it.copy(isChecking = false, availabilityMessage = "Available!") }
                }
            }
        }

        fun onDeploy() {
            viewModelScope.launch {
                // Verify we have bootstrap credentials
                val credsResult = authRepository.getBootstrapCredentials()
                if (credsResult !is LocusResult.Success) {
                    _uiState.update { it.copy(error = "Missing bootstrap credentials") }
                    return@launch
                }

                // Enqueue Worker to ensure process survival ("The Trap")
                val workRequest =
                    OneTimeWorkRequestBuilder<ProvisioningWorker>()
                        .setInputData(
                            workDataOf(
                                ProvisioningWorker.KEY_MODE to ProvisioningWorker.MODE_PROVISION,
                                ProvisioningWorker.KEY_DEVICE_NAME to _uiState.value.deviceName,
                            ),
                        )
                        .build()

                workManager.enqueueUniqueWork(
                    AuthRepository.PROVISIONING_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    workRequest,
                )

                _event.send(NewDeviceEvent.NavigateToProvisioning)
            }
        }

        companion object {
            private const val SIMULATED_DELAY_MS = 500L
        }
    }
