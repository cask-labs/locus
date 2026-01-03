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

data class RecoveryUiState(
    val buckets: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RecoveryViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecoveryUiState())
        val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

        fun loadBuckets() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Mocking bucket loading for UI development
                // Real implementation will come in Task 10/11
                delay(SIMULATED_DELAY_MS)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        buckets = listOf("locus-user-my-stack", "locus-user-test-stack"),
                    )
                }
            }
        }

        companion object {
            private const val SIMULATED_DELAY_MS = 1000L
            private const val SIM_STEP_DELAY_1 = 1000L
            private const val SIM_STEP_DELAY_2 = 1500L
            private const val SIM_STEP_DELAY_3 = 2000L
        }

        fun recover(bucketName: String) {
            // NOTE: Temporary simulation for UI verification. Task 10 will replace this with actual Service start.
            viewModelScope.launch {
                authRepository.updateProvisioningState(ProvisioningState.Working("Connecting to bucket: $bucketName"))
                delay(SIM_STEP_DELAY_1)
                authRepository.updateProvisioningState(ProvisioningState.Working("Verifying stack tags..."))
                delay(SIM_STEP_DELAY_2)
                authRepository.updateProvisioningState(ProvisioningState.Working("Recovering identity..."))
                delay(SIM_STEP_DELAY_3)
                authRepository.updateProvisioningState(ProvisioningState.Success)
            }
        }
    }
