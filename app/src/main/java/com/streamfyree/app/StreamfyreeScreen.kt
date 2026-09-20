package com.streamfyree.app.ui

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

// ---- Palette: warm brown, Spotify-shaped layout ----
private val BG = Color(0xFF0A0705)
private val BG2 = Color(0xFF130E0A)
private val WARM_WHITE = Color(0xFFF3EAE1)
private val MUTED = Color(0xFFAD9F94)
private val ACCENT = Color(0xFFE0A662)
private val ACCENT2 = Color(0xFF8C5A2B)
private val GLASS = Color(0xFF1B1512).copy(alpha = .60f)

private data class GenreTile(val label: String, val colors: List<Color>)

private val genreTiles = listOf(
    GenreTile("Pop", listOf(Color(0xFFDB9A4E), Color(0xFF7A4A22))),
    GenreTile("Hip-Hop", listOf(Color(0xFF6B4226), Color(0xFF241609))),
    GenreTile("R&B", listOf(Color(0xFFC97B3D), Color(0xFF4C301C))),
    GenreTile("Rock", listOf(Color(0xFF57402F), Color(0xFF17100B))),
    GenreTile("Chill", listOf(Color(0xFFB98A5E), Color(0xFF5E3E26))),
    GenreTile("Focus", listOf(Color(0xFF8A6A4A), Color(0xFF2F2013))),
    GenreTile("Party", listOf(Color(0xFFE0A868), Color(0xFF6A3E1B))),
    GenreTile("Workout", listOf(Color(0xFF7A5230), Color(0xFF20140C)))
)

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%d:%02d", m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamfyreeScreen(vm: MusicViewModel) {
    val state by vm.state.collectAsState()
    val mode by vm.mode.collectAsState()
    val current by vm.current.collectAsState()
    val queue by vm.queue.collectAsState()
    val library by vm.library.collectAsState()
    val history by vm.history.collectAsState()
    val discoverTracks by vm.discoverTracks.collectAsState()
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
            val track = current!!
            FullPlayer(
                track = track,
                isPlaying = isPlaying,
                mode = mode,
                progress = progress,
                saved = vm.isSaved(track),
                onToggle = vm::togglePlayPause,
                onPrevious = vm::previous,
                onNext = vm::next,
                onSeek = vm::seekTo,
                onSave = { if (vm.isSaved(track)) vm.unsaveTrack(track) else vm.saveTrack(track) },
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

    val bottomPad = if (current != null) 158.dp else 96.dp

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(BG2, BG, Color(0xFF040302)))
        )
    ) {
        // ambient frosted-glass glow blobs
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 46.dp, y = (-34).dp)
                .size(220.dp)
                .blur(95.dp)
                .background(ACCENT.copy(alpha = .17f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-70).dp)
                .size(210.dp)
                .blur(95.dp)
                .background(ACCENT2.copy(alpha = .15f), CircleShape)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, bottomPad),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { GreetingHeader { showQueue = true } }

            when (tab) {
                0 -> {
                    item { ChipRow() }

                    current?.let { track ->
                        item {
                            ContinueListeningCard(track, isPlaying, { showPlayer = true }, vm::togglePlayPause)
                        }
                    }

                    val quickPicks = (library.saved + history).distinctBy { it.id }.take(6)
                    if (quickPicks.isNotEmpty()) {
                        item { QuickPicksGrid(quickPicks, vm::play) }
                    }

                    item {
                        SearchBar(
                            value = query,
                            onValueChange = { query = it },
                            onSearch = { tab = 1; vm.search(query) }
                        )
                    }
                    item { PlaybackModes(mode, vm::setMode) }
                    state.error?.let { message -> item { ErrorPill(message) } }

                    if (discoverTracks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                "Fresh Mix",
                                "A randomized feed of real songs, refreshed each time you open the app"
                            )
                        }
                        items(discoverTracks, key = { "discover-" + it.id }) { track ->
                            SongCard(
                                track = track,
                                saved = vm.isSaved(track),
                                onPlay = { vm.play(track) },
                                onQueue = { vm.enqueue(track) },
                                onSave = {
                                    if (vm.isSaved(track)) vm.unsaveTrack(track)
                                    else vm.saveTrack(track)
                                }
                            )
                        }
                    }

                    if (state.tracks.isNotEmpty()) {
                        item { SectionHeader("Trending Now", "Real songs from your latest search") }
                        items(state.tracks.take(8), key = { "trend-" + it.id }) { track ->
                            SongCard(
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
                                items(history.take(8), key = { "hist-" + it.id }) { track ->
                                    HistoryTile(track) { vm.play(track) }
                                }
                            }
                        }
                    }

                    val madeForYou = library.saved.take(6)
                    if (madeForYou.isNotEmpty()) {
                        item { SectionHeader("Made for You", "Built from what you save") }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(madeForYou, key = { "mfy-" + it.id }) { track ->
                                    SongTile(track, Modifier.width(150.dp)) { vm.play(track) }
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
                    if (!state.loading && state.tracks.isEmpty() && query.isBlank()) {
                        item { SectionHeader("Browse all", "Jump into a search") }
                        item { BrowseGrid { picked -> query = picked; vm.search(picked) } }
                    } else if (!state.loading && state.tracks.isEmpty()) {
                        item { EmptyState(Icons.Default.Search, "No results", "Try a different search term.") }
                    }
                    if (state.tracks.isNotEmpty()) {
                        item { SectionHeader("Search Results", "Artwork and metadata from iTunes; lyric candidates are searched online") }
                        items(state.tracks, key = { "search-" + it.id }) { track ->
                            SongCard(
                                track = track,
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
                        items(library.saved, key = { "lib-" + it.id }) { track ->
                            SongCard(
                                track = track,
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
                        items(queue, key = { "queue-" + it.id }) { track ->
                            SongCard(
                                track = track,
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

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            current?.let { track ->
                MiniPlayerBar(
                    track = track,
                    isPlaying = isPlaying,
                    progress = progress,
                    onOpen = { showPlayer = true },
                    onToggle = vm::togglePlayPause,
                    onNext = vm::next
                )
            }
            BottomNavBar(tab) { tab = it }
        }
    }
}

@Composable
private fun BottomNavBar(tab: Int, onTab: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0F0B09).copy(alpha = .97f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(tab == 0, { onTab(0) }, Home, Home, "Home")
            NavItem(tab == 1, { onTab(1) }, Search, Search, "Search")
            NavItem(tab == 2, { onTab(2) }, Icons.Default.Favorite, Icons.Default.FavoriteBorder, "Library")
            NavItem(tab == 3, { onTab(3) }, Icons.Default.QueueMusic, Icons.Default.QueueMusic, "Queue")
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (selected) ACCENT else MUTED,
            modifier = Modifier.size(23.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (selected) ACCENT else MUTED,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun MiniPlayerBar(
    track: Track,
    isPlaying: Boolean,
    progress: PlaybackProgress,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit
) {
    var tint by remember(track.artwork) { mutableStateOf(ACCENT) }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        color = Color(0xFF17110D).copy(alpha = .97f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .07f))
    ) {
        Column {
            if (progress.durationMs > 0) {
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = .08f))) {
                    Box(
                        Modifier
                            .fillMaxWidth((progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f))
                            .height(2.dp)
                            .background(ACCENT)
                    )
                }
            }
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtworkImage(track, tint, { tint = it }, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = WARM_WHITE, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(track.artist, color = MUTED, fontSize = 11.sp, maxLines = 1)
                }
                IconButton(onClick = onToggle) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", tint = WARM_WHITE)
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, "Next", tint = WARM_WHITE)
                }
            }
        }
    }
}

@Composable
private fun FrostedPanel(
    shape: Shape,
    modifier: Modifier = Modifier,
    accent: Color = ACCENT,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.clip(shape)) {
        Box(
            Modifier
                .matchParentSize()
                .blur(30.dp)
                .background(Brush.linearGradient(listOf(accent.copy(alpha = .30f), ACCENT2.copy(alpha = .14f), Color.Transparent)))
        )
        Box(Modifier.matchParentSize().background(GLASS))
        Box(Modifier.matchParentSize().border(1.dp, Color.White.copy(alpha = .10f), shape))
        content()
    }
}

@Composable
private fun GreetingHeader(onQueue: () -> Unit) {
    val greeting = remember {
        val hour = java.time.LocalTime.now().hour
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    FrostedPanel(RoundedCornerShape(22.dp), Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), CircleShape, color = ACCENT.copy(alpha = .18f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.GraphicEq, null, tint = ACCENT, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(greeting, color = MUTED, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("Streamfyree", color = WARM_WHITE, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
            }
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = .05f),
                border = BorderStroke(1.dp, ACCENT.copy(alpha = .3f))
            ) {
                IconButton(onClick = onQueue) {
                    Icon(Icons.Default.QueueMusic, "Queue", tint = WARM_WHITE)
                }
            }
        }
    }
}

@Composable
private fun ChipRow() {
    var selected by remember { mutableIntStateOf(0) }
    val chips = listOf("All", "Music", "Charts")
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEachIndexed { i, label ->
            Surface(
                Modifier.clip(RoundedCornerShape(16.dp)).clickable { selected = i },
                RoundedCornerShape(16.dp),
                color = if (selected == i) ACCENT else Color.White.copy(alpha = .06f)
            ) {
                Text(
                    label,
                    Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    color = if (selected == i) BG else WARM_WHITE,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ContinueListeningCard(track: Track, isPlaying: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    var tint by remember(track.artwork) { mutableStateOf(ACCENT) }
    FrostedPanel(RoundedCornerShape(18.dp), Modifier.fillMaxWidth().clickable(onClick = onOpen), accent = tint) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(track, tint, { tint = it }, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("CONTINUE LISTENING", color = ACCENT, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(track.title, color = WARM_WHITE, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(track.artist, color = MUTED, fontSize = 12.sp, maxLines = 1)
            }
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(42.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = ACCENT, contentColor = BG)
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play")
            }
        }
    }
}

@Composable
private fun QuickPicksGrid(tracks: List<Track>, onPlay: (Track) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tracks.chunked(2).forEach { rowTracks ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowTracks.forEach { track ->
                    QuickPickTile(track, Modifier.weight(1f)) { onPlay(track) }
                }
                if (rowTracks.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickPickTile(track: Track, modifier: Modifier, onClick: () -> Unit) {
    var tint by remember(track.artwork) { mutableStateOf(ACCENT) }
    Surface(
        modifier = modifier.height(60.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = .06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .09f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(
                track, tint, { tint = it },
                Modifier.size(60.dp).clip(
                    RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 0.dp, bottomEnd = 0.dp)
                )
            )
            Text(
                track.title,
                Modifier.padding(horizontal = 10.dp).weight(1f),
                color = WARM_WHITE,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun BrowseGrid(onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        genreTiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { genre ->
                    Surface(
                        modifier = Modifier.weight(1f).height(88.dp).clip(RoundedCornerShape(14.dp)).clickable { onPick(genre.label) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent
                    ) {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(genre.colors))) {
                            Text(
                                genre.label,
                                Modifier.padding(12.dp),
                                color = WARM_WHITE,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.MusicNote, null,
                                tint = Color.White.copy(alpha = .35f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .size(38.dp)
                                    .rotate(18f)
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
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
        Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
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
private fun SongCard(
    track: Track,
    rank: Int? = null,
    saved: Boolean = false,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onSave: () -> Unit,
    removeFromQueue: Boolean = false
) {
    var tint by remember(track.artwork) { mutableStateOf(ACCENT) }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onPlay),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = .045f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .08f))
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            rank?.let {
                Text(it.toString(), Modifier.width(24.dp), color = MUTED, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            ArtworkImage(track, tint, { tint = it }, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
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
}

@Composable
private fun QueueSheet(
    queue: List<Track>,
    currentId: String?,
    onPlay: (Track) -> Unit,
    onRemove: (Track) -> Unit,
    onClear: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Queue", color = WARM_WHITE, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(queue.size.toString() + " songs", color = MUTED, fontSize = 13.sp)
            }
            TextButton(onClick = onClear) { Text("Clear", color = ACCENT) }
        }
        queue.take(15).forEach { track ->
            SongCard(
                track = track,
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
    saved: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSave: () -> Unit,
    onQueue: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("PLAYING NOW", color = MUTED, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Queue", tint = WARM_WHITE) }
        }
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
            IconButton(onClick = onSave) {
                Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Save", tint = if (saved) ACCENT else WARM_WHITE)
            }
        }
        if (mode != PlaybackMode.ONLINE && progress.durationMs > 0) {
            Slider(
                value = progress.positionMs.coerceIn(0, progress.durationMs).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..progress.durationMs.toFloat(),
                colors = SliderDefaults.colors(activeTrackColor = ACCENT, inactiveTrackColor = Color.White.copy(alpha = .14f), thumbColor = WARM_WHITE)
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatTime(progress.positionMs), color = MUTED, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text(formatTime(progress.durationMs), color = MUTED, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
        }
        if (track.lyricVideo) {
            Pill("LYRICS AVAILABLE", ACCENT)
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shuffle, null, tint = MUTED, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(18.dp))
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Previous", tint = WARM_WHITE, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.width(10.dp))
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = ACCENT, contentColor = BG)
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(10.dp))
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next", tint = WARM_WHITE, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.width(18.dp))
            Icon(Icons.Default.Repeat, null, tint = MUTED, modifier = Modifier.size(20.dp))
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
