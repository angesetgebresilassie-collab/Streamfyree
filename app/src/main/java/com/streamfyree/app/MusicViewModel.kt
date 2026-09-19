package com.streamfyree.app
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
class MusicViewModel(app:Application):AndroidViewModel(app){
 private val api=StreamApi()
 val player=ExoPlayer.Builder(app).build()
 private val _state=MutableStateFlow(SearchState()); val state:StateFlow<SearchState>=_state
 private val _mode=MutableStateFlow(PlaybackMode.NATIVE); val mode:StateFlow<PlaybackMode>=_mode
 private val _current=MutableStateFlow<Track?>(null); val current:StateFlow<Track?>=_current
 fun setMode(mode:PlaybackMode){_mode.value=mode}
 fun search(query:String){
  if(query.isBlank())return
  _state.value=_state.value.copy(query=query,loading=true,error=null)
  viewModelScope.launch(Dispatchers.IO){runCatching{api.search(query)}.onSuccess{_state.value=_state.value.copy(loading=false,tracks=it)}.onFailure{_state.value=_state.value.copy(loading=false,error=it.message)}}
 }
 fun play(track:Track){
  viewModelScope.launch(Dispatchers.IO){
   val resolved=if(_mode.value==PlaybackMode.NATIVE&&track.streamUrl==null)runCatching{api.resolve(track.id)}.getOrElse{track}else track
   _current.value=resolved
   if(_mode.value==PlaybackMode.NATIVE&&!resolved.streamUrl.isNullOrBlank()){player.setMediaItem(MediaItem.fromUri(resolved.streamUrl!!));player.prepare();player.play()}
  }
 }
 override fun onCleared(){player.release();super.onCleared()}
}
