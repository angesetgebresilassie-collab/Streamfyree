package com.example.data.api

import com.example.data.model.YouTubeSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("videoEmbeddable") videoEmbeddable: String = "true", // Only embeddable videos
        @Query("videoCategoryId") videoCategoryId: String? = null, // Allow all categories (lyric videos may be in Entertainment or Music)
        @Query("videoSyndicated") videoSyndicated: String? = null,
        @Query("maxResults") maxResults: Int = 10
    ): YouTubeSearchResponse
}
