package com.streamfyree.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException

class StreamfyreeDownloadManager(context: Context) {
    private val root = File(context.filesDir, "downloads").apply { mkdirs() }
    private val client = OkHttpClient.Builder().build()

    fun audioFile(trackId: String): File = File(root, safeName(trackId) + ".audio")
    private fun metaFile(trackId: String): File = File(root, safeName(trackId) + ".json")

    fun isDownloaded(trackId: String): Boolean = audioFile(trackId).isFile && audioFile(trackId).length() > 0L

    fun localTrack(trackId: String): Track? = downloadedTracks().firstOrNull { it.id == trackId }

    fun downloadedTracks(): List<Track> = root.listFiles().orEmpty()
        .filter { it.extension == "json" }
        .mapNotNull { file -> runCatching { decode(file.readText()) }.getOrNull() }
        .filter { isDownloaded(it.id) }
        .sortedByDescending { metaFile(it.id).lastModified() }

    suspend fun download(track: Track, onProgress: (Int) -> Unit = {}): File = withContext(Dispatchers.IO) {
        val url = track.streamUrl?.takeIf { it.isNotBlank() }
            ?: throw IOException("This track has not been resolved for download yet.")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Streamfyree/0.3 Android")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Download failed: HTTP ${response.code}")

        val body = response.body ?: throw IOException("Download returned no audio data.")
        val target = audioFile(track.id)
        val temp = File(root, safeName(track.id) + ".part")
        val total = body.contentLength()

        try {
            body.byteStream().use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        if (total > 0) onProgress(((copied * 100L) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            if (temp.length() <= 0L) throw IOException("Downloaded audio is empty.")
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) throw IOException("Could not finalize downloaded audio.")
            metaFile(track.id).writeText(encode(track))
            onProgress(100)
            target
        } catch (e: Exception) {
            temp.delete()
            throw e
        } finally {
            response.close()
        }
    }

    fun delete(trackId: String) {
        audioFile(trackId).delete()
        metaFile(trackId).delete()
    }

    fun clearAll() {
        downloadedTracks().forEach { delete(it.id) }
    }

    private fun safeName(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)

    private fun encode(t: Track): String = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("artist", t.artist)
        put("album", t.album)
        put("artwork", t.artwork ?: JSONObject.NULL)
        put("duration", t.durationMs)
        put("youtube", t.youtubeUrl ?: JSONObject.NULL)
        put("lyric", t.lyricVideo)
    }.toString()

    private fun decode(raw: String): Track {
        val o = JSONObject(raw)
        val id = o.getString("id")
        return Track(
            id = id,
            title = o.getString("title"),
            artist = o.optString("artist"),
            album = o.optString("album"),
            artwork = o.optString("artwork").ifBlank { null },
            durationMs = o.optLong("duration"),
            youtubeUrl = o.optString("youtube").ifBlank { null },
            streamUrl = audioFile(id).toURI().toString(),
            lyricVideo = o.optBoolean("lyric")
        )
    }
}
