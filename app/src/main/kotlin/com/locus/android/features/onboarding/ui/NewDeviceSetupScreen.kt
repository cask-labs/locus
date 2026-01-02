package com.locus.android.features.onboarding.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locus.android.features.onboarding.NewDeviceUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDeviceSetupScreen(
    uiState: NewDeviceUiState,
    onDeviceNameChanged: (String) -> Unit,
    onCheckAvailability: () -> Unit,
    onDeploy: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("New Device Setup") })
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Name your device stack (e.g. 'pixel-7')",
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.deviceName,
                    onValueChange = {
                        onDeviceNameChanged(it)
                    },
                    label = { Text("Device Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.error != null,
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (uiState.availabilityMessage != null) {
                    Text(
                        text = uiState.availabilityMessage,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (uiState.isNameValid && uiState.error == null) {
                    Button(
                        onClick = onCheckAvailability,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Check Availability")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Deploy button enabled only if available
                Button(
                    onClick = onDeploy,
                    enabled = uiState.availabilityMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Deploy Stack")
                }
            }
        }
    }
}
