package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MusicViewModel
import com.example.ui.NavTab
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.DirectVideoDialog
import com.example.ui.components.FullScreenPlayer
import com.example.ui.components.MiniPlayer
import com.example.ui.components.ShrunkYouTubeView
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MusicApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val youtubeApiKey by viewModel.youtubeApiKey.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "StreamMusic",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Live Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // API Key Config Button
                    IconButton(
                        onClick = { viewModel.setApiKeyDialogOpen(true) },
                        modifier = Modifier.testTag("top_bar_key_btn")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "YouTube API Key",
                                tint = if (youtubeApiKey.isNotBlank()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (youtubeApiKey.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }

                    // Theme toggle button
                    IconButton(
                        onClick = { viewModel.setDarkMode(!isDarkMode) },
                        modifier = Modifier.testTag("top_bar_theme_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                // Persistent Mini Player above the navigation bar
                AnimatedVisibility(
                    visible = playerState.currentSong != null,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    MiniPlayer(
                        playerState = playerState,
                        onPlayerClick = { viewModel.setFullScreenPlayerOpen(true) },
                        onTogglePlayPause = { viewModel.audioPlayer.togglePlayPause() },
                        onSkipNext = { viewModel.playNext() }
                    )
                }

                // Standard Material 3 Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = uiState.activeTab == NavTab.SEARCH,
                        onClick = { viewModel.setNavTab(NavTab.SEARCH) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.activeTab == NavTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        label = { Text("Live Search") },
                        modifier = Modifier.testTag("nav_search")
                    )

                    NavigationBarItem(
                        selected = uiState.activeTab == NavTab.FAVORITES,
                        onClick = { viewModel.setNavTab(NavTab.FAVORITES) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.activeTab == NavTab.FAVORITES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorites"
                            )
                        },
                        label = { Text("Favorites") },
                        modifier = Modifier.testTag("nav_favorites")
                    )

                    NavigationBarItem(
                        selected = uiState.activeTab == NavTab.HISTORY,
                        onClick = { viewModel.setNavTab(NavTab.HISTORY) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.activeTab == NavTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                contentDescription = "History"
                            )
                        },
                        label = { Text("History") },
                        modifier = Modifier.testTag("nav_history")
                    )

                    NavigationBarItem(
                        selected = uiState.activeTab == NavTab.SETTINGS,
                        onClick = { viewModel.setNavTab(NavTab.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.activeTab == NavTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Primary Screens
            when (uiState.activeTab) {
                NavTab.SEARCH -> SearchScreen(
                    uiState = uiState,
                    playerState = playerState,
                    youtubeApiKey = youtubeApiKey,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClearQuery = { viewModel.clearSearch() },
                    onTriggerSearch = { viewModel.triggerSearch(it) },
                    onSongClick = { viewModel.playSong(it) },
                    onFavoriteClick = { viewModel.toggleFavorite(it) },
                    onOpenApiKeyDialog = { viewModel.setApiKeyDialogOpen(true) }
                )

                NavTab.FAVORITES -> FavoritesScreen(
                    favorites = favorites,
                    playerState = playerState,
                    onSongClick = { viewModel.playSong(it) },
                    onFavoriteClick = { viewModel.toggleFavorite(it) },
                    onPlayAll = {
                        if (favorites.isNotEmpty()) {
                            viewModel.playSong(favorites.first())
                        }
                    }
                )

                NavTab.HISTORY -> HistoryScreen(
                    history = history,
                    playerState = playerState,
                    onSongClick = { viewModel.playSong(it) },
                    onFavoriteClick = { viewModel.toggleFavorite(it) }
                )

                NavTab.SETTINGS -> SettingsScreen(
                    isDarkMode = isDarkMode,
                    youtubeApiKey = youtubeApiKey,
                    onToggleDarkMode = { viewModel.setDarkMode(it) },
                    onOpenApiKeyDialog = { viewModel.setApiKeyDialogOpen(true) },
                    onOpenDirectVideoDialog = { viewModel.setDirectVideoDialogOpen(true) }
                )
            }

            // Completely inert background YouTube iFrame Player engine (1dp, non-touchable)
            ShrunkYouTubeView(audioPlayer = viewModel.audioPlayer)

            // Full-Screen Now-Playing Player
            FullScreenPlayer(
                isOpen = uiState.isFullScreenPlayerOpen,
                playerState = playerState,
                isShuffleEnabled = uiState.isShuffleEnabled,
                isRepeatEnabled = uiState.isRepeatEnabled,
                onClose = { viewModel.setFullScreenPlayerOpen(false) },
                onTogglePlayPause = { viewModel.audioPlayer.togglePlayPause() },
                onSeek = { seconds -> viewModel.audioPlayer.seekTo(seconds) },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onToggleFavorite = {
                    playerState.currentSong?.let { viewModel.toggleFavorite(it) }
                },
                onOpenApiKeyDialog = { viewModel.setApiKeyDialogOpen(true) }
            )

            // YouTube API Key Config Dialog
            ApiKeyDialog(
                isOpen = uiState.isApiKeyDialogOpen,
                currentKey = youtubeApiKey,
                onDismiss = { viewModel.setApiKeyDialogOpen(false) },
                onSaveKey = { newKey -> viewModel.saveYouTubeApiKey(newKey) }
            )

            // Direct YouTube Video Stream Dialog
            DirectVideoDialog(
                isOpen = uiState.isDirectVideoDialogOpen,
                onDismiss = { viewModel.setDirectVideoDialogOpen(false) },
                onPlayDirect = { videoIdOrUrl, title ->
                    viewModel.playDirectYouTubeVideo(videoIdOrUrl, title)
                }
            )
        }
    }
}
