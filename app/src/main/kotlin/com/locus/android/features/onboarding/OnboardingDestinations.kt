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
            CredentialsRoute(navController = navController)
        }

        composable(OnboardingDestinations.CHOICE) {
            ChoiceScreen(
                onNewDevice = { navController.navigate(OnboardingDestinations.NEW_DEVICE) },
                onRecovery = { navController.navigate(OnboardingDestinations.RECOVERY) },
            )
        }

        composable(OnboardingDestinations.NEW_DEVICE) {
            NewDeviceRoute(navController = navController)
        }

        composable(OnboardingDestinations.RECOVERY) {
            RecoveryRoute(navController = navController)
        }

        composable(OnboardingDestinations.PROVISIONING) {
            ProvisioningRoute(navController = navController)
        }

        composable(OnboardingDestinations.SUCCESS) {
            SuccessScreen(
                onContinue = { navController.navigate(OnboardingDestinations.PERMISSIONS) },
            )
        }

        composable(OnboardingDestinations.PERMISSIONS) {
            PermissionsRoute(
                onOnboardingComplete = onOnboardingComplete,
            )
        }
    }
}

@Composable
fun CredentialsRoute(
    navController: NavHostController,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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

@Composable
fun NewDeviceRoute(
    navController: NavHostController,
    viewModel: NewDeviceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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

@Composable
fun RecoveryRoute(
    navController: NavHostController,
    viewModel: RecoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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

@Composable
fun ProvisioningRoute(
    navController: NavHostController,
    viewModel: ProvisioningViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            navController.navigate(OnboardingDestinations.SUCCESS) {
                popUpTo(OnboardingDestinations.CHOICE) { inclusive = true }
            }
        }
    }

    ProvisioningScreen(state = state)
}

@Composable
fun PermissionsRoute(
    onOnboardingComplete: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(state.isGranted) {
        if (state.isGranted) {
            onOnboardingComplete()
        }
    }

    PermissionScreen(
        state = state,
        onOpenSettings = viewModel::openSettings,
        onCheckPermissions = viewModel::checkPermissions,
    )
}
