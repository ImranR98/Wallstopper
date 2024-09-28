package dev.imranr.wallstopper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ProgressBar(progress: Float, label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display the progress bar
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = progress // Sets the progress value
        )
        Spacer(modifier = Modifier.height(8.dp)) // Space between the progress bar and label

        // Display the label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall, // Small text style
            overflow = TextOverflow.Ellipsis
        )
    }
}
