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

                    // Routing Logic:
                    // 1. Authenticated + Complete -> Dashboard
                    // 2. Authenticated + NOT Complete (Permissions Pending) -> Permission Screen
                    // 3. Unauthenticated -> Onboarding (Welcome)

                    if (authState == AuthState.Authenticated && onboardingStage == OnboardingStage.COMPLETE) {
                        DashboardScreen()
                    } else if (onboardingStage == OnboardingStage.PERMISSIONS_PENDING) {
                        // The trap: Even if authenticated, force permissions
                        OnboardingNavigation(
                            mainViewModel = viewModel,
                            startDestination = OnboardingDestinations.PERMISSIONS,
                            onOnboardingComplete = { viewModel.completeOnboarding() },
                        )
                    } else {
                        // Default flow (Welcome)
                        // If we are Provisioning, we might want to jump there, but usually we just start at Welcome
                        // and let the state/user navigate, or if we have SetupPending we might need logic.
                        // But for now, standard OnboardingNavigation logic applies.
                        OnboardingNavigation(
                            mainViewModel = viewModel,
                            onOnboardingComplete = { viewModel.completeOnboarding() },
                        )
                    }
                }
            }
        }
    }
}
