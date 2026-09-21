package com.streamfyree.app

object PlaybackRequestHeaders {
    @Volatile
    private var headers: Map<String, String> = emptyMap()

    fun set(value: Map<String, String>) {
        headers = value.filterValues { it.isNotBlank() }
    }

    fun clear() {
        headers = emptyMap()
    }

    fun current(): Map<String, String> = headers
}
