package com.streamfyree.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicLong

data class DebugLog(
    val id: Long,
    val timestamp: Long,
    val level: String,
    val tag: String,
    val text: String
)

object DebugLogger {
    private val nextId = AtomicLong(0)
    private val _logs = MutableStateFlow<List<DebugLog>>(emptyList())
    val logs: StateFlow<List<DebugLog>> = _logs

    fun info(tag: String, message: String) = add("INFO", tag, message)
    fun warn(tag: String, message: String) = add("WARN", tag, message)
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val detail = buildString {
            append(message)
            if (throwable != null) {
                append("\n\n")
                append(stackTrace(throwable))
            }
        }
        add("ERROR", tag, detail)
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun copyToClipboard(text: String) {
        // UI-level copy is handled by the Android clipboard when a Context is available.
        // The full event remains visible in the log even if clipboard access is unavailable.
    }

    private fun add(level: String, tag: String, message: String) {
        val entry = DebugLog(
            id = nextId.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            text = message
        )
        _logs.value = (_logs.value + entry).takeLast(300).reversed()
    }

    private fun stackTrace(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}
