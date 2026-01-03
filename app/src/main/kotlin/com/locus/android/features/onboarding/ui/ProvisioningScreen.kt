package com.locus.android.features.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.locus.android.features.onboarding.ProvisioningUiState

@Composable
fun ProvisioningScreen(state: ProvisioningUiState) {
    Scaffold(
        topBar = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Text(
                    text = "Setting up Locus",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
        ) {
            // Log-style list
            LogList(
                history = state.history,
                currentStep = state.currentStep,
                error = state.error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun LogList(
    history: List<String>,
    currentStep: String?,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(history.size, currentStep, error) {
        listState.animateScrollToItem((history.size + (if (currentStep != null) 1 else 0)).coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(history) { step ->
            LogItem(text = step, isComplete = true)
        }

        if (currentStep != null && error == null) {
            item {
                LogItem(text = currentStep, isComplete = false, isLoading = true)
            }
        }

        if (error != null) {
            item {
                LogItem(text = error, isError = true)
            }
        }
    }
}

@Composable
fun LogItem(
    text: String,
    isComplete: Boolean = false,
    isLoading: Boolean = false,
    isError: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> MaterialTheme.colorScheme.errorContainer
                            isComplete -> MaterialTheme.colorScheme.primaryContainer
                            else -> Color.Transparent
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                isError ->
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                isComplete ->
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
