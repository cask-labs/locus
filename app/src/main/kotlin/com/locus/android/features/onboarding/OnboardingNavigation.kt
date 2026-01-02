package com.locus.android.features.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.locus.android.features.onboarding.ui.ChoiceScreen
import com.locus.android.features.onboarding.ui.CredentialEntryScreen
import com.locus.android.features.onboarding.ui.NewDeviceSetupScreen
import com.locus.android.features.onboarding.ui.RecoveryScreen
import com.locus.android.features.onboarding.ui.WelcomeScreen

object OnboardingDestinations {
    const val ROUTE = "onboarding_route"
    const val WELCOME = "welcome"
    const val CREDENTIALS = "credentials"
    const val CHOICE = "choice"
    const val NEW_DEVICE = "new_device"
    const val RECOVERY = "recovery"
}

fun NavGraphBuilder.onboardingGraph(
    navController: NavController,
    onOnboardingComplete: () -> Unit // Navigate to Dashboard or Provisioning Status
) {
    navigation(
        route = OnboardingDestinations.ROUTE,
        startDestination = OnboardingDestinations.WELCOME
    ) {
        composable(OnboardingDestinations.WELCOME) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(OnboardingDestinations.CREDENTIALS)
                }
            )
        }

        composable(OnboardingDestinations.CREDENTIALS) {
            CredentialEntryScreen(
                onCredentialsValid = {
                    navController.navigate(OnboardingDestinations.CHOICE)
                }
            )
        }

        composable(OnboardingDestinations.CHOICE) {
            ChoiceScreen(
                onNewDevice = {
                    navController.navigate(OnboardingDestinations.NEW_DEVICE)
                },
                onRecovery = {
                    navController.navigate(OnboardingDestinations.RECOVERY)
                }
            )
        }

        composable(OnboardingDestinations.NEW_DEVICE) {
            NewDeviceSetupScreen(
                onDeploy = {
                    // Start provisioning logic (Task 10)
                    // For now, we consider this "Input flow complete" and maybe go to a placeholder
                    onOnboardingComplete()
                }
            )
        }

        composable(OnboardingDestinations.RECOVERY) {
            RecoveryScreen(
                onBucketSelected = { bucketName ->
                     // Start recovery/linking logic (Task 10)
                     onOnboardingComplete()
                }
            )
        }
    }
}
