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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.locus.android.features.onboarding.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialEntryScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onCredentialsValid: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    // Effect to navigate when valid
    if (uiState.isCredentialsValid) {
        // Trigger navigation (handled by parent typically, but here we just call the callback)
        // We should ensure this is only called once. The callback should handle navigation.
        onCredentialsValid()
        // Note: In a real app we might want to reset the flag or have a strict event channel.
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Enter AWS Credentials") })
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
                OutlinedButton(
                    onClick = {
                        val clipData = clipboardManager.getText()
                        if (clipData != null) {
                            viewModel.pasteJson(clipData.text)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Paste JSON from Clipboard")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.credentials.accessKeyId,
                    onValueChange = { viewModel.updateAccessKeyId(it) },
                    label = { Text("Access Key ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.credentials.secretAccessKey,
                    onValueChange = { viewModel.updateSecretAccessKey(it) },
                    label = { Text("Secret Access Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.credentials.sessionToken,
                    onValueChange = { viewModel.updateSessionToken(it) },
                    label = { Text("Session Token") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { viewModel.validateCredentials() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Validate & Continue")
                    }
                }
            }
        }
    }
}
