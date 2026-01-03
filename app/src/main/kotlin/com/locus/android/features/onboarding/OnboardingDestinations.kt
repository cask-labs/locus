package com.locus.android.features.onboarding

import androidx.compose.runtime.Composable
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

        composable(OnboardingDestinations.CHOICE) {
            ChoiceScreen(
                onNewDevice = { navController.navigate(OnboardingDestinations.NEW_DEVICE) },
                onRecovery = { navController.navigate(OnboardingDestinations.RECOVERY) },
            )
        }

        composable(OnboardingDestinations.NEW_DEVICE) {
            val viewModel: NewDeviceViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            // Observe navigation events from ViewModel
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.event.collect { event ->
                    if (event is NewDeviceEvent.NavigateToProvisioning) {
                        navController.navigate(OnboardingDestinations.PROVISIONING)
                    }
                }
            }

            NewDeviceSetupScreen(
                uiState = state,
                onDeviceNameChanged = viewModel::onDeviceNameChanged,
                onCheckAvailability = viewModel::checkAvailability,
                onDeploy = viewModel::onDeploy,
            )
        }

        composable(OnboardingDestinations.RECOVERY) {
            val viewModel: RecoveryViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            // Observe navigation events
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.event.collect { event ->
                    if (event is RecoveryEvent.NavigateToProvisioning) {
                        navController.navigate(OnboardingDestinations.PROVISIONING)
                    }
                }
            }

            RecoveryScreen(
                uiState = state,
                onLoadBuckets = viewModel::loadBuckets,
                onBucketSelected = viewModel::onBucketSelected,
            )
        }

        composable(OnboardingDestinations.PROVISIONING) {
            val viewModel: ProvisioningViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            ProvisioningScreen(
                state = state,
                onSuccess = {
                    viewModel.markSuccess()
                    navController.navigate(OnboardingDestinations.SUCCESS) {
                        popUpTo(OnboardingDestinations.CHOICE) { inclusive = true }
                    }
                },
            )
        }

        composable(OnboardingDestinations.SUCCESS) {
            val viewModel: ProvisioningViewModel = hiltViewModel()
            SuccessScreen(
                onContinue = {
                    viewModel.advanceToPermissions()
                    navController.navigate(OnboardingDestinations.PERMISSIONS) {
                        popUpTo(OnboardingDestinations.SUCCESS) { inclusive = true }
                    }
                },
            )
        }

        composable(OnboardingDestinations.PERMISSIONS) {
            val viewModel: ProvisioningViewModel = hiltViewModel()
            PermissionScreen(
                onPermissionsGranted = {
                    viewModel.completeOnboarding()
                    // MainActivity should handle the route change to Dashboard via state observation,
                    // but we can also pop back stack here to be safe.
                    // The main activity routing logic will swap the nav graph entirely.
                },
            )
        }
    }
}

@Composable
fun CredentialsRoute(
    viewModel: OnboardingViewModel,
    state: OnboardingUiState,
    navController: NavHostController,
) {
    // Handling success transition manually for now based on a hypothetical event or state change.
    // In a real app we'd use Flow collection for events.
    androidx.compose.runtime.LaunchedEffect(Unit) {
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
