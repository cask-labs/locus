package com.locus.android.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.AppVersion
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.GetAppVersionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val getAppVersionUseCase: GetAppVersionUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<LocusResult<AppVersion>?>(null)
        val uiState: StateFlow<LocusResult<AppVersion>?> = _uiState.asStateFlow()

        init {
            loadAppVersion()
        }

        private fun loadAppVersion() {
            viewModelScope.launch {
                _uiState.value = getAppVersionUseCase()
            }
        }
    }
