package com.locus.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.locus.android.features.dashboard.DashboardScreen
import com.locus.android.features.onboarding.OnboardingDestinations
import com.locus.android.features.onboarding.OnboardingNavigation
import com.locus.android.ui.theme.LocusTheme
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.OnboardingStage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val authState by viewModel.authState.collectAsState()
                    val onboardingStage by viewModel.onboardingStage.collectAsState()

                    val isComplete = onboardingStage == OnboardingStage.COMPLETE
                    val isAuthenticated = authState == AuthState.Authenticated
                    val isPermissionsPending = onboardingStage == OnboardingStage.PERMISSIONS_PENDING
                    val isProvisioning = onboardingStage == OnboardingStage.PROVISIONING

                    when {
                        isProvisioning -> {
                            OnboardingNavigation(
                                startDestination = OnboardingDestinations.PROVISIONING,
                            )
                        }
                        isPermissionsPending -> {
                            OnboardingNavigation(
                                startDestination = OnboardingDestinations.PERMISSIONS,
                            )
                        }
                        isComplete && isAuthenticated -> {
                            DashboardScreen()
                        }
                        else -> {
                            OnboardingNavigation(
                                startDestination = OnboardingDestinations.WELCOME,
                            )
                        }
                    }
                }
            }
        }
    }
}
