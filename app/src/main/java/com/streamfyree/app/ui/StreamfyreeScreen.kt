package com.streamfyree.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.streamfyree.app.*
import kotlinx.coroutines.delay

@Composable
fun StreamfyreeScreen(vm: MusicViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var fullPlayerOpen by remember { mutableStateOf(false) }
    var queueSheetOpen by remember { mutableStateOf(false) }
    var lyricsSheetOpen by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val currentTrack by vm.current.collectAsState()

    // Periodically update progress slider
    LaunchedEffect(Unit) {
        while (true) {
            vm.refreshProgress()
            delay(500)
        }
    }

    if (fullPlayerOpen) {
        BackHandler { fullPlayerOpen = false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
    ) {
        // Main Screen Content based on selected bottom tab
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (currentTrack != null) 140.dp else 80.dp)
        ) {
            YumaHeader(
                onOpenSleepTimer = { showSleepTimerDialog = true },
                onOpenSettings = { selectedTab = 3 }
            )

            Crossfade(targetState = selectedTab, label = "TabTransition") { tab ->
                when (tab) {
                    0 -> YumaHomeScreen(vm, onPlayTrack = { vm.play(it) })
                    1 -> YumaSearchScreen(vm, onPlayTrack = { vm.play(it) })
                    2 -> YumaLibraryScreen(vm, onPlayTrack = { vm.play(it) })
                    3 -> YumaSettingsScreen(vm, onOpenSleepTimer = { showSleepTimerDialog = true })
                }
            }
        }

        // Mini Player & Bottom Floating Navigation Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentTrack != null) {
                    YumaMiniPlayer(
                        vm = vm,
                        onExpand = { fullPlayerOpen = true }
                    )
                }

                YumaFloatingBottomBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it }
                )
            }
        }

        // Full Player Screen Overlay
        AnimatedVisibility(
            visible = fullPlayerOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            YumaFullPlayerSheet(
                vm = vm,
                onDismiss = { fullPlayerOpen = false },
                onOpenQueue = { queueSheetOpen = true },
                onOpenLyrics = { lyricsSheetOpen = true }
            )
        }

        // Queue Sheet Overlay
        if (queueSheetOpen) {
            YumaQueueSheet(
                vm = vm,
                onDismiss = { queueSheetOpen = false }
            )
        }

        // Lyrics Sheet Overlay
        if (lyricsSheetOpen) {
            YumaLyricsSheet(
                vm = vm,
                onDismiss = { lyricsSheetOpen = false }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            YumaSleepTimerDialog(
                vm = vm,
                onDismiss = { showSleepTimerDialog = false }
            )
        }
    }
}

@Composable
private fun ArtworkImage(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6366F1).copy(alpha = 0.35f),
                        Color(0xFFA855F7).copy(alpha = 0.25f),
                        Color(0xFF1C1C1E)
                    )
                )
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun YumaHeader(
    onOpenSleepTimer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Yuma",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Player",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(
                onClick = onOpenSleepTimer,
                modifier = Modifier
                    .size(40.dp)
                    .glassCard(cornerRadius = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Sleep Timer",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .glassCard(cornerRadius = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun YumaHomeScreen(
    vm: MusicViewModel,
    onPlayTrack: (Track) -> Unit
) {
    val discoverTracks by vm.discoverTracks.collectAsState()
    val historyTracks by vm.history.collectAsState()
    val podcastTracks by vm.podcastTracks.collectAsState()
    var selectedCategory by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Daily Mix Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .glassCard(cornerRadius = 28.dp, borderWidth = 1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF6366F1).copy(alpha = 0.45f),
                                Color(0xFFA855F7).copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.72f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SPOTIFY & YTM HYBRID",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1),
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your Daily Discovery",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Audiophile quality stream & recommendations",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    Button(
                        onClick = { vm.playMix("pop hits") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play Mix", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val categories = listOf("All", "Music", "Podcasts", "Relax", "Workout", "Chill")
                categories.forEachIndexed { idx, name ->
                    val isSelected = selectedCategory == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF6366F1) else Color(0x1AFFFFFF))
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else Color(0x1FFFFFFF),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedCategory = idx
                                if (idx == 2) vm.loadPodcasts()
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        // Quick Picks 2x3 Grid
        if (historyTracks.isNotEmpty() || discoverTracks.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Quick Picks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val items = (historyTracks + discoverTracks).distinctBy { it.id }.take(6)
                    val chunks = items.chunked(2)
                    chunks.forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { track ->
                                Box(modifier = Modifier.weight(1f)) {
                                    YumaQuickPickTile(track = track, onClick = { onPlayTrack(track) })
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Recommended / Discover Carousel
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Made For You",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Refresh",
                        fontSize = 13.sp,
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { vm.refreshDiscover() }
                    )
                }

                if (discoverTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .glassCard(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF6366F1))
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(discoverTracks) { track ->
                            YumaTrackCardItem(track = track, onClick = { onPlayTrack(track) })
                        }
                    }
                }
            }
        }

        // Podcasts Section
        if (selectedCategory == 2 || podcastTracks.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Popular Podcasts",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(podcastTracks) { track ->
                            YumaTrackCardItem(track = track, onClick = { onPlayTrack(track) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YumaQuickPickTile(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .glassCard(cornerRadius = 16.dp, borderWidth = 1.dp)
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            artworkUrl = track.artwork,
            modifier = Modifier.size(48.dp),
            cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun YumaTrackCardItem(track: Track, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        Box {
            ArtworkImage(
                artworkUrl = track.artwork,
                modifier = Modifier
                    .size(150.dp)
                    .glassCard(cornerRadius = 24.dp, borderWidth = 1.dp),
                cornerRadius = 24.dp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = track.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun YumaSearchScreen(
    vm: MusicViewModel,
    onPlayTrack: (Track) -> Unit
) {
    val state by vm.state.collectAsState()
    var searchInput by remember { mutableStateOf(state.query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 28.dp, borderWidth = 1.dp)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    vm.search(it)
                },
                placeholder = {
                    Text(
                        "Search tracks, artists, albums...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (searchInput.isNotEmpty()) {
                IconButton(onClick = { searchInput = "" }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF6366F1))
            }
        } else if (state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.error ?: "Search failed",
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }
        } else if (state.tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Search across YouTube Music catalog",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.tracks) { track ->
                    YumaTrackRow(
                        track = track,
                        onPlay = { onPlayTrack(track) },
                        onEnqueue = { vm.enqueue(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun YumaTrackRow(
    track: Track,
    onPlay: () -> Unit,
    onEnqueue: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 18.dp, borderWidth = 1.dp)
            .clickable(onClick = onPlay)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            artworkUrl = track.artwork,
            modifier = Modifier.size(52.dp),
            cornerRadius = 14.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist + if (track.album.isNotBlank()) " • ${track.album}" else "",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Color(0xFF1C1C1E))
            ) {
                DropdownMenuItem(
                    text = { Text("Play Now", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        onPlay()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF6366F1)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        onEnqueue()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.QueueMusic,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun YumaLibraryScreen(
    vm: MusicViewModel,
    onPlayTrack: (Track) -> Unit
) {
    val library by vm.library.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Your Library",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (library.saved.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.LibraryMusic,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved tracks in your library",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(library.saved) { track ->
                    YumaTrackRow(
                        track = track,
                        onPlay = { onPlayTrack(track) },
                        onEnqueue = { vm.enqueue(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun YumaSettingsScreen(
    vm: MusicViewModel,
    onOpenSleepTimer: () -> Unit
) {
    val mode by vm.mode.collectAsState()
    val sleepTimer by vm.sleepTimerMinutes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Playback Engine Mode
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 24.dp)
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = "Playback Engine",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select high bitrate resolution & extraction fallback",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlaybackMode.values().forEach { m ->
                        val isSel = mode == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) Color(0xFF6366F1) else Color(0x1AFFFFFF))
                                .clickable { vm.setMode(m) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = m.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }

        // Sleep Timer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 24.dp)
                .clickable(onClick = onOpenSleepTimer)
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sleep Timer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (sleepTimer != null) "Active: $sleepTimer minutes remaining" else "Off",
                        fontSize = 12.sp,
                        color = if (sleepTimer != null) Color(0xFF6366F1) else Color.White.copy(alpha = 0.65f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFF6366F1)
                )
            }
        }

        // App Information
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 24.dp)
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = "YumaPlayer (Streamfyree Hybrid)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version 0.3.0 • YDS 2.1 Design System • Chaquopy 16.0.0",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun YumaMiniPlayer(
    vm: MusicViewModel,
    onExpand: () -> Unit
) {
    val currentTrack by vm.current.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val progress by vm.progress.collectAsState()

    val track = currentTrack ?: return

    val progressFraction = if (progress.durationMs > 0) {
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .glassCard(
                cornerRadius = 24.dp,
                borderColor = Color.White.copy(alpha = 0.15f),
                backgroundColor = Color(0xEE141622)
            )
            .clickable(onClick = onExpand)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtworkImage(
                    artworkUrl = track.artwork,
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 14.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { vm.togglePlayPause() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { vm.next() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }

            // Bottom Progress Line
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color(0xFF6366F1),
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
private fun YumaFloatingBottomBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val items = listOf(
        Triple("Home", Icons.Default.Home, 0),
        Triple("Search", Icons.Outlined.Search, 1),
        Triple("Library", Icons.Outlined.LibraryMusic, 2),
        Triple("Settings", Icons.Outlined.Settings, 3)
    )

    val barHeight = 68.dp
    val pillWidth = 56.dp
    val pillHeight = 32.dp

    Box(
        modifier = Modifier
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .height(barHeight)
            .glassCard(
                cornerRadius = 24.dp,
                borderColor = Color.White.copy(alpha = 0.12f),
                backgroundColor = Color(0xEE141622)
            ),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / items.size
            val density = LocalDensity.current
            val pillOffsetPx = remember(tabWidth, selectedTab) {
                with(density) { ((tabWidth * selectedTab) + ((tabWidth - pillWidth) / 2)).toPx() }
            }

            val animatedPillPx by animateFloatAsState(
                targetValue = pillOffsetPx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "YumaPillOffset"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = animatedPillPx
                        translationY = 10.dp.toPx()
                    }
                    .width(pillWidth)
                    .height(pillHeight)
                    .background(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (label, icon, tabIndex) ->
                    val isSelected = selectedTab == tabIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectTab(tabIndex) }
                            .padding(top = 10.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YumaFullPlayerSheet(
    vm: MusicViewModel,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit
) {
    val currentTrack by vm.current.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val progress by vm.progress.collectAsState()
    val repeatMode by vm.repeat.collectAsState()
    val shuffle by vm.shuffle.collectAsState()
    val isSaved = currentTrack?.let { vm.isSaved(it) } ?: false

    val track = currentTrack ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
    ) {
        // Ambient Blurred Artwork Background
        if (!track.artwork.isNullOrBlank()) {
            AsyncImage(
                model = track.artwork,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .alpha(0.4f),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF090A0F).copy(alpha = 0.5f),
                            Color(0xFF090A0F).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM ALBUM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = track.album.ifBlank { "Single" },
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = { vm.enqueue(track) }) {
                    Icon(
                        imageVector = Icons.Outlined.QueueMusic,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
            }

            // Player Cover Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                ArtworkImage(
                    artworkUrl = track.artwork,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 28.dp
                )
            }

            // Metadata Title & Heart Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        if (isSaved) vm.unsaveTrack(track) else vm.saveTrack(track)
                    }
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) Color(0xFF6366F1) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Seeker Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress.positionMs.toFloat().coerceAtMost(progress.durationMs.toFloat().coerceAtLeast(1f)),
                    onValueChange = { vm.seekTo(it.toLong()) },
                    valueRange = 0f..(progress.durationMs.toFloat().coerceAtLeast(1f)),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6366F1),
                        activeTrackColor = Color(0xFF6366F1),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(progress.positionMs),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Text(
                        text = formatTime(progress.durationMs),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffle) Color(0xFF6366F1) else Color.White.copy(alpha = 0.5f)
                    )
                }

                IconButton(
                    onClick = { vm.previous() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = { vm.togglePlayPause() },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                            )
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = { vm.next() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(onClick = { vm.cycleRepeat() }) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.OFF) Color(0xFF6366F1) else Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Action Capsules
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = onOpenLyrics,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = Color(0xFF6366F1)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lyrics", color = Color.White)
                }

                Button(
                    onClick = onOpenQueue,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QueueMusic,
                        contentDescription = null,
                        tint = Color(0xFF6366F1)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Queue", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun YumaLyricsSheet(
    vm: MusicViewModel,
    onDismiss: () -> Unit
) {
    val lyrics by vm.lyrics.collectAsState()
    val lyricsLoading by vm.lyricsLoading.collectAsState()
    val progress by vm.progress.collectAsState()

    val activeIndex = remember(lyrics, progress.positionMs) {
        lyrics?.lines?.let { LyricsManager.getActiveLineIndex(it, progress.positionMs) } ?: -1
    }

    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lyrics?.isSynced == true) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F).copy(alpha = 0.96f))
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Synced Lyrics",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (lyricsLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                }
            } else if (lyrics == null || lyrics?.lines.isNullOrEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No lyrics found for this track",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(lyrics!!.lines) { index, line ->
                        val isActive = index == activeIndex
                        Text(
                            text = line.text,
                            fontSize = if (isActive) 26.sp else 18.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isActive) Color(0xFF6366F1) else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (line.timeMs > 0) vm.seekTo(line.timeMs)
                                }
                                .padding(vertical = 4.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YumaQueueSheet(
    vm: MusicViewModel,
    onDismiss: () -> Unit
) {
    val queue by vm.queue.collectAsState()
    val currentTrack by vm.current.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F).copy(alpha = 0.96f))
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue (${queue.size})",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row {
                    TextButton(onClick = { vm.clearQueue() }) {
                        Text("Clear All", color = Color.Red)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(queue) { track ->
                        val isPlaying = track.id == currentTrack?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(
                                    cornerRadius = 16.dp,
                                    backgroundColor = if (isPlaying) Color(0xFF6366F1).copy(alpha = 0.25f) else Color(0x1AFFFFFF)
                                )
                                .clickable { vm.play(track) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkImage(
                                artworkUrl = track.artwork,
                                modifier = Modifier.size(46.dp),
                                cornerRadius = 12.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPlaying) Color(0xFF6366F1) else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { vm.removeFromQueue(track) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White.copy(alpha = 0.5f)
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
private fun YumaSleepTimerDialog(
    vm: MusicViewModel,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 15, 30, 45, 60, 90)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 28.dp)
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Sleep Timer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(18.dp))

                options.forEach { mins ->
                    Button(
                        onClick = {
                            vm.setSleepTimer(mins)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("$mins minutes", color = Color.White, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = {
                    vm.setSleepTimer(null)
                    onDismiss()
                }) {
                    Text("Turn Off Sleep Timer", color = Color.Red, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
