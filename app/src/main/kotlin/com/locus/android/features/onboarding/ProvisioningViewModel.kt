package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProvisioningUiState(
    val history: List<String> = emptyList(),
    val currentStep: String? = null,
    val error: String? = null,
    val isComplete: Boolean = false,
)

@HiltViewModel
class ProvisioningViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
    ) : ViewModel() {
        val uiState: StateFlow<ProvisioningUiState> =
            authRepository.getProvisioningState()
                .combine(MutableStateFlow(Unit)) { state, _ ->
                    when (state) {
                        is ProvisioningState.Idle -> ProvisioningUiState()
                        is ProvisioningState.Working ->
                            ProvisioningUiState(
                                history = state.history,
                                currentStep = state.currentStep,
                            )
                        is ProvisioningState.Success ->
                            ProvisioningUiState(
                                isComplete = true,
                            )
                        is ProvisioningState.Failure ->
                            ProvisioningUiState(
                                error = state.error.message ?: "Unknown error",
                            )
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                    initialValue = ProvisioningUiState(),
                )

        companion object {
            private const val TIMEOUT_MILLIS = 5000L
        }
    }
