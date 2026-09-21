package com.streamfyree.app.ui

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.streamfyree.app.*

private val BG=Color(0xFF0A0705)
private val CARD=Color(0xFF1C1611)
private val CARD2=Color(0xFF241B14)
private val TEXT=Color(0xFFF3EAE1)
private val MUTED=Color(0xFFAD9F94)
private val ACCENT=Color(0xFFE0A662)

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
    var playerOpen by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }

    LaunchedEffect(playing) {
        while (playing) { vm.refreshProgress(); kotlinx.coroutines.delay(500) }
        vm.refreshProgress()
    }

    Scaffold(
        containerColor=BG,
        bottomBar={
            Column {
                current?.let { t ->
                    MiniPlayer(t,playing,{vm.togglePlayPause()},{playerOpen=true})
                }
                NavigationBar(containerColor=Color(0xFF120D09)) {
                    NavigationBarItem(selected=tab==0,onClick={tab=0},icon={Icon(Icons.Default.Home,null)},label={Text("Home")})
                    NavigationBarItem(selected=tab==1,onClick={tab=1},icon={Icon(Icons.Default.Search,null)},label={Text("Search")})
                    NavigationBarItem(selected=tab==2,onClick={tab=2},icon={Icon(Icons.Default.Favorite,null)},label={Text("Library")})
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(Brush.verticalGradient(listOf(Color(0xFF17100B),BG)))) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding=PaddingValues(16.dp,16.dp,16.dp,24.dp),
                verticalArrangement=Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Streamfyree",color=TEXT,fontSize=30.sp,fontWeight=FontWeight.Bold)
                            Text(if(tab==0) "Fresh music for you" else if(tab==1) "Find something to play" else "Your collection",color=MUTED,fontSize=13.sp)
                        }
                        IconButton(onClick={queueOpen=true}) { Icon(Icons.Default.QueueMusic,null,tint=TEXT) }
                    }
                }
                if(tab==0) {
                    item {
                        SearchBox(query,{query=it},{if(query.isNotBlank()){tab=1;vm.search(query)}})
                    }
                    item { ModeRow(mode,vm::setMode) }
                    current?.let { item { ContinueCard(it,playing,{playerOpen=true},{vm.togglePlayPause()}) } }
                    if(discover.isNotEmpty()) {
                        item { Section("Fresh Mix","Real songs from iTunes metadata") }
                        items(discover.take(12),key={"d-"+it.id}) { t -> TrackRow(t,vm.isSaved(t),{playerOpen=true;vm.play(t)},{vm.enqueue(t)},{if(vm.isSaved(t))vm.unsaveTrack(t)else vm.saveTrack(t)}) }
                    }
                    if(history.isNotEmpty()) {
                        item { Section("Recently Played","Pick up where you left off") }
                        item { LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) { items(history.take(10),key={"h-"+it.id}) { t -> SmallTile(t){playerOpen=true;vm.play(t)} } } }
                    }
                } else if(tab==1) {
                    item { SearchBox(query,{query=it},{vm.search(query)}) }
                    item { ModeRow(mode,vm::setMode) }
                    if(state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(),color=ACCENT) }
                    state.error?.let { item { ErrorCard(it) } }
                    if(state.tracks.isEmpty() && !state.loading) item { EmptyCard("Search for a song","Try an artist, track, or album name.") }
                    items(state.tracks,key={"s-"+it.id}) { t -> TrackRow(t,vm.isSaved(t),{playerOpen=true;vm.play(t)},{vm.enqueue(t)},{if(vm.isSaved(t))vm.unsaveTrack(t)else vm.saveTrack(t)}) }
                } else {
                    item { Section("Saved Songs",library.saved.size.toString()+" saved tracks") }
                    if(library.saved.isEmpty()) item { EmptyCard("Nothing saved yet","Tap the heart on any track to add it here.") }
                    items(library.saved,key={"l-"+it.id}) { t -> TrackRow(t,true,{playerOpen=true;vm.play(t)},{vm.enqueue(t)},{vm.unsaveTrack(t)}) }
                }
            }
        }
    }

    if(playerOpen && current!=null) {
        ModalBottomSheet(onDismissRequest={playerOpen=false},containerColor=BG) {
            val t=current!!
            Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                if(mode==PlaybackMode.ONLINE || mode==PlaybackMode.AUTO) {
                    val url=t.youtubeUrl.orEmpty()
                    if(url.isNotBlank()) YouTubeView(url,Modifier.fillMaxWidth().aspectRatio(1.65f).clip(RoundedCornerShape(24.dp)))
                } else Artwork(t,Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)))
                Spacer(Modifier.height(16.dp))
                Text(t.title,color=TEXT,fontSize=23.sp,fontWeight=FontWeight.Bold,maxLines=2)
                Text(t.artist,color=MUTED,fontSize=14.sp)
                if(progress.durationMs>0) {
                    Slider(value=progress.positionMs.coerceIn(0,progress.durationMs).toFloat(),onValueChange={vm.seekTo(it.toLong())},valueRange=0f..progress.durationMs.toFloat(),colors=SliderDefaults.colors(thumbColor=ACCENT,activeTrackColor=ACCENT))
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={vm.previous()}){Icon(Icons.Default.SkipPrevious,null,tint=TEXT)}
                    FilledIconButton(onClick={vm.togglePlayPause},colors=IconButtonDefaults.filledIconButtonColors(containerColor=ACCENT,contentColor=BG),modifier=Modifier.size(64.dp)){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null)}
                    IconButton(onClick={vm.next}){Icon(Icons.Default.SkipNext,null,tint=TEXT)}
                }
                TextButton(onClick={if(vm.isSaved(t))vm.unsaveTrack(t)else vm.saveTrack(t)}){Text(if(vm.isSaved(t))"Remove from favorites" else "Add to favorites",color=ACCENT)}
            }
        }
    }
    if(queueOpen) {
        ModalBottomSheet(onDismissRequest={queueOpen=false},containerColor=BG) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Queue",color=TEXT,fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));TextButton(onClick=vm::clearQueue){Text("Clear",color=ACCENT)}}
                queue.take(20).forEach { t -> TrackRow(t,vm.isSaved(t),{queueOpen=false;playerOpen=true;vm.play(t)},{vm.removeFromQueue(t)},{}) }
            }
        }
    }
}

@Composable private fun SearchBox(value:String,onChange:(String)->Unit,onSearch:()->Unit){
    OutlinedTextField(value=value,onValueChange=onChange,modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search songs, artists, albums…",color=MUTED)},leadingIcon={Icon(Icons.Default.Search,null,tint=MUTED)},trailingIcon={IconButton(onClick=onSearch){Icon(Icons.Default.ArrowForward,null,tint=ACCENT)}},shape=RoundedCornerShape(22.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=ACCENT,unfocusedBorderColor=Color.White.copy(alpha=.12f),focusedTextColor=TEXT,unfocusedTextColor=TEXT,cursorColor=ACCENT))
}
@Composable private fun ModeRow(mode:PlaybackMode,onMode:(PlaybackMode)->Unit){
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
        listOf(PlaybackMode.AUTO to "Auto",PlaybackMode.NATIVE to "Native",PlaybackMode.ONLINE to "Online").forEach { (m,label) ->
            FilterChip(selected=mode==m,onClick={onMode(m)},label={Text(label)},leadingIcon={Icon(if(m==PlaybackMode.ONLINE)Icons.Default.Language else if(m==PlaybackMode.NATIVE)Icons.Default.Headphones else Icons.Default.Tune,null)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ACCENT,selectedLabelColor=BG))
        }
    }
}
@Composable private fun Section(title:String,sub:String){Column{Text(title,color=TEXT,fontSize=21.sp,fontWeight=FontWeight.Bold);Text(sub,color=MUTED,fontSize=13.sp)}}
@Composable private fun TrackRow(t:Track,saved:Boolean,onPlay:()->Unit,onQueue:()->Unit,onSave:()->Unit){
    Surface(Modifier.fillMaxWidth().clickable(onClick=onPlay),shape=RoundedCornerShape(18.dp),color=CARD){
        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
            Artwork(t,Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)){Text(t.title,color=TEXT,fontWeight=FontWeight.SemiBold,maxLines=1);Text(t.artist,color=MUTED,fontSize=12.sp,maxLines=1);if(t.lyricVideo)Text("LYRICS",color=ACCENT,fontSize=9.sp)}
            IconButton(onClick=onSave){Icon(if(saved)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(saved)ACCENT else MUTED)}
            IconButton(onClick=onQueue){Icon(Icons.Default.AddCircleOutline,null,tint=MUTED)}
        }
    }
}
@Composable private fun SmallTile(t:Track,onClick:()->Unit){Column(Modifier.width(110.dp).clickable(onClick=onClick)){Artwork(t,Modifier.size(110.dp).clip(RoundedCornerShape(16.dp)));Spacer(Modifier.height(6.dp));Text(t.title,color=TEXT,fontSize=12.sp,maxLines=1);Text(t.artist,color=MUTED,fontSize=11.sp,maxLines=1)}}
@Composable private fun Artwork(t:Track,modifier:Modifier){AsyncImage(model=t.artwork,contentDescription=t.title,modifier=modifier,contentScale=ContentScale.Crop)}
@Composable private fun ContinueCard(t:Track,playing:Boolean,onOpen:()->Unit,onToggle:()->Unit){
    Surface(Modifier.fillMaxWidth().clickable(onClick=onOpen),shape=RoundedCornerShape(24.dp),color=CARD2){
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Artwork(t,Modifier.size(74.dp).clip(RoundedCornerShape(16.dp)));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("CONTINUE LISTENING",color=ACCENT,fontSize=10.sp);Text(t.title,color=TEXT,fontSize=17.sp,fontWeight=FontWeight.Bold,maxLines=1);Text(t.artist,color=MUTED,fontSize=12.sp)};FilledIconButton(onClick=onToggle,colors=IconButtonDefaults.filledIconButtonColors(containerColor=ACCENT,contentColor=BG)){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null)}}
    }
}
@Composable private fun MiniPlayer(t:Track,playing:Boolean,onToggle:()->Unit,onOpen:()->Unit){
    Surface(Modifier.fillMaxWidth().clickable(onClick=onOpen),color=CARD2){Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){Artwork(t,Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(t.title,color=TEXT,maxLines=1,fontWeight=FontWeight.SemiBold);Text(t.artist,color=MUTED,fontSize=11.sp,maxLines=1)};IconButton(onClick=onToggle){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,tint=ACCENT)}}}
}
@Composable private fun ErrorCard(msg:String){Surface(Modifier.fillMaxWidth(),RoundedCornerShape(16.dp),color=Color(0xFF3A2924)){Text(msg,Modifier.padding(14.dp),color=TEXT)}}
@Composable private fun EmptyCard(title:String,sub:String){Surface(Modifier.fillMaxWidth(),RoundedCornerShape(20.dp),color=CARD){Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.MusicNote,null,tint=ACCENT,modifier=Modifier.size(34.dp));Text(title,color=TEXT,fontSize=17.sp,fontWeight=FontWeight.Bold);Text(sub,color=MUTED,fontSize=13.sp)}}}
@Composable private fun YouTubeView(url:String,modifier:Modifier){
    AndroidView(modifier=modifier,factory={ctx->WebView(ctx).apply{settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.mediaPlaybackRequiresUserGesture=false;setBackgroundColor(android.graphics.Color.BLACK)}},update={w->
        val id=Regex("[?&]v=([^&]+)").find(url)?.groupValues?.get(1) ?: Regex("youtu\\.be/([^?&/]+)").find(url)?.groupValues?.get(1)
        val target=if(!id.isNullOrBlank())"https://www.youtube-nocookie.com/embed/$id?playsinline=1&autoplay=1&rel=0" else url
        if(w.url!=target)w.loadUrl(target)
    })
}