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
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    fun checkPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        viewModel.updatePermissions(
            fine = fineLocation,
            coarse = coarseLocation,
            background = backgroundLocation,
            notifications = notifications
        )
    }

    // Initial check and observe lifecycle
    LaunchedEffect(Unit) {
        checkPermissions()
    }
    ObserveLifecycleResume {
        checkPermissions()
    }

    // Launchers
    val activity = LocalContext.current as? android.app.Activity
    val foregroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (!granted) {
                val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true
                viewModel.onPermissionDenied(shouldShowRationale)
            }
            checkPermissions()
        }
    )

    val backgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                     activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == true
                } else {
                    false
                }
                viewModel.onPermissionDenied(shouldShowRationale)
            }
            checkPermissions()
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.onboarding_permission_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState) {
            PermissionUiState.ForegroundPending -> {
                ForegroundPermissionContent(foregroundLauncher)
            }
            PermissionUiState.BackgroundPending -> {
                BackgroundPermissionContent(
                    backgroundLauncher,
                    onLaunchError = { openAppSettings(context) }
                )
            }
            PermissionUiState.Granted -> {
                Text(
                    text = "All required permissions granted!",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.completeOnboarding()
                        onPermissionsGranted()
                    }
                ) {
                    Text("Go to Dashboard")
                }
            }
            PermissionUiState.DeniedForever -> {
                RationaleContent(context)
            }
            PermissionUiState.CoarseLocationError -> {
                Text(
                    text = "Precise location is required for this app to work correctly. Please grant 'Precise' location.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { openAppSettings(context) }) {
                    Text(stringResource(id = R.string.onboarding_permission_open_settings))
                }
            }
        }
    }
}

@Composable
fun ObserveLifecycleResume(onResume: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
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
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
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
                    Log.e(TAG, "Activity not found when launching background permission request", e)
                    onLaunchError()
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception when launching background permission request", e)
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

@Composable
fun RationaleContent(context: Context) {
    Text(
        text = stringResource(id = R.string.onboarding_permission_denied_message),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(onClick = { openAppSettings(context) }) {
        Text(stringResource(id = R.string.onboarding_permission_open_settings))
    }
}

private fun openAppSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    context.startActivity(intent)
}
