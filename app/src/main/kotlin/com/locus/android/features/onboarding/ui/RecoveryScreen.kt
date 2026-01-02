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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.locus.android.features.onboarding.RecoveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(
    viewModel: RecoveryViewModel = hiltViewModel(),
    onBucketSelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Bucket") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
             Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
            ) {
                 if (uiState.isLoading) {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator()
                     }
                 } else if (uiState.error != null) {
                      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Text(
                             text = uiState.error!!,
                             color = MaterialTheme.colorScheme.error
                         )
                     }
                 } else {
                     LazyColumn(
                         modifier = Modifier.fillMaxSize()
                     ) {
                         if (uiState.buckets.isEmpty()) {
                             item {
                                 Text(
                                     text = "No Locus stores found.",
                                     modifier = Modifier.padding(16.dp),
                                     style = MaterialTheme.typography.bodyLarge
                                 )
                             }
                         } else {
                             items(uiState.buckets) { bucket ->
                                 ListItem(
                                     headlineContent = { Text(bucket) },
                                     modifier = Modifier.clickable { onBucketSelected(bucket) }
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
