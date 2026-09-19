package com.streamfyree.app
data class Track(val id:String,val title:String,val artist:String,val album:String="",val artwork:String?=null,val durationMs:Long=0,val youtubeUrl:String?=null,val streamUrl:String?=null,val lyricVideo:Boolean=false)
enum class PlaybackMode { NATIVE, YOUTUBE }
data class SearchState(val query:String="",val loading:Boolean=false,val tracks:List<Track> = emptyList(),val error:String?=null)
