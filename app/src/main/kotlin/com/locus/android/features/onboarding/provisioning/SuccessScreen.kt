package com.locus.android.features.onboarding.provisioning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SuccessScreen(onContinue: () -> Unit) {
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
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "You're all set!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your secure, private location tracker is ready. Now we just need a few permissions to start tracking.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Full width
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text("Continue to Permissions")
            }
        }
    }
}
