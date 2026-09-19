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
import org.json.JSONObject

private val Application.streamfyreeDataStore by preferencesDataStore("streamfyree")

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val api = StreamApi()
    private val controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null
    private val savedKey = stringSetPreferencesKey("saved_tracks")
    private val queueKey = stringPreferencesKey("queue_json")

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state
    private val _mode = MutableStateFlow(PlaybackMode.NATIVE)
    val mode: StateFlow<PlaybackMode> = _mode
    private val _current = MutableStateFlow<Track?>(null)
    val current: StateFlow<Track?> = _current
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue
    private val _library = MutableStateFlow(LibraryState())
    val library: StateFlow<LibraryState> = _library
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress: StateFlow<PlaybackProgress> = _progress

    init {
        viewModelScope.launch {
            val prefs = app.streamfyreeDataStore.data.first()
            _library.value = LibraryState(
                prefs[savedKey].orEmpty().mapNotNull { decodeTrack(it) }
            )
            _queue.value = decodeQueue(prefs[queueKey])
        }
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            runCatching { controller = controllerFuture.get() }.onSuccess { mediaController ->
                mediaController.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _current.value = _queue.value.firstOrNull { it.id == mediaItem?.mediaId }
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) playNextAutomatic()
                    }
                })
                _isPlaying.value = mediaController.isPlaying
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(app))
    }

    fun setMode(mode: PlaybackMode) { _mode.value = mode }

    fun search(query: String) {
        if (query.isBlank()) return
        _state.value = _state.value.copy(query = query, loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.search(query) }
                .onSuccess { _state.value = _state.value.copy(loading = false, tracks = it, error = null) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Search failed") }
        }
    }

    fun play(track: Track) {
        if (_mode.value == PlaybackMode.YOUTUBE) {
            _current.value = track
            _queue.value = listOf(track) + _queue.value.filterNot { it.id == track.id }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val resolved = if (track.streamUrl.isNullOrBlank()) runCatching { api.resolve(track.id) }.getOrElse { track } else track
            _current.value = resolved
            _queue.value = listOf(resolved) + _queue.value.filterNot { it.id == resolved.id }
            persistQueue(_queue.value)
            val url = resolved.streamUrl
            controller?.let { mediaController ->
                if (!url.isNullOrBlank()) {
                    val metadata = MediaMetadata.Builder()
                        .setTitle(resolved.title)
                        .setArtist(resolved.artist)
                        .setAlbumTitle(resolved.album)
                        .setArtworkUri(resolved.artwork?.let(Uri::parse))
                        .build()
                    mediaController.setMediaItem(
                        MediaItem.Builder().setMediaId(resolved.id).setUri(url).setMediaMetadata(metadata).build()
                    )
                    mediaController.prepare()
                    mediaController.play()
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

    fun next() {
        playNextAutomatic()
    }

    private fun playNextAutomatic() {
        val index = _queue.value.indexOfFirst { it.id == _current.value?.id }
        if (index >= 0 && index + 1 < _queue.value.size) play(_queue.value[index + 1])
    }

    fun previous() {
        val index = _queue.value.indexOfFirst { it.id == _current.value?.id }
        if (index > 0) play(_queue.value[index - 1])
    }

    fun togglePlayPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0)) }

    fun refreshProgress() {
        controller?.let {
            _progress.value = PlaybackProgress(it.currentPosition, it.duration.coerceAtLeast(0))
        }
    }

    fun saveTrack(track: Track) {
        val current = _library.value.saved.toMutableList()
        if (current.none { it.id == track.id }) {
            current += track
            _library.value = LibraryState(current)
            persistLibrary(current)
        }
    }

    fun unsaveTrack(track: Track) {
        val current = _library.value.saved.filterNot { it.id == track.id }
        _library.value = LibraryState(current)
        persistLibrary(current)
    }

    fun isSaved(track: Track) = _library.value.saved.any { it.id == track.id }

    fun clearQueue() { _queue.value = emptyList(); persistQueue(emptyList()) }

    private fun persistLibrary(tracks: List<Track>) {
        viewModelScope.launch {
            getApplication<Application>().streamfyreeDataStore.edit { prefs ->
                prefs[savedKey] = tracks.map(::encodeTrack).toSet()
            }
        }
    }

    private fun persistQueue(tracks: List<Track>) {
        viewModelScope.launch {
            getApplication<Application>().streamfyreeDataStore.edit { prefs ->
                val array = org.json.JSONArray()
                tracks.forEach { array.put(encodeTrack(it)) }
                prefs[queueKey] = array.toString()
            }
        }
    }

    private fun decodeQueue(raw: String?): List<Track> = runCatching {
        if (raw.isNullOrBlank()) return emptyList()
        val array = org.json.JSONArray(raw)
        List(array.length()) { decodeTrack(array.getString(it)) }.filterNotNull()
    }.getOrDefault(emptyList())

    private fun encodeTrack(t: Track): String = JSONObject().apply {
        put("id", t.id); put("title", t.title); put("artist", t.artist); put("album", t.album)
        put("artwork", t.artwork ?: JSONObject.NULL); put("duration", t.durationMs)
        put("youtube", t.youtubeUrl ?: JSONObject.NULL); put("lyric", t.lyricVideo)
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
            lyricVideo = o.optBoolean("lyric")
        )
    }.getOrNull()

    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }
}
