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

                    // If we are authenticated, we check if onboarding is complete or pending permissions.
                    // If not authenticated, we go to Onboarding.

                    when (authState) {
                        AuthState.Uninitialized, AuthState.SetupPending -> {
                            val start =
                                if (onboardingStage == OnboardingStage.PROVISIONING) {
                                    OnboardingDestinations.PROVISIONING
                                } else {
                                    OnboardingDestinations.WELCOME
                                }

                            OnboardingNavigation(
                                startDestination = start,
                                onOnboardingComplete = { viewModel.completeOnboarding() },
                            )
                        }
                        AuthState.Authenticated -> {
                            if (onboardingStage == OnboardingStage.PERMISSIONS_PENDING) {
                                // Trap: Force user to permissions screen
                                OnboardingNavigation(
                                    startDestination = OnboardingDestinations.PERMISSIONS,
                                    onOnboardingComplete = { viewModel.completeOnboarding() },
                                )
                            } else {
                                // Given the "Trap" requirement, we should probably check if permissions are granted.
                                // But for now, let's default to Dashboard if not explicitly PENDING.
                                DashboardScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
