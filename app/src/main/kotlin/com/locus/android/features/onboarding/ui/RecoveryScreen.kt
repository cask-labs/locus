package com.locus.android.features.onboarding.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locus.android.features.onboarding.RecoveryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(
    uiState: RecoveryUiState,
    onLoadBuckets: () -> Unit,
    onBucketSelected: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        onLoadBuckets()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Locus Bucket") })
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
                        .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (uiState.buckets.isEmpty()) {
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("No Locus stores found.")
                                }
                            }
                        } else {
                            items(uiState.buckets) { bucket ->
                                ListItem(
                                    headlineContent = { Text(bucket) },
                                    modifier = Modifier.clickable { onBucketSelected(bucket) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
