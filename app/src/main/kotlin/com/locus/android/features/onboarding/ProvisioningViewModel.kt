package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProvisioningViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        val uiState: StateFlow<ProvisioningState> =
            authRepository.getProvisioningState()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = ProvisioningState.Idle,
                )

        companion object {
            private const val STOP_TIMEOUT_MILLIS = 5000L
        }

        fun markSuccess() {
            viewModelScope.launch {
                authRepository.setOnboardingStage(OnboardingStage.PERMISSIONS_PENDING)
            }
        }

        fun advanceToPermissions() {
            // Already handled in markSuccess, but redundant call to ensure stage consistency before navigation
            viewModelScope.launch {
                authRepository.setOnboardingStage(OnboardingStage.PERMISSIONS_PENDING)
            }
        }

        fun completeOnboarding() {
            viewModelScope.launch {
                authRepository.setOnboardingStage(OnboardingStage.COMPLETE)
                // Navigation to dashboard is handled by MainActivity observing OnboardingStage
            }
        }
    }
