package com.streamfyree.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberArtworkDominantColor(artworkUrl: String?, defaultColor: Color = Color(0xFF1DB954)): Color {
    val context = LocalContext.current
    var dominantColor by remember(artworkUrl) { mutableStateOf(defaultColor) }

    LaunchedEffect(artworkUrl) {
        if (artworkUrl.isNullOrBlank()) {
            dominantColor = defaultColor
            return@LaunchedEffect
        }

        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                val loader = coil3.SingletonImageLoader.get(context)
                val request = coil3.request.ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .size(100)
                    .build()
                val image = (loader.execute(request) as? coil3.request.SuccessResult)?.image
                val bitmap = (image as? coil3.BitmapImage)?.bitmap
                bitmap?.let { Palette.from(it).generate() }
            }.getOrNull()
        }

        extracted?.let { p ->
            val rgb = p.vibrantSwatch?.rgb
                ?: p.dominantSwatch?.rgb
                ?: p.lightVibrantSwatch?.rgb
                ?: p.mutedSwatch?.rgb
            if (rgb != null) {
                dominantColor = Color(rgb)
            }
        }
    }

    return dominantColor
}
