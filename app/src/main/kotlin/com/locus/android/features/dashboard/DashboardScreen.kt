package com.locus.android.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.locus.core.domain.result.LocusResult

@Composable
@Suppress("ktlint:standard:function-naming")
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            when (val result = uiState) {
                is LocusResult.Success -> {
                    Text(text = "Hello Locus v${result.data.versionName}")
                }

                is LocusResult.Failure -> {
                    Text(text = "Error: ${result.error.message}")
                }

                null -> {
                    Text(text = "Loading...")
                }
            }
        }
    }
}
