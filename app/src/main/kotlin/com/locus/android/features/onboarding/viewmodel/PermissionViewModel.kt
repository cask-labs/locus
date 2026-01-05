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

        @Suppress("UnusedPrivateMember")
        fun updatePermissions(
            fine: Boolean,
            coarse: Boolean,
            background: Boolean,
            notifications: Boolean,
        ) {
            val newState =
                when {
                    coarse && !fine -> PermissionUiState.CoarseLocationError
                    !fine -> {
                        if (_uiState.value == PermissionUiState.DeniedForever) {
                            PermissionUiState.DeniedForever
                        } else {
                            PermissionUiState.ForegroundPending
                        }
                    }
                    !background -> PermissionUiState.BackgroundPending
                    else -> PermissionUiState.Granted
                }
            _uiState.value = newState
        }

        fun onPermissionDenied(shouldShowRationale: Boolean) {
            if (!shouldShowRationale) {
                // If we shouldn't show rationale after a denial, it means "Don't ask again"
                // was checked or the permission is permanently denied.
                _uiState.value = PermissionUiState.DeniedForever
            } else {
                // If we should show rationale, we reset to Pending so the UI can show
                // the prompt/rationale again.
                // (Or we could have a specific 'Rationale' state, but ForegroundPending
                // with text is usually sufficient)
                // For now, staying in Pending allows the user to try again.
                // But we might want to track that a denial happened.
            }
        }

        fun completeOnboarding() {
            viewModelScope.launch {
                authRepository.setOnboardingStage(OnboardingStage.COMPLETE)
            }
        }
    }
