package com.locus.android.features.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.locus.android.MainViewModel
import com.locus.android.features.onboarding.permissions.PermissionScreen
import com.locus.android.features.onboarding.provisioning.ProvisioningScreen
import com.locus.android.features.onboarding.provisioning.SuccessScreen
import com.locus.android.features.onboarding.ui.ChoiceScreen
import com.locus.android.features.onboarding.ui.CredentialEntryScreen
import com.locus.android.features.onboarding.ui.NewDeviceSetupScreen
import com.locus.android.features.onboarding.ui.RecoveryScreen
import com.locus.android.features.onboarding.ui.WelcomeScreen
import kotlinx.coroutines.launch

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
    mainViewModel: MainViewModel,
    navController: NavHostController = rememberNavController(),
    startDestination: String = OnboardingDestinations.WELCOME,
    onOnboardingComplete: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        addWelcomeScreen(navController)
        addCredentialsScreen(navController)
        addChoiceScreen(navController)
        addNewDeviceScreen(navController)
        addRecoveryScreen(navController)
        addProvisioningScreen(navController)
        addSuccessScreen(navController, mainViewModel)
        addPermissionsScreen(onOnboardingComplete)
    }
}

private fun NavGraphBuilder.addWelcomeScreen(navController: NavHostController) {
    composable(OnboardingDestinations.WELCOME) {
        WelcomeScreen(
            onGetStarted = { navController.navigate(OnboardingDestinations.CREDENTIALS) },
        )
    }
}

private fun NavGraphBuilder.addCredentialsScreen(navController: NavHostController) {
    composable(OnboardingDestinations.CREDENTIALS) {
        val viewModel: OnboardingViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsState()

        CredentialsRoute(
            viewModel = viewModel,
            state = state,
            navController = navController,
        )
    }
}

private fun NavGraphBuilder.addChoiceScreen(navController: NavHostController) {
    composable(OnboardingDestinations.CHOICE) {
        ChoiceScreen(
            onNewDevice = { navController.navigate(OnboardingDestinations.NEW_DEVICE) },
            onRecovery = { navController.navigate(OnboardingDestinations.RECOVERY) },
        )
    }
}

private fun NavGraphBuilder.addNewDeviceScreen(navController: NavHostController) {
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
}

private fun NavGraphBuilder.addRecoveryScreen(navController: NavHostController) {
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
}

private fun NavGraphBuilder.addProvisioningScreen(navController: NavHostController) {
    composable(OnboardingDestinations.PROVISIONING) {
        ProvisioningScreen(
            onSuccess = {
                navController.navigate(OnboardingDestinations.SUCCESS) {
                    popUpTo(OnboardingDestinations.PROVISIONING) { inclusive = true }
                }
            },
        )
    }
}

private fun NavGraphBuilder.addSuccessScreen(
    navController: NavHostController,
    mainViewModel: MainViewModel,
) {
    composable(OnboardingDestinations.SUCCESS) {
        val scope = rememberCoroutineScope()
        SuccessScreen(
            onContinue = {
                scope.launch {
                    mainViewModel.advanceToPermissions()
                    navController.navigate(OnboardingDestinations.PERMISSIONS) {
                        popUpTo(OnboardingDestinations.SUCCESS) { inclusive = true }
                    }
                }
            },
        )
    }
}

private fun NavGraphBuilder.addPermissionsScreen(onOnboardingComplete: () -> Unit) {
    composable(OnboardingDestinations.PERMISSIONS) {
        PermissionScreen(
            onPermissionsGranted = onOnboardingComplete,
        )
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
