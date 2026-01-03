package com.locus.android.features.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locus.android.ui.theme.LocusTheme
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.result.DomainException

@Composable
fun ProvisioningScreen(
    state: ProvisioningState,
    onSuccess: () -> Unit,
) {
    // Auto-advance on success
    LaunchedEffect(state) {
        if (state is ProvisioningState.Success) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Setting up Locus",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        when (state) {
            is ProvisioningState.Idle -> {
                Text("Waiting to start...")
            }
            is ProvisioningState.Working -> {
                LogList(
                    currentStep = state.currentStep,
                    history = state.history,
                    modifier = Modifier.weight(1f),
                )
            }
            is ProvisioningState.Success -> {
                // Should navigate away, but show completed state briefly
                Text("Setup Complete!", color = MaterialTheme.colorScheme.primary)
            }
            is ProvisioningState.Failure -> {
                ErrorView(error = state.error)
            }
        }
    }
}

@Composable
private fun LogList(
    currentStep: String,
    history: List<String>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom
    LaunchedEffect(history.size, currentStep) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(history) { step ->
            LogItem(text = step, isCompleted = true)
        }
        item {
            LogItem(text = currentStep, isCompleted = false)
        }
    }
}

@Composable
private fun LogItem(
    text: String,
    isCompleted: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ErrorView(error: DomainException) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Setup Failed",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = error.message ?: "Unknown error occurred",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProvisioningPreview() {
    LocusTheme {
        ProvisioningScreen(
            state = ProvisioningState.Working(
                currentStep = "Deploying Stack...",
                history = listOf("Validating input", "Creating Bucket"),
            ),
            onSuccess = {},
        )
    }
}
