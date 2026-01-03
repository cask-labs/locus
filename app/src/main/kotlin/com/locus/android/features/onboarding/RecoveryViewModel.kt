package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.BucketValidationStatus
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.usecase.RecoverAccountUseCase
import com.locus.core.domain.usecase.ScanBucketsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
        private val scanBucketsUseCase: ScanBucketsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecoveryUiState())
        val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

        val provisioningState: StateFlow<ProvisioningState> =
            authRepository.getProvisioningState()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = ProvisioningState.Idle,
                )

        fun loadBuckets() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val credsResult = authRepository.getBootstrapCredentials()
                if (credsResult is com.locus.core.domain.result.LocusResult.Success && credsResult.data != null) {
                    val result = scanBucketsUseCase(credsResult.data!!)
                    if (result is com.locus.core.domain.result.LocusResult.Success) {
                        // Transform List<Pair<String, Status>> to List<String> (valid buckets only)
                        val validBuckets =
                            result.data
                                .filter { it.second is BucketValidationStatus.Available }
                                .map { it.first }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                buckets = validBuckets,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to scan buckets",
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No credentials found",
                        )
                    }
                }
            }
        }

        fun recoverAccount(bucketName: String) {
            viewModelScope.launch {
                val credsResult = authRepository.getBootstrapCredentials()
                if (credsResult is com.locus.core.domain.result.LocusResult.Success && credsResult.data != null) {
                    recoverAccountUseCase(credsResult.data!!, bucketName)
                }
            }
        }
    }
