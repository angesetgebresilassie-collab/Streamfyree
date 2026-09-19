package com.streamfyree.app.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.streamfyree.app.*
@Composable
fun StreamfyreeScreen(vm:MusicViewModel){
 val state by vm.state.collectAsState(); val mode by vm.mode.collectAsState(); val current by vm.current.collectAsState()
 var query by remember{mutableStateOf("")}
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface,MaterialTheme.colorScheme.surfaceContainerHigh,MaterialTheme.colorScheme.surface)))){
  Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){
   Spacer(Modifier.height(42.dp)); Text("Streamfyree",style=MaterialTheme.typography.headlineLarge)
   Text("Your music, your way",color=MaterialTheme.colorScheme.onSurfaceVariant)
   Spacer(Modifier.height(18.dp))
   OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search songs, artists, albums…")},leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={IconButton(onClick={vm.search(query)}){Icon(Icons.Default.ArrowForward,"Search")}},shape=RoundedCornerShape(22.dp))
   Spacer(Modifier.height(14.dp))
   Row(verticalAlignment=Alignment.CenterVertically){
    Text("Playback",style=MaterialTheme.typography.titleMedium); Spacer(Modifier.width(10.dp))
    FilterChip(selected=mode==PlaybackMode.NATIVE,onClick={vm.setMode(PlaybackMode.NATIVE)},label={Text("Native")})
    Spacer(Modifier.width(8.dp))
    FilterChip(selected=mode==PlaybackMode.YOUTUBE,onClick={vm.setMode(PlaybackMode.YOUTUBE)},label={Text("YouTube")})
   }
   Spacer(Modifier.height(12.dp)); if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
   state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
   LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(bottom=120.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    items(state.tracks,key={it.id}){track->TrackRow(track){vm.play(track)}}
   }
  }
  current?.let{track->
   Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),shape=RoundedCornerShape(24.dp),tonalElevation=8.dp){
    Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
     AsyncImage(model=track.artwork,contentDescription=null,modifier=Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
     Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)){Text(track.title,maxLines=1);Text(track.artist,maxLines=1,color=MaterialTheme.colorScheme.onSurfaceVariant)}
     IconButton(onClick={if(vm.player.isPlaying)vm.player.pause()else vm.player.play()}){Icon(if(vm.player.isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow,null)}
    }
   }
  }
 }
}
@Composable private fun TrackRow(track:Track,onClick:()->Unit){
 Surface(Modifier.fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(20.dp),tonalElevation=2.dp){
  Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
   AsyncImage(model=track.artwork,contentDescription=null,modifier=Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)))
   Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)){Text(track.title,maxLines=1,style=MaterialTheme.typography.titleMedium);Text(track.artist,maxLines=1,color=MaterialTheme.colorScheme.onSurfaceVariant);if(track.lyricVideo)Text("Lyric video",style=MaterialTheme.typography.labelSmall)}
   Icon(Icons.Default.PlayCircle,null)
  }
 }
}
