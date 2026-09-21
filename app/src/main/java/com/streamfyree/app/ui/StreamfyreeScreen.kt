package com.streamfyree.app.ui

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.streamfyree.app.*

private val BG = Color(0xFF080808)
private val PANEL = Color(0xFF171717)
private val TILE = Color(0xFF262626)
private val TEXT = Color(0xFFF8F8F8)
private val MUTED = Color(0xFFA3A3A3)
private val ACCENT = Color(0xFFFFB1B5)
private val ACCENT_DEEP = Color(0xFF7E232F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamfyreeScreen(vm: MusicViewModel) {
    val state by vm.state.collectAsState()
    val current by vm.current.collectAsState()
    val queue by vm.queue.collectAsState()
    val library by vm.library.collectAsState()
    val history by vm.history.collectAsState()
    val discover by vm.discoverTracks.collectAsState()
    val playing by vm.isPlaying.collectAsState()
    val progress by vm.progress.collectAsState()
    val mode by vm.mode.collectAsState()

    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var filterTab by remember { mutableIntStateOf(0) }
    var playerOpen by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }

    LaunchedEffect(playing) {
        while (playing) {
            vm.refreshProgress()
            kotlinx.coroutines.delay(500)
        }
        vm.refreshProgress()
    }

    BackHandler(enabled = playerOpen) { playerOpen = false }
    BackHandler(enabled = queueOpen && !playerOpen) { queueOpen = false }

    Box(Modifier.fillMaxSize().background(BG)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TopBar(
                tab = tab,
                onHome = { tab = 0 },
                onSearch = { tab = 1 },
                onLibrary = { tab = 2 },
                onQueue = { queueOpen = true },
                profileArt = current?.artwork ?: history.firstOrNull()?.artwork ?: discover.firstOrNull()?.artwork
            )

            when (tab) {
                0 -> HomeContent(
                    filterTab = filterTab,
                    onFilterTab = { filterTab = it },
                    current = current,
                    history = history,
                    discover = discover,
                    onPlay = {
                        playerOpen = true
                        vm.play(it)
                    },
                    onOpenPlayer = { playerOpen = true },
                    onSeeLibrary = { tab = 2 }
                )
                1 -> SearchContent(
                    query = query,
                    onQuery = { query = it },
                    onSearch = { vm.search(query) },
                    state = state,
                    mode = mode,
                    onMode = vm::setMode,
                    onPlay = {
                        playerOpen = true
                        vm.play(it)
                    },
                    onQueue = vm::enqueue,
                    isSaved = vm::isSaved,
                    onSave = { track ->
                        if (vm.isSaved(track)) vm.unsaveTrack(track) else vm.saveTrack(track)
                    }
                )
                else -> LibraryContent(
                    library = library,
                    onPlay = {
                        playerOpen = true
                        vm.play(it)
                    },
                    onQueue = vm::enqueue,
                    onSave = vm::unsaveTrack,
                    onBack = { tab = 0 }
                )
            }

            Spacer(Modifier.weight(1f))

            current?.let { track ->
                MiniPlayer(
                    track = track,
                    playing = playing,
                    onToggle = vm::togglePlayPause,
                    onPrevious = vm::previous,
                    onNext = vm::next,
                    onOpen = { playerOpen = true }
                )
            }

            Spacer(Modifier.height(10.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (playerOpen && current != null) {
        FullPlayer(
            track = current!!,
            playing = playing,
            progress = progress,
            mode = mode,
            queue = queue,
            isSaved = vm.isSaved(current!!),
            onDismiss = { playerOpen = false },
            onToggle = vm::togglePlayPause,
            onPrevious = vm::previous,
            onNext = vm::next,
            onSeek = vm::seekTo,
            onToggleSaved = {
                if (vm.isSaved(current!!)) vm.unsaveTrack(current!!) else vm.saveTrack(current!!)
            },
            onQueue = { queueOpen = true }
        )
    }

    if (queueOpen && !playerOpen) {
        QueueSheet(
            queue = queue,
            isSaved = vm::isSaved,
            onDismiss = { queueOpen = false },
            onPlay = {
                queueOpen = false
                playerOpen = true
                vm.play(it)
            },
            onRemove = vm::removeFromQueue,
            onClear = vm::clearQueue
        )
    }
}

@Composable
private fun TopBar(
    tab: Int,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onQueue: () -> Unit,
    profileArt: String?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIcon(Icons.Default.Home, tab == 0, onHome, 58.dp)
        Spacer(Modifier.width(12.dp))
        CircleIcon(Icons.Default.Search, tab == 1, onSearch, 58.dp)
        Spacer(Modifier.weight(1f))
        CircleIcon(Icons.Default.NotificationsNone, false, {}, 42.dp, false)
        CircleIcon(Icons.Default.Groups, false, onQueue, 46.dp, false)
        Spacer(Modifier.width(7.dp))
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onLibrary),
            contentAlignment = Alignment.Center
        ) {
            if (!profileArt.isNullOrBlank()) {
                AsyncImage(
                    model = profileArt,
                    contentDescription = "Library",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize().background(TILE), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = TEXT)
                }
            }
        }
    }
}

@Composable
private fun CircleIcon(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    size: Dp,
    filled: Boolean = true
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                when {
                    filled && selected -> Color.White
                    filled -> Color(0xFF181818)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            tint = if (filled && selected) Color.Black else TEXT,
            modifier = Modifier.size(if (size >= 56.dp) 28.dp else 24.dp)
        )
    }
}

@Composable
private fun HomeContent(
    filterTab: Int,
    onFilterTab: (Int) -> Unit,
    current: Track?,
    history: List<Track>,
    discover: List<Track>,
    onPlay: (Track) -> Unit,
    onOpenPlayer: () -> Unit,
    onSeeLibrary: () -> Unit
) {
    val tiles = buildList {
        add(LibraryTile("Liked Songs", history.firstOrNull(), Icons.Default.Favorite))
        if (discover.isNotEmpty()) add(LibraryTile("MOONLIGHT", discover[0]))
        if (history.isNotEmpty()) add(LibraryTile("Anime On Replay", history[0]))
        if (discover.size > 1) add(LibraryTile("MEMCH0 MIX", discover[1]))
        if (history.size > 1) add(LibraryTile("TV SERIES", history[1]))
        if (discover.size > 2) add(LibraryTile("EVE", discover[2]))
        if (history.size > 2) add(LibraryTile("SHANENA BABY", history[2]))
        if (discover.size > 3) add(LibraryTile("DAILY ANIME", discover[3]))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 30.dp, end = 30.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Surface(
                Modifier.fillMaxWidth(),
                RoundedCornerShape(20.dp),
                color = PANEL
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                    FilterTabs(filterTab, onFilterTab)
                    Spacer(Modifier.height(22.dp))
                    tiles.chunked(2).forEach { rowTiles ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTiles.forEach { tile ->
                                PlaylistTile(
                                    tile = tile,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        tile.track?.let(onPlay) ?: onSeeLibrary()
                                    }
                                )
                            }
                            if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        if (filterTab != 2) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                    Text("Made For", color = MUTED, fontSize = 15.sp)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Text("MuwMix", color = TEXT, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onSeeLibrary) {
                            Text("Show all", color = MUTED, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                val mixes = discover.take(6)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(mixes, key = { "mix-" + it.id }) { track ->
                        DailyMixTile(
                            track = track,
                            number = (mixes.indexOf(track) + 1).coerceAtMost(6),
                            onClick = { onPlay(track) }
                        )
                    }
                }
            }

            current?.let { track ->
                item {
                    SectionHeader("Now playing", "Open player", onOpenPlayer)
                    Spacer(Modifier.height(8.dp))
                    NowPlayingCard(track, onOpenPlayer)
                }
            }

            if (history.isNotEmpty()) {
                item {
                    SectionHeader("Recently played", "Show all", onSeeLibrary)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(history.take(10), key = { "recent-" + it.id }) { track ->
                            RecentTile(track) { onPlay(track) }
                        }
                    }
                }
            }
        } else {
            item {
                EmptyCard(
                    "Podcasts are coming soon",
                    "Your music library is ready for albums, mixes and saved songs."
                )
            }
        }
    }
}

@Composable
private fun FilterTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf("All", "Music", "Podcasts").forEachIndexed { index, label ->
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(24.dp),
                color = if (selected == index) Color.White else TILE,
                contentColor = if (selected == index) Color.Black else TEXT
            ) {
                Text(
                    label,
                    Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    fontSize = 15.sp,
                    fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

private data class LibraryTile(
    val title: String,
    val track: Track?,
    val icon: ImageVector? = null
)

@Composable
private fun PlaylistTile(
    tile: LibraryTile,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier.height(74.dp).clickable(onClick = onClick),
        RoundedCornerShape(8.dp),
        color = TILE
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(74.dp).background(
                    if (tile.icon != null) {
                        Brush.linearGradient(listOf(Color(0xFF5D1CFF), Color(0xFF9CEBFF)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF151515)))
                    }
                ),
                contentAlignment = Alignment.Center
            ) {
                if (tile.track?.artwork != null) {
                    AsyncImage(
                        model = tile.track.artwork,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
                }
                Icon(
                    tile.icon ?: Icons.Default.MusicNote,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                tile.title,
                color = TEXT,
                modifier = Modifier.padding(horizontal = 12.dp),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DailyMixTile(track: Track, number: Int, onClick: () -> Unit) {
    Column(Modifier.width(210.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(TILE)
        ) {
            AsyncImage(
                model = track.artwork,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Row(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(color = Color(0xFF00F3FF), shape = RoundedCornerShape(2.dp)) {
                    Text(
                        "Daily Mix",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = if (number % 2 == 0) Color(0xFFD6F000) else Color(0xFF00F3FF),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        "%02d".format(number),
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            track.artist + ", " + track.album.ifBlank { track.title } + " and more",
            color = MUTED,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(title, color = TEXT, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onAction) {
            Text(action, color = MUTED, fontSize = 13.sp)
        }
    }
}

@Composable
private fun NowPlayingCard(track: Track, onOpen: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        RoundedCornerShape(18.dp),
        color = TILE
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(track, Modifier.size(66.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("NOW PLAYING", color = ACCENT, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(track.title, color = TEXT, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = MUTED, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.GraphicEq, null, tint = Color(0xFF00EB8D), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun RecentTile(track: Track, onClick: () -> Unit) {
    Column(Modifier.width(122.dp).clickable(onClick = onClick)) {
        Artwork(track, Modifier.size(122.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.height(6.dp))
        Text(track.title, color = TEXT, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, color = MUTED, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SearchContent(
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    state: SearchState,
    mode: PlaybackMode,
    onMode: (PlaybackMode) -> Unit,
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    isSaved: (Track) -> Boolean,
    onSave: (Track) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Search", color = TEXT, fontSize = 32.sp, fontWeight = FontWeight.Bold) }
        item { SearchBox(query, onQuery, onSearch) }
        item { ModeRow(mode, onMode) }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = ACCENT) }
        state.error?.let { item { ErrorCard(it) } }
        if (state.tracks.isEmpty() && !state.loading) {
            item { EmptyCard("Search for your next obsession", "Try an artist, track, album, or character song.") }
        }
        items(state.tracks, key = { "search-" + it.id }) { track ->
            TrackRow(
                track,
                isSaved(track),
                onPlay = { onPlay(track) },
                onQueue = { onQueue(track) },
                onSave = { onSave(track) }
            )
        }
    }
}

@Composable
private fun LibraryContent(
    library: LibraryState,
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onSave: (Track) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your Library", color = TEXT, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(library.saved.size.toString() + " saved tracks", color = MUTED, fontSize = 14.sp)
                }
                TextButton(onClick = onBack) { Text("Home", color = MUTED) }
            }
        }
        if (library.saved.isEmpty()) {
            item { EmptyCard("Nothing saved yet", "Tap the heart on any track to add it here.") }
        }
        items(library.saved, key = { "library-" + it.id }) { track ->
            TrackRow(
                track,
                true,
                onPlay = { onPlay(track) },
                onQueue = { onQueue(track) },
                onSave = { onSave(track) }
            )
        }
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, onSearch: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Search songs, artists, albums…", color = MUTED) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MUTED) },
        trailingIcon = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.ArrowForward, null, tint = ACCENT)
            }
        },
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ACCENT,
            unfocusedBorderColor = Color.White.copy(alpha = .12f),
            focusedTextColor = TEXT,
            unfocusedTextColor = TEXT,
            cursorColor = ACCENT,
            focusedContainerColor = TILE,
            unfocusedContainerColor = TILE
        )
    )
}

@Composable
private fun ModeRow(mode: PlaybackMode, onMode: (PlaybackMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            PlaybackMode.AUTO to "Auto",
            PlaybackMode.NATIVE to "Native",
            PlaybackMode.ONLINE to "Online"
        ).forEach { (m, label) ->
            FilterChip(
                selected = mode == m,
                onClick = { onMode(m) },
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        if (m == PlaybackMode.ONLINE) Icons.Default.Language
                        else if (m == PlaybackMode.NATIVE) Icons.Default.Headphones
                        else Icons.Default.Tune,
                        null
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ACCENT,
                    selectedLabelColor = Color.Black,
                    selectedLeadingIconColor = Color.Black
                )
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    saved: Boolean,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onPlay),
        RoundedCornerShape(18.dp),
        color = TILE
    ) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(track, Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = TEXT, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = MUTED, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (track.lyricVideo) Text("LYRICS", color = ACCENT, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onSave) {
                Icon(
                    if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    null,
                    tint = if (saved) ACCENT else MUTED
                )
            }
            IconButton(onClick = onQueue) {
                Icon(Icons.Default.AddCircleOutline, null, tint = MUTED)
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    track: Track,
    playing: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(42.dp))
            .clickable(onClick = onOpen)
    ) {
        AsyncImage(
            model = track.artwork,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(.32f),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(ACCENT_DEEP, Color(0xFF8B2730).copy(alpha = .84f), ACCENT_DEEP)
                )
            )
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Artwork(track, Modifier.size(62.dp).clip(RoundedCornerShape(18.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = TEXT, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = Color.White.copy(alpha = .72f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PlayerMiniButton(Icons.Default.SkipPrevious, onPrevious)
            PlayerMiniButton(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, onToggle, true)
            PlayerMiniButton(Icons.Default.SkipNext, onNext)
        }
    }
}

@Composable
private fun PlayerMiniButton(
    icon: ImageVector,
    onClick: () -> Unit,
    emphasized: Boolean = false
) {
    Box(
        Modifier
            .padding(start = 3.dp)
            .size(if (emphasized) 50.dp else 42.dp)
            .clip(CircleShape)
            .background(if (emphasized) Color.White.copy(alpha = .16f) else Color.White.copy(alpha = .10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(if (emphasized) 28.dp else 22.dp))
    }
}

@Composable
private fun FullPlayer(
    track: Track,
    playing: Boolean,
    progress: PlaybackProgress,
    mode: PlaybackMode,
    queue: List<Track>,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleSaved: () -> Unit,
    onQueue: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().background(BG)) {
            AsyncImage(
                model = track.artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(.30f),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF555555).copy(alpha = .34f),
                            BG.copy(alpha = .90f),
                            BG
                        )
                    )
                )
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 22.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = TEXT, modifier = Modifier.size(34.dp))
                    }
                    Text(
                        "Now Playing",
                        color = TEXT.copy(alpha = .86f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = { if (queue.isNotEmpty()) onQueue() }) {
                        Icon(Icons.Default.MoreVert, null, tint = TEXT)
                    }
                }

                Spacer(Modifier.height(14.dp))

                if ((mode == PlaybackMode.ONLINE || mode == PlaybackMode.AUTO) && !track.youtubeUrl.isNullOrBlank()) {
                    YouTubeView(
                        track.youtubeUrl!!,
                        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(30.dp))
                    )
                } else {
                    Artwork(
                        track,
                        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(30.dp))
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    track.title,
                    color = TEXT,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    color = Color.White.copy(alpha = .78f),
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(12.dp))

                Slider(
                    value = progress.positionMs.coerceIn(0L, progress.durationMs.coerceAtLeast(1L)).toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..progress.durationMs.coerceAtLeast(1L).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent,
                        activeTrackColor = Color.White.copy(alpha = .82f),
                        inactiveTrackColor = Color.White.copy(alpha = .18f)
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(progress.positionMs), color = MUTED, fontSize = 12.sp)
                    Text(formatTime(progress.durationMs), color = MUTED, fontSize = 12.sp)
                }

                Spacer(Modifier.height(10.dp))

                Surface(
                    Modifier.fillMaxWidth(),
                    RoundedCornerShape(42.dp),
                    color = Color.White.copy(alpha = .11f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .10f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 26.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(58.dp)) {
                            Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        FilledIconButton(
                            onClick = onToggle,
                            modifier = Modifier.size(74.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = ACCENT,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(58.dp)) {
                            Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Surface(
                    Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
                    RoundedCornerShape(34.dp),
                    color = Color.White.copy(alpha = .11f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .10f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(25.dp))
                        }
                        IconButton(onClick = onToggleSaved) {
                            Icon(
                                if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                null,
                                tint = if (isSaved) ACCENT else Color.White,
                                modifier = Modifier.size(27.dp)
                            )
                        }
                        IconButton(onClick = onQueue) {
                            Icon(Icons.Default.QueueMusic, null, tint = Color.White, modifier = Modifier.size(27.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSheet(
    queue: List<Track>,
    isSaved: (Track) -> Boolean,
    onDismiss: () -> Unit,
    onPlay: (Track) -> Unit,
    onRemove: (Track) -> Unit,
    onClear: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PANEL,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MUTED) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Queue", color = TEXT, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(queue.size.toString() + " tracks", color = MUTED, fontSize = 13.sp)
                }
                TextButton(onClick = onClear) { Text("Clear", color = ACCENT) }
            }
            Spacer(Modifier.height(10.dp))
            queue.take(20).forEach { track ->
                TrackRow(
                    track = track,
                    saved = isSaved(track),
                    onPlay = { onPlay(track) },
                    onQueue = { onRemove(track) },
                    onSave = {}
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun Artwork(track: Track, modifier: Modifier) {
    AsyncImage(
        model = track.artwork,
        contentDescription = track.title,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun ErrorCard(message: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), color = Color(0xFF352025)) {
        Text(message, Modifier.padding(15.dp), color = TEXT)
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = TILE) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MusicNote, null, tint = ACCENT, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = TEXT, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = MUTED, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun YouTubeView(url: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { webView ->
            val id = Regex("[?&]v=([^&]+)").find(url)?.groupValues?.get(1)
                ?: Regex("youtu\\.be/([^?&/]+)").find(url)?.groupValues?.get(1)
            val target = if (!id.isNullOrBlank()) {
                "https://www.youtube-nocookie.com/embed/" + id + "?playsinline=1&autoplay=1&rel=0"
            } else {
                url
            }
            if (webView.url != target) webView.loadUrl(target)
        }
    )
}
