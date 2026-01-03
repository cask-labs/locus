package com.locus.android.features.onboarding.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locus.android.ui.theme.LocusTheme

@Suppress("LongMethod")
@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    var step by remember { mutableStateOf(1) } // 1: Foreground, 2: Background (if needed)

    // Launcher for Step 1: Foreground
    val foregroundLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineLocation || coarseLocation) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    step = 2
                } else {
                    onPermissionsGranted()
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
                // Handle denial: Ideally we show a rationale or check "Don't ask again"
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
                    if (step == 1) {
                        "Locus needs location access to track your movement. We respect your privacy and only " +
                            "store data in your private cloud."
                    } else {
                        "To track you while the screen is off, Locus needs 'Allow all the time' permission. " +
                            "Please select this in the next screen."
                    },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
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
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(if (step == 1) "Grant Access" else "Open Settings")
            }
        }
    }
}

@Preview(showBackground = true)
@Suppress("LongMethod")
@Composable
fun PermissionScreenPreview() {
    LocusTheme {
        PermissionScreen(onPermissionsGranted = {})
    }
}
