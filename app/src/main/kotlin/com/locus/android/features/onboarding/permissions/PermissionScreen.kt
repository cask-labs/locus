package com.locus.android.features.onboarding.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    // Stage 1: Foreground
    val foregroundPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Stage 2: Background (Requires Foreground first)
    // Only available on Q+
    val backgroundPermissionName =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            // No background permission needed pre-Q, effectively granted
            Manifest.permission.ACCESS_FINE_LOCATION
        }

    val backgroundPermissionState = rememberPermissionState(backgroundPermissionName)

    val context = LocalContext.current
    var showBackgroundRationale by remember { mutableStateOf(false) }

    LaunchedEffect(foregroundPermissionState.status, backgroundPermissionState.status) {
        if (foregroundPermissionState.status.isGranted) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || backgroundPermissionState.status.isGranted) {
                onPermissionsGranted()
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Location Permissions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!foregroundPermissionState.status.isGranted) {
                // Foreground Request
                Text(
                    text = "Locus needs 'While Using' location permission to record your tracks.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(onClick = { foregroundPermissionState.launchPermissionRequest() }) {
                    Text("Grant Foreground Location")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !backgroundPermissionState.status.isGranted) {
                // Background Request (Android 10+)
                // Android 11+ (R) requires user to go to settings if we request background

                Text(
                    text = "To track you while the screen is off, Locus needs 'Allow all the time' location permission.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "On the next screen, please select 'Allow all the time'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(onClick = { backgroundPermissionState.launchPermissionRequest() }) {
                    Text("Grant Background Location")
                }
            } else {
                Text("All permissions granted!")
            }
        }
    }
}
