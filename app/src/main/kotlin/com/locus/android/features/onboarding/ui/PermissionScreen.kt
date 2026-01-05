@file:Suppress("TooManyFunctions")

package com.locus.android.features.onboarding.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.locus.android.R
import com.locus.android.features.onboarding.viewmodel.PermissionUiState
import com.locus.android.features.onboarding.viewmodel.PermissionViewModel

private const val TAG = "PermissionScreen"

@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    CheckPermissionsEffect(context, viewModel)

    val activity = LocalContext.current as? android.app.Activity
    val foregroundLauncher =
        rememberForegroundLauncher(context, activity, viewModel)
    val backgroundLauncher =
        rememberBackgroundLauncher(context, activity, viewModel)

    PermissionContent(
        uiState = uiState,
        foregroundLauncher = foregroundLauncher,
        backgroundLauncher = backgroundLauncher,
        onComplete = {
            viewModel.completeOnboarding()
            onPermissionsGranted()
        },
        onOpenSettings = { openAppSettings(context) },
    )
}

@Composable
private fun CheckPermissionsEffect(
    context: Context,
    viewModel: PermissionViewModel,
) {
    fun checkPermissions() {
        val fine = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        val bg =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        val notif =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true
            }

        viewModel.updatePermissions(fine, coarse, bg, notif)
    }

    LaunchedEffect(Unit) { checkPermissions() }
    ObserveLifecycleResume { checkPermissions() }
}

@Composable
private fun rememberForegroundLauncher(
    context: Context,
    activity: android.app.Activity?,
    viewModel: PermissionViewModel,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
    onResult = {
        // Trigger check via ViewModel update (UI will recompose and call checkPermissions via effect
        // or we can manually call updatePermissions here.
        // Actually, logic is cleaner if we just re-evaluate permissions.
        // But since we can't easily call checkPermissions from here without passing it down,
        // let's replicate the check logic or rely on onResume if system dialog dismissed.
        // Replicating logic for immediate feedback:
        val fine = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (!fine) {
            val showRationale =
                activity?.shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == true
            if (!showRationale) {
                viewModel.onPermissionDenied(isPermanentlyDenied = true)
            }
        }
        // Also trigger general update to refresh state
        val coarse = hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        viewModel.updatePermissions(fine, coarse, false, false) // BG/Notif status unknown but fine check is primary
    },
)

@Composable
private fun rememberBackgroundLauncher(
    context: Context,
    activity: android.app.Activity?,
    viewModel: PermissionViewModel,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = {
        val bg =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        if (!bg) {
            val showRationale =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity?.shouldShowRequestPermissionRationale(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    ) == true
                } else {
                    false
                }
            if (!showRationale) {
                viewModel.onPermissionDenied(isPermanentlyDenied = true)
            }
        }
        // Refresh
        viewModel.updatePermissions(true, true, bg, true) // Assuming FG/Notif granted if we are here
    },
)

private fun hasPermission(
    context: Context,
    permission: String,
) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

@Composable
private fun PermissionContent(
    uiState: PermissionUiState,
    foregroundLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    backgroundLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onComplete: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.onboarding_permission_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState) {
            PermissionUiState.ForegroundPending ->
                ForegroundPermissionContent(foregroundLauncher)
            PermissionUiState.BackgroundPending ->
                BackgroundPermissionContent(
                    backgroundLauncher,
                    onLaunchError = onOpenSettings,
                )
            PermissionUiState.Granted ->
                SimplePermissionMessage(
                    messageId = R.string.onboarding_permission_granted_all,
                    buttonTextId = R.string.onboarding_permission_go_dashboard,
                    onButtonClick = onComplete,
                )
            PermissionUiState.DeniedForever ->
                SimplePermissionMessage(
                    messageId = R.string.onboarding_permission_denied_message,
                    buttonTextId = R.string.onboarding_permission_open_settings,
                    onButtonClick = onOpenSettings,
                    isError = true,
                )
            PermissionUiState.CoarseLocationError ->
                SimplePermissionMessage(
                    messageId = R.string.onboarding_permission_coarse_error,
                    buttonTextId = R.string.onboarding_permission_open_settings,
                    onButtonClick = onOpenSettings,
                    isError = true,
                )
        }
    }
}

@Composable
private fun SimplePermissionMessage(
    messageId: Int,
    buttonTextId: Int,
    onButtonClick: () -> Unit,
    isError: Boolean = false,
) {
    Text(
        text = stringResource(id = messageId),
        textAlign = TextAlign.Center,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        style = if (isError) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(24.dp))
    if (isError) {
        OutlinedButton(onClick = onButtonClick) {
            Text(stringResource(id = buttonTextId))
        }
    } else {
        Button(onClick = onButtonClick) {
            Text(stringResource(id = buttonTextId))
        }
    }
}

@Composable
fun ObserveLifecycleResume(onResume: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    onResume()
                }
            }
        lifecycle.addObserver(observer)
    }
}

@Composable
fun ForegroundPermissionContent(launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>) {
    Text(
        text = stringResource(id = R.string.onboarding_permission_foreground_rationale),
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = {
            val permissions =
                mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            launcher.launch(permissions.toTypedArray())
        },
    ) {
        Text(stringResource(id = R.string.onboarding_permission_grant_foreground))
    }
}

@Composable
fun BackgroundPermissionContent(
    launcher: ManagedActivityResultLauncher<String, Boolean>,
    onLaunchError: () -> Unit,
) {
    Text(
        text = stringResource(id = R.string.onboarding_permission_background_rationale),
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(id = R.string.onboarding_permission_background_instruction),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    launcher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "Activity not found", e)
                    onLaunchError()
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception", e)
                    onLaunchError()
                }
            } else {
                launcher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        },
    ) {
        Text(stringResource(id = R.string.onboarding_permission_grant_background))
    }
}

private fun openAppSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    context.startActivity(intent)
}
