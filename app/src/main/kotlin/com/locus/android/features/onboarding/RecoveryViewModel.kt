package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
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
        private val authRepository: AuthRepository,
        private val recoverAccountUseCase: RecoverAccountUseCase,
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
        }

        fun recover(bucketName: String) {
            viewModelScope.launch {
                val bootstrapResult = authRepository.getBootstrapCredentials()
                if (bootstrapResult is LocusResult.Failure) {
                    return@launch
                }
                val creds = (bootstrapResult as LocusResult.Success).data

                authRepository.setOnboardingStage(com.locus.core.domain.model.auth.OnboardingStage.PROVISIONING)

                // Trigger the use case
                recoverAccountUseCase(creds, bucketName)
            }
        }
    }
