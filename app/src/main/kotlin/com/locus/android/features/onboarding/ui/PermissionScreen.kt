package com.locus.android.features.onboarding.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.locus.android.features.onboarding.PermissionUiState

@Composable
fun PermissionScreen(
    state: PermissionUiState,
    onOpenSettings: () -> Unit,
    onCheckPermissions: () -> Unit = {},
) {
    // Launchers
    val fgLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { _ -> onCheckPermissions() },
        )

    val bgLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { _ -> onCheckPermissions() },
        )

    PermissionLifecycleObserver(onCheckPermissions)

    Scaffold { padding ->
        PermissionContent(
            state = state,
            onOpenSettings = onOpenSettings,
            onForegroundRequest = { fgLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            onBackgroundRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            },
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun PermissionLifecycleObserver(onCheckPermissions: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    onCheckPermissions()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun PermissionContent(
    state: PermissionUiState,
    onOpenSettings: () -> Unit,
    onForegroundRequest: () -> Unit,
    onBackgroundRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Location Access",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionExplanation(state)

        Spacer(modifier = Modifier.height(32.dp))

        PermissionButtons(
            state = state,
            onForegroundRequest = onForegroundRequest,
            onBackgroundRequest = onBackgroundRequest,
        )

        if (state.shouldShowRationale || state.isPermanentlyDenied) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun PermissionExplanation(state: PermissionUiState) {
    val isBackgroundRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val explanation =
        when {
            !state.hasForeground ->
                "Locus needs access to your location to track your path. " +
                    "This data is stored only on your device and your private S3 bucket."
            isBackgroundRequired && !state.hasBackground ->
                "To track you while the screen is off, Locus needs 'Allow all the time' access. " +
                    "Please select this option in Settings."
            else -> "Permissions granted!"
        }

    Text(
        text = explanation,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PermissionButtons(
    state: PermissionUiState,
    onForegroundRequest: () -> Unit,
    onBackgroundRequest: () -> Unit,
) {
    val isBackgroundRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    if (!state.hasForeground) {
        Button(
            onClick = onForegroundRequest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Grant Location Access")
        }
    } else if (isBackgroundRequired && !state.hasBackground) {
        Button(
            onClick = onBackgroundRequest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Allow All The Time")
        }
    }
}
