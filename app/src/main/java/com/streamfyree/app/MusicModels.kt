package com.streamfyree.app

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long = 0L,
    val youtubeUrl: String? = null,
    val streamUrl: String? = null,
    val lyricVideo: Boolean = false
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist ?: "",
        album = album ?: "",
        artwork = artworkUrl,
        durationMs = durationMs,
        youtubeUrl = youtubeUrl,
        streamUrl = streamUrl,
        lyricVideo = lyricVideo
    )
}

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
) {
    fun toMusicTrack(): MusicTrack = MusicTrack(
        id = id,
        title = title,
        artist = artist.ifBlank { null },
        album = album.ifBlank { null },
        artworkUrl = artwork,
        durationMs = durationMs,
        youtubeUrl = youtubeUrl,
        streamUrl = streamUrl,
        lyricVideo = lyricVideo
    )
}

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
