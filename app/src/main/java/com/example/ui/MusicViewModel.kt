package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiClient
import com.example.data.local.AppDatabase
import com.example.data.local.TrackEntity
import com.example.data.local.UserPreferences
import com.example.data.model.Song
import com.example.data.model.YouTubeSearchItem
import com.example.player.AudioSourceType
import com.example.player.PlaybackStatus
import com.example.player.PlayerState
import com.example.player.YouTubeAudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavTab {
    SEARCH,
    FAVORITES,
    HISTORY,
    SETTINGS
}

data class UiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<Song> = emptyList(),
    val searchError: String? = null,
    val activeTab: NavTab = NavTab.SEARCH,
    val isMatchingYouTube: Boolean = false,
    val matchingSongTitle: String? = null,
    val isFullScreenPlayerOpen: Boolean = false,
    val isApiKeyDialogOpen: Boolean = false,
    val isDirectVideoDialogOpen: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val searchMode: String = "iTunes" // "iTunes" or "YouTube"
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    val userPreferences = UserPreferences(application)
    private val database = AppDatabase.getDatabase(application)
    private val musicDao = database.musicDao()

    val audioPlayer = YouTubeAudioPlayer(application)
    val playerState: StateFlow<PlayerState> = audioPlayer.playerState

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val youtubeApiKey: StateFlow<String> = userPreferences.youtubeApiKey
    val isDarkMode: StateFlow<Boolean> = userPreferences.isDarkMode

    val favorites: StateFlow<List<Song>> = musicDao.getFavorites()
        .map { entities -> entities.map { it.toSong() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<Song>> = musicDao.getHistory()
        .map { entities -> entities.map { it.toSong() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null
    private var currentQueue: List<Song> = emptyList()
    private var currentQueueIndex: Int = -1

    // Guard against infinite buffering loops
    private var currentSongRetryCount = 0
    private val failedLyricVideoIds = mutableSetOf<String>()

    init {
        // Observe ended state for auto-advancing to next song
        viewModelScope.launch {
            playerState.collect { state ->
                if (state.status == PlaybackStatus.ENDED) {
                    if (_uiState.value.isRepeatEnabled) {
                        state.currentSong?.let { playSong(it) }
                    } else {
                        playNext()
                    }
                }
            }
        }

        // When a video encounters embed/copyright restriction or buffering timeout, automatically attempt at most one alternate lyric video
        audioPlayer.onPlaybackErrorListener = { failedSong, failedVideoId, _ ->
            viewModelScope.launch {
                failedLyricVideoIds.add(failedVideoId)
                val apiKey = youtubeApiKey.value
                if (apiKey.isNotBlank() && currentSongRetryCount < 1) {
                    currentSongRetryCount++
                    _uiState.value = _uiState.value.copy(
                        isMatchingYouTube = true,
                        matchingSongTitle = "Finding alternate 7Clouds lyric stream..."
                    )
                    val alternateId = findBestLyricVideo(
                        song = failedSong,
                        apiKey = apiKey,
                        excludeVideoIds = failedLyricVideoIds
                    )
                    _uiState.value = _uiState.value.copy(
                        isMatchingYouTube = false,
                        matchingSongTitle = null
                    )
                    if (!alternateId.isNullOrBlank()) {
                        failedLyricVideoIds.add(alternateId)
                        withContext(Dispatchers.IO) {
                            musicDao.updateYoutubeVideoId(failedSong.id, alternateId)
                        }
                        audioPlayer.playSongWithYouTube(failedSong, alternateId)
                        return@launch
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isMatchingYouTube = false,
                    matchingSongTitle = null
                )
                audioPlayer.fallbackToPreviewWithReason(
                    failedSong,
                    "YouTube stream restricted. Switched to iTunes audio preview."
                )
            }
        }
    }

    fun setNavTab(tab: NavTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun setFullScreenPlayerOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isFullScreenPlayerOpen = isOpen)
    }

    fun setApiKeyDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isApiKeyDialogOpen = isOpen)
    }

    fun setDirectVideoDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isDirectVideoDialogOpen = isOpen)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.trim().length >= 2) {
            searchJob = viewModelScope.launch {
                delay(400) // Debounce typing
                performLiveSearch(query.trim())
            }
        } else if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), searchError = null)
        }
    }

    fun triggerSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performLiveSearch(query.trim())
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(searchQuery = "", searchResults = emptyList(), searchError = null)
    }

    fun performLiveSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchError = null)
            try {
                // 1. Fetch iTunes live metadata (100% free, always works)
                val response = withContext(Dispatchers.IO) {
                    ApiClient.iTunesApi.searchSongs(term = query, limit = 35)
                }
                val rawResults = response.results?.map { it.toSong() } ?: emptyList()

                // Check favorites status from DB
                val favoritesList = favorites.value.map { it.id }.toSet()
                val updatedResults = rawResults.map { song ->
                    song.copy(isFavorite = favoritesList.contains(song.id))
                }

                currentQueue = updatedResults
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = updatedResults,
                    searchError = if (updatedResults.isEmpty()) "No songs found for \"$query\". Try another search." else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchError = "Search failed: ${e.localizedMessage ?: "Network error"}. Please check your connection."
                )
            }
        }
    }

    fun playSong(song: Song) {
        val queue = _uiState.value.searchResults.ifEmpty { favorites.value }.ifEmpty { history.value }
        currentQueue = queue
        currentQueueIndex = queue.indexOfFirst { it.id == song.id }

        // Reset retry counters for this song
        currentSongRetryCount = 0
        failedLyricVideoIds.clear()

        viewModelScope.launch {
            val cachedTrack = withContext(Dispatchers.IO) { musicDao.getTrackById(song.id) }
            val apiKey = youtubeApiKey.value

            // Record to history
            withContext(Dispatchers.IO) {
                val entity = TrackEntity.fromSong(
                    song = song,
                    isFav = cachedTrack?.isFavorite ?: song.isFavorite,
                    playedAt = System.currentTimeMillis()
                )
                musicDao.insertOrUpdate(entity)
            }

            // If it's a direct user-specified YouTube video, play immediately
            if (song.id.startsWith("yt_") && !song.youtubeVideoId.isNullOrBlank()) {
                audioPlayer.playSongWithYouTube(song, song.youtubeVideoId)
                return@launch
            }

            if (apiKey.isNotBlank()) {
                // Search for full-length LYRICS video (like 7Clouds) instead of original clip
                _uiState.value = _uiState.value.copy(
                    isMatchingYouTube = true,
                    matchingSongTitle = "Finding 7Clouds lyrics video: ${song.title} - ${song.artist}"
                )
                try {
                    val videoId = findBestLyricVideo(song, apiKey)

                    if (!videoId.isNullOrBlank()) {
                        // Persist videoId in DB
                        withContext(Dispatchers.IO) {
                            musicDao.updateYoutubeVideoId(song.id, videoId)
                        }
                        audioPlayer.playSongWithYouTube(song, videoId)
                    } else {
                        fallbackToPreview(song, "No embeddable lyrics video found. Playing iTunes preview.")
                    }
                } catch (e: Exception) {
                    fallbackToPreview(song, "YouTube API: ${e.localizedMessage}. Playing iTunes preview.")
                } finally {
                    _uiState.value = _uiState.value.copy(
                        isMatchingYouTube = false,
                        matchingSongTitle = null
                    )
                }
            } else {
                // No YouTube API Key set yet
                if (!song.previewUrl.isNullOrBlank()) {
                    audioPlayer.playPreview(song.previewUrl, song)
                    // Prompt user to enter YouTube API key
                    _uiState.value = _uiState.value.copy(isApiKeyDialogOpen = true)
                } else {
                    _uiState.value = _uiState.value.copy(isApiKeyDialogOpen = true)
                }
            }
        }
    }

    private fun cleanSearchTitle(title: String): String {
        return title
            .replace(Regex("\\s*-\\s*(Single|EP|Bonus Track|Remastered|Deluxe Edition|Remix|Explicit|Official).*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\(feat\\..*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(with.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(ft\\..*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Official.*?\\)", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun scoreLyricVideo(item: YouTubeSearchItem): Int {
        val videoId = item.id?.videoId
        if (videoId.isNullOrBlank()) return -1000

        val channel = item.snippet?.channelTitle.orEmpty().lowercase()
        val title = item.snippet?.title.orEmpty().lowercase()

        var score = 0

        // Highest priority: 7clouds channels or titles (user explicit preference)
        if (channel.contains("7cloud") || title.contains("7cloud")) {
            score += 1500
        }

        // High priority: dedicated lyric channels
        if (channel.contains("lyric") || channel.contains("tracks") || channel.contains("shadow") ||
            channel.contains("cassiopeia") || channel.contains("superb") || channel.contains("dan music") ||
            channel.contains("royal") || channel.contains("chill") || channel.contains("vibe")) {
            score += 400
        }

        // High bonus for "lyrics" or "lyric video" in title
        if (title.contains("lyrics") || title.contains("lyric video")) {
            score += 600
        }

        // Severe penalty for official clips/music videos (which copyright owners restrict from embedding)
        if (title.contains("official music video") || title.contains("official video") || 
            title.contains("clip officiel") || title.contains("music video")) {
            score -= 700
        }
        if (channel.contains("vevo") || channel.endsWith(" - topic")) {
            score -= 500
        }
        if (title.contains("live") && !title.contains("lyrics")) {
            score -= 300
        }
        if (title.contains("trailer") || title.contains("teaser") || title.contains("reaction") || title.contains("review")) {
            score -= 800
        }

        return score
    }

    private suspend fun findBestLyricVideo(
        song: Song,
        apiKey: String,
        excludeVideoIds: Set<String> = emptySet()
    ): String? {
        val cleanTitle = cleanSearchTitle(song.title)

        // Query 1: Specifically search for 7clouds lyrics video as requested by user
        try {
            val query7Clouds = "$cleanTitle ${song.artist} lyrics 7clouds"
            val response7Clouds = withContext(Dispatchers.IO) {
                ApiClient.youTubeApi.searchVideos(
                    query = query7Clouds,
                    apiKey = apiKey,
                    maxResults = 10
                )
            }
            val candidates = response7Clouds.items?.filter {
                val id = it.id?.videoId
                !id.isNullOrBlank() && !excludeVideoIds.contains(id)
            }
            val best7Clouds = candidates?.maxByOrNull { scoreLyricVideo(it) }
            if (best7Clouds != null && scoreLyricVideo(best7Clouds) >= 1000) {
                return best7Clouds.id?.videoId
            }
        } catch (e: Exception) {
            // Proceed to query 2
        }

        // Query 2: Search for lyrics video prioritizing lyric channels
        try {
            val queryLyrics = "$cleanTitle ${song.artist} lyrics"
            val responseLyrics = withContext(Dispatchers.IO) {
                ApiClient.youTubeApi.searchVideos(
                    query = queryLyrics,
                    apiKey = apiKey,
                    maxResults = 10
                )
            }
            val candidates = responseLyrics.items?.filter {
                val id = it.id?.videoId
                !id.isNullOrBlank() && !excludeVideoIds.contains(id)
            }
            val bestLyric = candidates?.maxByOrNull { scoreLyricVideo(it) }
            if (bestLyric != null && scoreLyricVideo(bestLyric) > 0) {
                return bestLyric.id?.videoId
            }
        } catch (e: Exception) {
            // Proceed to query 3
        }

        // Query 3: Search "lyric video"
        try {
            val queryFallback = "$cleanTitle ${song.artist} lyric video"
            val responseFallback = withContext(Dispatchers.IO) {
                ApiClient.youTubeApi.searchVideos(
                    query = queryFallback,
                    apiKey = apiKey,
                    maxResults = 10
                )
            }
            val candidates = responseFallback.items?.filter {
                val id = it.id?.videoId
                !id.isNullOrBlank() && !excludeVideoIds.contains(id)
            }
            val bestFallback = candidates?.maxByOrNull { scoreLyricVideo(it) }
            if (bestFallback != null && scoreLyricVideo(bestFallback) > -500) {
                return bestFallback.id?.videoId
            }
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    private fun fallbackToPreview(song: Song, note: String) {
        if (!song.previewUrl.isNullOrBlank()) {
            audioPlayer.fallbackToPreviewWithReason(song, note)
        } else {
            // Prompt user
            _uiState.value = _uiState.value.copy(isApiKeyDialogOpen = true)
        }
    }

    fun playDirectYouTubeVideo(videoId: String, title: String) {
        val trimmed = videoId.trim()
        val cleanId = extractVideoId(trimmed)
        if (cleanId.isNotBlank()) {
            val customSong = Song(
                id = "yt_$cleanId",
                title = title,
                artist = "YouTube Audio Stream",
                album = "Direct Stream",
                artworkUrl = "https://img.youtube.com/vi/$cleanId/hqdefault.jpg",
                youtubeVideoId = cleanId
            )
            audioPlayer.playSongWithYouTube(customSong, cleanId)
            viewModelScope.launch(Dispatchers.IO) {
                musicDao.insertOrUpdate(TrackEntity.fromSong(customSong, playedAt = System.currentTimeMillis()))
            }
        }
    }

    private fun extractVideoId(input: String): String {
        return when {
            input.contains("v=") -> input.substringAfter("v=").substringBefore("&")
            input.contains("youtu.be/") -> input.substringAfter("youtu.be/").substringBefore("?")
            input.contains("youtube.com/embed/") -> input.substringAfter("embed/").substringBefore("?")
            else -> input
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val newFav = !song.isFavorite
            val existing = musicDao.getTrackById(song.id)
            if (existing != null) {
                musicDao.updateFavorite(song.id, newFav)
            } else {
                musicDao.insertOrUpdate(TrackEntity.fromSong(song, isFav = newFav))
            }

            // Update UI list
            _uiState.value = _uiState.value.copy(
                searchResults = _uiState.value.searchResults.map {
                    if (it.id == song.id) it.copy(isFavorite = newFav) else it
                }
            )
        }
    }

    fun playNext() {
        if (currentQueue.isEmpty()) return
        if (_uiState.value.isShuffleEnabled) {
            val randomIndex = (currentQueue.indices).random()
            currentQueueIndex = randomIndex
            playSong(currentQueue[randomIndex])
        } else {
            val nextIndex = (currentQueueIndex + 1) % currentQueue.size
            currentQueueIndex = nextIndex
            playSong(currentQueue[nextIndex])
        }
    }

    fun playPrevious() {
        if (currentQueue.isEmpty()) return
        val prevIndex = if (currentQueueIndex - 1 < 0) currentQueue.size - 1 else currentQueueIndex - 1
        currentQueueIndex = prevIndex
        playSong(currentQueue[prevIndex])
    }

    fun toggleShuffle() {
        _uiState.value = _uiState.value.copy(isShuffleEnabled = !_uiState.value.isShuffleEnabled)
    }

    fun toggleRepeat() {
        _uiState.value = _uiState.value.copy(isRepeatEnabled = !_uiState.value.isRepeatEnabled)
    }

    fun saveYouTubeApiKey(key: String) {
        userPreferences.setYoutubeApiKey(key)
        _uiState.value = _uiState.value.copy(isApiKeyDialogOpen = false)
        // If there's an active song waiting for playback with no videoId, retry matching!
        val current = playerState.value.currentSong
        if (current != null && playerState.value.sourceType != AudioSourceType.YOUTUBE_IFRAME) {
            playSong(current)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        userPreferences.setDarkMode(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
