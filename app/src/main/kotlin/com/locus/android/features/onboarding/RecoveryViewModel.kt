package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.usecase.RecoverAccountUseCase
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
        private val recoverAccountUseCase: RecoverAccountUseCase,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecoveryUiState())
        val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

        fun loadBuckets() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Mocking bucket loading for UI development
                // Real implementation will come in Task 10/11 (Actually ScanBucketsUseCase)
                // For now, let's keep the mock for buckets but handle selection.
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
                authRepository.setOnboardingStage(OnboardingStage.PROVISIONING)
                onSuccess()

                launchRecovery(bucketName)
            }
        }

        private fun launchRecovery(bucketName: String) {
            viewModelScope.launch {
                val credsResult = authRepository.getBootstrapCredentials()
                if (credsResult is com.locus.core.domain.result.LocusResult.Success && credsResult.data != null) {
                    recoverAccountUseCase(credsResult.data!!, bucketName)
                }
            }
        }

        companion object {
            private const val SIMULATED_DELAY_MS = 1000L
        }
    }
