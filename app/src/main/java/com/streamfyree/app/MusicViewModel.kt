package com.streamfyree.app

import android.app.Application
import android.content.ComponentName
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
import kotlinx.coroutines.launch

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val api = StreamApi()
    private val controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state

    private val _mode = MutableStateFlow(PlaybackMode.NATIVE)
    val mode: StateFlow<PlaybackMode> = _mode

    private val _current = MutableStateFlow<Track?>(null)
    val current: StateFlow<Track?> = _current

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    init {
        val token = SessionToken(
            app,
            ComponentName(app, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            runCatching { controller = controllerFuture.get() }
                .onSuccess { mediaController ->
                    mediaController.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            _current.value = _queue.value.firstOrNull { it.id == mediaItem?.mediaId }
                        }
                    })
                    _isPlaying.value = mediaController.isPlaying
                }
        }, androidx.core.content.ContextCompat.getMainExecutor(app))
    }

    fun setMode(mode: PlaybackMode) {
        _mode.value = mode
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _state.value = _state.value.copy(query = query, loading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.search(query) }
                .onSuccess {
                    _state.value = _state.value.copy(loading = false, tracks = it, error = null)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.message ?: "Search failed"
                    )
                }
        }
    }

    fun play(track: Track) {
        if (_mode.value == PlaybackMode.YOUTUBE) {
            _current.value = track
            _queue.value = listOf(track) + _queue.value.filterNot { it.id == track.id }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val resolved = if (track.streamUrl.isNullOrBlank()) {
                runCatching { api.resolve(track.id) }.getOrElse { track }
            } else {
                track
            }

            _current.value = resolved
            _queue.value = listOf(resolved) + _queue.value.filterNot { it.id == resolved.id }

            val url = resolved.streamUrl
            val mediaController = controller
            if (!url.isNullOrBlank() && mediaController != null) {
                val metadata = MediaMetadata.Builder()
                    .setTitle(resolved.title)
                    .setArtist(resolved.artist)
                    .setAlbumTitle(resolved.album)
                    .setArtworkUri(resolved.artwork?.let(android.net.Uri::parse))
                    .build()

                mediaController.setMediaItem(
                    MediaItem.Builder()
                        .setMediaId(resolved.id)
                        .setUri(url)
                        .setMediaMetadata(metadata)
                        .build()
                )
                mediaController.prepare()
                mediaController.play()
            }
        }
    }

    fun enqueue(track: Track) {
        if (_queue.value.none { it.id == track.id }) {
            _queue.value = _queue.value + track
        }
    }

    fun removeFromQueue(track: Track) {
        _queue.value = _queue.value.filterNot { it.id == track.id }
    }

    fun next() {
        val items = _queue.value
        val currentIndex = items.indexOfFirst { it.id == _current.value?.id }
        if (currentIndex >= 0 && currentIndex + 1 < items.size) {
            play(items[currentIndex + 1])
        }
    }

    fun previous() {
        val items = _queue.value
        val currentIndex = items.indexOfFirst { it.id == _current.value?.id }
        if (currentIndex > 0) {
            play(items[currentIndex - 1])
        }
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }
}
