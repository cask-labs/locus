package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.locus.android.features.onboarding.work.ProvisioningWorker
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecoveryUiState(
    val buckets: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class RecoveryEvent {
    data object NavigateToProvisioning : RecoveryEvent()
}

@HiltViewModel
class RecoveryViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val workManager: WorkManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecoveryUiState())
        val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

        private val _event = Channel<RecoveryEvent>(Channel.BUFFERED)
        val event = _event.receiveAsFlow()

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

        fun onBucketSelected(bucketName: String) {
            viewModelScope.launch {
                val credsResult = authRepository.getBootstrapCredentials()
                if (credsResult !is LocusResult.Success) {
                    _uiState.update { it.copy(error = "Missing bootstrap credentials") }
                    return@launch
                }

                // Enqueue Worker to ensure process survival ("The Trap")
                val workRequest =
                    OneTimeWorkRequestBuilder<ProvisioningWorker>()
                        .setInputData(
                            workDataOf(
                                ProvisioningWorker.KEY_MODE to ProvisioningWorker.MODE_RECOVER,
                                ProvisioningWorker.KEY_BUCKET_NAME to bucketName,
                            ),
                        )
                        .build()

                workManager.enqueueUniqueWork(
                    AuthRepository.PROVISIONING_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    workRequest,
                )

                _event.send(RecoveryEvent.NavigateToProvisioning)
            }
        }

        companion object {
            private const val SIMULATED_DELAY_MS = 1000L
        }
    }
