package com.streamfyree.app

import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

class ItunesApi {
    private val client = OkHttpClient()

    fun search(query: String): List<Track> {
        val url = "https://itunes.apple.com/search?term=" + Uri.encode(query) + "&media=music&entity=song&limit=24"
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) throw IOException("iTunes returned " + response.code)
        val array = JSONArray(response.body?.string() ?: "[]")
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val title = item.optString("trackName")
            val artist = item.optString("artistName")
            Track(
                id = "itunes:" + item.optLong("trackId"),
                title = title,
                artist = artist,
                album = item.optString("collectionName"),
                artwork = item.optString("artworkUrl100").replace("100x100", "600x600").ifBlank { null },
                durationMs = item.optLong("trackTimeMillis"),
                youtubeUrl = "https://www.youtube.com/results?search_query=" + Uri.encode(artist + " " + title + " lyrics"),
                lyricVideo = true
            )
        }
    }
}
