package com.streamfyree.app

import com.chaquo.python.Python
import org.json.JSONObject

data class YtDlpResult(
    val id: String,
    val title: String,
    val artist: String,
    val youtubeUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val lyricVideo: Boolean
)

class YtDlpBridge {
    private val module by lazy { Python.getInstance().getModule("ytbridge") }

    fun findLyricsAndResolve(artist: String, title: String): YtDlpResult {
        check(Python.isStarted()) {
            "yt-dlp runtime is not ready"
        }

        val raw = module.callAttr("find_lyrics_and_resolve", artist, title).toString()
        val o = JSONObject(raw)

        return YtDlpResult(
            id = o.getString("id"),
            title = o.optString("title", title),
            artist = o.optString("artist", artist),
            youtubeUrl = o.getString("youtube_url"),
            streamUrl = o.getString("stream_url"),
            durationMs = o.optLong("duration_ms", 0L),
            lyricVideo = true
        )
    }
}
