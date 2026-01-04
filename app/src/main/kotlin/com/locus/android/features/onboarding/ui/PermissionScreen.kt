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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.locus.android.ui.theme.LocusTheme

private const val STEP_FOREGROUND = 1
private const val STEP_BACKGROUND = 2
private const val STEP_COMPLETE = 3

@Suppress("LongMethod", "ComplexMethod")
@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Detect if we have foreground permission initially
    fun hasForegroundPermission(): Boolean {
        val fineLocation =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fineLocation
    }

    // Detect if we have background permission initially (Q+)
    fun hasBackgroundPermission(): Boolean {
        val versionQ = Build.VERSION_CODES.Q
        return if (Build.VERSION.SDK_INT >= versionQ) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required on < Q
        }
    }

    var step by remember {
        mutableStateOf(
            if (hasForegroundPermission()) {
                if (hasBackgroundPermission()) STEP_COMPLETE else STEP_BACKGROUND
            } else {
                STEP_FOREGROUND
            },
        )
    }

    var showRationale by remember { mutableStateOf(false) }
    var isPermanentDenial by remember { mutableStateOf(false) }

    // Re-check permissions on resume (e.g. returning from settings)
    LaunchedEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (hasForegroundPermission()) {
                        if (hasBackgroundPermission()) {
                            onPermissionsGranted()
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            step = STEP_BACKGROUND
                            isPermanentDenial = false
                            showRationale = false
                        }
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    fun openSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        context.startActivity(intent)
    }

    val foregroundLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            if (fineLocation) {
                showRationale = false
                isPermanentDenial = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    step = STEP_BACKGROUND
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

    val backgroundLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                onPermissionsGranted()
            } else {
                // Background Location denial handling
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
                text = if (step == STEP_FOREGROUND) "Location Access" else "Always Allow",
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
                        step == STEP_FOREGROUND ->
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
                        if (step == STEP_FOREGROUND) {
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
                            step == STEP_FOREGROUND -> "Grant Access"
                            // Handle API 29 vs 30+ difference for Background Location Button
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                                "Open Settings" // API 30+ enforces Settings redirect
                            else -> "Grant Access" // API 29 shows system dialog
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
