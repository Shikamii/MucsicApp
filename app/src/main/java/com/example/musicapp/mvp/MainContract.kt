package com.example.sun_mucsic.mvp

import com.example.sun_mucsic.model.Song

interface MainContract {
    
    interface View {
        fun showSongs(songs: List<Song>)
        fun showCurrentSong(song: Song)
        fun updatePlayButton(isPlaying: Boolean)
        fun updateProgress(progress: Int, duration: Int)
        fun showError(message: String)
        fun updateSeekBar(progress: Int)
    }
    
    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun loadSongs()
        fun playSong(song: Song)
        fun playPause()
        fun seekTo(position: Int)
        fun previous()
        fun next()
        fun onSongSelected(song: Song)
    }
} 