package com.example.sun_mucsic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaSession2
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import com.example.musicapp.MainActivity
import com.example.musicapp.R
import com.example.sun_mucsic.model.Song
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media.session.MediaSessionCompat

class MusicService : Service() {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Song? = null
    private var isPlaying = false
    private lateinit var mediaSession: MediaSessionCompat
    private var serviceCallback: MusicServiceCallback? = null

    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_NEXT = "action_next"
    }

    interface MusicServiceCallback {
        fun onSongChanged(song: Song)
        fun onPlayStateChanged(isPlaying: Boolean)
        fun onProgressChanged(progress: Int, duration: Int)
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> playPause()
            ACTION_PREVIOUS -> serviceCallback?.onSongChanged(currentSong!!) // Handle previous
            ACTION_NEXT -> serviceCallback?.onSongChanged(currentSong!!) // Handle next
        }
        return START_STICKY
    }

    fun setCallback(callback: MusicServiceCallback) {
        this.serviceCallback = callback
    }

        fun playSong(song: Song) {
        try {
            mediaPlayer?.release()

            // Check if this is an assets file
            if (song.uri.toString().startsWith("file:///android_asset/")) {
                // Play from assets
                playFromAssets(song)
            } else {
                // Play from other URI (for future compatibility)
                playFromUri(song)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: simulate playback if file doesn't exist
            simulatePlayback(song)
        }
    }

    private fun playFromAssets(song: Song) {
        try {
            val fileName = song.uri.toString().substringAfter("file:///android_asset/")
            val assetFileDescriptor = assets.openFd(fileName)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(assetFileDescriptor.fileDescriptor,
                           assetFileDescriptor.startOffset,
                           assetFileDescriptor.length)

                prepareAsync()
                setOnPreparedListener {
                    start()
                    this@MusicService.isPlaying = true
                    this@MusicService.currentSong = song
                    serviceCallback?.onSongChanged(song)
                    serviceCallback?.onPlayStateChanged(true)
                    updateNotification()
                    startRealProgressTracking()
                }

                setOnCompletionListener {
                    this@MusicService.isPlaying = false
                    serviceCallback?.onPlayStateChanged(false)
                }

                setOnErrorListener { _, _, _ ->
                    this@MusicService.isPlaying = false
                    serviceCallback?.onPlayStateChanged(false)
                    // Fallback to simulation if file can't be played
                    simulatePlayback(song)
                    true
                }
            }
            assetFileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
            simulatePlayback(song)
        }
    }

    private fun playFromUri(song: Song) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, song.uri)
            prepareAsync()
            setOnPreparedListener {
                start()
                this@MusicService.isPlaying = true
                this@MusicService.currentSong = song
                serviceCallback?.onSongChanged(song)
                serviceCallback?.onPlayStateChanged(true)
                updateNotification()
                startRealProgressTracking()
            }
            setOnCompletionListener {
                this@MusicService.isPlaying = false
                serviceCallback?.onPlayStateChanged(false)
            }
            setOnErrorListener { _, _, _ ->
                this@MusicService.isPlaying = false
                serviceCallback?.onPlayStateChanged(false)
                simulatePlayback(song)
                true
            }
        }
    }

    private fun simulatePlayback(song: Song) {
        // Fallback simulation when real file doesn't exist
        this.isPlaying = true
        this.currentSong = song
        serviceCallback?.onSongChanged(song)
        serviceCallback?.onPlayStateChanged(true)
        updateNotification()
        startProgressTracking()
    }

    fun playPause() {
        mediaPlayer?.let { player ->
            try {
                if (isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    player.start()
                    isPlaying = true
                    startRealProgressTracking()
                }
                serviceCallback?.onPlayStateChanged(isPlaying)
                updateNotification()
            } catch (e: Exception) {
                // If MediaPlayer fails, try simulation
                isPlaying = !isPlaying
                serviceCallback?.onPlayStateChanged(isPlaying)
                updateNotification()
                if (isPlaying) {
                    startProgressTracking()
                }
            }
        } ?: run {
            // No MediaPlayer available, toggle simulation
            isPlaying = !isPlaying
            serviceCallback?.onPlayStateChanged(isPlaying)
            updateNotification()
            if (isPlaying) {
                startProgressTracking()
            }
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun isPlaying(): Boolean = isPlaying

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    private fun startProgressTracking() {
        val progressRunnable = object : Runnable {
            override fun run() {
                if (isPlaying) {
                    // For demo purposes, simulate progress
                    val simulatedProgress = (System.currentTimeMillis() % 30000).toInt() // 30 second loop
                    val simulatedDuration = currentSong?.duration?.toInt() ?: 30000

                    serviceCallback?.onProgressChanged(simulatedProgress, simulatedDuration)
                    Handler(mainLooper).postDelayed(this, 1000)
                }
            }
        }
        Handler(mainLooper).post(progressRunnable)
    }

    private fun startRealProgressTracking() {
        val progressRunnable = object : Runnable {
            override fun run() {
                if (isPlaying && mediaPlayer != null) {
                    try {
                        val progress = getCurrentPosition()
                        val duration = getDuration()
                        serviceCallback?.onProgressChanged(progress, duration)
                        Handler(mainLooper).postDelayed(this, 1000)
                    } catch (e: Exception) {
                        // If real tracking fails, fallback to simulation
                        startProgressTracking()
                    }
                }
            }
        }
        Handler(mainLooper).post(progressRunnable)
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSession.isActive = true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            0,
            playPauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val previousIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_PREVIOUS
        }
        val previousPendingIntent = PendingIntent.getService(
            this,
            1,
            previousIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this,
            2,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentSong?.title ?: "Music Player")
            .setContentText(currentSong?.artist ?: "Unknown Artist")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_skip_previous,
                "Previous",
                previousPendingIntent
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(
                R.drawable.ic_skip_next,
                "Next",
                nextPendingIntent
            )
            .setStyle(
                MediaNotificationCompat.MediaStyle() // Sửa ở đây
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaSession.release()
    }
}