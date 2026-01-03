package com.locus.android.features.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionUiState(
    val hasForeground: Boolean = false,
    val hasBackground: Boolean = false,
    val shouldShowRationale: Boolean = false,
    val isPermanentlyDenied: Boolean = false,
    val isGranted: Boolean = false,
)

@HiltViewModel
class PermissionViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PermissionUiState())
        val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

        init {
            checkPermissions()
            // Set persistent stage to verify we are in the trap
            viewModelScope.launch {
                authRepository.setOnboardingStage(OnboardingStage.PERMISSIONS_PENDING)
            }
        }

        fun checkPermissions() {
            val hasForeground =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

            val hasBackground =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true // Background location permission not separate before Android 10
                }

            _uiState.update {
                it.copy(
                    hasForeground = hasForeground,
                    hasBackground = hasBackground,
                    isGranted = hasForeground && hasBackground,
                )
            }
        }

        fun openSettings() {
            val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        }
    }
