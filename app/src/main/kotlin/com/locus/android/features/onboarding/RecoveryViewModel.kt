package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.locus.android.features.onboarding.work.ProvisioningWorker
import com.locus.core.domain.model.auth.BucketValidationStatus
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.ScanBucketsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
        private val workManager: WorkManager,
        private val scanBucketsUseCase: ScanBucketsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecoveryUiState())
        val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

        fun loadBuckets() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val credsResult = authRepository.getBootstrapCredentials()
                if (credsResult is LocusResult.Failure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = credsResult.error.message ?: "Missing bootstrap credentials",
                        )
                    }
                    return@launch
                }
                val creds = (credsResult as LocusResult.Success).data

                val scanResult = scanBucketsUseCase(creds)
                when (scanResult) {
                    is LocusResult.Success -> {
                        val validBuckets =
                            scanResult.data
                                .filter { it.second is BucketValidationStatus.Available }
                                .map { it.first }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                buckets = validBuckets,
                                error = if (validBuckets.isEmpty()) "No valid Locus buckets found." else null,
                            )
                        }
                    }
                    is LocusResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = scanResult.error.message ?: "Failed to scan buckets",
                            )
                        }
                    }
                }
            }
        }

        fun recover(bucketName: String) {
            viewModelScope.launch {
                authRepository.setOnboardingStage(OnboardingStage.PROVISIONING)

                val workRequest =
                    OneTimeWorkRequest.Builder(ProvisioningWorker::class.java)
                        .setInputData(
                            workDataOf(
                                ProvisioningWorker.KEY_MODE to ProvisioningWorker.MODE_RECOVER,
                                ProvisioningWorker.KEY_BUCKET_NAME to bucketName,
                            ),
                        )
                        .build()

                workManager.enqueueUniqueWork(
                    ProvisioningWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest,
                )
            }
        }
    }
