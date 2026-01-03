package com.locus.android.features.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.locus.android.features.onboarding.permissions.PermissionScreen
import com.locus.android.features.onboarding.provisioning.ProvisioningScreen
import com.locus.android.features.onboarding.provisioning.SuccessScreen
import com.locus.android.features.onboarding.ui.ChoiceScreen
import com.locus.android.features.onboarding.ui.CredentialEntryScreen
import com.locus.android.features.onboarding.ui.NewDeviceSetupScreen
import com.locus.android.features.onboarding.ui.RecoveryScreen
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
    onOnboardingComplete: () -> Unit = {},
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

            NewDeviceSetupScreen(
                uiState = state,
                onDeviceNameChanged = viewModel::onDeviceNameChanged,
                onCheckAvailability = viewModel::checkAvailability,
                onDeploy = {
                    viewModel.deployStack()
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
                onBucketSelected = { bucketName ->
                    viewModel.recoverAccount(bucketName)
                    navController.navigate(OnboardingDestinations.PROVISIONING)
                },
            )
        }

        composable(OnboardingDestinations.PROVISIONING) {
            // Re-using ViewModels?
            // NewDeviceViewModel and RecoveryViewModel both handle provisioning.
            // Ideally we'd have a shared ProvisioningViewModel or pass the context.
            // But Hilt scopes ViewModels to the graph entry or fragment.
            // If we use hiltViewModel() here, we get a NEW instance.
            // We need to share the view model from the previous screen or use a shared graph.
            // For simplicity in Phase 1, we can have a ProvisioningViewModel that observes the Repository State.
            // Since AuthRepository holds the state globally, a new ViewModel observing it is fine.

            // Note: We use NewDeviceViewModel here as a generic "Provisioning Observer"
            // because it exposes provisioningState. RecoveryViewModel does too.
            // Let's assume NewDeviceViewModel is sufficient to observe.

            ProvisioningScreen(
                onSuccess = {
                    navController.navigate(OnboardingDestinations.SUCCESS) {
                        popUpTo(OnboardingDestinations.PROVISIONING) { inclusive = true }
                    }
                },
            )
        }

        composable(OnboardingDestinations.SUCCESS) {
            SuccessScreen(
                onContinue = {
                    navController.navigate(OnboardingDestinations.PERMISSIONS) {
                        popUpTo(OnboardingDestinations.SUCCESS) { inclusive = true }
                    }
                },
            )
        }

        composable(OnboardingDestinations.PERMISSIONS) {
            PermissionScreen(
                onPermissionsGranted = onOnboardingComplete,
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
