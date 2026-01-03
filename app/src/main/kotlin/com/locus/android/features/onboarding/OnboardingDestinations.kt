package com.locus.android.features.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.locus.android.features.onboarding.ui.ChoiceScreen
import com.locus.android.features.onboarding.ui.CredentialEntryScreen
import com.locus.android.features.onboarding.ui.NewDeviceSetupScreen
import com.locus.android.features.onboarding.ui.PermissionScreen
import com.locus.android.features.onboarding.ui.ProvisioningScreen
import com.locus.android.features.onboarding.ui.RecoveryScreen
import com.locus.android.features.onboarding.ui.SuccessScreen
import com.locus.android.features.onboarding.ui.WelcomeScreen

object OnboardingDestinations {
    const val WELCOME = "welcome"
    const val CREDENTIALS = "credentials"
    const val CHOICE = "choice"
    const val NEW_DEVICE = "new_device"
    const val RECOVERY = "recovery"
    const val PROVISIONING = "provisioning"
    const val SUCCESS = "success"
    const val PERMISSIONS = "permissions"
}

@Composable
fun OnboardingNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = OnboardingDestinations.WELCOME,
    @Suppress("UnusedParameter") onOnboardingComplete: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(OnboardingDestinations.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(OnboardingDestinations.CREDENTIALS) },
            )
        }

        composable(OnboardingDestinations.CREDENTIALS) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            CredentialsRoute(
                viewModel = viewModel,
                state = state,
                navController = navController,
            )
        }

        addSelectionRoutes(navController)

        composable(OnboardingDestinations.PROVISIONING) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            val provisioningState by viewModel.provisioningState.collectAsState()

            ProvisioningScreen(
                state = provisioningState,
                onSuccess = {
                    navController.navigate(OnboardingDestinations.SUCCESS)
                },
            )
        }

        composable(OnboardingDestinations.SUCCESS) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            SuccessScreen(
                onContinue = {
                    viewModel.acknowledgeSuccess()
                    navController.navigate(OnboardingDestinations.PERMISSIONS)
                },
            )
        }

        composable(OnboardingDestinations.PERMISSIONS) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            PermissionScreen(
                onPermissionsGranted = {
                    viewModel.completeOnboarding()
                    // No navigation needed, MainActivity should switch to Dashboard
                },
            )
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.addSelectionRoutes(navController: NavHostController) {
    composable(OnboardingDestinations.CHOICE) {
        ChoiceScreen(
            onNewDevice = { navController.navigate(OnboardingDestinations.NEW_DEVICE) },
            onRecovery = { navController.navigate(OnboardingDestinations.RECOVERY) },
        )
    }

    composable(OnboardingDestinations.NEW_DEVICE) {
        val viewModel: NewDeviceViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsState()

        NewDeviceSetupScreen(
            uiState = state,
            onDeviceNameChanged = viewModel::onDeviceNameChanged,
            onCheckAvailability = viewModel::checkAvailability,
            onDeploy = {
                viewModel.deploy()
                navController.navigate(OnboardingDestinations.PROVISIONING)
            },
        )
    }

    composable(OnboardingDestinations.RECOVERY) {
        val viewModel: RecoveryViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsState()

        RecoveryScreen(
            uiState = state,
            onLoadBuckets = viewModel::loadBuckets,
            onBucketSelected = {
                viewModel.recover(it)
                navController.navigate(OnboardingDestinations.PROVISIONING)
            },
        )
    }
}

@Composable
fun CredentialsRoute(
    viewModel: OnboardingViewModel,
    state: OnboardingUiState,
    navController: NavHostController,
) {
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is OnboardingEvent.CredentialsValidated -> {
                    navController.navigate(OnboardingDestinations.CHOICE)
                }
                is OnboardingEvent.NavigateTo -> {
                    navController.navigate(event.route)
                }
            }
        }
    }

    CredentialEntryScreen(
        uiState = state,
        onAccessKeyIdChanged = viewModel::onAccessKeyIdChanged,
        onSecretAccessKeyChanged = viewModel::onSecretAccessKeyChanged,
        onSessionTokenChanged = viewModel::onSessionTokenChanged,
        onPasteJson = viewModel::pasteJson,
        onValidate = viewModel::validateCredentials,
    )
}
