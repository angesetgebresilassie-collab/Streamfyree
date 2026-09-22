package com.streamfyree.app.ui

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
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

private fun Color.darken(fraction: Float): Color = lerp(this, Color.Black, fraction.coerceIn(0f, 1f))
private fun Color.lighten(fraction: Float): Color = lerp(this, Color.White, fraction.coerceIn(0f, 1f))

/** Loads the artwork and extracts a vibrant dominant color, animating between songs. */
@Composable
private fun rememberArtworkColor(artwork: String?): Color {
    val context = LocalContext.current
    var target by remember { mutableStateOf(ACCENT) }
    LaunchedEffect(artwork) {
        if (artwork.isNullOrBlank()) {
            target = ACCENT
            return@LaunchedEffect
        }
        val palette = withContext(Dispatchers.IO) {
            runCatching {
                val loader = coil3.SingletonImageLoader.get(context)
                val request = coil3.request.ImageRequest.Builder(context)
                    .data(artwork)
                    .size(160)
                    .build()
                val image = (loader.execute(request) as? coil3.request.SuccessResult)?.image
                val bitmap = (image as? coil3.BitmapImage)?.bitmap
                bitmap?.let { Palette.from(it).generate() }
            }.getOrNull()
        }
        palette?.let { p ->
            val rgb = p.vibrantSwatch?.rgb
                ?: p.lightVibrantSwatch?.rgb
                ?: p.mutedSwatch?.rgb
                ?: p.darkVibrantSwatch?.rgb
                ?: p.dominantSwatch?.rgb
            if (rgb != null) target = Color(rgb)
        }
    }
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(700),
        label = "artwork-color"
    )
    return animated
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamfyreeScreen(vm: MusicViewModel) {
    val state by vm.state.collectAsState()
    val current by vm.current.collectAsState()
    val queue by vm.queue.collectAsState()
    val library by vm.library.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val history by vm.history.collectAsState()
    val discover by vm.discoverTracks.collectAsState()
    val playing by vm.isPlaying.collectAsState()
    val progress by vm.progress.collectAsState()
    val mode by vm.mode.collectAsState()
    val repeat by vm.repeat.collectAsState()
    val shuffle by vm.shuffle.collectAsState()
    val accent = rememberArtworkColor(current?.artwork)

    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var filterTab by remember { mutableIntStateOf(0) }
    var playerOpen by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }

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
            Modifier.fillMaxSize()
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
                    onPlay = {
                        playerOpen = true
                        vm.play(it)
                    },
                    onQueue = vm::enqueue,
                    isSaved = vm::isSaved,
                    onSave = { track ->
                        if (vm.isSaved(track)) vm.unsaveTrack(track) else vm.saveTrack(track)
                    },
                    onAddToPlaylist = { addToPlaylistTrack = it }
                )
                else -> LibraryContent(
                    library = library,
                    playlists = playlists,
                    onCreatePlaylist = vm::createPlaylist,
                    onDeletePlaylist = vm::deletePlaylist,
                    onPlayPlaylist = { pl ->
                        playerOpen = true
                        vm.playPlaylist(pl)
                    },
                    onRemoveFromPlaylist = vm::removeTrackFromPlaylist,
                    onPlayTrack = {
                        playerOpen = true
                        vm.play(it)
                    },
                    onQueueTrack = vm::enqueue,
                    onSaveTrack = vm::unsaveTrack,
                    onAddToPlaylist = { addToPlaylistTrack = it },
                    onBack = { tab = 0 }
                )
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = current != null,
                enter = slideInVertically { it } + fadeIn(tween(280)),
                exit = slideOutVertically { it } + fadeOut(tween(200))
            ) {
                current?.let { track ->
                    MiniPlayer(
                        track = track,
                        accent = accent,
                        playing = playing,
                        progress = progress,
                        onToggle = vm::togglePlayPause,
                        onPrevious = vm::previous,
                        onNext = vm::next,
                        onOpen = { playerOpen = true }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (playerOpen && current != null) {
        FullPlayer(
            track = current!!,
            accent = accent,
            playing = playing,
            progress = progress,
            mode = mode,
            repeat = repeat,
            shuffle = shuffle,
            resolving = state.loading,
            queue = queue,
            isSaved = vm.isSaved(current!!),
            onDismiss = { playerOpen = false },
            onToggle = vm::togglePlayPause,
            onPrevious = vm::previous,
            onNext = vm::next,
            onSeek = vm::seekTo,
            onShuffle = vm::toggleShuffle,
            onCycleRepeat = vm::cycleRepeat,
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

    addToPlaylistTrack?.let { track ->
        AddToPlaylistSheet(
            track = track,
            playlists = playlists,
            onDismiss = { addToPlaylistTrack = null },
            onSelectPlaylist = { plId ->
                vm.addTrackToPlaylist(plId, track)
                addToPlaylistTrack = null
            },
            onCreatePlaylist = { title ->
                vm.createPlaylist(title)
            }
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
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIcon(Icons.Default.Home, tab == 0, onHome, 44.dp)
        Spacer(Modifier.width(10.dp))
        CircleIcon(Icons.Default.Search, tab == 1, onSearch, 44.dp)
        Spacer(Modifier.weight(1f))
        CircleIcon(Icons.Default.NotificationsNone, false, {}, 34.dp, false)
        CircleIcon(Icons.Default.Groups, false, onQueue, 36.dp, false)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onLibrary),
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
            modifier = Modifier.size(size * 0.5f)
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
        RoundedCornerShape(16.dp),
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
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    isSaved: (Track) -> Boolean,
    onSave: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                Modifier.fillMaxWidth(),
                RoundedCornerShape(20.dp),
                color = PANEL
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                    Text("Search", color = TEXT, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    SearchBox(query, onQuery, onSearch)
                }
            }
        }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)), color = ACCENT, trackColor = TILE) }
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
                onSave = { onSave(track) },
                onAddToPlaylist = { onAddToPlaylist(track) }
            )
        }
    }
}

@Composable
private fun LibraryContent(
    library: LibraryState,
    playlists: List<Playlist>,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onRemoveFromPlaylist: (String, String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onQueueTrack: (Track) -> Unit,
    onSaveTrack: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onBack: () -> Unit
) {
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (selectedPlaylist != null) {
        val currentPl = playlists.find { it.id == selectedPlaylist!!.id } ?: selectedPlaylist!!
        PlaylistDetailView(
            playlist = currentPl,
            onPlayAll = { onPlayPlaylist(currentPl) },
            onPlayTrack = onPlayTrack,
            onRemoveTrack = { trackId -> onRemoveFromPlaylist(currentPl.id, trackId) },
            onDeletePlaylist = {
                onDeletePlaylist(currentPl.id)
                selectedPlaylist = null
            },
            onBack = { selectedPlaylist = null }
        )
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your Library", color = TEXT, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("${library.saved.size} saved tracks • ${playlists.size} playlists", color = MUTED, fontSize = 14.sp)
                }
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "New Playlist", tint = ACCENT)
                }
                TextButton(onClick = onBack) { Text("Home", color = MUTED) }
            }
        }

        if (playlists.isNotEmpty()) {
            item {
                Text("Playlists", color = TEXT, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            items(playlists, key = { "pl-" + it.id }) { pl ->
                Surface(
                    Modifier.fillMaxWidth().clickable { selectedPlaylist = pl },
                    RoundedCornerShape(18.dp),
                    color = TILE
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(PANEL),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!pl.artwork.isNullOrBlank()) {
                                AsyncImage(
                                    model = pl.artwork,
                                    contentDescription = pl.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.QueueMusic, null, tint = ACCENT, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pl.title, color = TEXT, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pl.subtitle, color = MUTED, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onPlayPlaylist(pl) }) {
                            Icon(Icons.Default.PlayArrow, "Play", tint = ACCENT)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Liked Songs", color = TEXT, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (library.saved.isEmpty()) {
            item { EmptyCard("Nothing saved yet", "Tap the heart on any track to add it here.") }
        } else {
            items(library.saved, key = { "library-" + it.id }) { track ->
                TrackRow(
                    track,
                    true,
                    onPlay = { onPlayTrack(track) },
                    onQueue = { onQueueTrack(track) },
                    onSave = { onSaveTrack(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) }
                )
            }
        }
    }

    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Playlist", color = TEXT) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    placeholder = { Text("Playlist Title", color = MUTED) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TEXT, unfocusedTextColor = TEXT,
                        focusedBorderColor = ACCENT, unfocusedBorderColor = MUTED
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) {
                        onCreatePlaylist(newTitle)
                        showCreateDialog = false
                    }
                }) { Text("Create", color = ACCENT) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = MUTED) }
            },
            containerColor = PANEL
        )
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    onPlayAll: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRemoveTrack: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TEXT)
                }
                Column(Modifier.weight(1f)) {
                    Text(playlist.title, color = TEXT, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(playlist.subtitle, color = MUTED, fontSize = 13.sp)
                }
                IconButton(onClick = onDeletePlaylist) {
                    Icon(Icons.Default.Delete, "Delete Playlist", tint = MUTED)
                }
            }
        }

        if (playlist.tracks.isNotEmpty()) {
            item {
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play Playlist", fontWeight = FontWeight.Bold)
                }
            }

            items(playlist.tracks, key = { "pltrack-" + it.id }) { track ->
                Surface(
                    Modifier.fillMaxWidth().clickable { onPlayTrack(track) },
                    RoundedCornerShape(18.dp),
                    color = TILE
                ) {
                    Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Artwork(track, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, color = TEXT, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = MUTED, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onRemoveTrack(track.id) }) {
                            Icon(Icons.Default.Close, "Remove", tint = MUTED)
                        }
                    }
                }
            }
        } else {
            item {
                EmptyCard("Playlist is empty", "Add tracks from search or library to this playlist.")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlaylistSheet(
    track: Track,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PANEL,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MUTED) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Add to Playlist", color = TEXT, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, null, tint = ACCENT)
                    Spacer(Modifier.width(4.dp))
                    Text("New", color = ACCENT)
                }
            }
            Text("Select a playlist for '${track.title}'", color = MUTED, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))

            if (playlists.isEmpty()) {
                EmptyCard("No playlists yet", "Create your first playlist using the New button above.")
            } else {
                playlists.forEach { pl ->
                    Surface(
                        Modifier.fillMaxWidth().clickable { onSelectPlaylist(pl.id) },
                        RoundedCornerShape(16.dp),
                        color = TILE
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlaylistAdd, null, tint = ACCENT)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(pl.title, color = TEXT, fontWeight = FontWeight.SemiBold)
                                Text(pl.subtitle, color = MUTED, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Playlist", color = TEXT) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Playlist Title", color = MUTED) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TEXT, unfocusedTextColor = TEXT,
                        focusedBorderColor = ACCENT, unfocusedBorderColor = MUTED
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        onCreatePlaylist(title)
                        showCreateDialog = false
                    }
                }) { Text("Create", color = ACCENT) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = MUTED) }
            },
            containerColor = PANEL
        )
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
private fun TrackRow(
    track: Track,
    saved: Boolean,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onSave: () -> Unit,
    onAddToPlaylist: () -> Unit
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
            IconButton(onClick = onAddToPlaylist) {
                Icon(Icons.Default.PlaylistAdd, "Add to Playlist", tint = MUTED)
            }
            IconButton(onClick = onQueue) {
                Icon(Icons.Default.AddCircleOutline, "Enqueue", tint = MUTED)
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    track: Track,
    accent: Color,
    playing: Boolean,
    progress: PlaybackProgress,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit
) {
    val deep = accent.darken(.52f)
    val mid = accent.darken(.34f)
    val fraction = if (progress.durationMs > 0)
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
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
            modifier = Modifier.fillMaxSize().blur(34.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(deep.copy(alpha = .93f), mid.copy(alpha = .82f), deep.copy(alpha = .93f))
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
                Text(track.artist, color = Color.White.copy(alpha = .74f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PlayerMiniButton(Icons.Default.SkipPrevious, onPrevious)
            PlayerMiniButton(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, onToggle, true)
            PlayerMiniButton(Icons.Default.SkipNext, onNext)
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(.86f)
                .padding(bottom = 7.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = .22f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.lighten(.15f))
            )
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
    accent: Color,
    playing: Boolean,
    progress: PlaybackProgress,
    mode: PlaybackMode,
    repeat: RepeatMode,
    shuffle: Boolean,
    resolving: Boolean = false,
    queue: List<Track>,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleSaved: () -> Unit,
    onQueue: () -> Unit
) {
    val context = LocalContext.current
    var heartPressed by remember { mutableStateOf(false) }

    val playColor = accent.lighten(.06f)
    val onPlayColor = if (playColor.luminance() > .5f) Color.Black else Color.White
    val activeTint = accent.lighten(.24f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.let { w ->
                WindowCompat.setDecorFitsSystemWindows(w, false)
                w.statusBarColor = android.graphics.Color.TRANSPARENT
                w.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
        }

        val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }

        AnimatedVisibility(
            visibleState = visibleState,
            enter = slideInVertically(tween(340)) { it } + fadeIn(tween(240)),
            exit = slideOutVertically(tween(240)) { it } + fadeOut(tween(160))
        ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(BG)
        ) {
            Crossfade(
                targetState = track.artwork,
                animationSpec = androidx.compose.animation.core.tween(400),
                label = "artwork-background"
            ) { artwork ->
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(48.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.darken(.18f).copy(alpha = .55f),
                                accent.darken(.46f).copy(alpha = .74f),
                                accent.darken(.74f).copy(alpha = .97f)
                            )
                        )
                    )
            )

            val availableWidth = maxWidth
            val availableHeight = maxHeight

            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 22.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        onClick = onDismiss
                    )
                    Text(
                        "Now Playing",
                        color = TEXT.copy(alpha = .9f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    GlassIconButton(
                        icon = Icons.Default.MoreVert,
                        onClick = { if (queue.isNotEmpty()) onQueue() }
                    )
                }

                val artworkSize = minOf(availableWidth * .90f, availableHeight * .48f)
                Spacer(Modifier.height(10.dp))

                Crossfade(
                    targetState = track.artwork,
                    animationSpec = androidx.compose.animation.core.tween(200),
                    label = "main-artwork"
                ) { artwork ->
                    Box(
                        Modifier
                            .size(artworkSize)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(30.dp))
                    ) {
                        AsyncImage(
                            model = artwork,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.height(if (availableHeight < 760.dp) 14.dp else 20.dp))

                Text(
                    track.title,
                    color = TEXT,
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    track.artist,
                    color = Color.White.copy(alpha = .82f),
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                androidx.compose.animation.AnimatedVisibility(visible = resolving) {
                    Column {
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                            color = ACCENT,
                            trackColor = Color.White.copy(alpha = .14f)
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Slider(
                    value = progress.positionMs
                        .coerceIn(0L, progress.durationMs.coerceAtLeast(1L))
                        .toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..progress.durationMs.coerceAtLeast(1L).toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = activeTint,
                        inactiveTrackColor = Color.White.copy(alpha = .20f)
                    )
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(progress.positionMs), color = MUTED, fontSize = 12.sp)
                    Text(formatTime(progress.durationMs), color = MUTED, fontSize = 12.sp)
                }

                Spacer(Modifier.height(8.dp))

                GlassPill(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (availableHeight < 760.dp) 132.dp else 154.dp)
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onShuffle,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                "Shuffle",
                                tint = if (shuffle) activeTint else Color.White.copy(alpha = .6f),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(58.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = onToggle,
                            modifier = Modifier.size(if (availableHeight < 760.dp) 104.dp else 120.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = playColor,
                                contentColor = onPlayColor
                            )
                        ) {
                            Crossfade(
                                targetState = playing,
                                animationSpec = androidx.compose.animation.core.tween(90),
                                label = "play-pause"
                            ) { isNowPlaying ->
                                Icon(
                                    if (isNowPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(58.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                "Next",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = onCycleRepeat,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                if (repeat == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                "Repeat",
                                tint = if (repeat == RepeatMode.OFF) Color.White.copy(alpha = .6f) else activeTint,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                GlassPill(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(310.dp)
                        .height(70.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Listening to ${track.title} by ${track.artist}"
                                    )
                                    putExtra(Intent.EXTRA_TITLE, track.title)
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Share track")
                                )
                            }
                        ) {
                            Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(25.dp))
                        }

                        IconButton(
                            onClick = {
                                heartPressed = true
                                onToggleSaved()
                            }
                        ) {
                            val scale by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (heartPressed) 1.22f else 1f,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = .55f,
                                    stiffness = 500f
                                ),
                                finishedListener = { heartPressed = false },
                                label = "heart-scale"
                            )
                            Icon(
                                if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                "Favorite",
                                tint = if (isSaved) ACCENT else Color.White,
                                modifier = Modifier.size(28.dp).scale(scale)
                            )
                        }

                        IconButton(onClick = onQueue) {
                            Icon(
                                Icons.Default.QueueMusic,
                                "Queue",
                                tint = Color.White,
                                modifier = Modifier.size(27.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun GlassPill(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(48.dp),
        color = Color.White.copy(alpha = .11f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .14f)),
        shadowElevation = 2.dp
    ) {
        Row(content = content)
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = .06f))
    ) {
        Icon(icon, null, tint = TEXT, modifier = Modifier.size(29.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                    onSave = {},
                    onAddToPlaylist = {}
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
