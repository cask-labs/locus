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
import com.locus.android.features.onboarding.OnboardingNavigation
import com.locus.android.ui.theme.LocusTheme
import com.locus.core.domain.model.auth.AuthState
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

                    val isComplete =
                        onboardingStage ==
                            com.locus.core.domain.model.auth.OnboardingStage.COMPLETE
                    val isAuthenticated = authState == AuthState.Authenticated
                    val isPermissionsPending =
                        onboardingStage ==
                            com.locus.core.domain.model.auth.OnboardingStage.PERMISSIONS_PENDING

                    when {
                        isComplete && isAuthenticated -> {
                            DashboardScreen()
                        }
                        isPermissionsPending -> {
                            OnboardingNavigation(
                                startDestination =
                                    com.locus.android.features.onboarding.OnboardingDestinations.PERMISSIONS,
                            )
                        }
                        else -> {
                            OnboardingNavigation(
                                startDestination =
                                    com.locus.android.features.onboarding.OnboardingDestinations.WELCOME,
                            )
                        }
                    }
                }
            }
        }
    }
}
