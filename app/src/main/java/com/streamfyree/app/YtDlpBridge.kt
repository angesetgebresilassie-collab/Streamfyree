package com.streamfyree.app

import com.chaquo.python.Python
import org.json.JSONObject

data class YtDlpResult(val id:String,val title:String,val artist:String,val youtubeUrl:String,val streamUrl:String,val durationMs:Long,val lyricVideo:Boolean)

class YtDlpBridge {
    private val module by lazy { Python.getInstance().getModule("ytbridge") }
    fun findLyricsAndResolve(artist:String,title:String):YtDlpResult {
        check(Python.isStarted()) { "Python runtime is not ready" }
        val o=JSONObject(module.callAttr("find_lyrics_and_resolve",artist,title).toString())
        return YtDlpResult(o.getString("id"),o.optString("title",title),o.optString("artist",artist),
            o.getString("youtube_url"),o.getString("stream_url"),o.optLong("duration_ms"),true)
    }
}
