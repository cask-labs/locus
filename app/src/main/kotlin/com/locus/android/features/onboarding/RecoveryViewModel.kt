package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.data.worker.ProvisioningWorker
import com.locus.core.domain.model.auth.OnboardingStage
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
                // Mock bucket loading for now
                delay(SIMULATED_DELAY_MS)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        buckets = listOf("locus-user-my-stack", "locus-user-test-stack"),
                    )
                }
            }
        }

        fun onBucketSelected(
            bucketName: String,
            onSuccess: () -> Unit,
        ) {
            viewModelScope.launch {
                // 1. Set stage to PROVISIONING to trap the user
                authRepository.setOnboardingStage(OnboardingStage.PROVISIONING)

                // 2. Start persistent background work
                authRepository.startProvisioning(
                    mode = ProvisioningWorker.MODE_RECOVERY,
                    param = bucketName,
                )

                onSuccess()
            }
        }

        companion object {
            private const val SIMULATED_DELAY_MS = 1000L
        }
    }
