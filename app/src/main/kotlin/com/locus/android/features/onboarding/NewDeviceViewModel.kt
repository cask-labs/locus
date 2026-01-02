package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewDeviceUiState(
    val deviceName: String = "",
    val isChecking: Boolean = false,
    val isAvailable: Boolean? = null,
    val error: String? = null
)

@HiltViewModel
class NewDeviceViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(NewDeviceUiState())
    val uiState: StateFlow<NewDeviceUiState> = _uiState.asStateFlow()

    private val nameRegex = Regex("^[a-z0-9-]+$")

    fun updateDeviceName(name: String) {
        val isValidFormat = nameRegex.matches(name) || name.isEmpty()
        if (isValidFormat) {
            _uiState.value = _uiState.value.copy(deviceName = name, error = null, isAvailable = null)
        } else {
             // Optionally show error immediately or just block input.
             // Requirement says "Checks regex (lowercase, alphanumeric, hyphens)".
             // We'll block invalid input updates or show error. Let's just update and validate on check for now,
             // but strictly sticking to regex for input is better user experience if we want to prevent invalid chars.
             // For now, let's just not update the state if it violates regex, effectively blocking it, but allowing empty.
             // Actually, it's better to allow typing and show error.
             _uiState.value = _uiState.value.copy(deviceName = name, error = "Use lowercase, numbers, and hyphens only")
        }
    }

    fun checkAvailability() {
        val name = _uiState.value.deviceName
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Device name cannot be empty")
            return
        }

        if (!nameRegex.matches(name)) {
            _uiState.value = _uiState.value.copy(error = "Invalid format")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, error = null)
            // TODO: Implement actual availability check using S3/CloudFormation checks if needed.
            // For Phase 1 Task 8, we mock/stub this as per plan.
            // "Mock/Stub for now, or actual call if Repo ready"

            // Simulating network delay
            kotlinx.coroutines.delay(500)

            _uiState.value = _uiState.value.copy(
                isChecking = false,
                isAvailable = true // Mocked success
            )
        }
    }
}
