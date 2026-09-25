package com.streamfyree.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.streamfyree.app.LyricLine
import com.streamfyree.app.MusicTrack
import com.streamfyree.app.MusicViewModel
import java.util.Locale
import kotlin.math.max

private val Bg = Color(0xFF08090D)
private val Glass = Color(0x14FFFFFF)
private val GlassStrong = Color(0x20FFFFFF)
private val Border = Color(0x22FFFFFF)
private val TextMain = Color(0xFFF7F7F8)
private val TextMuted = Color(0xA6F7F7F8)
private val DefaultAccent = Color(0xFFD8BEA6)
private enum class Screen { HOME, PLAYER, LYRICS, QUEUE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YumaCopyScreen(vm: MusicViewModel) {
    val query by vm.searchQuery.collectAsState()
    val results by vm.searchResults.collectAsState()
    val searching by vm.isSearching.collectAsState()
    val current by vm.currentTrack.collectAsState()
    val playing by vm.isPlaying.collectAsState()
    val position by vm.playbackPositionMs.collectAsState()
    val duration by vm.durationMs.collectAsState()
    val queue by vm.queue.collectAsState()
    val currentIndex by vm.currentTrackIndex.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val lyricIndex by vm.currentLyricIndex.collectAsState()
    val lyricsLoading by vm.isLyricsLoading.collectAsState()
    val buffering by vm.isBuffering.collectAsState()
    val speed by vm.playbackSpeed.collectAsState()
    val repeat by vm.repeatMode.collectAsState()
    val shuffle by vm.shuffleMode.collectAsState()

    var screen by remember { mutableStateOf(Screen.HOME) }
    val accent = rememberArtworkDominantColor(current?.artworkUrl, DefaultAccent)
    val animatedAccent by animateColorAsState(accent, tween(450), label = "accent")

    Box(Modifier.fillMaxSize().background(Bg)) {
        current?.artworkUrl?.let {
            AsyncImage(it, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(70.dp))
        }
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(.45f), Bg.copy(.97f)))
        ))
        AnimatedContent(screen, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "screen") { s ->
            when (s) {
                Screen.HOME -> Home(query, results, searching, current, playing, animatedAccent,
                    vm::onSearchQueryChange, vm::onSearch,
                    { vm.onTrackSelect(it); screen = Screen.PLAYER }, { screen = Screen.PLAYER },
                    vm::onPlayPauseToggle)
                Screen.PLAYER -> Player(current, playing, buffering, position, duration, animatedAccent,
                    speed, repeat, shuffle, { screen = Screen.HOME }, vm::onPlayPauseToggle,
                    vm::onSeekTo, vm::onSkipToNext, vm::onSkipToPrevious, vm::onToggleRepeat,
                    vm::onToggleShuffle, vm::onSetPlaybackSpeed,
                    { screen = Screen.LYRICS }, { screen = Screen.QUEUE })
                Screen.LYRICS -> Lyrics(current, lyrics, lyricIndex, lyricsLoading, animatedAccent,
                    { screen = Screen.PLAYER }, vm::onSeekTo)
                Screen.QUEUE -> Queue(queue, currentIndex, animatedAccent,
                    { screen = Screen.PLAYER }, vm::onSelectQueueTrack)
            }
        }
    }
}

@Composable
private fun Home(
    query: String, results: List<MusicTrack>, searching: Boolean, current: MusicTrack?,
    playing: Boolean, accent: Color, onQuery: (String) -> Unit, onSearch: () -> Unit,
    onSelect: (MusicTrack) -> Unit, openPlayer: () -> Unit, playPause: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val chips = listOf("Afrobeats", "Hip-Hop", "Chill", "Pop", "Anime", "Classics")
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(20.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Streamfyree", color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Good music, beautifully played.", color = TextMuted, fontSize = 13.sp)
            }
            Box(Modifier.size(42.dp).clip(CircleShape).background(GlassStrong)
                .border(1.dp, Border, CircleShape), Alignment.Center) {
                Icon(Icons.Rounded.GraphicEq, null, tint = accent)
            }
        }
        OutlinedTextField(
            value = query, onValueChange = onQuery, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search songs, artists, albums", color = TextMuted) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = accent) },
            trailingIcon = { if (query.isNotEmpty()) IconButton({ onQuery("") }) {
                Icon(Icons.Rounded.Close, null, tint = TextMuted)
            }},
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassStrong, unfocusedContainerColor = Glass,
                focusedBorderColor = accent, unfocusedBorderColor = Border,
                focusedTextColor = TextMain, unfocusedTextColor = TextMain),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onSearch() })
        )
        LazyRow(contentPadding = PaddingValues(16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chips) { chip ->
                Surface(color = Glass, shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.border(1.dp, Border, RoundedCornerShape(18.dp)).clickable {
                        onQuery(chip); onSearch()
                    }) {
                    Text(chip, color = TextMain, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
        }
        Text(if (query.isBlank()) "Search & discover" else "Results",
            color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        if (searching) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else if (results.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.MusicNote, null, tint = accent, modifier = Modifier.size(54.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Find something you want to hear", color = TextMuted)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, if (current != null) 100.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.id }) { track -> TrackRow(track, accent) { onSelect(track) } }
            }
        }
        current?.let { MiniPlayer(it, playing, accent, openPlayer, playPause) }
    }
}

@Composable
private fun TrackRow(track: MusicTrack, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Glass)
        .border(1.dp, Border, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(track.artworkUrl, null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(13.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = TextMain, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist ?: "Unknown artist", color = TextMuted, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = accent)
    }
}

@Composable
private fun MiniPlayer(track: MusicTrack, playing: Boolean, accent: Color, open: () -> Unit, toggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(12.dp, 8.dp).clip(RoundedCornerShape(22.dp))
        .background(GlassStrong).border(1.dp, Border, RoundedCornerShape(22.dp))
        .clickable(onClick = open).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(track.artworkUrl, null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = TextMain, fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(track.artist ?: "", color = TextMuted, fontSize = 12.sp, maxLines = 1)
        }
        IconButton(toggle) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = accent) }
    }
}

@Composable
private fun Player(
    track: MusicTrack?, playing: Boolean, buffering: Boolean, position: Long, duration: Long,
    accent: Color, speed: Float, repeat: Int, shuffle: Boolean, back: () -> Unit,
    toggle: () -> Unit, seek: (Long) -> Unit, next: () -> Unit, previous: () -> Unit,
    toggleRepeat: () -> Unit, toggleShuffle: () -> Unit, setSpeed: (Float) -> Unit,
    lyrics: () -> Unit, queue: () -> Unit
) {
    if (track == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Nothing playing", color = TextMuted) }
        return
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(back) { Icon(Icons.Rounded.KeyboardArrowDown, "Back", tint = TextMain) }
            Text("NOW PLAYING", color = TextMuted, fontSize = 11.sp, letterSpacing = 2.sp,
                modifier = Modifier.weight(1f))
            IconButton(queue) { Icon(Icons.Rounded.QueueMusic, "Queue", tint = TextMain) }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
            AsyncImage(track.artworkUrl, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)))
        }
        Column(Modifier.padding(top = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist ?: "Unknown artist", color = TextMuted, fontSize = 15.sp)
                }
                IconButton(lyrics) { Icon(Icons.Rounded.Subtitles, "Lyrics", tint = accent) }
            }
            Slider(value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                onValueChange = { seek((it * max(1L, duration)).toLong()) },
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent,
                    inactiveTrackColor = GlassStrong))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(time(position), color = TextMuted, fontSize = 11.sp)
                Text(if (buffering) "Buffering…" else time(duration), color = TextMuted, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(toggleShuffle) { Icon(Icons.Rounded.Shuffle, null, tint = if (shuffle) accent else TextMuted) }
                IconButton(previous) { Icon(Icons.Rounded.SkipPrevious, null, tint = TextMain, modifier = Modifier.size(30.dp)) }
                FilledIconButton(toggle, Modifier.size(68.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent)) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null,
                        tint = Color.Black, modifier = Modifier.size(34.dp))
                }
                IconButton(next) { Icon(Icons.Rounded.SkipNext, null, tint = TextMain, modifier = Modifier.size(30.dp)) }
                IconButton(toggleRepeat) { Icon(Icons.Rounded.Repeat, null,
                    tint = if (repeat != 0) accent else TextMuted) }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), Arrangement.SpaceBetween) {
                Text("Speed " + speed + "x", color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.clickable { setSpeed(if (speed == 1f) 1.25f else 1f) }.padding(8.dp))
                Text("Lyrics", color = accent, fontSize = 12.sp, modifier = Modifier.clickable(onClick = lyrics).padding(8.dp))
                Text("Queue", color = accent, fontSize = 12.sp, modifier = Modifier.clickable(onClick = queue).padding(8.dp))
            }
        }
    }
}

@Composable
private fun Lyrics(track: MusicTrack?, lines: List<LyricLine>, index: Int, loading: Boolean,
    accent: Color, back: () -> Unit, seek: (Long) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(back) { Icon(Icons.Rounded.ArrowBack, null, tint = TextMain) }
            Column(Modifier.weight(1f)) {
                Text("Lyrics", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(track?.title ?: "", color = TextMuted, fontSize = 12.sp)
            }
        }
        if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = accent)
        } else if (lines.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Lyrics unavailable", color = TextMuted)
        } else LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(lines.size) { i ->
                val line = lines[i]
                Text(line.text, color = if (i == index) TextMain else TextMuted.copy(alpha = .62f),
                    fontSize = if (i == index) 24.sp else 18.sp,
                    fontWeight = if (i == index) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.clickable { seek(line.startMs) })
            }
        }
    }
}

@Composable
private fun Queue(queue: List<MusicTrack>, current: Int, accent: Color, back: () -> Unit, select: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(back) { Icon(Icons.Rounded.ArrowBack, null, tint = TextMain) }
            Text("Queue", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(queue.size) { i ->
                val track = queue[i]
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(if (i == current) accent.copy(.14f) else Glass)
                    .clickable { select(i) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(track.artworkUrl, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist ?: "", color = TextMuted, fontSize = 12.sp)
                    }
                    if (i == current) Icon(Icons.Rounded.Equalizer, null, tint = accent)
                }
            }
        }
    }
}

private fun time(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.getDefault(), "%d:%02d", total / 60, total % 60)
}
