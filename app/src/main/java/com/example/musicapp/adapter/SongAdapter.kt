package com.example.sun_mucsic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.sun_mucsic.model.Song

class SongAdapter(
    private var songs: List<Song>,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    
    private var currentPlayingSong: Song? = null
    
    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumArt: ImageView = itemView.findViewById(R.id.album_art)
        val songTitle: TextView = itemView.findViewById(R.id.song_title)
        val songArtist: TextView = itemView.findViewById(R.id.song_artist)
        val playingIndicator: ImageView = itemView.findViewById(R.id.playing_indicator)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        
        holder.songTitle.text = song.title
        holder.songArtist.text = song.artist
        
        // Set album art (using default music icon for now)
        holder.albumArt.setImageResource(R.drawable.ic_music_note)
        
        // Show playing indicator if this is the current song
        if (song == currentPlayingSong) {
            holder.playingIndicator.visibility = View.VISIBLE
            holder.playingIndicator.setImageResource(R.drawable.ic_playing_animation)
        } else {
            holder.playingIndicator.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            onSongClick(song)
        }
    }
    
    override fun getItemCount(): Int = songs.size
    
    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }
    
    fun setCurrentPlayingSong(song: Song?) {
        val previousIndex = songs.indexOf(currentPlayingSong)
        val newIndex = songs.indexOf(song)
        
        currentPlayingSong = song
        
        if (previousIndex != -1) {
            notifyItemChanged(previousIndex)
        }
        if (newIndex != -1) {
            notifyItemChanged(newIndex)
        }
    }
} 