package com.locus.android.features.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locus.android.ui.theme.LocusTheme
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.result.DomainException

@Composable
fun ProvisioningScreen(
    state: ProvisioningState,
    onNavigateToSuccess: () -> Unit = {},
) {
    Scaffold { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            Text(
                text = "Setting up Locus",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Configuring your private cloud infrastructure...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            when (state) {
                is ProvisioningState.Working -> {
                    ProvisioningLogList(
                        currentStep = state.currentStep,
                        history = state.history,
                        modifier = Modifier.weight(1f),
                    )
                }
                is ProvisioningState.Success -> {
                    LaunchedEffect(Unit) {
                        onNavigateToSuccess()
                    }
                    ProvisioningSuccess()
                }
                is ProvisioningState.Failure -> {
                    ProvisioningError(state.error)
                }
                else -> {
                    // Idle or other transitional states
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ProvisioningLogList(
    currentStep: String,
    history: List<String>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom
    LaunchedEffect(history.size, currentStep) {
        // Scroll to the item after the last history item (which is the current step placeholder)
        listState.animateScrollToItem(history.size)
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        // History Items (Completed)
        items(history) { step ->
            LogItem(message = step, isCompleted = true)
        }

        // Current Item (Active)
        item {
            LogItem(message = currentStep, isCompleted = false)
        }
    }
}

@Composable
fun LogItem(
    message: String,
    isCompleted: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = message,
            style =
                if (isCompleted) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
            color =
                if (isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
    }
}

@Composable
fun ProvisioningSuccess() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text("Setup Complete!", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ProvisioningError(error: DomainException) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "Setup Failed",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error.message ?: "Unknown error",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProvisioningScreenPreview() {
    LocusTheme {
        ProvisioningScreen(
            state =
                ProvisioningState.Working(
                    currentStep = "Deploying Stack...",
                    history = listOf("Validated Input", "Loaded Template"),
                ),
        )
    }
}
