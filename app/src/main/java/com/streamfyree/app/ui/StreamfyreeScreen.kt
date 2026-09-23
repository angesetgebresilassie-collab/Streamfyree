package com.streamfyree.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.layout.ContentScale
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

    // Periodically refresh progress for seeker slider
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
            .background(YumaColors.Background)
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
                    0 -> YumaHomeContent(vm, onPlayTrack = { vm.play(it) })
                    1 -> YumaSearchContent(vm, onPlayTrack = { vm.play(it) })
                    2 -> YumaLibraryContent(vm, onPlayTrack = { vm.play(it) })
                    3 -> YumaSettingsContent(vm, onOpenSleepTimer = { showSleepTimerDialog = true })
                }
            }
        }

        // Mini Player & Bottom Navigation Bar stacked at bottom
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

                YumaBottomNav(
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
    cornerRadius: Dp = 16.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        YumaColors.PrimaryAccent.copy(alpha = 0.35f),
                        YumaColors.SecondaryAccent.copy(alpha = 0.25f),
                        YumaColors.GlassCardBg
                    )
                )
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
                tint = YumaColors.TextMuted,
                modifier = Modifier.size(32.dp)
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(YumaColors.PrimaryAccent, YumaColors.SecondaryAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Streamfyree",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = YumaColors.TextPrimary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onOpenSleepTimer,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(YumaColors.GlassCardBg)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Sleep Timer",
                    tint = YumaColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(YumaColors.GlassCardBg)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = YumaColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun YumaHomeContent(
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
                    .height(180.dp)
                    .glassCard(cornerRadius = 24.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                YumaColors.PrimaryAccent.copy(alpha = 0.45f),
                                YumaColors.SecondaryAccent.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.7f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DAILY MIX",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = YumaColors.PrimaryAccent,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your Music Discovery",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = YumaColors.TextPrimary
                        )
                        Text(
                            text = "Personalized recommendation feed",
                            fontSize = 13.sp,
                            color = YumaColors.TextSecondary
                        )
                    }

                    Button(
                        onClick = { vm.playMix("pop hits") },
                        colors = ButtonDefaults.buttonColors(containerColor = YumaColors.PrimaryAccent),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play Mix", fontWeight = FontWeight.Bold)
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
                            .background(if (isSelected) YumaColors.PrimaryAccent else YumaColors.GlassCardBg)
                            .border(
                                0.5.dp,
                                if (isSelected) Color.Transparent else YumaColors.GlassCardBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedCategory = idx
                                if (idx == 2) vm.loadPodcasts()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else YumaColors.TextSecondary
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = YumaColors.TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val items = (historyTracks + discoverTracks).distinctBy { it.id }.take(6)
                    val chunks = items.chunked(2)
                    chunks.forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = YumaColors.TextPrimary
                    )
                    Text(
                        text = "Refresh",
                        fontSize = 12.sp,
                        color = YumaColors.PrimaryAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { vm.refreshDiscover() }
                    )
                }

                if (discoverTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .glassCard(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = YumaColors.PrimaryAccent)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = YumaColors.TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
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
            .height(56.dp)
            .glassCard(cornerRadius = 14.dp)
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            artworkUrl = track.artwork,
            modifier = Modifier.size(44.dp),
            cornerRadius = 10.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = YumaColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                fontSize = 11.sp,
                color = YumaColors.TextSecondary,
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
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box {
            ArtworkImage(
                artworkUrl = track.artwork,
                modifier = Modifier
                    .size(140.dp)
                    .glassCard(cornerRadius = 18.dp),
                cornerRadius = 18.dp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(YumaColors.PrimaryAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = YumaColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            fontSize = 11.sp,
            color = YumaColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun YumaSearchContent(
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
        // Search Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 24.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = YumaColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(10.dp))
            TextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    vm.search(it)
                },
                placeholder = {
                    Text(
                        "Search tracks, artists, albums...",
                        color = YumaColors.TextMuted,
                        fontSize = 14.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = YumaColors.TextPrimary,
                    unfocusedTextColor = YumaColors.TextPrimary
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (searchInput.isNotEmpty()) {
                IconButton(onClick = {
                    searchInput = ""
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = YumaColors.TextSecondary
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
                CircularProgressIndicator(color = YumaColors.PrimaryAccent)
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
                        tint = YumaColors.TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Search for your favorite songs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = YumaColors.TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
            .glassCard(cornerRadius = 16.dp)
            .clickable(onClick = onPlay)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            artworkUrl = track.artwork,
            modifier = Modifier.size(50.dp),
            cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = YumaColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist + if (track.album.isNotBlank()) " • ${track.album}" else "",
                fontSize = 12.sp,
                color = YumaColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = YumaColors.TextSecondary
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(YumaColors.SurfaceElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("Play Now", color = YumaColors.TextPrimary) },
                    onClick = {
                        menuExpanded = false
                        onPlay()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = YumaColors.PrimaryAccent
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = YumaColors.TextPrimary) },
                    onClick = {
                        menuExpanded = false
                        onEnqueue()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.QueueMusic,
                            contentDescription = null,
                            tint = YumaColors.TextSecondary
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun YumaLibraryContent(
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
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = YumaColors.TextPrimary,
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
                        tint = YumaColors.TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved tracks in your library",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = YumaColors.TextSecondary
                    )
                    Text(
                        text = "Like songs in the player to save them here",
                        fontSize = 12.sp,
                        color = YumaColors.TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
private fun YumaSettingsContent(
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
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = YumaColors.TextPrimary
        )

        // Playback Engine Mode
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Playback Mode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = YumaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose audio resolution and streaming engine mode",
                    fontSize = 12.sp,
                    color = YumaColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlaybackMode.values().forEach { m ->
                        val isSel = mode == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) YumaColors.PrimaryAccent else YumaColors.GlassCardBg)
                                .clickable { vm.setMode(m) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = m.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else YumaColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Sleep Timer Setting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .clickable(onClick = onOpenSleepTimer)
                .padding(16.dp)
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
                        color = YumaColors.TextPrimary
                    )
                    Text(
                        text = if (sleepTimer != null) "Active: $sleepTimer minutes remaining" else "Off",
                        fontSize = 12.sp,
                        color = if (sleepTimer != null) YumaColors.PrimaryAccent else YumaColors.TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = YumaColors.PrimaryAccent
                )
            }
        }

        // App Information Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Streamfyree Yuma Edition",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = YumaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version 0.3.0 • Chaquopy 16.0.0 • YDS 2.1 Glassmorphism",
                    fontSize = 12.sp,
                    color = YumaColors.TextSecondary
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
                cornerRadius = 20.dp,
                backgroundColor = YumaColors.GlassBar
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
                    modifier = Modifier.size(46.dp),
                    cornerRadius = 12.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = YumaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 11.sp,
                        color = YumaColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { vm.togglePlayPause() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(YumaColors.PrimaryAccent)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { vm.next() }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = YumaColors.TextPrimary
                    )
                }
            }

            // Bottom Progress Bar Line
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = YumaColors.PrimaryAccent,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
private fun YumaBottomNav(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val items = listOf(
        Triple("Home", Icons.Default.Home, 0),
        Triple("Search", Icons.Outlined.Search, 1),
        Triple("Library", Icons.Outlined.LibraryMusic, 2),
        Triple("Settings", Icons.Outlined.Settings, 3)
    )

    Box(
        modifier = Modifier
            .padding(bottom = 12.dp, start = 24.dp, end = 24.dp)
            .fillMaxWidth()
            .height(60.dp)
            .glassCard(
                cornerRadius = 30.dp,
                backgroundColor = YumaColors.GlassBar
            )
    ) {
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
                        .clip(CircleShape)
                        .clickable { onSelectTab(tabIndex) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) YumaColors.PrimaryAccent else YumaColors.TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) YumaColors.PrimaryAccent else YumaColors.TextMuted
                    )
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
            .background(YumaColors.Background)
    ) {
        // Ambient Blurred Artwork Background
        if (!track.artwork.isNullOrBlank()) {
            AsyncImage(
                model = track.artwork,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
                    .alpha(0.35f),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            YumaColors.Background.copy(alpha = 0.6f),
                            YumaColors.Background.copy(alpha = 0.95f)
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
                        tint = YumaColors.TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM STREAMFYREE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = YumaColors.PrimaryAccent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = track.album.ifBlank { "Single" },
                        fontSize = 12.sp,
                        color = YumaColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = { vm.enqueue(track) }) {
                    Icon(
                        imageVector = Icons.Outlined.QueueMusic,
                        contentDescription = "Options",
                        tint = YumaColors.TextPrimary
                    )
                }
            }

            // Glassmorphic Artwork Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .glassCard(cornerRadius = 28.dp, borderWidth = 1.dp, borderColor = Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                ArtworkImage(
                    artworkUrl = track.artwork,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 28.dp
                )
            }

            // Title & Artist with Heart Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = YumaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        fontSize = 16.sp,
                        color = YumaColors.TextSecondary,
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
                        tint = if (isSaved) YumaColors.PrimaryAccent else YumaColors.TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Seeker Slider & Timers
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress.positionMs.toFloat().coerceAtMost(progress.durationMs.toFloat().coerceAtLeast(1f)),
                    onValueChange = { vm.seekTo(it.toLong()) },
                    valueRange = 0f..(progress.durationMs.toFloat().coerceAtLeast(1f)),
                    colors = SliderDefaults.colors(
                        thumbColor = YumaColors.PrimaryAccent,
                        activeTrackColor = YumaColors.PrimaryAccent,
                        inactiveTrackColor = YumaColors.GlassCardBg
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(progress.positionMs),
                        fontSize = 12.sp,
                        color = YumaColors.TextSecondary
                    )
                    Text(
                        text = formatTime(progress.durationMs),
                        fontSize = 12.sp,
                        color = YumaColors.TextSecondary
                    )
                }
            }

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffle) YumaColors.PrimaryAccent else YumaColors.TextMuted
                    )
                }

                IconButton(
                    onClick = { vm.previous() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = YumaColors.TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { vm.togglePlayPause() },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(YumaColors.PrimaryAccent, YumaColors.SecondaryAccent)
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
                        tint = YumaColors.TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { vm.cycleRepeat() }) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.OFF) YumaColors.PrimaryAccent else YumaColors.TextMuted
                    )
                }
            }

            // Bottom Action Row (Lyrics & Queue Sheet Toggles)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = onOpenLyrics,
                    colors = ButtonDefaults.buttonColors(containerColor = YumaColors.GlassCardBg),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, YumaColors.GlassCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = YumaColors.PrimaryAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lyrics", color = YumaColors.TextPrimary)
                }

                Button(
                    onClick = onOpenQueue,
                    colors = ButtonDefaults.buttonColors(containerColor = YumaColors.GlassCardBg),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, YumaColors.GlassCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QueueMusic,
                        contentDescription = null,
                        tint = YumaColors.PrimaryAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Queue", color = YumaColors.TextPrimary)
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
    val currentTrack by vm.current.collectAsState()

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
            .background(YumaColors.Background.copy(alpha = 0.95f))
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Synced Lyrics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = YumaColors.TextPrimary
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = YumaColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (lyricsLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = YumaColors.PrimaryAccent)
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
                            tint = YumaColors.TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No lyrics found for this track",
                            fontSize = 16.sp,
                            color = YumaColors.TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(lyrics!!.lines) { index, line ->
                        val isActive = index == activeIndex
                        Text(
                            text = line.text,
                            fontSize = if (isActive) 24.sp else 18.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) YumaColors.PrimaryAccent else YumaColors.TextSecondary.copy(alpha = 0.6f),
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
            .background(YumaColors.Background.copy(alpha = 0.95f))
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Play Queue (${queue.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = YumaColors.TextPrimary
                )

                Row {
                    TextButton(onClick = { vm.clearQueue() }) {
                        Text("Clear", color = Color.Red)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = YumaColors.TextPrimary
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
                        color = YumaColors.TextSecondary,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(queue) { track ->
                        val isPlaying = track.id == currentTrack?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(
                                    cornerRadius = 14.dp,
                                    backgroundColor = if (isPlaying) YumaColors.PrimaryAccent.copy(alpha = 0.2f) else YumaColors.GlassCardBg
                                )
                                .clickable { vm.play(track) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkImage(
                                artworkUrl = track.artwork,
                                modifier = Modifier.size(44.dp),
                                cornerRadius = 10.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPlaying) YumaColors.PrimaryAccent else YumaColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    fontSize = 11.sp,
                                    color = YumaColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { vm.removeFromQueue(track) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = YumaColors.TextMuted
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
                .glassCard(cornerRadius = 24.dp)
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Sleep Timer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = YumaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                options.forEach { mins ->
                    Button(
                        onClick = {
                            vm.setSleepTimer(mins)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YumaColors.GlassCardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("$mins minutes", color = YumaColors.TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = {
                    vm.setSleepTimer(null)
                    onDismiss()
                }) {
                    Text("Turn Off Sleep Timer", color = Color.Red)
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
