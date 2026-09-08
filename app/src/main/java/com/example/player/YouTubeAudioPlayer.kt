package com.example.player

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

enum class AudioSourceType {
    NONE,
    YOUTUBE_IFRAME,
    ITUNES_PREVIEW
}

data class PlayerState(
    val currentSong: Song? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentPositionSec: Float = 0f,
    val durationSec: Float = 0f,
    val sourceType: AudioSourceType = AudioSourceType.NONE,
    val activeYoutubeVideoId: String? = null,
    val errorMessage: String? = null,
    val isMuted: Boolean = false
) {
    val progressFraction: Float
        get() = if (durationSec > 0f) (currentPositionSec / durationSec).coerceIn(0f, 1f) else 0f

    val formattedCurrentTime: String
        get() {
            val totalSec = currentPositionSec.toInt()
            val m = totalSec / 60
            val s = totalSec % 60
            return "%d:%02d".format(m, s)
        }

    val formattedDuration: String
        get() {
            val totalSec = durationSec.toInt()
            val m = totalSec / 60
            val s = totalSec % 60
            return "%d:%02d".format(m, s)
        }
}

class YouTubeAudioPlayer(val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var webView: WebView? = null
    private var isIframeReady = false
    private var pendingVideoId: String? = null

    // Backup player for iTunes 30s audio previews when YouTube key is missing/pending
    private var mediaPlayer: MediaPlayer? = null
    private var previewProgressRunnable: Runnable? = null
    private var bufferingWatchdogRunnable: Runnable? = null

    var onPlaybackErrorListener: ((song: Song, failedVideoId: String, errorCode: Int) -> Unit)? = null

    val htmlContent: String = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <meta name="referrer" content="origin">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { width: 100%; height: 100%; background-color: #000000; overflow: hidden; }
                #player { width: 100%; height: 100%; }
            </style>
            <script src="https://www.youtube.com/iframe_api"></script>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var player;
                var progressInterval;

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        playerVars: {
                            'autoplay': 1,
                            'controls': 0,
                            'disablekb': 1,
                            'enablejsapi': 1,
                            'fs': 0,
                            'modestbranding': 1,
                            'playsinline': 1,
                            'rel': 0,
                            'origin': 'https://www.youtube.com',
                            'widget_referrer': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError,
                            'onAutoplayBlocked': onAutoplayBlocked
                        }
                    });
                }

                function onPlayerReady(event) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onReady();
                    }
                    startProgressTracker();
                }

                function onAutoplayBlocked() {
                    try {
                        if (player && player.playVideo) {
                            player.playVideo();
                        }
                    } catch(e) {}
                }

                function onPlayerStateChange(event) {
                    // -1 (unstarted), 0 (ended), 1 (playing), 2 (paused), 3 (buffering), 5 (video cued)
                    if (event.data === 5 || event.data === -1) {
                        try {
                            if (player && player.playVideo) {
                                player.playVideo();
                            }
                        } catch(e) {}
                    }
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onStateChange(event.data);
                    }
                }

                function onPlayerError(event) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onError(event.data);
                    }
                }

                // Global iframe message listener to catch internal embed errors (e.g., 152 / 153)
                window.addEventListener("message", function(e) {
                    if (e.data && typeof e.data === 'string') {
                        try {
                            var parsed = JSON.parse(e.data);
                            if (parsed.event === 'onError' || parsed.info === 'error') {
                                if (window.AndroidBridge) {
                                    window.AndroidBridge.onError(parsed.data || 152);
                                }
                            }
                        } catch(err) {}
                    }
                });

                function loadVideo(videoId) {
                    if (player) {
                        try {
                            if (player.loadVideoById) {
                                player.loadVideoById({
                                    videoId: videoId,
                                    startSeconds: 0
                                });
                            }
                            if (player.playVideo) {
                                player.playVideo();
                            }
                        } catch(e) {}
                    }
                }

                function playAudio() {
                    if (player && player.playVideo) {
                        player.playVideo();
                    }
                }

                function pauseAudio() {
                    if (player && player.pauseVideo) {
                        player.pauseVideo();
                    }
                }

                function seek(sec) {
                    if (player && player.seekTo) {
                        player.seekTo(sec, true);
                    }
                }

                function startProgressTracker() {
                    if (progressInterval) clearInterval(progressInterval);
                    progressInterval = setInterval(function() {
                        if (player && player.getCurrentTime && player.getDuration) {
                            try {
                                var curr = player.getCurrentTime() || 0;
                                var dur = player.getDuration() || 0;
                                if (window.AndroidBridge) {
                                    window.AndroidBridge.onProgress(curr, dur);
                                }
                            } catch(e) {}
                        }
                    }, 500);
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    fun attachWebView(view: WebView) {
        if (this.webView === view) return
        this.webView = view

        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            // IMPORTANT: Never override userAgentString with a fake Chrome mobile browser string!
            // Chrome User-Agents in WebView trigger YouTube's '152-13. Open vids in YouTube' app-intent error.
            // Using the system WebView user agent correctly identifies as an embedded app player.
        }

        view.webChromeClient = WebChromeClient()
        view.webViewClient = object : WebViewClient() {}

        view.addJavascriptInterface(object {
            @JavascriptInterface
            fun onReady() {
                mainHandler.post {
                    isIframeReady = true
                    pendingVideoId?.let { videoId ->
                        pendingVideoId = null
                        executeLoadVideo(videoId)
                    }
                }
            }

            @JavascriptInterface
            fun onStateChange(state: Int) {
                mainHandler.post {
                    when (state) {
                        -1 -> _playerState.value = _playerState.value.copy(status = PlaybackStatus.BUFFERING)
                        0 -> {
                            stopBufferingWatchdog()
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.ENDED)
                        }
                        1 -> {
                            stopBufferingWatchdog()
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.PLAYING, errorMessage = null)
                        }
                        2 -> {
                            stopBufferingWatchdog()
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                        }
                        3 -> _playerState.value = _playerState.value.copy(status = PlaybackStatus.BUFFERING)
                        5 -> _playerState.value = _playerState.value.copy(status = PlaybackStatus.BUFFERING)
                    }
                }
            }

            @JavascriptInterface
            fun onProgress(curr: Double, dur: Double) {
                mainHandler.post {
                    if (_playerState.value.sourceType == AudioSourceType.YOUTUBE_IFRAME) {
                        _playerState.value = _playerState.value.copy(
                            currentPositionSec = curr.toFloat(),
                            durationSec = if (dur > 0) dur.toFloat() else _playerState.value.durationSec
                        )
                    }
                }
            }

            @JavascriptInterface
            fun onError(code: Int) {
                mainHandler.post {
                    stopBufferingWatchdog()
                    val msg = when (code) {
                        2 -> "Invalid YouTube video ID parameter"
                        5 -> "HTML5 player playback error (5)"
                        100 -> "Video not found or removed"
                        101, 150 -> "Playback restricted by copyright holder"
                        152, 153 -> "YouTube embed playback restricted (Error $code)"
                        else -> "YouTube Player error code $code"
                    }
                    val currentSong = _playerState.value.currentSong
                    val activeId = _playerState.value.activeYoutubeVideoId
                    if (currentSong != null && !activeId.isNullOrBlank() && onPlaybackErrorListener != null) {
                        // Allow ViewModel to search for an alternative lyric video (e.g. 7clouds)
                        onPlaybackErrorListener?.invoke(currentSong, activeId, code)
                    } else {
                        // If no listener or alternate search, gracefully fallback to iTunes preview
                        fallbackToPreviewWithReason(currentSong, "$msg. Switched to iTunes preview.")
                    }
                }
            }
        }, "AndroidBridge")

        view.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
    }

    fun fallbackToPreviewWithReason(song: Song?, reason: String) {
        stopBufferingWatchdog()
        val fallbackPreview = song?.previewUrl
        if (!fallbackPreview.isNullOrBlank()) {
            _playerState.value = _playerState.value.copy(
                errorMessage = reason
            )
            playPreview(fallbackPreview, song)
        } else {
            _playerState.value = _playerState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = reason
            )
        }
    }

    fun playSongWithYouTube(song: Song, videoId: String) {
        stopPreview()
        stopBufferingWatchdog()

        _playerState.value = _playerState.value.copy(
            currentSong = song.copy(youtubeVideoId = videoId),
            status = PlaybackStatus.BUFFERING,
            sourceType = AudioSourceType.YOUTUBE_IFRAME,
            activeYoutubeVideoId = videoId,
            currentPositionSec = 0f,
            durationSec = if (song.durationMs > 0) song.durationMs / 1000f else 0f,
            errorMessage = null
        )

        // Start 15-second watchdog: if YouTube hangs on buffering (due to 152/150 restrictions), fallback
        startBufferingWatchdog()

        if (isIframeReady && webView != null) {
            executeLoadVideo(videoId)
        } else {
            pendingVideoId = videoId
        }
    }

    private fun startBufferingWatchdog() {
        stopBufferingWatchdog()
        val runnable = Runnable {
            val current = _playerState.value
            if (current.sourceType == AudioSourceType.YOUTUBE_IFRAME && current.status == PlaybackStatus.BUFFERING) {
                val currentSong = current.currentSong
                val activeId = current.activeYoutubeVideoId
                if (currentSong != null && !activeId.isNullOrBlank() && onPlaybackErrorListener != null) {
                    onPlaybackErrorListener?.invoke(currentSong, activeId, 152)
                } else {
                    fallbackToPreviewWithReason(
                        currentSong,
                        "YouTube stream restricted. Switched to iTunes audio."
                    )
                }
            }
        }
        bufferingWatchdogRunnable = runnable
        mainHandler.postDelayed(runnable, 15000)
    }

    private fun stopBufferingWatchdog() {
        bufferingWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        bufferingWatchdogRunnable = null
    }

    fun playPreview(previewUrl: String, song: Song? = null) {
        stopBufferingWatchdog()
        stopYouTube()
        stopPreview()

        val targetSong = song ?: _playerState.value.currentSong
        _playerState.value = _playerState.value.copy(
            currentSong = targetSong,
            status = PlaybackStatus.BUFFERING,
            sourceType = AudioSourceType.ITUNES_PREVIEW,
            currentPositionSec = 0f,
            durationSec = 30f,
            errorMessage = null
        )

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(previewUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                    val durSec = mp.duration / 1000f
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.PLAYING,
                        durationSec = if (durSec > 0) durSec else 30f
                    )
                    startPreviewProgress()
                }
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.ENDED,
                        currentPositionSec = _playerState.value.durationSec
                    )
                    stopPreviewProgress()
                }
                setOnErrorListener { _, what, extra ->
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = "Audio preview error: $what/$extra"
                    )
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _playerState.value = _playerState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = "Failed to load preview: ${e.localizedMessage}"
            )
        }
    }

    fun togglePlayPause() {
        val current = _playerState.value
        if (current.status == PlaybackStatus.PLAYING) {
            pause()
        } else if (current.status == PlaybackStatus.PAUSED || current.status == PlaybackStatus.ENDED) {
            resume()
        }
    }

    fun resume() {
        when (_playerState.value.sourceType) {
            AudioSourceType.YOUTUBE_IFRAME -> {
                webView?.evaluateJavascript("playAudio();", null)
                _playerState.value = _playerState.value.copy(status = PlaybackStatus.PLAYING)
            }
            AudioSourceType.ITUNES_PREVIEW -> {
                mediaPlayer?.let {
                    it.start()
                    _playerState.value = _playerState.value.copy(status = PlaybackStatus.PLAYING)
                    startPreviewProgress()
                }
            }
            AudioSourceType.NONE -> {}
        }
    }

    fun pause() {
        when (_playerState.value.sourceType) {
            AudioSourceType.YOUTUBE_IFRAME -> {
                webView?.evaluateJavascript("pauseAudio();", null)
                _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
            }
            AudioSourceType.ITUNES_PREVIEW -> {
                mediaPlayer?.let {
                    if (it.isPlaying) it.pause()
                    _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                    stopPreviewProgress()
                }
            }
            AudioSourceType.NONE -> {}
        }
    }

    fun seekTo(seconds: Float) {
        when (_playerState.value.sourceType) {
            AudioSourceType.YOUTUBE_IFRAME -> {
                webView?.evaluateJavascript("seek($seconds);", null)
                _playerState.value = _playerState.value.copy(currentPositionSec = seconds)
            }
            AudioSourceType.ITUNES_PREVIEW -> {
                mediaPlayer?.let {
                    val ms = (seconds * 1000).toInt()
                    it.seekTo(ms)
                    _playerState.value = _playerState.value.copy(currentPositionSec = seconds)
                }
            }
            AudioSourceType.NONE -> {}
        }
    }

    private fun executeLoadVideo(videoId: String) {
        webView?.evaluateJavascript("loadVideo('$videoId');", null)
    }

    private fun stopYouTube() {
        stopBufferingWatchdog()
        webView?.evaluateJavascript("pauseAudio();", null)
    }

    private fun stopPreview() {
        stopPreviewProgress()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
        }
    }

    private fun startPreviewProgress() {
        stopPreviewProgress()
        val runnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val pos = mp.currentPosition / 1000f
                        _playerState.value = _playerState.value.copy(currentPositionSec = pos)
                        mainHandler.postDelayed(this, 500)
                    }
                }
            }
        }
        previewProgressRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun stopPreviewProgress() {
        previewProgressRunnable?.let { mainHandler.removeCallbacks(it) }
        previewProgressRunnable = null
    }

    fun release() {
        stopBufferingWatchdog()
        stopPreview()
        stopYouTube()
        webView?.destroy()
        webView = null
    }
}
