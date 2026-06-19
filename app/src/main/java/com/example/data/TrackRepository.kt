package com.example.data

import kotlinx.coroutines.flow.Flow

class TrackRepository(private val trackDao: DownloadedTrackDao) {
    val allTracks: Flow<List<DownloadedTrack>> = trackDao.getAllTracks()

    suspend fun insert(track: DownloadedTrack): Long {
        return trackDao.insertTrack(track)
    }

    suspend fun getTrackById(id: Int): DownloadedTrack? {
        return trackDao.getTrackById(id)
    }

    suspend fun getTrackByUrl(url: String): DownloadedTrack? {
        return trackDao.getTrackByUrl(url)
    }

    suspend fun delete(track: DownloadedTrack) {
        trackDao.deleteTrack(track)
    }

    suspend fun deleteById(id: Int) {
        trackDao.deleteTrackById(id)
    }
}
