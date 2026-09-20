package com.streamfyree.app.ui

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.palette.graphics.Palette
import coil3.request.ImageRequest
import coil3.toBitmap
import coil3.compose.AsyncImage
import com.streamfyree.app.*

private val BG = Color(0xFF090807)
private val WARM_WHITE = Color(0xFFF1E9E1)
private val MUTED = Color(0xFFB9ADA3)
private val ACCENT = Color(0xFFD8BEA6)
private val GLASS = Color(0xFF171411).copy(alpha = .78f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamfyreeScreen(vm: MusicViewModel) {
    val state by vm.state.collectAsState()
    val mode by vm.mode.collectAsState()
    val current by vm.current.collectAsState()
    val queue by vm.queue.collectAsState()
    val library by vm.library.collectAsState()
    val history by vm.history.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val progress by vm.progress.collectAsState()

    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var showPlayer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, current?.id) {
        while (isPlaying) {
            vm.refreshProgress()
            kotlinx.coroutines.delay(500)
        }
        vm.refreshProgress()
    }

    if (showPlayer && current != null) {
        ModalBottomSheet(
            onDismissRequest = { showPlayer = false },
            containerColor = BG,
            tonalElevation = 0.dp
        ) {
            FullPlayer(
                track = current!!,
                isPlaying = isPlaying,
                mode = mode,
                progress = progress,
                onToggle = vm::togglePlayPause,
                onPrevious = vm::previous,
                onNext = vm::next,
                onSeek = vm::seekTo,
                onQueue = { showPlayer = false; showQueue = true }
            )
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = BG,
            tonalElevation = 0.dp
        ) {
            QueueSheet(
                queue = queue,
                currentId = current?.id,
                onPlay = vm::play,
                onRemove = vm::removeFromQueue,
                onClear = vm::clearQueue
            )
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF14110F), BG, Color(0xFF050504)))
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 142.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Header { showQueue = true } }

            when (tab) {
                0 -> {
                    item {
                        if (current != null) {
                            NowPlayingCard(current!!, progress, isPlaying) { showPlayer = true }
                        } else {
                            WelcomeCard()
                        }
                    }
                    item {
                        SearchBar(
                            value = query,
                            onValueChange = { query = it },
                            onSearch = {
                                tab = 1
                                vm.search(query)
                            }
                        )
                    }
                    item { PlaybackModes(mode, vm::setMode) }
                    state.error?.let { message ->
                        item { ErrorPill(message) }
                    }

                    val madeForYou = (library.saved + history).distinctBy { it.id }.take(6)
                    if (madeForYou.isNotEmpty()) {
                        item { SectionHeader("Made for You", "Built from the music you actually save and play") }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(madeForYou, key = { it.id }) { track ->
                                    SongTile(track, Modifier.width(154.dp)) { vm.play(track) }
                                }
                            }
                        }
                    }

                    if (state.tracks.isNotEmpty()) {
                        item { SectionHeader("Trending Now", "Real songs from your latest search") }
                        items(state.tracks.take(8), key = { it.id }) { track ->
                            SongRow(
                                track = track,
                                rank = state.tracks.indexOf(track) + 1,
                                saved = vm.isSaved(track),
                                onPlay = { vm.play(track) },
                                onQueue = { vm.enqueue(track) },
                                onSave = { if (vm.isSaved(track)) vm.unsaveTrack(track) else vm.saveTrack(track) }
                            )
                        }
                    }

                    if (history.isNotEmpty()) {
                        item { SectionHeader("Recently Played", "Pick up where you left off") }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(history.take(8), key = { it.id }) { track ->
                                    HistoryTile(track) { vm.play(track) }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item { SearchBar(query, { query = it }, { vm.search(query) }, "Search songs, artists, albums…") }
                    item { PlaybackModes(mode, vm::setMode) }
                    state.error?.let { message -> item { ErrorPill(message) } }
                    if (state.loading) {
                        item {
                            LinearProgressIndicator(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                                color = ACCENT,
                                trackColor = Color.White.copy(alpha = .1f)
                            )
                        }
                    }
                    if (!state.loading && state.tracks.isEmpty()) {
                        item { EmptyState(Icons.Default.Search, "Search your music", "Real artwork and metadata come from iTunes.") }
                    }
                    if (state.tracks.isNotEmpty()) {
                        item { SectionHeader("Search Results", "Artwork and metadata from iTunes; lyric candidates are searched online") }
                        items(state.tracks, key = { it.id }) { track ->
                            SongRow(
                                track = track,
                                rank = null,
                                saved = vm.isSaved(track),
                                onPlay = { vm.play(track) },
                                onQueue = { vm.enqueue(track) },
                                onSave = { if (vm.isSaved(track)) vm.unsaveTrack(track) else vm.saveTrack(track) }
                            )
                        }
                    }
                }

                2 -> {
                    item { SectionHeader("Your Library", "Saved music") }
                    if (library.saved.isEmpty()) {
                        item { EmptyState(Icons.Default.FavoriteBorder, "Your library is empty", "Save a real song and it will appear here.") }
                    } else {
                        items(library.saved, key = { it.id }) { track ->
                            SongRow(
                                track = track,
                                rank = null,
                                saved = true,
                                onPlay = { vm.play(track) },
                                onQueue = { vm.enqueue(track) },
                                onSave = { vm.unsaveTrack(track) }
                            )
                        }
                    }
                }

                3 -> {
                    item { SectionHeader("Queue", queue.size.toString() + " songs") }
                    if (queue.isEmpty()) {
                        item { EmptyState(Icons.Default.QueueMusic, "Nothing queued", "Add a song from a result.") }
                    } else {
                        item {
                            TextButton(onClick = vm::clearQueue) {
                                Text("Clear queue", color = ACCENT)
                            }
                        }
                        items(queue, key = { it.id }) { track ->
                            SongRow(
                                track = track,
                                rank = null,
                                saved = vm.isSaved(track),
                                onPlay = { vm.play(track) },
                                onQueue = { vm.removeFromQueue(track) },
                                onSave = { if (vm.isSaved(track)) vm.unsaveTrack(track) else vm.saveTrack(track) },
                                removeFromQueue = true
                            )
                        }
                    }
                }
            }
        }

        current?.let { track ->
            MiniPlayer(
                track = track,
                isPlaying = isPlaying,
                progress = progress,
                onOpen = { showPlayer = true },
                onToggle = vm::togglePlayPause,
                onNext = vm::next,
                onQueue = { tab = 3 }
            )
        }

        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(10.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(26.dp)),
            containerColor = Color(0xFF12100F).copy(alpha = .95f),
            tonalElevation = 0.dp
        ) {
            NavItem(tab == 0, { tab = 0 }, Icons.Default.Home, "Home")
            NavItem(tab == 1, { tab = 1 }, Icons.Default.Search, "Search")
            NavItem(tab == 2, { tab = 2 }, Icons.Default.Favorite, "Library")
            NavItem(tab == 3, { tab = 3 }, Icons.Default.QueueMusic, "Queue")
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) BG else WARM_WHITE.copy(alpha = .65f)
        )
        Text(
            label,
            color = if (selected) ACCENT else WARM_WHITE.copy(alpha = .65f),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun Header(onQueue: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(44.dp), CircleShape, color = ACCENT.copy(alpha = .11f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.GraphicEq, null, tint = ACCENT, modifier = Modifier.size(25.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Streamfyree", color = WARM_WHITE, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp)
            Text("Music without limits", color = MUTED, fontSize = 13.sp)
        }
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = .045f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ACCENT.copy(alpha = .3f))
        ) {
            IconButton(onClick = onQueue) {
                Icon(Icons.Default.QueueMusic, "Queue", tint = WARM_WHITE)
            }
        }
    }
}

@Composable
private fun WelcomeCard() {
    GlassCard(RoundedCornerShape(28.dp), Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Pill("DISCOVER", ACCENT)
            Spacer(Modifier.height(11.dp))
            Text("Your music,\nwithout the noise.", color = WARM_WHITE, fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.9).sp)
            Spacer(Modifier.height(8.dp))
            Text("Search real songs, then build your home screen from what you actually play.", color = MUTED, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun NowPlayingCard(track: Track, progress: PlaybackProgress, isPlaying: Boolean, onOpen: () -> Unit) {
    var tint by remember(track.artwork) { mutableStateOf(ACCENT) }
    GlassCard(RoundedCornerShape(28.dp), Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Box(Modifier.fillMaxWidth().height(250.dp)) {
            ArtworkImage(track, tint, { tint = it }, Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(184.dp).clip(RoundedCornerShape(24.dp)))
            Box(
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(listOf(BG.copy(alpha = .98f), BG.copy(alpha = .74f), tint.copy(alpha = .16f)))
                )
            )
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Text("NOW PLAYING", color = ACCENT, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.15.sp)
                Spacer(Modifier.height(10.dp))
                Text(track.title, color = WARM_WHITE, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(track.artist, color = WARM_WHITE.copy(alpha = .8f), fontSize = 14.sp, maxLines = 1)
                if (track.lyricVideo) {
                    Spacer(Modifier.height(7.dp))
                    Pill("LYRICS", tint)
                }
                Spacer(Modifier.weight(1f))
                if (progress.durationMs > 0) {
                    Slider(
                        value = progress.positionMs.coerceIn(0, progress.durationMs).toFloat(),
                        onValueChange = {},
                        valueRange = 0f..progress.durationMs.toFloat(),
                        colors = SliderDefaults.colors(activeTrackColor = ACCENT, inactiveTrackColor = Color.White.copy(alpha = .14f), thumbColor = WARM_WHITE)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shuffle, null, tint = WARM_WHITE, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(14.dp))
                    Icon(Icons.Default.SkipPrevious, null, tint = WARM_WHITE, modifier = Modifier.size(27.dp))
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onOpen,
                        Modifier.size(52.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = ACCENT, contentColor = BG)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Open player")
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.SkipNext, null, tint = WARM_WHITE, modifier = Modifier.size(27.dp))
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.FavoriteBorder, null, tint = WARM_WHITE, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit, placeholder: String = "Search for songs, artists, albums…") {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = .045f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .18f))
    ) {
        Row(Modifier.padding(start = 14.dp, end = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = WARM_WHITE, modifier = Modifier.size(23.dp))
            TextField(
                value = value,
                onValueChange = onValueChange,
                Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(placeholder, color = MUTED, fontSize = 15.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = WARM_WHITE,
                    unfocusedTextColor = WARM_WHITE,
                    cursorColor = ACCENT
                )
            )
            IconButton(onClick = onSearch, enabled = value.isNotBlank()) {
                Icon(Icons.Default.ArrowForward, "Search", tint = if (value.isBlank()) MUTED else ACCENT)
            }
        }
    }
}

@Composable
private fun PlaybackModes(mode: PlaybackMode, onMode: (PlaybackMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Mode("Native Audio", Icons.Default.Headphones, mode == PlaybackMode.NATIVE) { onMode(PlaybackMode.NATIVE) }
        Mode("Online", Icons.Default.Language, mode == PlaybackMode.ONLINE) { onMode(PlaybackMode.ONLINE) }
        Mode("Auto", Icons.Default.Tune, mode == PlaybackMode.AUTO) { onMode(PlaybackMode.AUTO) }
    }
}

@Composable
private fun Mode(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.clickable(onClick = onClick),
        RoundedCornerShape(18.dp),
        color = if (selected) ACCENT else Color.White.copy(alpha = .035f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) ACCENT else Color.White.copy(alpha = .13f))
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) BG else WARM_WHITE, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = if (selected) BG else WARM_WHITE, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, color = WARM_WHITE, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.45).sp)
        Text(subtitle, color = MUTED, fontSize = 13.sp)
    }
}

@Composable
private fun SongTile(track: Track, modifier: Modifier, onClick: () -> Unit) {
    var tint by remember(track.artwork) { mutableStateOf(ACCENT) }
    GlassCard(RoundedCornerShape(20.dp), modifier.clickable(onClick = onClick)) {
        Column {
            ArtworkImage(track, tint, { tint = it }, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)))
            Column(Modifier.padding(10.dp)) {
                Text(track.title, color = WARM_WHITE, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(track.artist, color = MUTED, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HistoryTile(track: Track, onClick: () -> Unit) {
    Column(Modifier.width(108.dp).clickable(onClick = onClick)) {
        ArtworkImage(track, ACCENT, {}, Modifier.size(108.dp).clip(RoundedCornerShape(16.dp)))
        Spacer(Modifier.height(6.dp))
        Text(track.title, color = WARM_WHITE, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(track.artist, color = MUTED, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun SongRow(
    track: Track,
    rank: Int?,
    saved: Boolean,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onSave: () -> Unit,
    removeFromQueue: Boolean = false
) {
    var tint by remember(track.artwork) { mutableStateOf(ACCENT) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        rank?.let {
            Text(it.toString(), Modifier.width(28.dp), color = MUTED, fontSize = 15.sp)
        }
        ArtworkImage(track, tint, { tint = it }, Modifier.size(58.dp).clip(RoundedCornerShape(13.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(track.title, color = WARM_WHITE, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (track.lyricVideo) {
                    Spacer(Modifier.width(6.dp))
                    Pill("LYRICS", tint)
                }
            }
            Text(track.artist, color = MUTED, fontSize = 12.sp, maxLines = 1)
        }
        IconButton(onClick = onSave) {
            Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Save", tint = if (saved) ACCENT else MUTED, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onQueue) {
            Icon(if (removeFromQueue) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircleOutline, "Queue", tint = MUTED, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.MiniPlayer(track: Track, isPlaying: Boolean, progress: PlaybackProgress, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit, onQueue: () -> Unit) {
    Surface(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 76.dp).clickable(onClick = onOpen),
        RoundedCornerShape(24.dp),
        color = Color(0xFF201C18).copy(alpha = .94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ACCENT.copy(alpha = .18f))
    ) {
        Box {
            Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtworkImage(track, ACCENT, {}, Modifier.size(50.dp).clip(RoundedCornerShape(13.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = WARM_WHITE, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(track.artist, color = MUTED, fontSize = 11.sp, maxLines = 1)
                }
                IconButton(onClick = onToggle) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", tint = WARM_WHITE) }
                IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next", tint = WARM_WHITE) }
                IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Queue", tint = WARM_WHITE) }
            }
            if (progress.durationMs > 0) {
                Box(
                    Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth((progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(ACCENT)
                )
            }
        }
    }
}

@Composable
private fun ArtworkImage(track: Track, tint: Color, onTint: (Color) -> Unit, modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val request = remember(track.artwork) {
        ImageRequest.Builder(context).data(track.artwork).build()
    }
    Box(modifier.background(tint.copy(alpha = .12f))) {
        AsyncImage(
            model = request,
            contentDescription = track.title,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { success ->
                val bitmap = runCatching { success.result.image.toBitmap() }.getOrNull() ?: return@AsyncImage
                Palette.from(bitmap).generate { palette ->
                    val swatch = palette?.vibrantSwatch
                        ?: palette?.lightVibrantSwatch
                        ?: palette?.mutedSwatch
                        ?: palette?.dominantSwatch
                    swatch?.rgb?.let { onTint(Color(it)) }
                }
            }
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, tint.copy(alpha = .1f), BG.copy(alpha = .58f)))
            )
        )
    }
}

@Composable
private fun GlassCard(shape: RoundedCornerShape, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = GLASS,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .09f)),
        tonalElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
private fun Pill(text: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = tint.copy(alpha = .15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = .36f))
    ) {
        Text(text, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = WARM_WHITE, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .55.sp)
    }
}

@Composable
private fun ErrorPill(message: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), color = Color(0xFF3A2924).copy(alpha = .72f)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = ACCENT, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = WARM_WHITE, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    GlassCard(RoundedCornerShape(24.dp), Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = ACCENT, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, color = WARM_WHITE, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = MUTED, fontSize = 13.sp)
        }
    }
}

@Composable
private fun QueueSheet(
    queue: List<Track>,
    currentId: String?,
    onPlay: (Track) -> Unit,
    onRemove: (Track) -> Unit,
    onClear: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Queue", color = WARM_WHITE, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(queue.size.toString() + " songs", color = MUTED, fontSize = 13.sp)
            }
            TextButton(onClick = onClear) { Text("Clear", color = ACCENT) }
        }
        Spacer(Modifier.height(8.dp))
        queue.take(15).forEach { track ->
            SongRow(
                track = track,
                rank = null,
                saved = currentId == track.id,
                onPlay = { onPlay(track) },
                onQueue = { onRemove(track) },
                onSave = {},
                removeFromQueue = true
            )
        }
    }
}

@Composable
private fun FullPlayer(
    track: Track,
    isPlaying: Boolean,
    mode: PlaybackMode,
    progress: PlaybackProgress,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onQueue: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (mode == PlaybackMode.ONLINE || mode == PlaybackMode.AUTO) {
            YouTubeEmbed(track.youtubeUrl ?: "", Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(24.dp)))
        } else {
            ArtworkImage(track, ACCENT, {}, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)))
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(track.title, color = WARM_WHITE, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(track.artist, color = MUTED, fontSize = 14.sp, maxLines = 1)
            }
            IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Queue", tint = WARM_WHITE) }
        }
        if (mode != PlaybackMode.ONLINE && progress.durationMs > 0) {
            Slider(
                value = progress.positionMs.coerceIn(0, progress.durationMs).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..progress.durationMs.toFloat(),
                colors = SliderDefaults.colors(activeTrackColor = ACCENT, inactiveTrackColor = Color.White.copy(alpha = .14f), thumbColor = WARM_WHITE)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Previous", tint = WARM_WHITE, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.width(16.dp))
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = ACCENT, contentColor = BG)
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(16.dp))
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next", tint = WARM_WHITE, modifier = Modifier.size(32.dp)) }
        }
    }
}

@Composable
private fun YouTubeEmbed(url: String, modifier: Modifier) {
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
            val directId = Regex("[?&]v=([^&]+)").find(url)?.groupValues?.get(1)
                ?: Regex("youtu\\.be/([^?&/]+)").find(url)?.groupValues?.get(1)
            val target = if (!directId.isNullOrBlank()) {
                "https://www.youtube-nocookie.com/embed/" + directId + "?playsinline=1&autoplay=1&rel=0"
            } else {
                url.ifBlank { "https://www.youtube.com/" }
            }
            if (webView.url != target) webView.loadUrl(target)
        }
    )
}
