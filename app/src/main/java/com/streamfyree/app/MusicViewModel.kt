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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Application.streamfyreeDataStore by preferencesDataStore("streamfyree")

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val newPipe = NewPipeBridge()
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
    val queueTrackList: StateFlow<List<Track>> = _queue
    private val _library = MutableStateFlow(LibraryState())
    val library: StateFlow<LibraryState> = _library
    private val _history = MutableStateFlow<List<Track>>(emptyList())
    val history: StateFlow<List<Track>> = _history
    private val _discoverTracks = MutableStateFlow<List<Track>>(emptyList())
    val discoverTracks: StateFlow<List<Track>> = _discoverTracks
    private val _podcastTracks = MutableStateFlow<List<Track>>(emptyList())
    val podcastTracks: StateFlow<List<Track>> = _podcastTracks
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes
    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress: StateFlow<PlaybackProgress> = _progress
    private val _repeat = MutableStateFlow(RepeatMode.OFF)
    val repeat: StateFlow<RepeatMode> = _repeat
    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle

    // --- StateFlows required by SpotPlayer UI ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MusicTrack>>(emptyList())
    val searchResults: StateFlow<List<MusicTrack>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _musicQueue = MutableStateFlow<List<MusicTrack>>(emptyList())
    val queue: StateFlow<List<MusicTrack>> = _musicQueue.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0: OFF, 1: ALL, 2: ONE
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _lyricLines = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyricLines.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()

    private val _isDebugOverlayVisible = MutableStateFlow(false)
    val isDebugOverlayVisible: StateFlow<Boolean> = _isDebugOverlayVisible.asStateFlow()

    val logs = DebugLogger.logs

    init {
        DebugLogger.info("PLAYER", "MusicViewModel initialized; waiting for MediaController")
        viewModelScope.launch {
            val prefs = app.streamfyreeDataStore.data.first()
            _library.value = LibraryState(prefs[savedKey].orEmpty().mapNotNull(::decodeTrack))
            val restoredQueue = decodeList(prefs[queueKey])
            _queue.value = restoredQueue
            _musicQueue.value = restoredQueue.map { it.toMusicTrack() }
            _history.value = decodeList(prefs[historyKey]).take(20)
        }
        refreshDiscover()
        loadPodcasts()

        // Progress polling loop
        viewModelScope.launch {
            while (true) {
                delay(300)
                controller?.let { c ->
                    val pos = c.currentPosition.coerceAtLeast(0L)
                    val dur = c.duration.coerceAtLeast(0L)
                    _playbackPositionMs.value = pos
                    _durationMs.value = dur
                    _progress.value = PlaybackProgress(pos, dur)

                    // Update active lyric index
                    val currentLines = _lyricLines.value
                    if (currentLines.isNotEmpty()) {
                        _currentLyricIndex.value = LyricsManager.getActiveLineIndex(currentLines, pos)
                    }
                }
            }
        }

        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            val c = runCatching { controllerFuture.get() }.getOrNull() ?: return@addListener
            controller = c
            controllerReady = true
            DebugLogger.info("PLAYER", "MediaController connected successfully")
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(v: Boolean) {
                    _isPlaying.value = v
                    _isBuffering.value = false
                    DebugLogger.info("PLAYER", "isPlaying=$v")
                }
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    val matchingTrack = _queue.value.firstOrNull { it.id == item?.mediaId } ?: _current.value
                    if (matchingTrack != null) {
                        _current.value = matchingTrack
                        _currentTrack.value = matchingTrack.toMusicTrack()
                        val idx = _queue.value.indexOfFirst { it.id == matchingTrack.id }
                        if (idx >= 0) _currentTrackIndex.value = idx
                    }
                }
                override fun onPlaybackStateChanged(state: Int) {
                    DebugLogger.info("PLAYER", "Playback state changed: $state")
                    _isBuffering.value = (state == Player.STATE_BUFFERING)
                    if (state == Player.STATE_ENDED) onTrackEnded()
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val detail = buildString {
                        append("PlaybackException code=${error.errorCode} (${error.errorCodeName})")
                        append("\nmessage=${error.message}")
                        append("\nmediaId=${c.currentMediaItem?.mediaId}")
                        append("\nuri=${c.currentMediaItem?.localConfiguration?.uri}")
                        error.cause?.let { append("\ncause=${it::class.java.name}: ${it.message}") }
                    }
                    DebugLogger.error("EXOPLAYER", detail, error)
                    val errText = "Playback error: ${error.message ?: error.errorCodeName}"
                    _state.value = _state.value.copy(error = errText)
                    _errorMessage.value = errText
                }
            })
            _isPlaying.value = c.isPlaying
        }, androidx.core.content.ContextCompat.getMainExecutor(app))
    }

    // SpotPlayer UI actions
    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSearch() {
        val q = _searchQuery.value
        if (q.isBlank()) return
        _isSearching.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { itunes.search(q) }
                .onSuccess { tracks ->
                    _searchResults.value = tracks.map { it.toMusicTrack() }
                    _isSearching.value = false
                }
                .onFailure {
                    _isSearching.value = false
                    _errorMessage.value = it.message ?: "Search failed"
                }
        }
    }

    fun onTrackSelect(musicTrack: MusicTrack) {
        val track = musicTrack.toTrack()
        play(track)
    }

    fun onPlayPauseToggle() {
        togglePlayPause()
    }

    fun onSeekTo(posMs: Long) {
        seekTo(posMs)
    }

    fun onSkipToNext() {
        next()
    }

    fun onSkipToPrevious() {
        previous()
    }

    fun onToggleRepeat() {
        cycleRepeat()
        _repeatMode.value = when (_repeat.value) {
            RepeatMode.OFF -> 0
            RepeatMode.ALL -> 1
            RepeatMode.ONE -> 2
        }
    }

    fun onToggleShuffle() {
        toggleShuffle()
        _shuffleMode.value = _shuffle.value
    }

    fun onSetPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        viewModelScope.launch(Dispatchers.Main.immediate) {
            controller?.setPlaybackSpeed(speed)
        }
    }

    fun onSelectQueueTrack(index: Int) {
        val q = _queue.value
        if (index in q.indices) {
            _currentTrackIndex.value = index
            play(q[index])
        }
    }

    fun onToggleDebugOverlay() {
        _isDebugOverlayVisible.value = !_isDebugOverlayVisible.value
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

    fun loadPodcasts(query: String = "popular podcasts") {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { itunes.searchPodcasts(query) }
                .onSuccess { _podcastTracks.value = it.distinctBy { p -> p.id } }
        }
    }

    fun playMix(query: String) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { itunes.search(query) }
                .onSuccess { tracks ->
                    if (tracks.isNotEmpty()) {
                        _queue.value = tracks
                        _musicQueue.value = tracks.map { it.toMusicTrack() }
                        persistQueue(tracks)
                        withContext(Dispatchers.Main) { play(tracks.first()) }
                    } else {
                        _state.value = _state.value.copy(loading = false, error = "No mix tracks found for '$query'")
                    }
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = "Failed to load mix")
                }
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        if (minutes == null || minutes <= 0) return

        sleepTimerJob = viewModelScope.launch {
            delay(minutes * 60 * 1000L)
            withContext(Dispatchers.Main.immediate) {
                controller?.pause()
                _sleepTimerMinutes.value = null
                DebugLogger.info("SLEEP_TIMER", "Sleep timer finished; playback paused.")
            }
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
        _current.value = track
        _currentTrack.value = track.toMusicTrack()
        loadLyricsForTrack(track)
        DebugLogger.info("RESOLVE", "Starting playback for '${track.title}' — ${track.artist}")
        _state.value = _state.value.copy(loading = true, error = null)
        _isBuffering.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var attempts = 0
                while (!controllerReady && attempts < 50) {
                    delay(100)
                    attempts++
                }
                if (!controllerReady) {
                    DebugLogger.error("PLAYER", "MediaController did not become ready within 5 seconds")
                    val err = "Player not ready. Please try again."
                    _state.value = _state.value.copy(loading = false, error = err)
                    _errorMessage.value = err
                    _isBuffering.value = false
                    return@launch
                }

                val resolved = if (native) {
                    DebugLogger.info("YTDLP", "Resolving stream for '${track.artist} — ${track.title}'")
                    val search = ytDlp.findLyricsAndResolve(track.artist, track.title)
                    DebugLogger.info("NEWPIPE", "Resolving fresh audio stream from video=${search.id}")
                    val r = runCatching {
                        newPipe.resolve(search.youtubeUrl, search.artist, search.title)
                    }.onFailure {
                        DebugLogger.error("NEWPIPE", "NewPipe extraction failed; using yt-dlp audio URL fallback: ${it.message}", it)
                    }.getOrNull()

                    if (r != null && r.streamUrl.isNotBlank()) {
                        DebugLogger.info("NEWPIPE", "Resolved audio-only stream; URL present=${r.streamUrl.isNotBlank()}")
                        track.copy(
                            id = r.id,
                            title = track.title.ifBlank { r.title },
                            artist = track.artist.ifBlank { r.artist },
                            album = track.album,
                            artwork = track.artwork,
                            youtubeUrl = r.youtubeUrl,
                            streamUrl = r.streamUrl,
                            durationMs = if (r.durationMs > 0) r.durationMs else track.durationMs,
                            lyricVideo = true
                        )
                    } else {
                        DebugLogger.info("YTDLP", "Using yt-dlp fallback stream for video=${search.id}")
                        PlaybackRequestHeaders.set(search.httpHeaders)
                        track.copy(
                            id = search.id,
                            title = track.title.ifBlank { search.title },
                            artist = track.artist.ifBlank { search.artist },
                            album = track.album,
                            artwork = track.artwork,
                            youtubeUrl = search.youtubeUrl,
                            streamUrl = search.streamUrl,
                            durationMs = if (search.durationMs > 0) search.durationMs else track.durationMs,
                            lyricVideo = true
                        )
                    }
                } else {
                    track.copy(
                        youtubeUrl = "https://www.youtube.com/results?search_query=" +
                            java.net.URLEncoder.encode("${track.artist} ${track.title} lyrics", "UTF-8"),
                        lyricVideo = true
                    )
                }

                _state.value = _state.value.copy(loading = false, error = null)
                _current.value = resolved
                _currentTrack.value = resolved.toMusicTrack()
                loadLyricsForTrack(resolved)

                val newQueueList = listOf(resolved) + _queue.value.filterNot { it.id == resolved.id }
                _queue.value = newQueueList
                _musicQueue.value = newQueueList.map { it.toMusicTrack() }
                _currentTrackIndex.value = 0
                persistQueue(_queue.value)
                recordPlayed(resolved)

                val url = resolved.streamUrl
                if (url.isNullOrBlank()) {
                    DebugLogger.error("RESOLVE", "Resolver returned an empty stream URL")
                    val err = "No stream URL available for this track"
                    _state.value = _state.value.copy(error = err)
                    _errorMessage.value = err
                    _isBuffering.value = false
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
                    DebugLogger.error("PLAYER", "MediaController became null after stream resolution")
                    val err = "Player connection lost"
                    _state.value = _state.value.copy(error = err)
                    _errorMessage.value = err
                    _isBuffering.value = false
                    return@launch
                }

                DebugLogger.info("PLAYER", "Preparing resolved stream; URL host=${runCatching { Uri.parse(url).host }.getOrNull() ?: "unknown"}")
                withContext(Dispatchers.Main.immediate) {
                    c.stop()
                    c.setMediaItem(mediaItem)
                    c.prepare()
                    c.playWhenReady = true
                }
            } catch (e: Exception) {
                DebugLogger.error("RESOLVE", "Playback pipeline failed: ${e.message ?: e::class.java.simpleName}", e)
                val errText = e.message ?: "Failed to play track"
                _state.value = _state.value.copy(loading = false, error = errText)
                _errorMessage.value = errText
                _isBuffering.value = false
                e.printStackTrace()
            }
        }
    }

    fun enqueue(track: Track) {
        if (_queue.value.none { it.id == track.id }) {
            val updated = _queue.value + track
            _queue.value = updated
            _musicQueue.value = updated.map { it.toMusicTrack() }
            persistQueue(updated)
        }
    }

    fun removeFromQueue(track: Track) {
        val updated = _queue.value.filterNot { it.id == track.id }
        _queue.value = updated
        _musicQueue.value = updated.map { it.toMusicTrack() }
        persistQueue(updated)
    }

    fun next() {
        val q = _queue.value
        if (q.isEmpty()) return
        val i = q.indexOfFirst { it.id == _current.value?.id }
        when {
            i >= 0 && i + 1 < q.size -> play(q[i + 1])
            _repeat.value == RepeatMode.ALL -> play(q.first())
        }
    }

    private fun onTrackEnded() {
        when (_repeat.value) {
            RepeatMode.ONE -> _current.value?.let { play(it) }
            else -> next()
        }
    }

    fun previous() {
        val pos = controller?.currentPosition ?: 0L
        if (pos > 3000L) { seekTo(0L); return }
        val q = _queue.value
        if (q.isEmpty()) return
        val i = q.indexOfFirst { it.id == _current.value?.id }
        when {
            i > 0 -> play(q[i - 1])
            _repeat.value == RepeatMode.ALL -> play(q.last())
            else -> seekTo(0L)
        }
    }

    fun cycleRepeat() {
        _repeat.value = when (_repeat.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = when (_repeat.value) {
            RepeatMode.OFF -> 0
            RepeatMode.ALL -> 1
            RepeatMode.ONE -> 2
        }
    }

    fun toggleShuffle() {
        val enabled = !_shuffle.value
        _shuffle.value = enabled
        _shuffleMode.value = enabled
        if (enabled) {
            val cur = _current.value
            val rest = _queue.value.filterNot { it.id == cur?.id }.shuffled()
            val updated = listOfNotNull(cur) + rest
            _queue.value = updated
            _musicQueue.value = updated.map { it.toMusicTrack() }
            persistQueue(updated)
        }
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

    fun clearQueue() {
        _queue.value = emptyList()
        _musicQueue.value = emptyList()
        persistQueue(emptyList())
    }

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

    fun loadLyricsForTrack(track: Track) {
        viewModelScope.launch {
            _isLyricsLoading.value = true
            _lyricLines.value = emptyList()
            val data = LyricsManager.fetchLyrics(track.artist, track.title)
            _lyricLines.value = data?.lines ?: emptyList()
            _isLyricsLoading.value = false
        }
    }

    override fun onCleared() {
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
