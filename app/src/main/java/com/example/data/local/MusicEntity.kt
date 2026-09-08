package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Song

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val durationMs: Long,
    val youtubeVideoId: String?,
    val previewUrl: String?,
    val genre: String?,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val addedTimestamp: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        durationMs = durationMs,
        youtubeVideoId = youtubeVideoId,
        previewUrl = previewUrl,
        genre = genre,
        isFavorite = isFavorite
    )

    companion object {
        fun fromSong(song: Song, isFav: Boolean = song.isFavorite, playedAt: Long = 0L): TrackEntity {
            return TrackEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                artworkUrl = song.artworkUrl,
                durationMs = song.durationMs,
                youtubeVideoId = song.youtubeVideoId,
                previewUrl = song.previewUrl,
                genre = song.genre,
                isFavorite = isFav,
                lastPlayedTimestamp = playedAt,
                addedTimestamp = System.currentTimeMillis()
            )
        }
    }
}
