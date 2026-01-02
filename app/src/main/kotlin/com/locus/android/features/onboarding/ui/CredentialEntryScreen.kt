package com.locus.android.features.onboarding.ui

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.locus.android.features.onboarding.OnboardingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialEntryScreen(
    uiState: OnboardingUiState,
    onAccessKeyIdChanged: (String) -> Unit,
    onSecretAccessKeyChanged: (String) -> Unit,
    onSessionTokenChanged: (String) -> Unit,
    onPasteJson: (String) -> Unit,
    onValidate: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Enter Credentials") })
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
                OutlinedButton(
                    onClick = {
                        val text = clipboardManager.getText()?.text
                        if (!text.isNullOrBlank()) {
                            onPasteJson(text)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Paste from Clipboard (JSON)")
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = uiState.accessKeyId,
                    onValueChange = onAccessKeyIdChanged,
                    label = { Text("Access Key ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.secretAccessKey,
                    onValueChange = onSecretAccessKeyChanged,
                    label = { Text("Secret Access Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.sessionToken,
                    onValueChange = onSessionTokenChanged,
                    label = { Text("Session Token") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = onValidate,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Validate")
                    }
                }
            }
        }
    }
}
