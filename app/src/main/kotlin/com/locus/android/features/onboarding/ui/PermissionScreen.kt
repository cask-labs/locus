package com.locus.android.features.onboarding.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.locus.android.ui.theme.LocusTheme

@Suppress("LongMethod", "ComplexMethod")
@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) } // 1: Foreground, 2: Background (if needed)
    var showRationale by remember { mutableStateOf(false) }
    var isPermanentDenial by remember { mutableStateOf(false) }

    fun openSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        context.startActivity(intent)
    }

    // Launcher for Step 1: Foreground
    val foregroundLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineLocation || coarseLocation) {
                showRationale = false
                isPermanentDenial = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    step = 2
                } else {
                    onPermissionsGranted()
                }
            } else {
                // Denied
                val activity = context.findActivity()
                val shouldShowRationale =
                    activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            it,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                    } ?: true

                if (shouldShowRationale) {
                    showRationale = true
                    isPermanentDenial = false
                } else {
                    showRationale = false
                    isPermanentDenial = true
                }
            }
        }

    // Launcher for Step 2: Background (API 29+)
    val backgroundLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                onPermissionsGranted()
            } else {
                // Background Location denial
                // If it returns false immediately, it might be system blocked or user denied.
                // We treat this as a need to go to settings in most cases for Background Location
                // because the "Allow all the time" option is often hidden in the system dialog
                // or the dialog doesn't show at all on Android 11+ if already denied once.

                // For Background location on Android 11+, if denied, we pretty much always
                // want to send them to settings as the "Allow all the time" option is strict.
                isPermanentDenial = true
                showRationale = false
            }
        }

    Scaffold { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (step == 1) "Location Access" else "Always Allow",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text =
                    when {
                        isPermanentDenial ->
                            "Location permission is permanently denied. Please enable it in Settings to continue."
                        showRationale ->
                            "Locus strictly requires location access to function. We only store data in " +
                                "your private S3 bucket."
                        step == 1 ->
                            "Locus needs location access to track your movement. We respect your privacy and only " +
                                "store data in your private cloud."
                        else ->
                            "To track you while the screen is off, Locus needs 'Allow all the time' permission. " +
                                "Please select this in the next screen."
                    },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color =
                    if (isPermanentDenial || showRationale) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )

            if (isPermanentDenial || showRationale) {
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isPermanentDenial) {
                        openSettings()
                    } else {
                        if (step == 1) {
                            foregroundLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    text =
                        when {
                            isPermanentDenial -> "Open Settings"
                            step == 1 -> "Grant Access"
                            // For Background, we often end up in Settings anyway on modern Android
                            else -> "Open Settings"
                        },
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Preview(showBackground = true)
@Composable
fun PermissionScreenPreview() {
    LocusTheme {
        PermissionScreen(onPermissionsGranted = {})
    }
}
