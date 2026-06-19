package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrack(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val localPath: String,
    val url: String,
    val fileSize: Long,
    val duration: Long, // in seconds
    val downloadTime: Long = System.currentTimeMillis(),
    val thumbnailUrl: String? = null,
    val artist: String = "Desconhecido",
    val format: String = "mp3" // mp3 or mp4
)
