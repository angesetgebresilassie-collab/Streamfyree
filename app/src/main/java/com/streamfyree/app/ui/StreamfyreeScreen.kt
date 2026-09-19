package com.streamfyree.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebSettings
import android.webkit.WebView
import coil3.compose.AsyncImage
import com.streamfyree.app.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamfyreeScreen(vm: MusicViewModel) {
    val state by vm.state.collectAsState()
    val mode by vm.mode.collectAsState()
    val current by vm.current.collectAsState()
    val queue by vm.queue.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val progress by vm.progress.collectAsState()
    val library by vm.library.collectAsState()
    LaunchedEffect(isPlaying, current?.id) {
        while (isPlaying) { vm.refreshProgress(); kotlinx.coroutines.delay(500) }
        vm.refreshProgress()
    }
    var query by remember { mutableStateOf("") }
    var showPlayer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }

    if (showPlayer && current != null) {
        ModalBottomSheet(onDismissRequest = { showPlayer = false }) {
            FullPlayer(
                track = current!!,
                isPlaying = isPlaying,
                onToggle = vm::togglePlayPause,
                onPrevious = vm::previous,
                onNext = vm::next,
                onQueue = { showPlayer = false; showQueue = true },
                mode = mode,
                progress = progress,
                onSeek = vm::seekTo
            )
        }
    }

    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Up next", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = vm::clearQueue) { Text("Clear") }
                }
                Spacer(Modifier.height(14.dp))
                if (queue.isEmpty()) {
                    Text("Your queue is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(queue, key = { it.id }) { track ->
                            QueueRow(track, current?.id == track.id, { vm.play(track) }) {
                                vm.removeFromQueue(track)
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF17132B), Color(0xFF0D0D12), Color(0xFF08080B))
            )
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 44.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("STREAMFYREE", letterSpacing = 3.sp, style = MaterialTheme.typography.labelLarge)
                        Text("Find your next favorite.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = .08f)) {
                        IconButton(onClick = { showQueue = true }) {
                            Icon(Icons.Default.QueueMusic, "Queue")
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Home") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Library") })
                }
            }

            if (tab == 1) {
                item {
                    Text("Your library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                if (library.saved.isEmpty()) {
                    item { Text("Save songs with the heart button to find them here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(library.saved, key = { it.id }) { track ->
                        TrackRow(track, { vm.play(track) }, { vm.enqueue(track) }, { vm.unsaveTrack(track) }, true)
                    }
                }
            } else {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Songs, artists, albums…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = { vm.search(query) }) {
                            Icon(Icons.Default.ArrowForward, "Search")
                        }
                    },
                    shape = RoundedCornerShape(22.dp)
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == PlaybackMode.NATIVE,
                        onClick = { vm.setMode(PlaybackMode.NATIVE) },
                        label = { Text("Native audio") },
                        leadingIcon = { Icon(Icons.Default.Headphones, null, Modifier.size(18.dp)) }
                    )
                    FilterChip(
                        selected = mode == PlaybackMode.YOUTUBE,
                        onClick = { vm.setMode(PlaybackMode.YOUTUBE) },
                        label = { Text("YouTube") },
                        leadingIcon = { Icon(Icons.Default.PlayCircle, null, Modifier.size(18.dp)) }
                    )
                }
            }

            item {
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
                }
            }

            if (tab == 0 && !state.loading && state.tracks.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White.copy(alpha = .06f)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.GraphicEq, null, Modifier.size(54.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Search the world of music", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Artwork and metadata come from iTunes while playable audio is resolved by your Streamfyree backend.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (tab == 0 && state.tracks.isNotEmpty()) {
                item {
                    Text("Results", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(state.tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        onPlay = { vm.play(track) },
                        onQueue = { vm.enqueue(track) },
                        onSave = { if (vm.isSaved(track)) vm.unsaveTrack(track) else vm.saveTrack(track) },
                        saved = vm.isSaved(track)
                    )
                }
            }
            }
        }

        current?.let { track ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF24212F).copy(alpha = .97f),
                tonalElevation = 8.dp
            ) {
                Row(
                    Modifier.clickable { showPlayer = true }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.artwork,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(15.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, maxLines = 1, fontWeight = FontWeight.SemiBold)
                        Text(track.artist, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = vm::togglePlayPause) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onPlay: () -> Unit, onQueue: () -> Unit, onSave: () -> Unit, saved: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = .055f)
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = track.artwork,
                contentDescription = null,
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(17.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, fontWeight = FontWeight.SemiBold)
                Text(track.artist, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (track.lyricVideo) {
                        Text("LYRIC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (track.album.isNotBlank()) {
                        Text(track.album, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = onSave) { Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Save") }
            IconButton(onClick = onQueue) { Icon(Icons.Default.Add, "Add to queue") }
            IconButton(onClick = onPlay) { Icon(Icons.Default.PlayCircleFilled, "Play") }
        }
    }
}

@Composable
private fun QueueRow(track: Track, selected: Boolean, onPlay: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.artwork,
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            Text(track.artist, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, "Play") }
        IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "Remove") }
    }
}

@Composable
private fun FullPlayer(
    track: Track,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onQueue: () -> Unit,
    mode: PlaybackMode,
    progress: PlaybackProgress,
    onSeek: (Long) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (mode == PlaybackMode.YOUTUBE && track.youtubeUrl != null) {
            YouTubeEmbed(
                videoUrl = track.youtubeUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(26.dp))
            )
        } else {
            AsyncImage(
                model = track.artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(30.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(track.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(track.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        if (mode == PlaybackMode.NATIVE) {
            val duration = progress.durationMs.coerceAtLeast(1)
            Slider(value = progress.positionMs.coerceIn(0, duration).toFloat(), onValueChange = { onSeek(it.toLong()) }, valueRange = 0f..duration.toFloat())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Previous") }
                FilledIconButton(onClick = onToggle, modifier = Modifier.size(68.dp)) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", Modifier.size(34.dp))
                }
                IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next") }
                IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Queue") }
            }
        } else {
            Text("Use the YouTube player controls for playback.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Queue") }
        }
    }
}

@Composable
private fun YouTubeEmbed(videoUrl: String, modifier: Modifier = Modifier) {
    val videoId = remember(videoUrl) {
        Regex("[?&]v=([^&]+)").find(videoUrl)?.groupValues?.get(1)
            ?: videoUrl.substringAfterLast("/").substringBefore("?")
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { webView ->
            val html = """
                <!doctype html>
                <html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                <style>html,body,iframe{margin:0;width:100%;height:100%;border:0;background:#000}</style>
                </head><body>
                <iframe src="https://www.youtube-nocookie.com/embed/$videoId?playsinline=1&autoplay=1&rel=0"
                  allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe>
                </body></html>
            """.trimIndent()
            webView.loadDataWithBaseURL(
                "https://www.youtube-nocookie.com/",
                html,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}
