package com.streamfyree.app

import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ItunesApi {
    private val client = OkHttpClient()

    fun search(query: String): List<Track> {
        val url = "https://itunes.apple.com/search?term=" + Uri.encode(query) + "&media=music&entity=song&limit=24"
        return parseResponse(url, isPodcast = false)
    }

    fun searchPodcasts(query: String): List<Track> {
        val url = "https://itunes.apple.com/search?term=" + Uri.encode(query) + "&media=podcast&entity=podcastEpisode&limit=20"
        val results = parseResponse(url, isPodcast = true)
        if (results.isNotEmpty()) return results
        val fallbackUrl = "https://itunes.apple.com/search?term=" + Uri.encode(query) + "&media=podcast&limit=20"
        return parseResponse(fallbackUrl, isPodcast = true)
    }

    private fun parseResponse(url: String, isPodcast: Boolean): List<Track> {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) throw IOException("iTunes returned " + response.code)
        val root = JSONObject(response.body?.string() ?: "{}")
        val array = root.optJSONArray("results") ?: JSONArray()
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val title = item.optString("trackName").ifBlank { item.optString("collectionName") }
            val artist = item.optString("artistName")
            val rawArt = item.optString("artworkUrl100")
            val art = if (rawArt.isNotBlank()) {
                rawArt.replace("100x100bb", "600x600bb").replace("100x100", "600x600")
            } else {
                item.optString("artworkUrl600").ifBlank { null }
            }
            val podcastId = item.optLong("trackId").takeIf { it != 0L } ?: item.optLong("collectionId")
            Track(
                id = (if (isPodcast) "podcast:" else "itunes:") + podcastId,
                title = title,
                artist = artist,
                album = item.optString("collectionName").ifBlank { if (isPodcast) "Podcast" else "Single" },
                artwork = art,
                durationMs = item.optLong("trackTimeMillis").takeIf { it > 0 } ?: (item.optLong("trackDurationMillis")),
                youtubeUrl = "https://www.youtube.com/results?search_query=" + Uri.encode("$artist $title podcast"),
                lyricVideo = false
            )
        }
    }
}
