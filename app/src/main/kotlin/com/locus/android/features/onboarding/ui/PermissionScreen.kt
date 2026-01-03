package com.locus.android.features.onboarding.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    var currentStep by remember { mutableStateOf(PermissionStep.FOREGROUND) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Resume check to handle return from Settings
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (checkForeground(context)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (checkBackground(context)) {
                                onPermissionsGranted()
                            } else {
                                currentStep = PermissionStep.BACKGROUND
                            }
                        } else {
                            onPermissionsGranted()
                        }
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    PermissionContent(
        step = currentStep,
        onForegroundGranted = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                currentStep = PermissionStep.BACKGROUND
            } else {
                onPermissionsGranted()
            }
        },
        onBackgroundGranted = onPermissionsGranted,
    )
}

@Composable
private fun PermissionContent(
    step: PermissionStep,
    onForegroundGranted: () -> Unit,
    onBackgroundGranted: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (step == PermissionStep.FOREGROUND) {
            ForegroundPermissionContent(onForegroundGranted)
        } else {
            BackgroundPermissionContent(onBackgroundGranted)
        }
    }
}

@Composable
private fun ForegroundPermissionContent(onGranted: () -> Unit) {
    val context = LocalContext.current
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fine || coarse) {
                onGranted()
            } else {
                // Simple check: if we asked and got denied, we might need settings
                // Ideally we check shouldShowRequestPermissionRationale, but in Compose
                // it's tricky without Activity reference. For now, assume if denied twice
                // (user sees this screen again), they might have permanently denied.
                // A robust solution uses Accompanist Permissions or manual Activity check.
                permanentlyDenied = true
            }
        }

    Text(
        text = "Location Access",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text =
            "Locus needs location access to track your movement. " +
                "We'll start by asking for standard location permissions.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))

    if (permanentlyDenied) {
        Button(
            onClick = { openAppSettings(context) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Settings")
        }
    } else {
        Button(
            onClick = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Grant Location Access")
        }
    }
}

@Composable
private fun BackgroundPermissionContent(onGranted: () -> Unit) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                onGranted()
            }
        }

    Text(
        text = "Always-On Tracking",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(16.dp))

    val isAndroid11Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    val description =
        if (isAndroid11Plus) {
            "To track you while the app is in the background, you must manually select " +
                "'Allow all the time' in the system settings."
        } else {
            "To track you while the app is in the background, please select " +
                "'Allow all the time' in the next screen."
        }

    Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = {
            if (isAndroid11Plus) {
                // Redirect to settings for Android 11+
                openAppSettings(context)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    launcher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    onGranted() // Should not happen given logic
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (isAndroid11Plus) "Open Settings" else "Allow All The Time")
    }
}

private fun checkForeground(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PermissionChecker.PERMISSION_GRANTED
}

private fun checkBackground(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PermissionChecker.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun openAppSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(intent)
}

private enum class PermissionStep {
    FOREGROUND,
    BACKGROUND,
}
