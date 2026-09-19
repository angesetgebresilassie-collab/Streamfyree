package com.streamfyree.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val api = StreamApi()
    val player: ExoPlayer = ExoPlayer.Builder(app).build()

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
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
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
        viewModelScope.launch(Dispatchers.IO) {
            val resolved = if (
                _mode.value == PlaybackMode.NATIVE && track.streamUrl.isNullOrBlank()
            ) {
                runCatching { api.resolve(track.id) }.getOrElse { track }
            } else {
                track
            }

            _current.value = resolved
            _queue.value = listOf(resolved) + _queue.value.filterNot { it.id == resolved.id }

            if (_mode.value == PlaybackMode.NATIVE && !resolved.streamUrl.isNullOrBlank()) {
                player.setMediaItem(MediaItem.fromUri(resolved.streamUrl!!))
                player.prepare()
                player.play()
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
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
