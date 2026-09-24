package com.streamfyree.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class LyricsData(
    val trackTitle: String,
    val artistName: String,
    val isSynced: Boolean,
    val lines: List<LyricLine>,
    val plainLyrics: String? = null
)

object LyricsManager {

    suspend fun fetchLyrics(artist: String, title: String): LyricsData? = withContext(Dispatchers.IO) {
        runCatching {
            val queryArtist = URLEncoder.encode(artist, "UTF-8")
            val queryTitle = URLEncoder.encode(title, "UTF-8")
            val urlString = "https://lrclib.net/api/get?artist_name=$queryArtist&track_name=$queryTitle"
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Streamfyree/1.0 (Android)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val syncedLrc = json.optString("syncedLyrics").ifBlank { null }
                val plain = json.optString("plainLyrics").ifBlank { null }

                if (!syncedLrc.isNullOrBlank()) {
                    val parsed = parseLrc(syncedLrc)
                    return@withContext LyricsData(title, artist, isSynced = true, lines = parsed, plainLyrics = plain)
                } else if (!plain.isNullOrBlank()) {
                    val lines = plain.lines().filter { it.isNotBlank() }.map { LyricLine(0L, it) }
                    return@withContext LyricsData(title, artist, isSynced = false, lines = lines, plainLyrics = plain)
                }
            }

            // Fallback search endpoint if direct get returned 404
            val searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode("$artist $title", "UTF-8")
            val searchConn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Streamfyree/1.0 (Android)")
            }

            if (searchConn.responseCode == 200) {
                val searchJsonStr = searchConn.inputStream.bufferedReader().use { it.readText() }
                val array = org.json.JSONArray(searchJsonStr)
                if (array.length() > 0) {
                    val item = array.getJSONObject(0)
                    val syncedLrc = item.optString("syncedLyrics").ifBlank { null }
                    val plain = item.optString("plainLyrics").ifBlank { null }
                    if (!syncedLrc.isNullOrBlank()) {
                        val parsed = parseLrc(syncedLrc)
                        return@withContext LyricsData(title, artist, isSynced = true, lines = parsed, plainLyrics = plain)
                    } else if (!plain.isNullOrBlank()) {
                        val lines = plain.lines().filter { it.isNotBlank() }.map { LyricLine(0L, it) }
                        return@withContext LyricsData(title, artist, isSynced = false, lines = lines, plainLyrics = plain)
                    }
                }
            }
            null
        }.getOrNull()
    }

    fun parseLrc(lrcContent: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")

        lrcContent.lines().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val (minStr, secStr, msStr, text) = match.destructured
                val min = minStr.toLongOrNull() ?: 0L
                val sec = secStr.toLongOrNull() ?: 0L
                var ms = msStr.toLongOrNull() ?: 0L
                if (msStr.length == 2) ms *= 10

                val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                if (text.isNotBlank()) {
                    result.add(LyricLine(totalMs, text.trim()))
                }
            }
        }
        return result.sortedBy { it.timeMs }
    }

    fun getActiveLineIndex(lines: List<LyricLine>, currentPosMs: Long): Int {
        if (lines.isEmpty()) return -1
        var activeIndex = -1
        for (i in lines.indices) {
            if (currentPosMs >= lines[i].timeMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }
}
