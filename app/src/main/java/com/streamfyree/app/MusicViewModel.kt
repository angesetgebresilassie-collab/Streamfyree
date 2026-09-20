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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Application.streamfyreeDataStore by preferencesDataStore("streamfyree")

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val ytDlp = YtDlpBridge()
    private val itunes = ItunesApi()
    private val controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null

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
            _library.value = LibraryState(prefs[savedKey].orEmpty().mapNotNull { decodeTrack(it) })
            _queue.value = decodeList(prefs[queueKey])
            _history.value = decodeList(prefs[historyKey]).take(20)
        }

        refreshDiscover()

        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            val mediaController = runCatching { controllerFuture.get() }.getOrNull() ?: return@addListener
            controller = mediaController
            mediaController.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _current.value = _queue.value.firstOrNull { it.id == mediaItem?.mediaId } ?: _current.value
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) playNextAutomatic()
                }
            })
            _isPlaying.value = mediaController.isPlaying
        }, androidx.core.content.ContextCompat.getMainExecutor(app))
    }

    fun setMode(mode: PlaybackMode) { _mode.value = mode }

    fun refreshDiscover() {
        viewModelScope.launch(Dispatchers.IO) {
            val pool = listOf(
                "pop hits",
                "afrobeats",
                "hip hop",
                "r&b",
                "amapiano",
                "dance",
                "rock classics",
                "latin hits",
                "ethiopian music",
                "chill"
            )
            val tracks = mutableListOf<Track>()
            pool.shuffled().take(4).forEach { term ->
                runCatching { itunes.search(term) }
                    .onSuccess { tracks += it }
            }
            _discoverTracks.value = tracks
                .distinctBy { it.id }
                .shuffled()
                .take(20)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _state.value = _state.value.copy(query = query, loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { itunes.search(query) }
                .onSuccess { tracks ->
                    _state.value = _state.value.copy(loading = false, tracks = tracks, error = null)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(loading = false, error = error.message ?: "iTunes search failed")
                }
        }
    }

    fun play(track: Track) {
        when (_mode.value) {
            PlaybackMode.ONLINE -> playOnline(track)
            PlaybackMode.NATIVE -> playNative(track, false)
            PlaybackMode.AUTO -> playNative(track, true)
        }
    }

    private fun playOnline(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            val chosen = runCatching {
                val resolved = ytDlp.findLyricsAndResolve(track.artist, track.title)
                track.copy(
                    youtubeUrl = resolved.youtubeUrl,
                    streamUrl = resolved.streamUrl,
                    lyricVideo = true
                )
            }.getOrElse { error ->
                _state.value = _state.value.copy(
                    error = error.message ?: "Online playback search failed"
                )
                track
            }

            _current.value = chosen
            _queue.value = listOf(chosen) + _queue.value.filterNot { it.id == chosen.id }
            persistQueue(_queue.value)
            recordPlayed(chosen)

            val url = chosen.streamUrl
            if (!url.isNullOrBlank()) {
                val metadata = MediaMetadata.Builder()
                    .setTitle(chosen.title)
                    .setArtist(chosen.artist)
                    .setAlbumTitle(chosen.album)
                    .setArtworkUri(chosen.artwork?.let(Uri::parse))
                    .build()
                val mediaController = controller ?: runCatching { controllerFuture.get() }.getOrNull()
                mediaController?.let {
                    it.setMediaItem(
                        MediaItem.Builder()
                            .setMediaId(chosen.id)
                            .setUri(url)
                            .setMediaMetadata(metadata)
                            .build()
                    )
                    it.prepare()
                    it.play()
                }
            }
        }
    }

    private fun playNative(track: Track, fallbackToOnline: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val resolved = ytDlp.findLyricsAndResolve(track.artist, track.title)
                track.copy(
                    id = resolved.id,
                    title = resolved.title,
                    artist = resolved.artist,
                    youtubeUrl = resolved.youtubeUrl,
                    streamUrl = resolved.streamUrl,
                    durationMs = resolved.durationMs,
                    lyricVideo = true
                )
            }

            result.onSuccess { resolved ->
                _current.value = resolved
                _queue.value = listOf(resolved) + _queue.value.filterNot { it.id == resolved.id }
                persistQueue(_queue.value)
                recordPlayed(resolved)
                val url = resolved.streamUrl
                if (!url.isNullOrBlank()) {
                    val metadata = MediaMetadata.Builder()
                        .setTitle(resolved.title)
                        .setArtist(resolved.artist)
                        .setAlbumTitle(resolved.album)
                        .setArtworkUri(resolved.artwork?.let(Uri::parse))
                        .build()
                    val mediaController = controller ?: runCatching { controllerFuture.get() }.getOrNull()
                    mediaController?.let {
                        it.setMediaItem(
                            MediaItem.Builder()
                                .setMediaId(resolved.id)
                                .setUri(url)
                                .setMediaMetadata(metadata)
                                .build()
                        )
                        it.prepare()
                        it.play()
                    }
                }
                _state.value = _state.value.copy(error = null)
            }.onFailure { error ->
                if (fallbackToOnline) {
                    playOnline(track)
                    _state.value = _state.value.copy(error = "Native audio is unavailable, so Auto switched to Online.")
                } else {
                    _state.value = _state.value.copy(error = error.message ?: "Native playback failed")
                }
            }
        }
    }

    fun enqueue(track: Track) {
        if (_queue.value.none { it.id == track.id }) {
            _queue.value = _queue.value + track
            persistQueue(_queue.value)
        }
    }

    fun removeFromQueue(track: Track) {
        _queue.value = _queue.value.filterNot { it.id == track.id }
        persistQueue(_queue.value)
    }

    fun next() { playNextAutomatic() }

    private fun playNextAutomatic() {
        val index = _queue.value.indexOfFirst { it.id == _current.value?.id }
        if (index >= 0 && index + 1 < _queue.value.size) play(_queue.value[index + 1])
    }

    fun previous() {
        val index = _queue.value.indexOfFirst { it.id == _current.value?.id }
        if (index > 0) play(_queue.value[index - 1])
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0)) }

    fun refreshProgress() {
        controller?.let { _progress.value = PlaybackProgress(it.currentPosition, it.duration.coerceAtLeast(0)) }
    }

    fun saveTrack(track: Track) {
        val currentSaved = _library.value.saved.toMutableList()
        if (currentSaved.none { it.id == track.id }) {
            currentSaved += track
            _library.value = LibraryState(currentSaved)
            persistLibrary(currentSaved)
        }
    }

    fun unsaveTrack(track: Track) {
        val currentSaved = _library.value.saved.filterNot { it.id == track.id }
        _library.value = LibraryState(currentSaved)
        persistLibrary(currentSaved)
    }

    fun isSaved(track: Track) = _library.value.saved.any { it.id == track.id }

    fun clearQueue() {
        _queue.value = emptyList()
        persistQueue(emptyList())
    }

    private fun recordPlayed(track: Track) {
        _history.value = (listOf(track) + _history.value.filterNot { it.id == track.id }).take(20)
        persistQueue(_history.value, historyKey)
    }

    private fun persistLibrary(tracks: List<Track>) {
        viewModelScope.launch {
            getApplication<Application>().streamfyreeDataStore.edit { prefs ->
                prefs[savedKey] = tracks.map(::encodeTrack).toSet()
            }
        }
    }

    private fun persistQueue(tracks: List<Track>, key: androidx.datastore.preferences.core.Preferences.Key<String> = queueKey) {
        viewModelScope.launch {
            getApplication<Application>().streamfyreeDataStore.edit { prefs ->
                prefs[key] = encodeList(tracks)
            }
        }
    }

    private fun encodeList(tracks: List<Track>): String {
        val array = JSONArray()
        tracks.forEach { array.put(encodeTrack(it)) }
        return array.toString()
    }

    private fun decodeList(raw: String?): List<Track> = runCatching {
        if (raw.isNullOrBlank()) return emptyList()
        val array = JSONArray(raw)
        List(array.length()) { decodeTrack(array.getString(it)) }.filterNotNull()
    }.getOrDefault(emptyList())

    private fun encodeTrack(track: Track): String = JSONObject().apply {
        put("id", track.id)
        put("title", track.title)
        put("artist", track.artist)
        put("album", track.album)
        put("artwork", track.artwork ?: JSONObject.NULL)
        put("duration", track.durationMs)
        put("youtube", track.youtubeUrl ?: JSONObject.NULL)
        put("stream", track.streamUrl ?: JSONObject.NULL)
        put("lyric", track.lyricVideo)
    }.toString()

    private fun decodeTrack(raw: String): Track? = runCatching {
        val o = JSONObject(raw)
        Track(
            id = o.getString("id"),
            title = o.getString("title"),
            artist = o.getString("artist"),
            album = o.optString("album"),
            artwork = o.optString("artwork").ifBlank { null },
            durationMs = o.optLong("duration"),
            youtubeUrl = o.optString("youtube").ifBlank { null },
            streamUrl = o.optString("stream").ifBlank { null },
            lyricVideo = o.optBoolean("lyric")
        )
    }.getOrNull()

    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }
}
