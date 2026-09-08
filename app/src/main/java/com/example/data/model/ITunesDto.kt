package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ITunesSearchResponse(
    @Json(name = "resultCount") val resultCount: Int? = 0,
    @Json(name = "results") val results: List<ITunesSongDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ITunesSongDto(
    @Json(name = "trackId") val trackId: Long? = null,
    @Json(name = "trackName") val trackName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "collectionName") val collectionName: String? = null,
    @Json(name = "artworkUrl100") val artworkUrl100: String? = null,
    @Json(name = "trackTimeMillis") val trackTimeMillis: Long? = null,
    @Json(name = "previewUrl") val previewUrl: String? = null,
    @Json(name = "primaryGenreName") val primaryGenreName: String? = null
) {
    fun toSong(): Song {
        val id = trackId?.toString() ?: "${artistName}_${trackName}".hashCode().toString()
        return Song(
            id = id,
            title = trackName ?: "Unknown Track",
            artist = artistName ?: "Unknown Artist",
            album = collectionName ?: "",
            artworkUrl = artworkUrl100 ?: "",
            durationMs = trackTimeMillis ?: 0L,
            youtubeVideoId = null,
            previewUrl = previewUrl,
            genre = primaryGenreName
        )
    }
}
