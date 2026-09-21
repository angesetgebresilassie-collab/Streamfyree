package com.streamfyree.app

import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
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
 * Primary YouTube resolver.
 *
 * NewPipe Extractor parses YouTube's current web/internal responses and exposes
 * separate audio streams. We deliberately choose AudioStream only, so the
 * player never receives a video-only URL.
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
    }

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
