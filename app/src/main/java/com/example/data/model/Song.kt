package com.example.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val durationMs: Long = 0L,
    val youtubeVideoId: String? = null,
    val previewUrl: String? = null,
    val genre: String? = null,
    val isFavorite: Boolean = false
) {
    val highResArtworkUrl: String
        get() = if (artworkUrl.contains("100x100bb")) {
            artworkUrl.replace("100x100bb", "600x600bb")
        } else {
            artworkUrl
        }

    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
