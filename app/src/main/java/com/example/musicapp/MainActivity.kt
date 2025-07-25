package com.example.musicapp

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sun_mucsic.adapter.SongAdapter
import com.example.sun_mucsic.model.Song
import com.example.sun_mucsic.mvp.MainContract
import com.example.sun_mucsic.mvp.MainPresenter
import com.example.sun_mucsic.service.MusicService

class MainActivity : AppCompatActivity(), MainContract.View, MusicService.MusicServiceCallback {

    private lateinit var presenter: MainPresenter
    private var musicService: MusicService? = null
    private var isServiceBound = false

    // UI Components
    private lateinit var recyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private lateinit var currentSongTitle: TextView
    private lateinit var currentSongArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            musicService?.setCallback(this@MainActivity)
            isServiceBound = true

            presenter = MainPresenter(this@MainActivity, musicService)
            presenter.attachView(this@MainActivity)
            presenter.loadSongs()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startMusicService()
        } else {
            Toast.makeText(this, "Permission required for music playback", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissionsAndStartService()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view_songs)
        currentSongTitle = findViewById(R.id.current_song_title)
        currentSongArtist = findViewById(R.id.current_song_artist)
        seekBar = findViewById(R.id.seek_bar)
        currentTime = findViewById(R.id.current_time)
        totalTime = findViewById(R.id.total_time)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnPrevious = findViewById(R.id.btn_previous)
        btnNext = findViewById(R.id.btn_next)

        songAdapter = SongAdapter(emptyList()) { song ->
            presenter.onSongSelected(song)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = songAdapter
        }

        setupClickListeners()
        setupSeekBar()
    }

    private fun setupClickListeners() {
        btnPlayPause.setOnClickListener {
            presenter.playPause()
        }

        btnPrevious.setOnClickListener {
            presenter.previous()
        }

        btnNext.setOnClickListener {
            presenter.next()
        }
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    presenter.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun checkPermissionsAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startMusicService()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startMusicService()
        }
    }

    private fun startMusicService() {
        val intent = Intent(this, MusicService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // MVP View Interface Implementation
    override fun showSongs(songs: List<Song>) {
        songAdapter.updateSongs(songs)
    }

    override fun showCurrentSong(song: Song) {
        currentSongTitle.text = song.title
        currentSongArtist.text = song.artist
        songAdapter.setCurrentPlayingSong(song)
    }

    override fun updatePlayButton(isPlaying: Boolean) {
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    override fun updateProgress(progress: Int, duration: Int) {
        seekBar.max = duration
        seekBar.progress = progress
        currentTime.text = formatTime(progress)
        totalTime.text = formatTime(duration)
    }

    override fun updateSeekBar(progress: Int) {
        seekBar.progress = progress
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // MusicService Callback Implementation
    override fun onSongChanged(song: Song) {
        showCurrentSong(song)
    }

    override fun onPlayStateChanged(isPlaying: Boolean) {
        updatePlayButton(isPlaying)
    }

    override fun onProgressChanged(progress: Int, duration: Int) {
        updateProgress(progress, duration)
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }
}