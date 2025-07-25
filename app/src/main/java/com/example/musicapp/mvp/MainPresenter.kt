package com.example.sun_mucsic.mvp

import android.content.Context
import com.example.sun_mucsic.model.MusicRepository
import com.example.sun_mucsic.model.Song
import com.example.sun_mucsic.service.MusicService

class MainPresenter(
    private val context: Context,
    private val musicService: MusicService?
) : MainContract.Presenter {
    
    private var view: MainContract.View? = null
    private val musicRepository = MusicRepository(context)
    private var songs: List<Song> = emptyList()
    private var currentSongIndex = 0
    
    override fun attachView(view: MainContract.View) {
        this.view = view
    }
    
    override fun detachView() {
        this.view = null
    }
    
    override fun loadSongs() {
        try {
            songs = musicRepository.getAllSongs()
            view?.showSongs(songs)
            if (songs.isNotEmpty()) {
                view?.showCurrentSong(songs[currentSongIndex])
            }
        } catch (e: Exception) {
            view?.showError("Failed to load songs: ${e.message}")
        }
    }
    
    override fun playSong(song: Song) {
        currentSongIndex = songs.indexOf(song)
        if (currentSongIndex != -1) {
            musicService?.playSong(song)
            view?.showCurrentSong(song)
            view?.updatePlayButton(true)
        }
    }
    
    override fun playPause() {
        musicService?.let { service ->
            if (service.isPlaying()) {
                service.playPause()
                view?.updatePlayButton(false)
            } else {
                if (songs.isNotEmpty()) {
                    service.playSong(songs[currentSongIndex])
                    view?.updatePlayButton(true)
                } else {

                }
            }
        }
    }
    
    override fun seekTo(position: Int) {
        musicService?.seekTo(position)
    }
    
    override fun previous() {
        if (songs.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else songs.size - 1
            playSong(songs[currentSongIndex])
        }
    }
    
    override fun next() {
        if (songs.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex < songs.size - 1) currentSongIndex + 1 else 0
            playSong(songs[currentSongIndex])
        }
    }
    
    override fun onSongSelected(song: Song) {
        playSong(song)
    }
    
    fun updateProgress(progress: Int, duration: Int) {
        view?.updateProgress(progress, duration)
    }
} 