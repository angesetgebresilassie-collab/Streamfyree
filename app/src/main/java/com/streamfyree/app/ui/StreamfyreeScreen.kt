package com.streamfyree.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.streamfyree.app.LyricLine
import com.streamfyree.app.MusicTrack
import com.streamfyree.app.MusicViewModel
import java.util.Locale

// Yuma Design System (YDS 2.1) Color Palette
private val SpotGreen = Color(0xFF1DB954)
private val SpotGreenBright = Color(0xFF1ED760)
private val SpotDarkBg = Color(0xFF0F0F12)
private val SpotSurface = Color(0xFF18181F)
private val SpotSurfaceVariant = Color(0xFF24242E)
private val SpotCardBg = Color(0xFF1E1E28)
private val SpotTextPrimary = Color(0xFFFFFFFF)
private val SpotTextSecondary = Color(0xFFA7A7B3)
private val SpotAccentGlow = Color(0x331DB954)

enum class ScreenTab {
    SEARCH, PLAYER, LYRICS, QUEUE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamfyreeScreen(viewModel: MusicViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.playbackPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val currentTrackIndex by viewModel.currentTrackIndex.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()

    val lyrics by viewModel.lyrics.collectAsState()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    val isDebugOverlayVisible by viewModel.isDebugOverlayVisible.collectAsState()
    val logs by viewModel.logs.collectAsState()

    val accentColor = rememberArtworkDominantColor(
        artworkUrl = currentTrack?.artworkUrl,
        defaultColor = SpotGreen
    )

    var activeTab by remember { mutableStateOf(ScreenTab.SEARCH) }
    var isExpandedPlayerVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SpotDarkBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // YDS 2.1 Ambient Backdrop Glow based on active track artwork
            currentTrack?.artworkUrl?.let { artUrl ->
                AsyncImage(
                    model = artUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(80.dp)
                        .background(Color.Black.copy(alpha = 0.65f))
                )
            }

            // Glassmorphism Dark tint overlay over ambient blur
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SpotDarkBg.copy(alpha = 0.85f),
                                SpotDarkBg.copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // YDS Top Header Bar
                SpotTopHeader(
                    activeTab = activeTab,
                    accentColor = accentColor,
                    onTabSelected = { activeTab = it },
                    onToggleDebug = { viewModel.onToggleDebugOverlay() }
                )

                // Main Content Body based on selected Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        ScreenTab.SEARCH -> SpotSearchTab(
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            onSearch = { viewModel.onSearch() },
                            isSearching = isSearching,
                            searchResults = searchResults,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            accentColor = accentColor,
                            onTrackSelect = { track ->
                                viewModel.onTrackSelect(track)
                            }
                        )
                        ScreenTab.PLAYER -> SpotFullPlayerContent(
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            isBuffering = isBuffering,
                            playbackSpeed = playbackSpeed,
                            repeatMode = repeatMode,
                            shuffleMode = shuffleMode,
                            accentColor = accentColor,
                            onPlayPauseToggle = { viewModel.onPlayPauseToggle() },
                            onSeekTo = { viewModel.onSeekTo(it) },
                            onSkipToNext = { viewModel.onSkipToNext() },
                            onSkipToPrevious = { viewModel.onSkipToPrevious() },
                            onToggleRepeat = { viewModel.onToggleRepeat() },
                            onToggleShuffle = { viewModel.onToggleShuffle() },
                            onSetPlaybackSpeed = { viewModel.onSetPlaybackSpeed(it) },
                            onOpenLyrics = { activeTab = ScreenTab.LYRICS },
                            onOpenQueue = { activeTab = ScreenTab.QUEUE }
                        )
                        ScreenTab.LYRICS -> SpotLyricsTab(
                            currentTrack = currentTrack,
                            lyrics = lyrics,
                            isLyricsLoading = isLyricsLoading,
                            currentLyricIndex = currentLyricIndex,
                            positionMs = positionMs,
                            accentColor = accentColor,
                            onSeekTo = { viewModel.onSeekTo(it) }
                        )
                        ScreenTab.QUEUE -> SpotQueueTab(
                            queue = queue,
                            currentIndex = currentTrackIndex,
                            accentColor = accentColor,
                            onSelectTrack = { index ->
                                viewModel.onSelectQueueTrack(index)
                            }
                        )
                    }
                }

                // Persistent YDS 2.1 Floating Mini Player
                if (currentTrack != null && activeTab != ScreenTab.PLAYER && !isExpandedPlayerVisible) {
                    SpotMiniPlayer(
                        track = currentTrack!!,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        isBuffering = isBuffering,
                        accentColor = accentColor,
                        onPlayPauseToggle = { viewModel.onPlayPauseToggle() },
                        onSkipNext = { viewModel.onSkipToNext() },
                        onClick = { activeTab = ScreenTab.PLAYER }
                    )
                }
            }

            // Error Toast Bar
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White
                ) {
                    Text(text = error, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Debug Log Overlay
            if (isDebugOverlayVisible) {
                DebugLogOverlay(
                    logs = logs,
                    onDismiss = { viewModel.onToggleDebugOverlay() }
                )
            }
        }
    }
}

// SpotPlayer / Yuma Header Bar
@Composable
private fun SpotTopHeader(
    activeTab: ScreenTab,
    accentColor: Color,
    onTabSelected: (ScreenTab) -> Unit,
    onToggleDebug: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = "Yuma Logo",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "YumaPlayer",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = SpotTextPrimary
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpotHeaderTabButton(
                icon = Icons.Rounded.Search,
                label = "Search",
                isSelected = activeTab == ScreenTab.SEARCH,
                accentColor = accentColor,
                onClick = { onTabSelected(ScreenTab.SEARCH) }
            )
            SpotHeaderTabButton(
                icon = Icons.Rounded.PlayCircle,
                label = "Player",
                isSelected = activeTab == ScreenTab.PLAYER,
                accentColor = accentColor,
                onClick = { onTabSelected(ScreenTab.PLAYER) }
            )
            SpotHeaderTabButton(
                icon = Icons.Rounded.Subtitles,
                label = "Lyrics",
                isSelected = activeTab == ScreenTab.LYRICS,
                accentColor = accentColor,
                onClick = { onTabSelected(ScreenTab.LYRICS) }
            )
            SpotHeaderTabButton(
                icon = Icons.Rounded.QueueMusic,
                label = "Queue",
                isSelected = activeTab == ScreenTab.QUEUE,
                accentColor = accentColor,
                onClick = { onTabSelected(ScreenTab.QUEUE) }
            )
            IconButton(
                onClick = onToggleDebug,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = "Debug Logs",
                    tint = SpotTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SpotHeaderTabButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bg = if (isSelected) accentColor else Color.Transparent
    val contentColor = if (isSelected) Color.Black else SpotTextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            if (isSelected) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
            }
        }
    }
}

// Search Tab Component with Quick Picks Shelf
@Composable
private fun SpotSearchTab(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean,
    searchResults: List<MusicTrack>,
    currentTrack: MusicTrack?,
    isPlaying: Boolean,
    accentColor: Color,
    onTrackSelect: (MusicTrack) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val quickPicks = remember {
        listOf("Top Hits", "Lofi Chill", "Anime Openings", "Afrobeats", "Hip-Hop Classics", "Acoustic Pop")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .shadow(8.dp, RoundedCornerShape(28.dp)),
            placeholder = {
                Text(
                    text = "Search songs, artists, or albums",
                    color = SpotTextSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = accentColor
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = SpotTextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SpotSurface,
                unfocusedContainerColor = SpotSurface,
                disabledContainerColor = SpotSurface,
                focusedIndicatorColor = accentColor,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = SpotTextPrimary,
                unfocusedTextColor = SpotTextPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                onSearch()
            })
        )

        // Quick Picks Chips Shelf
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(quickPicks) { pick ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SpotSurfaceVariant,
                    modifier = Modifier.clickable {
                        onQueryChange(pick)
                        onSearch()
                    }
                ) {
                    Text(
                        text = pick,
                        style = MaterialTheme.typography.labelMedium,
                        color = SpotTextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = SpotSurfaceVariant,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "Search songs, artists, or albums above" else "No tracks found",
                        color = SpotTextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(searchResults) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    SpotTrackItemCard(
                        track = track,
                        isCurrentTrack = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        accentColor = accentColor,
                        onClick = { onTrackSelect(track) }
                    )
                }
            }
        }
    }
}

// Spot Track Card Item
@Composable
private fun SpotTrackItemCard(
    track: MusicTrack,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTrack) SpotSurfaceVariant else SpotSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SpotCardBg)
            ) {
                if (track.artworkUrl != null) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = SpotTextSecondary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }

                if (isCurrentTrack) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isCurrentTrack) accentColor else SpotTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpotTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatMillis(track.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = SpotTextSecondary
            )
        }
    }
}

// YDS Full Expanded Player Content
@Composable
private fun SpotFullPlayerContent(
    currentTrack: MusicTrack?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isBuffering: Boolean,
    playbackSpeed: Float,
    repeatMode: Int,
    shuffleMode: Boolean,
    accentColor: Color,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipToNext: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit
) {
    if (currentTrack == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No track currently playing",
                color = SpotTextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Large Artwork Card with Glow
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .shadow(24.dp, RoundedCornerShape(20.dp), spotColor = accentColor)
                .clip(RoundedCornerShape(20.dp))
                .background(SpotCardBg)
        ) {
            if (currentTrack.artworkUrl != null) {
                AsyncImage(
                    model = currentTrack.artworkUrl,
                    contentDescription = currentTrack.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = SpotTextSecondary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(80.dp)
                )
            }
        }

        // Track Details Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTrack.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = SpotTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTrack.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = SpotTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { /* Favorite toggle */ }) {
                Icon(
                    imageVector = Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = accentColor
                )
            }
        }

        // Seeking Slider & Durations
        Column(modifier = Modifier.fillMaxWidth()) {
            val sliderPosition = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
            var userSeekingPos by remember { mutableStateOf<Float?>(null) }

            Slider(
                value = userSeekingPos ?: sliderPosition.coerceIn(0f, 1f),
                onValueChange = { userSeekingPos = it },
                onValueChangeFinished = {
                    userSeekingPos?.let {
                        onSeekTo((it * durationMs).toLong())
                    }
                    userSeekingPos = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = SpotSurfaceVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentPosMs = userSeekingPos?.let { (it * durationMs).toLong() } ?: positionMs
                Text(
                    text = formatMillis(currentPosMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotTextSecondary
                )
                Text(
                    text = formatMillis(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotTextSecondary
                )
            }
        }

        // Playback Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleMode) accentColor else SpotTextSecondary
                )
            }

            IconButton(onClick = onSkipToPrevious) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = SpotTextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Big Play/Pause FAB
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable { onPlayPauseToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.Black,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            IconButton(onClick = onSkipToNext) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = SpotTextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = onToggleRepeat) {
                Icon(
                    imageVector = if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode > 0) accentColor else SpotTextSecondary
                )
            }
        }

        // Quick Bottom Action Row (Speed, Lyrics, Queue)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    val nextSpeed = when (playbackSpeed) {
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        else -> 1.0f
                    }
                    onSetPlaybackSpeed(nextSpeed)
                }
            ) {
                Text(
                    text = "${playbackSpeed}x",
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onOpenLyrics) {
                Icon(
                    imageVector = Icons.Rounded.Subtitles,
                    contentDescription = "Lyrics",
                    tint = SpotTextSecondary
                )
            }

            IconButton(onClick = onOpenQueue) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = "Queue",
                    tint = SpotTextSecondary
                )
            }
        }
    }
}

// Persistent Mini Player Bar
@Composable
private fun SpotMiniPlayer(
    track: MusicTrack,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isBuffering: Boolean,
    accentColor: Color,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SpotSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SpotCardBg)
                ) {
                    if (track.artworkUrl != null) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = SpotTextSecondary,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = SpotTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpotTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPlayPauseToggle) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = accentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = SpotTextPrimary
                        )
                    }
                }

                IconButton(onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Skip Next",
                        tint = SpotTextPrimary
                    )
                }
            }

            // Bottom Progress Bar
            val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = accentColor,
                trackColor = SpotSurfaceVariant
            )
        }
    }
}

// Synchronized Lyrics View
@Composable
private fun SpotLyricsTab(
    currentTrack: MusicTrack?,
    lyrics: List<LyricLine>,
    isLyricsLoading: Boolean,
    currentLyricIndex: Int,
    positionMs: Long,
    accentColor: Color,
    onSeekTo: (Long) -> Unit
) {
    if (currentTrack == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Play a track to view lyrics", color = SpotTextSecondary)
        }
        return
    }

    if (isLyricsLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accentColor)
        }
        return
    }

    if (lyrics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No lyrics available for this track", color = SpotTextSecondary)
        }
        return
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentLyricIndex) {
        if (currentLyricIndex in lyrics.indices) {
            listState.animateScrollToItem(
                index = (currentLyricIndex - 2).coerceAtLeast(0)
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isCurrent = index == currentLyricIndex
            val color = if (isCurrent) accentColor else SpotTextSecondary.copy(alpha = 0.5f)
            val fontSize = if (isCurrent) 24.sp else 18.sp

            Text(
                text = line.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = fontSize,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                ),
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeekTo(line.timeMs) }
            )
        }
    }
}

// Up Next Queue Sheet
@Composable
private fun SpotQueueTab(
    queue: List<MusicTrack>,
    currentIndex: Int,
    accentColor: Color,
    onSelectTrack: (Int) -> Unit
) {
    if (queue.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Queue is empty", color = SpotTextSecondary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Playing Queue",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = SpotTextPrimary,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            itemsIndexed(queue) { index, track ->
                val isCurrent = index == currentIndex
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectTrack(index) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) SpotSurfaceVariant else SpotSurface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCurrent) accentColor else SpotTextSecondary,
                            modifier = Modifier.width(28.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isCurrent) accentColor else SpotTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.labelSmall,
                                color = SpotTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = "Playing",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Utility Duration Formatter
private fun formatMillis(millis: Long): String {
    if (millis <= 0) return "0:00"
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
