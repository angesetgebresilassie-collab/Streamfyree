package com.streamfyree.app

import android.net.Uri
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Spotify Web API client using the Client Credentials flow (app-only auth,
 * no user login required). This only ever reads public catalog data —
 * playlist metadata and track listings via search — and never streams audio.
 *
 * Spotify no longer serves 30-second preview clips or full playback to apps
 * without a signed-in Premium user and the Web Playback SDK, so a track that
 * comes from a Spotify playlist is played the same way as any other track in
 * this app: by title/artist, resolved through NewPipeBridge against YouTube.
 * Spotify only supplies the browse/feed layer here, not the audio.
 *
 * Requires a free Spotify app Client ID/Secret from
 * https://developer.spotify.com/dashboard, provided via BuildConfig
 * (see app/build.gradle.kts — set as env vars or your user-level
 * ~/.gradle/gradle.properties, never the repo's own gradle.properties).
 */
class SpotifyApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiresAtMs: Long = 0L

    private fun accessToken(): String {
        cachedToken?.let { if (System.currentTimeMillis() < tokenExpiresAtMs) return it }

        synchronized(this) {
            cachedToken?.let { if (System.currentTimeMillis() < tokenExpiresAtMs) return it }

            val clientId = BuildConfig.SPOTIFY_CLIENT_ID
            val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
            if (clientId.isBlank() || clientSecret.isBlank()) {
                throw IOException(
                    "Spotify isn't configured: set SPOTIFY_CLIENT_ID / SPOTIFY_CLIENT_SECRET " +
                        "(env vars or your user-level gradle.properties) and rebuild."
                )
            }

            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .header("Authorization", Credentials.basic(clientId, clientSecret))
                .post(FormBody.Builder().add("grant_type", "client_credentials").build())
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Spotify auth failed: HTTP ${response.code}")
                val json = JSONObject(response.body?.string() ?: "{}")
                val newToken = json.getString("access_token")
                val expiresInSec = json.optInt("expires_in", 3600)
                cachedToken = newToken
                // Refresh a little early so a request never races an expiring token.
                tokenExpiresAtMs = System.currentTimeMillis() + (expiresInSec - 60).coerceAtLeast(30) * 1000L
                return newToken
            }
        }
    }

    private fun get(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${accessToken()}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Spotify returned HTTP ${response.code}")
            return JSONObject(response.body?.string() ?: "{}")
        }
    }

    /** Searches public Spotify playlists matching [query] (a genre, mood, or theme). */
    fun searchPlaylists(query: String, limit: Int = 12): List<Playlist> {
        val url = "https://api.spotify.com/v1/search?q=" + Uri.encode(query) +
            "&type=playlist&limit=$limit"
        val items = get(url).optJSONObject("playlists")?.optJSONArray("items") ?: return emptyList()

        val results = mutableListOf<Playlist>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val name = item.optString("name")
            if (id.isBlank() || name.isBlank()) continue
            val owner = item.optJSONObject("owner")?.optString("display_name").orEmpty()
            val artwork = item.optJSONArray("images")?.optJSONObject(0)?.optString("url")
            results += Playlist(
                id = id,
                title = name,
                subtitle = owner.ifBlank { "Spotify playlist" },
                artwork = artwork,
                tracks = emptyList()
            )
        }
        return results
    }

    /** Fetches a playlist's name/artwork plus its full track list (paginated). */
    fun getPlaylist(playlistId: String): Playlist {
        val meta = get(
            "https://api.spotify.com/v1/playlists/$playlistId" +
                "?fields=id,name,owner.display_name,images"
        )
        val name = meta.optString("name").ifBlank { "Spotify playlist" }
        val owner = meta.optJSONObject("owner")?.optString("display_name").orEmpty()
        val playlistArtwork = meta.optJSONArray("images")?.optJSONObject(0)?.optString("url")

        val tracks = mutableListOf<Track>()
        var nextUrl: String? = "https://api.spotify.com/v1/playlists/$playlistId/tracks" +
            "?fields=items(track(id,name,artists(name),album(name,images))),next&limit=50"

        while (nextUrl != null) {
            val page = get(nextUrl)
            val items = page.optJSONArray("items") ?: break
            for (i in 0 until items.length()) {
                val track = items.optJSONObject(i)?.optJSONObject("track") ?: continue
                val id = track.optString("id")
                val title = track.optString("name")
                if (id.isBlank() || title.isBlank()) continue
                val artist = track.optJSONArray("artists")?.optJSONObject(0)?.optString("name").orEmpty()
                val album = track.optJSONObject("album")?.optString("name").orEmpty()
                val trackArtwork = track.optJSONObject("album")
                    ?.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                tracks += Track(
                    id = "spotify:$id",
                    title = title,
                    artist = artist,
                    album = album,
                    artwork = trackArtwork ?: playlistArtwork,
                    lyricVideo = false
                )
            }
            nextUrl = page.optString("next").ifBlank { null }
        }

        return Playlist(
            id = playlistId,
            title = name,
            subtitle = owner.ifBlank { "Spotify playlist" },
            artwork = playlistArtwork,
            tracks = tracks
        )
    }
}
