package com.example.sun_mucsic.model

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class MusicRepository(private val context: Context) {
    
    fun getAllSongs(): List<Song> {
        // Try to load songs from assets first, fallback to sample data
        val assetsManager = context.assets
        return try {
            val audioFiles = assetsManager.list("")?.filter { it.endsWith(".mp3") }
            if (!audioFiles.isNullOrEmpty()) {
                getSongsFromAssets(audioFiles)
            } else {
                getSampleSongs()
            }
        } catch (e: Exception) {
            getSampleSongs()
        }
    }
    
    private fun getSongsFromAssets(audioFiles: List<String>): List<Song> {
        val songTitles = listOf(
            "12600 lettres (Debut)" to "Franco & TP OK Jazz",
            "Again & Again" to "Isatori",
            "Ain't No Mountain High Enough" to "Marvin Gaye & Tammi Terrell",
            "All I Have to Do Is Dream" to "The Everly Brothers",
            "All Night" to "Siddy Ranks",
            "Escape (The Pina Colada Song)" to "Rupert Holmes"
        )
        
        return audioFiles.mapIndexed { index, fileName ->
            val (title, artist) = if (index < songTitles.size) {
                songTitles[index]
            } else {
                fileName.removeSuffix(".mp3") to "Unknown Artist"
            }
            
            Song(
                id = index.toLong() + 1,
                title = title,
                artist = artist,
                album = "Assets Collection",
                duration = 240000, // Default duration, will be updated when file is loaded
                uri = Uri.parse("file:///android_asset/$fileName")
            )
        }
    }
    
    private fun getSampleSongs(): List<Song> {
        return listOf(
            Song(
                id = 1,
                title = "12600 lettres (Debut)",
                artist = "Franco & TP OK Jazz",
                album = "Classic Collection",
                duration = 240000,
                uri = Uri.parse("android.resource://${context.packageName}/raw/sample1")
            ),
            Song(
                id = 2,
                title = "Again & Again",
                artist = "Isatori",
                album = "Modern Beats",
                duration = 185000,
                uri = Uri.parse("android.resource://${context.packageName}/raw/sample2")
            ),
            Song(
                id = 3,
                title = "Ain't No Mountain High Enough",
                artist = "Marvin Gaye & Tammi Terrell",
                album = "Greatest Hits",
                duration = 267000,
                uri = Uri.parse("android.resource://${context.packageName}/raw/sample3")
            ),
            Song(
                id = 4,
                title = "All I Have to Do Is Dream",
                artist = "The Everly Brothers",
                album = "Classic Rock",
                duration = 154000,
                uri = Uri.parse("android.resource://${context.packageName}/raw/sample4")
            ),
            Song(
                id = 5,
                title = "All Night",
                artist = "Siddy Ranks",
                album = "Reggae Vibes",
                duration = 198000,
                uri = Uri.parse("android.resource://${context.packageName}/raw/sample5")
            ),
            Song(
                id = 6,
                title = "Escape (The Pina Colada Song)",
                artist = "Rupert Holmes",
                album = "80s Classics",
                duration = 278000,
                uri = Uri.parse("android.resource://${context.packageName}/raw/sample6")
            )
        )
    }
    
    private fun getActualSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)
                val duration = it.getLong(durationColumn)
                val data = it.getString(dataColumn)
                
                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        uri = Uri.parse(data)
                    )
                )
            }
        }
        
        return songs
    }
} 