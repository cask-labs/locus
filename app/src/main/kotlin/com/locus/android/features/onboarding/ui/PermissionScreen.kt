package com.locus.android.features.onboarding.ui

import android.Manifest
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.locus.android.R

private const val TAG = "PermissionScreen"

@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(PermissionStep.FOREGROUND) }
    var showRationale by remember { mutableStateOf(false) }

    fun checkPermissions() {
        val step = determinePermissionStep(context)
        currentStep = step
        if (step == PermissionStep.COMPLETE) {
            onPermissionsGranted()
        }
    }

    ObserveLifecycleResume {
        checkPermissions()
    }

    val activity = LocalContext.current as? android.app.Activity

    val foregroundLauncher =
        rememberForegroundLauncher(
            onResult = { granted ->
                if (!granted) {
                    val shouldShowRationale =
                        activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true
                    showRationale = !shouldShowRationale
                }
                checkPermissions()
            },
        )

    val backgroundLauncher =
        rememberBackgroundLauncher(
            onResult = { granted ->
                if (!granted) {
                    val shouldShowRationale =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            activity?.shouldShowRequestPermissionRationale(
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                            ) == true
                        } else {
                            false
                        }
                    showRationale = !shouldShowRationale
                }
                checkPermissions()
            },
        )

    PermissionContent(
        currentStep = currentStep,
        showRationale = showRationale,
        context = context,
        foregroundLauncher = foregroundLauncher,
        backgroundLauncher = backgroundLauncher,
    )
}

@Composable
private fun rememberForegroundLauncher(onResult: (Boolean) -> Unit) =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            onResult(granted)
        },
    )

@Composable
private fun rememberBackgroundLauncher(onResult: (Boolean) -> Unit) =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            onResult(isGranted)
        },
    )

@Composable
private fun PermissionContent(
    currentStep: PermissionStep,
    showRationale: Boolean,
    context: Context,
    foregroundLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    backgroundLauncher: ManagedActivityResultLauncher<String, Boolean>,
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

        when (currentStep) {
            PermissionStep.FOREGROUND -> ForegroundPermissionContent(foregroundLauncher)
            PermissionStep.BACKGROUND -> BackgroundPermissionContent(context, backgroundLauncher)
            PermissionStep.COMPLETE -> { /* Navigate away via callback */ }
        }

        if (showRationale) {
            RationaleContent(context)
        }
    }
}

private fun determinePermissionStep(context: Context): PermissionStep {
    val hasForeground =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    val hasBackground =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Background location implied in older versions
        }

    return when {
        !hasForeground -> PermissionStep.FOREGROUND
        !hasBackground -> PermissionStep.BACKGROUND
        else -> PermissionStep.COMPLETE
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
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
    ) {
        Text(stringResource(id = R.string.onboarding_permission_grant_foreground))
    }
}

@Composable
fun BackgroundPermissionContent(
    context: Context,
    launcher: ManagedActivityResultLauncher<String, Boolean>,
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
                } catch (
                    @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
                ) {
                    Log.e(TAG, "Failed to launch background permission request", e)
                    openAppSettings(context)
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
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(id = R.string.onboarding_permission_denied_message),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(modifier = Modifier.height(8.dp))
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

enum class PermissionStep {
    FOREGROUND,
    BACKGROUND,
    COMPLETE,
}
