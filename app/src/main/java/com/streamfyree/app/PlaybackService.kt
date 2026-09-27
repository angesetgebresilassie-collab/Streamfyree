package com.streamfyree.app

import android.content.Intent
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        // Apply the exact headers yt-dlp used when creating the signed
        // googlevideo URL. A valid signed URL can otherwise return 403.
        val httpDataSourceFactory = DataSource.Factory {
            val dataSource = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .createDataSource()

            PlaybackRequestHeaders.current().forEach { (name, value) ->
                dataSource.setRequestProperty(name, value)
            }
            dataSource
        }

        // Downloaded tracks are played back from local file:// URIs.
        // DefaultHttpDataSource only understands http/https and throws
        // immediately for anything else — this is why downloads would
        // complete successfully but silently fail to play. Wrapping it in
        // DefaultDataSource.Factory routes file/content/asset URIs to the
        // right local data source and falls back to the header-aware HTTP
        // factory above for streamed tracks.
        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(this, httpDataSourceFactory)
        )

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setId("streamfyree")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
