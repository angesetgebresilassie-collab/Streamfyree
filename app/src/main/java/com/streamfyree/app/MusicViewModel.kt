package com.streamfyree.app

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Application.streamfyreeDataStore by preferencesDataStore("streamfyree")

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val ytDlp = YtDlpBridge()
    private val itunes = ItunesApi()
    private val controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null
    private var controllerReady = false

    private val savedKey = stringSetPreferencesKey("saved_tracks")
    private val queueKey = stringPreferencesKey("queue_json")
    private val historyKey = stringPreferencesKey("history_json")

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state
    private val _mode = MutableStateFlow(PlaybackMode.AUTO)
    val mode: StateFlow<PlaybackMode> = _mode
    private val _current = MutableStateFlow<Track?>(null)
    val current: StateFlow<Track?> = _current
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue
    private val _library = MutableStateFlow(LibraryState())
    val library: StateFlow<LibraryState> = _library
    private val _history = MutableStateFlow<List<Track>>(emptyList())
    val history: StateFlow<List<Track>> = _history
    private val _discoverTracks = MutableStateFlow<List<Track>>(emptyList())
    val discoverTracks: StateFlow<List<Track>> = _discoverTracks
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress: StateFlow<PlaybackProgress> = _progress

    init {
        viewModelScope.launch {
            val prefs = app.streamfyreeDataStore.data.first()
            _library.value = LibraryState(prefs[savedKey].orEmpty().mapNotNull(::decodeTrack))
            _queue.value = decodeList(prefs[queueKey])
            _history.value = decodeList(prefs[historyKey]).take(20)
        }
        refreshDiscover()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            val c = runCatching { controllerFuture.get() }.getOrNull() ?: return@addListener
            controller = c
            controllerReady = true
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(v: Boolean) { _isPlaying.value = v }
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    _current.value = _queue.value.firstOrNull { it.id == item?.mediaId } ?: _current.value
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) playNextAutomatic()
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _state.value = _state.value.copy(error = "Playback error: ${error.message}")
                }
            })
            _isPlaying.value = c.isPlaying
        }, androidx.core.content.ContextCompat.getMainExecutor(app))
    }

    fun setMode(mode: PlaybackMode) { _mode.value = mode }

    fun refreshDiscover() {
        viewModelScope.launch(Dispatchers.IO) {
            val pool = listOf("pop hits","afrobeats","hip hop","r&b","amapiano","dance","rock classics","latin hits","ethiopian music","chill")
            val tracks = mutableListOf<Track>()
            pool.shuffled().take(4).forEach { term -> runCatching { itunes.search(term) }.onSuccess { tracks += it } }
            _discoverTracks.value = tracks.distinctBy { it.id }.shuffled().take(20)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _state.value = _state.value.copy(query = query, loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { itunes.search(query) }
                .onSuccess { _state.value = _state.value.copy(loading = false, tracks = it, error = null) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Search failed") }
        }
    }

    fun play(track: Track) {
        when (_mode.value) {
            PlaybackMode.ONLINE -> resolveAndPlay(track, true)
            PlaybackMode.NATIVE -> resolveAndPlay(track, true)
            PlaybackMode.AUTO -> resolveAndPlay(track, true)
        }
    }

    private fun resolveAndPlay(track: Track, native: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var attempts = 0
                while (!controllerReady && attempts < 50) {
                    delay(100)
                    attempts++
                }
                if (!controllerReady) {
                    _state.value = _state.value.copy(error = "Player not ready. Please try again.")
                    return@launch
                }

                _state.value = _state.value.copy(loading = true, error = null)

                val resolved = if (native) {
                    val r = ytDlp.findLyricsAndResolve(track.artist, track.title)
                    track.copy(
                        id = r.id,
                        title = r.title,
                        artist = r.artist,
                        youtubeUrl = r.youtubeUrl,
                        streamUrl = r.streamUrl,
                        durationMs = r.durationMs,
                        lyricVideo = true
                    )
                } else {
                    track.copy(
                        youtubeUrl = "https://www.youtube.com/results?search_query=" +
                            java.net.URLEncoder.encode("${track.artist} ${track.title} lyrics", "UTF-8"),
                        lyricVideo = true
                    )
                }

                _state.value = _state.value.copy(loading = false, error = null)
                _current.value = resolved
                _queue.value = listOf(resolved) + _queue.value.filterNot { it.id == resolved.id }
                persistQueue(_queue.value)
                recordPlayed(resolved)

                val url = resolved.streamUrl
                if (url.isNullOrBlank()) {
                    _state.value = _state.value.copy(error = "No stream URL available for this track")
                    return@launch
                }

                val metadata = MediaMetadata.Builder()
                    .setTitle(resolved.title)
                    .setArtist(resolved.artist)
                    .setAlbumTitle(resolved.album)
                    .setArtworkUri(resolved.artwork?.let { Uri.parse(it) })
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(resolved.id)
                    .setUri(url)
                    .setMediaMetadata(metadata)
                    .build()

                val c = controller
                if (c == null) {
                    _state.value = _state.value.copy(error = "Player connection lost")
                    return@launch
                }

                // MediaController is a Player implementation and must be accessed
                // from its application looper. The resolver runs on Dispatchers.IO,
                // so marshal every controller call back to the main looper.
                withContext(Dispatchers.Main.immediate) {
                    c.stop()
                    c.setMediaItem(mediaItem)
                    c.prepare()
                    c.playWhenReady = true
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to play track"
                )
                e.printStackTrace()
            }
        }
    }

    fun enqueue(track: Track) {
        if (_queue.value.none { it.id == track.id }) {
            _queue.value += track
            persistQueue(_queue.value)
        }
    }
    fun removeFromQueue(track: Track) {
        _queue.value = _queue.value.filterNot { it.id == track.id }
        persistQueue(_queue.value)
    }
    fun next() { playNextAutomatic() }
    private fun playNextAutomatic() {
        val i = _queue.value.indexOfFirst { it.id == _current.value?.id }
        if (i >= 0 && i + 1 < _queue.value.size) play(_queue.value[i + 1])
    }
    fun previous() {
        val i = _queue.value.indexOfFirst { it.id == _current.value?.id }
        if (i > 0) play(_queue.value[i - 1])
    }
    fun togglePlayPause() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            controller?.let { if (it.isPlaying) it.pause() else it.play() }
        }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            controller?.seekTo(positionMs.coerceAtLeast(0))
        }
    }

    fun refreshProgress() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            controller?.let {
                _progress.value = PlaybackProgress(it.currentPosition, it.duration.coerceAtLeast(0))
            }
        }
    }

    fun saveTrack(track: Track) {
        if (_library.value.saved.none { it.id == track.id }) {
            val saved = _library.value.saved + track
            _library.value = LibraryState(saved)
            persistLibrary(saved)
        }
    }
    fun unsaveTrack(track: Track) {
        val saved = _library.value.saved.filterNot { it.id == track.id }
        _library.value = LibraryState(saved)
        persistLibrary(saved)
    }
    fun isSaved(track: Track) = _library.value.saved.any { it.id == track.id }
    fun clearQueue() { _queue.value = emptyList(); persistQueue(emptyList()) }

    private fun recordPlayed(track: Track) {
        _history.value = (listOf(track) + _history.value.filterNot { it.id == track.id }).take(20)
        persistQueue(_history.value, historyKey)
    }
    private fun persistLibrary(tracks: List<Track>) = viewModelScope.launch {
        getApplication<Application>().streamfyreeDataStore.edit { it[savedKey] = tracks.map(::encodeTrack).toSet() }
    }
    private fun persistQueue(tracks: List<Track>, key: androidx.datastore.preferences.core.Preferences.Key<String> = queueKey) = viewModelScope.launch {
        getApplication<Application>().streamfyreeDataStore.edit { it[key] = encodeList(tracks) }
    }
    private fun encodeList(tracks: List<Track>): String = JSONArray().apply { tracks.forEach { put(encodeTrack(it)) } }.toString()
    private fun decodeList(raw: String?): List<Track> = runCatching {
        if (raw.isNullOrBlank()) return emptyList()
        val a = JSONArray(raw)
        List(a.length()) { decodeTrack(a.getString(it)) }.filterNotNull()
    }.getOrDefault(emptyList())
    private fun encodeTrack(t: Track): String = JSONObject().apply {
        put("id", t.id); put("title", t.title); put("artist", t.artist); put("album", t.album)
        put("artwork", t.artwork ?: JSONObject.NULL); put("duration", t.durationMs)
        put("youtube", t.youtubeUrl ?: JSONObject.NULL); put("stream", t.streamUrl ?: JSONObject.NULL); put("lyric", t.lyricVideo)
    }.toString()
    private fun decodeTrack(raw: String): Track? = runCatching {
        val o = JSONObject(raw)
        Track(o.getString("id"), o.getString("title"), o.getString("artist"), o.optString("album"),
            o.optString("artwork").ifBlank { null }, o.optLong("duration"),
            o.optString("youtube").ifBlank { null }, o.optString("stream").ifBlank { null }, o.optBoolean("lyric"))
    }.getOrNull()
    override fun onCleared() {
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
