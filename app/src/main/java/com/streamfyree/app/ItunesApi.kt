package com.streamfyree.app

import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ItunesApi {
    private val client=OkHttpClient()
    fun search(query:String):List<Track>{
        val url="https://itunes.apple.com/search?term="+Uri.encode(query)+"&media=music&entity=song&limit=24"
        val r=client.newCall(Request.Builder().url(url).build()).execute()
        if(!r.isSuccessful) throw IOException("iTunes returned "+r.code)
        val a=JSONObject(r.body?.string()?:"{}").optJSONArray("results")?:JSONArray()
        return List(a.length()){i->
            val x=a.getJSONObject(i); val title=x.optString("trackName"); val artist=x.optString("artistName")
            Track("itunes:"+x.optLong("trackId"),title,artist,x.optString("collectionName"),
                x.optString("artworkUrl100").replace("100x100","600x600").ifBlank{null},
                x.optLong("trackTimeMillis"),
                "https://www.youtube.com/results?search_query="+Uri.encode("$artist $title lyrics"),null,true)
        }
    }
}
