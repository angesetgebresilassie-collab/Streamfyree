package com.streamfyree.app

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artwork: String? = null,
    val durationMs: Long = 0,
    val youtubeUrl: String? = null,
    val streamUrl: String? = null,
    val lyricVideo: Boolean = false
)

enum class PlaybackMode { AUTO, NATIVE, ONLINE }

enum class RepeatMode { OFF, ALL, ONE }

data class SearchState(
    val query: String = "",
    val loading: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val error: String? = null
)

data class PlaybackProgress(
    val positionMs: Long = 0,
    val durationMs: Long = 0
)

data class LibraryState(
    val saved: List<Track> = emptyList()
)

data class Playlist(
    val id: String,
    val title: String,
    val subtitle: String,
    val artwork: String?,
    val tracks: List<Track>
)
