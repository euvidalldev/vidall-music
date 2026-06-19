package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.data.DownloadedTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class LocalTrackPlayer(private val context: Context) {
    private val TAG = "LocalTrackPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    // Player State Flows
    private val _currentTrack = MutableStateFlow<DownloadedTrack?>(null)
    val currentTrack: StateFlow<DownloadedTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(1L) // Avoid dividing by zero initially
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isRepeating = MutableStateFlow(false)
    val isRepeating: StateFlow<Boolean> = _isRepeating.asStateFlow()

    private val _isShuffling = MutableStateFlow(false)
    val isShuffling: StateFlow<Boolean> = _isShuffling.asStateFlow()

    private var activeQueue: List<DownloadedTrack> = emptyList()
    private var currentQueueIndex: Int = -1

    init {
        setupMediaPlayer()
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener {
                Log.d(TAG, "Playback completed.")
                onTrackCompleted()
            }
            setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "MediaPlayer Error: what=$what, extra=$extra")
                _isPlaying.value = false
                stopProgressUpdate()
                true
            }
        }
    }

    private fun onTrackCompleted() {
        if (_isRepeating.value) {
            seekTo(0)
            resume()
        } else {
            next()
        }
    }

    fun play(track: DownloadedTrack, queue: List<DownloadedTrack> = emptyList()) {
        try {
            stop()
            activeQueue = queue
            currentQueueIndex = queue.indexOfFirst { it.id == track.id }
            if (currentQueueIndex == -1 && queue.isNotEmpty()) {
                activeQueue = queue + track
                currentQueueIndex = activeQueue.lastIndex
            }

            val file = File(track.localPath)
            if (!file.exists()) {
                Log.e(TAG, "Local song file does not exist: ${track.localPath}")
                _isPlaying.value = false
                return
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                start()
                
                _duration.value = duration.toLong()
                _currentTrack.value = track
                _isPlaying.value = true
                
                setOnCompletionListener { onTrackCompleted() }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer Error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    stopProgressUpdate()
                    true
                }
            }

            startProgressUpdate()
            Log.d(TAG, "Playing track: ${track.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed playing track: ${e.message}", e)
            _isPlaying.value = false
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _isPlaying.value = false
                stopProgressUpdate()
                Log.d(TAG, "Paused. Position: ${_playbackPosition.value}ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pause exception: ${e.message}")
        }
    }

    fun resume() {
        try {
            if (mediaPlayer != null && !_isPlaying.value) {
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressUpdate()
                Log.d(TAG, "Resumed playback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Resume exception: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _playbackPosition.value = positionMs
            Log.d(TAG, "Seeking to ${positionMs}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Seek exception: ${e.message}")
        }
    }

    fun skipForward() {
        mediaPlayer?.let {
            val target = (it.currentPosition + 10000).coerceAtMost(it.duration)
            seekTo(target.toLong())
        }
    }

    fun skipBackward() {
        mediaPlayer?.let {
            val target = (it.currentPosition - 10000).coerceAtLeast(0)
            seekTo(target.toLong())
        }
    }

    fun next() {
        if (activeQueue.isEmpty()) return
        
        val nextIndex = if (_isShuffling.value) {
            activeQueue.indices.random()
        } else {
            val idx = currentQueueIndex + 1
            if (idx in activeQueue.indices) idx else 0
        }

        if (nextIndex in activeQueue.indices) {
            play(activeQueue[nextIndex], activeQueue)
        }
    }

    fun previous() {
        if (activeQueue.isEmpty()) return

        val prevIndex = if (_isShuffling.value) {
            activeQueue.indices.random()
        } else {
            val idx = currentQueueIndex - 1
            if (idx >= 0) idx else activeQueue.lastIndex
        }

        if (prevIndex in activeQueue.indices) {
            play(activeQueue[prevIndex], activeQueue)
        }
    }

    fun toggleRepeat() {
        _isRepeating.value = !_isRepeating.value
    }

    fun toggleShuffle() {
        _isShuffling.value = !_isShuffling.value
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            _isPlaying.value = false
            stopProgressUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Stop exception: ${e.message}")
        }
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressJob = scope.launch {
            while (isActive) {
                try {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            _playbackPosition.value = it.currentPosition.toLong()
                        }
                    }
                } catch (ignore: Exception) {}
                delay(250) // Update every quarterback of a second is smooth enough
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null
        scope.cancel()
    }
}
