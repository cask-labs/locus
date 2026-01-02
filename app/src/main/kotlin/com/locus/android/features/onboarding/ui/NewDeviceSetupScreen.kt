package com.locus.android.features.onboarding.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.locus.android.features.onboarding.NewDeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDeviceSetupScreen(
    viewModel: NewDeviceViewModel = hiltViewModel(),
    onDeploy: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("New Device Setup") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Name your device",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This will be part of your S3 bucket name. Use lowercase letters, numbers, and hyphens.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = uiState.deviceName,
                    onValueChange = { viewModel.updateDeviceName(it) },
                    label = { Text("Device Name (e.g. pixel-7)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.error != null
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (uiState.isChecking) {
                    CircularProgressIndicator()
                } else if (uiState.isAvailable == true) {
                     Text(
                        text = "Name is available!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDeploy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Deploy & Start")
                    }
                } else {
                    Button(
                        onClick = { viewModel.checkAvailability() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.deviceName.isNotEmpty()
                    ) {
                        Text("Check Availability")
                    }
                }
            }
        }
    }
}
