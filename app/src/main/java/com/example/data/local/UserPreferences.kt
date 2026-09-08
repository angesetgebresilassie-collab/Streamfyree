package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("music_stream_prefs", Context.MODE_PRIVATE)

    private val _youtubeApiKey = MutableStateFlow(loadApiKey())
    val youtubeApiKey: StateFlow<String> = _youtubeApiKey.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private fun loadApiKey(): String {
        val saved = prefs.getString(KEY_YOUTUBE_API_KEY, null)
        if (!saved.isNullOrBlank()) return saved

        // Check BuildConfig from .env
        val buildConfigKey = runCatching { BuildConfig.YOUTUBE_API_KEY }.getOrNull()
        if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "YOUR_YOUTUBE_API_KEY") {
            return buildConfigKey
        }
        return ""
    }

    fun setYoutubeApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_YOUTUBE_API_KEY, trimmed).apply()
        _youtubeApiKey.value = trimmed
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }

    companion object {
        private const val KEY_YOUTUBE_API_KEY = "youtube_api_key"
        private const val KEY_DARK_MODE = "is_dark_mode"
    }
}
