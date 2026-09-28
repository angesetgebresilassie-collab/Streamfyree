package com.streamfyree.app

import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.util.concurrent.TimeUnit

data class NewPipeResult(
    val id: String,
    val title: String,
    val artist: String,
    val youtubeUrl: String,
    val streamUrl: String,
    val durationMs: Long
)

/**
 * Sole YouTube resolver (search + audio resolution).
 *
 * NewPipe Extractor parses YouTube's current web/internal responses directly,
 * with no external process or bundled runtime required. It exposes separate
 * audio streams, so the player never receives a video-only URL. This used to
 * share duties with a yt-dlp/Chaquopy Python bridge that only searched for a
 * candidate video; that bridge has been retired in favor of NewPipe's own
 * search, which has proven to be the reliable path end-to-end.
 */
class NewPipeBridge {
    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
        private val initLock = Any()
        @Volatile private var initialized = false

        private fun ensureInitialized() {
            if (initialized) return
            synchronized(initLock) {
                if (initialized) return
                NewPipe.init(
                    OkHttpDownloader(),
                    org.schabi.newpipe.extractor.localization.Localization.DEFAULT,
                    org.schabi.newpipe.extractor.localization.ContentCountry.DEFAULT
                )
                initialized = true
            }
        }

        // Ports the same "which result is actually a clean lyric/audio video"
        // heuristic that used to live in the yt-dlp search bridge, so search
        // quality doesn't regress now that it's gone.
        private fun tokens(value: String): Set<String> =
            Regex("[\\w']+").findAll(value.lowercase())
                .map { it.value }
                .filter { it.length > 1 }
                .toSet()

        private fun score(candidateTitle: String, artist: String, title: String): Int {
            val low = candidateTitle.lowercase()
            var s = 0
            if ("lyrics" in low || "lyric" in low) s += 100
            if ("official lyric" in low) s += 35
            if ("audio" in low) s += 10
            if ("visualizer" in low) s += 5
            if ("karaoke" in low) s -= 30
            if ("cover" in low) s -= 20
            if ("reaction" in low) s -= 50
            s += 8 * tokens(title).intersect(tokens(candidateTitle)).size
            s += 6 * tokens(artist).intersect(tokens(candidateTitle)).size
            return s
        }
    }

    /** Searches YouTube for the best lyric/audio video for [artist] — [title], then resolves it. */
    fun searchAndResolve(artist: String, title: String): NewPipeResult {
        ensureInitialized()
        val service = ServiceList.YouTube
        val query = "$artist $title lyrics"

        val queryHandler = service.searchQHFactory.fromQuery(query, listOf("videos"), "")
        val searchInfo = SearchInfo.getInfo(service, queryHandler)
        val candidates = searchInfo.relatedItems.filterIsInstance<StreamInfoItem>()
        if (candidates.isEmpty()) throw IOException("No YouTube results for \"$query\"")

        val best = candidates.maxByOrNull { score(it.name.orEmpty(), artist, title) }
            ?: throw IOException("No YouTube results for \"$query\"")

        return resolve(best.url, artist, title)
    }

    /** Resolves a known YouTube URL directly to a playable audio stream. */
    fun resolve(youtubeUrl: String, fallbackArtist: String, fallbackTitle: String): NewPipeResult {
        ensureInitialized()

        val info = StreamInfo.getInfo(youtubeUrl)
        val audio = info.audioStreams
            .asSequence()
            .filter { it.isUrl && it.content.isNotBlank() }
            .filter { it.getContent().startsWith("https://") || it.getContent().startsWith("http://") }
            .sortedWith(
                compareByDescending<AudioStream> { it.getAverageBitrate() }
                    .thenByDescending { it.getBitrate() }
            )
            .firstOrNull()
            ?: throw IOException("NewPipe found no direct audio stream")

        val streamUrl = audio.getContent()
        PlaybackRequestHeaders.set(
            mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to "https://www.youtube.com/"
            )
        )

        return NewPipeResult(
            id = info.id,
            title = info.name.ifBlank { fallbackTitle },
            artist = info.uploaderName.ifBlank { fallbackArtist },
            youtubeUrl = info.url.ifBlank { youtubeUrl },
            streamUrl = streamUrl,
            durationMs = info.duration.coerceAtLeast(0L) * 1000L
        )
    }
}

/**
 * Small Android downloader adapter required by NewPipe Extractor.
 * NewPipe Extractor itself is not an HTTP client; it delegates requests here.
 */
private class OkHttpDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val builder = OkHttpRequest.Builder()
            .url(request.url())

        request.headers().forEach { (name, values) ->
            values.forEach { value -> builder.addHeader(name, value) }
        }

        builder.header("User-Agent", request.headers()["User-Agent"]?.firstOrNull() ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")

        val body = request.dataToSend()?.toRequestBody()
        builder.method(request.httpMethod(), body)

        client.newCall(builder.build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBody,
                response.request.url.toString()
            )
        }
    }
}
