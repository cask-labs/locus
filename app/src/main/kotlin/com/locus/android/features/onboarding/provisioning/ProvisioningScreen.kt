package com.locus.android.features.onboarding.provisioning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.locus.android.features.onboarding.NewDeviceViewModel
import com.locus.core.domain.model.auth.ProvisioningState

@Composable
fun ProvisioningScreen(
    viewModel: NewDeviceViewModel = hiltViewModel(),
    onSuccess: () -> Unit,
) {
    val state by viewModel.provisioningState.collectAsState()

    LaunchedEffect(state) {
        if (state is ProvisioningState.Success) {
            onSuccess()
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
                text = "Setting up Locus",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (val s = state) {
                is ProvisioningState.Working -> {
                    // Current Step
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(text = s.currentStep, style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // History Log
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .padding(16.dp),
                    ) {
                        // Show newest at bottom if we were appending, but typically newest is at bottom.
                        // However, we want the list to stick to bottom.
                        // Let's assume history is ordered [oldest, ..., newest]
                        LazyColumn(reverseLayout = true) {
                            items(s.history.reversed()) { historyItem ->
                                Text(
                                    text = historyItem,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                is ProvisioningState.Failure -> {
                    // Use Warning or similar since Error is not in Default? Actually Error should be in Default.
                    // Let's check Material Icons. Icons.Default.Warning is safer if Error is missing.
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Setup Failed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = s.error.message ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // History Log in Failure
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .padding(16.dp),
                    ) {
                        LazyColumn(reverseLayout = true) {
                            items(s.history.reversed()) { historyItem ->
                                Text(
                                    text = historyItem,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.reset() }) {
                        Text("Try Again")
                    }
                }

                is ProvisioningState.Success -> {
                    // Handled by LaunchedEffect, but show success momentarily
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text("Setup Complete!")
                }

                else -> {
                    Text("Initializing...")
                }
            }
        }
    }
}
