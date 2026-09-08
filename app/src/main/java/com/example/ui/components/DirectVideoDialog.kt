package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DirectVideoDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onPlayDirect: (videoIdOrUrl: String, customTitle: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var videoInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("direct_video_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Stream Direct YouTube Audio",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter a YouTube video URL or ID to load audio directly in the shrunk YouTube iFrame.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = videoInput,
                    onValueChange = { videoInput = it },
                    label = { Text("Video ID or URL") },
                    placeholder = { Text("e.g. dQw4w9WgXcQ or youtu.be/...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_video_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Track Title (Optional)") },
                    placeholder = { Text("Live Audio Stream") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_title_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (videoInput.isNotBlank()) {
                        val title = if (titleInput.isNotBlank()) titleInput.trim() else "YouTube Live Audio"
                        onPlayDirect(videoInput.trim(), title)
                        onDismiss()
                    }
                },
                enabled = videoInput.isNotBlank(),
                modifier = Modifier.testTag("start_direct_stream_btn")
            ) {
                Text("Stream Audio")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
