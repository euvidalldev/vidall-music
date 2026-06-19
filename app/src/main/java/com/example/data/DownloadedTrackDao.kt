package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTrackDao {
    @Query("SELECT * FROM downloaded_tracks ORDER BY downloadTime DESC")
    fun getAllTracks(): Flow<List<DownloadedTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: DownloadedTrack): Long

    @Query("SELECT * FROM downloaded_tracks WHERE id = :id")
    suspend fun getTrackById(id: Int): DownloadedTrack?

    @Query("SELECT * FROM downloaded_tracks WHERE url = :url LIMIT 1")
    suspend fun getTrackByUrl(url: String): DownloadedTrack?

    @Delete
    suspend fun deleteTrack(track: DownloadedTrack)

    @Query("DELETE FROM downloaded_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: Int)
}
