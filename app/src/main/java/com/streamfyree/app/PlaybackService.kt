package com.streamfyree.app

import android.content.Intent
import androidx.media3.datasource.DataSource
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

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

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
