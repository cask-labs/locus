package com.locus.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        val authState: StateFlow<AuthState> =
            authRepository.getAuthState()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = AuthState.Uninitialized,
                )

        private val _onboardingStage = MutableStateFlow<OnboardingStage>(OnboardingStage.IDLE)
        val onboardingStage: StateFlow<OnboardingStage> = _onboardingStage.asStateFlow()

        init {
            refreshOnboardingStage()
        }

        fun refreshOnboardingStage() {
            viewModelScope.launch {
                _onboardingStage.value = authRepository.getOnboardingStage()
            }
        }

        fun completeOnboarding() {
            viewModelScope.launch {
                authRepository.setOnboardingStage(OnboardingStage.COMPLETE)
                _onboardingStage.value = OnboardingStage.COMPLETE
            }
        }

        suspend fun advanceToPermissions() {
            authRepository.setOnboardingStage(OnboardingStage.PERMISSIONS_PENDING)
            _onboardingStage.value = OnboardingStage.PERMISSIONS_PENDING
        }
    }
