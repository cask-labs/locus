package com.locus.android.features.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PermissionUiState {
    data object ForegroundPending : PermissionUiState()

    data object BackgroundPending : PermissionUiState()

    data object DeniedForever : PermissionUiState()

    data object Granted : PermissionUiState()

    data object CoarseLocationError : PermissionUiState()
}

@HiltViewModel
class PermissionViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow<PermissionUiState>(PermissionUiState.ForegroundPending)
        val uiState = _uiState.asStateFlow()

        fun updatePermissions(
            fine: Boolean,
            coarse: Boolean,
            background: Boolean,
            @Suppress("UnusedParameter") notifications: Boolean,
        ) {
            val newState =
                when {
                    coarse && !fine -> PermissionUiState.CoarseLocationError
                    !fine -> PermissionUiState.ForegroundPending
                    !background -> PermissionUiState.BackgroundPending
                    else -> PermissionUiState.Granted
                }
            _uiState.value = newState
        }

        fun onPermissionDenied(isPermanentlyDenied: Boolean) {
            if (isPermanentlyDenied) {
                _uiState.value = PermissionUiState.DeniedForever
            }
        }

        fun completeOnboarding() {
            viewModelScope.launch {
                authRepository.setOnboardingStage(OnboardingStage.COMPLETE)
            }
        }
    }
