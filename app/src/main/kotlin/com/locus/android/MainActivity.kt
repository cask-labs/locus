package com.locus.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.locus.android.features.dashboard.DashboardScreen
import com.locus.android.features.onboarding.OnboardingDestinations
import com.locus.android.features.onboarding.onboardingGraph
import com.locus.android.ui.theme.LocusTheme
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val authState by authRepository.getAuthState().collectAsState(initial = AuthState.Uninitialized)
                    val navController = rememberNavController()

                    val startDestination = when (authState) {
                        AuthState.Authenticated -> "dashboard"
                        else -> OnboardingDestinations.ROUTE
                    }

                    NavHost(navController = navController, startDestination = startDestination) {
                        onboardingGraph(
                            navController = navController,
                            onOnboardingComplete = {
                                // Task 10 will handle more complex provisioning transition
                                // For now, we assume success leads to Dashboard
                                navController.navigate("dashboard") {
                                    popUpTo(OnboardingDestinations.ROUTE) { inclusive = true }
                                }
                            }
                        )

                        composable("dashboard") {
                            DashboardScreen()
                        }
                    }
                }
            }
        }
    }
}
