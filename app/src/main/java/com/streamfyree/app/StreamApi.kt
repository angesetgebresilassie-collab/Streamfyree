package com.streamfyree.app

import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class StreamApi {
    private val client = OkHttpClient()

    fun search(query: String): List<Track> {
        val url = BuildConfig.STREAM_API_BASE.trimEnd('/') + "/search?q=" + Uri.encode(query)
        val array = JSONArray(get(url))
        return List(array.length()) { trackFromJson(array.getJSONObject(it)) }
    }

    fun resolve(id: String): Track {
        val url = BuildConfig.STREAM_API_BASE.trimEnd('/') + "/resolve/" + Uri.encode(id)
        return trackFromJson(JSONObject(get(url)))
    }

    fun track(id: String): Track {
        val url = BuildConfig.STREAM_API_BASE.trimEnd('/') + "/track/" + Uri.encode(id)
        return trackFromJson(JSONObject(get(url)))
    }

    private fun get(url: String): String {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) {
            throw IOException("Server returned " + response.code)
        }
        return response.body?.string() ?: throw IOException("Empty server response")
    }

    private fun trackFromJson(o: JSONObject) = Track(
        id = o.getString("id"),
        title = o.optString("title"),
        artist = o.optString("artist"),
        album = o.optString("album"),
        artwork = o.optString("artwork").ifBlank { null },
        durationMs = o.optLong("duration_ms"),
        youtubeUrl = o.optString("youtube_url").ifBlank { null },
        streamUrl = o.optString("stream_url").ifBlank { null },
        lyricVideo = o.optBoolean("lyric_video")
    )
}
