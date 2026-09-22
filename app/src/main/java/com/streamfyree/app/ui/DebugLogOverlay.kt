package com.streamfyree.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streamfyree.app.DebugLog
import com.streamfyree.app.DebugLogger
import com.streamfyree.app.MusicViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugLogOverlay(vm: MusicViewModel) {
    val logs by DebugLogger.logs.collectAsState()
    var open by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (logs.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(46.dp)
                    .clickable { open = true },
                shape = RoundedCornerShape(23.dp),
                color = Color(0xFF352025),
                contentColor = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BugReport, contentDescription = "Debug logs")
                }
            }
        }
    }

    if (open) {
        DebugLogDialog(
            logs = logs,
            onDismiss = { open = false },
            onClear = { DebugLogger.clear() }
        )
    }
}

@Composable
private fun DebugLogDialog(
    logs: List<DebugLog>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B0B0B)
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Debug Log", color = Color.White, fontSize = 24.sp)
                        Text(
                            "${logs.size} events • newest first",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.DeleteSweep, "Clear logs", tint = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (logs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No debug events yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            DebugLogRow(log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugLogRow(log: DebugLog) {
    val context = LocalContext.current
    val time = remember(log.timestamp) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(log.timestamp))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = when (log.level) {
            "ERROR" -> Color(0xFF35191E)
            "WARN" -> Color(0xFF332A18)
            else -> Color(0xFF171717)
        }
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(time, color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Text(log.level, color = Color.White, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Text(log.tag, color = Color(0xFFFFB1B5), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Streamfyree debug log", log.text))
                        }
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                log.text,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
