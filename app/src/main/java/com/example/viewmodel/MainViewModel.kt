package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.player.LocalTrackPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class AppTab {
    HOME,     // Paste YouTube URL & trigger extract
    QUEUE,    // Active downloading list & completed history
    LIBRARY,  // Local library playlist (with search / sorting)
    PLAYER    // Elegant immersive audio controller
}

class MainViewModel(
    application: Application,
    private val repository: TrackRepository,
    val player: LocalTrackPlayer
) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"

    private val context: Context get() = getApplication()

    // Screen tab selection
    private val _activeTab = MutableStateFlow(AppTab.HOME)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    // UI Input fields
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isAudioOnly = MutableStateFlow(true) // audio vs video format
    val isAudioOnly: StateFlow<Boolean> = _isAudioOnly.asStateFlow()

    private val _audioFormatOption = MutableStateFlow("mp3") // mp3, best, ogg
    val audioFormatOption: StateFlow<String> = _audioFormatOption.asStateFlow()

    private val _videoQualityOption = MutableStateFlow("720") // 1080, 720, 480
    val videoQualityOption: StateFlow<String> = _videoQualityOption.asStateFlow()

    // Core Data flows
    private val _activeTasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val activeTasks: StateFlow<List<DownloadTask>> = _activeTasks
        .map { it.values.toList().sortedBy { task -> task.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedTracks: StateFlow<List<DownloadedTrack>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player observed states bridged into ViewModel for convenience
    val currentPlayingTrack: StateFlow<DownloadedTrack?> = player.currentTrack
    val isPlaying: StateFlow<Boolean> = player.isPlaying
    val playbackPosition: StateFlow<Long> = player.playbackPosition
    val duration: StateFlow<Long> = player.duration
    val isRepeating: StateFlow<Boolean> = player.isRepeating
    val isShuffling: StateFlow<Boolean> = player.isShuffling

    init {
        Log.d(TAG, "MainViewModel initialized")
    }

    fun setActiveTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun setUrlInput(url: String) {
        _urlInput.value = url.trim()
    }

    fun setIsAudioOnly(isAudio: Boolean) {
        _isAudioOnly.value = isAudio
    }

    fun setAudioFormat(format: String) {
        _audioFormatOption.value = format
    }

    fun setVideoQuality(quality: String) {
        _videoQualityOption.value = quality
    }

    /**
     * Start the download flow: resolves link via Cobalt and downloads the file streams
     */
    fun startDownload(url: String) {
        val targetUrl = url.trim()
        if (targetUrl.isEmpty()) {
            showToast("Insira um link do YouTube ou outra plataforma!")
            return
        }

        // Simple validation check
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            showToast("Insira um endereço URL válido!")
            return
        }

        val taskId = UUID.randomUUID().toString()
        val streamType = if (_isAudioOnly.value) "Música" else "Vídeo"
        val initialTask = DownloadTask(
            id = taskId,
            title = "Carregando informações do link...",
            originalUrl = targetUrl,
            status = DownloadStatus.RESOLVING,
            isAudio = _isAudioOnly.value,
            format = if (_isAudioOnly.value) _audioFormatOption.value else "mp4"
        )

        // Queue the task
        updateTaskState(taskId, initialTask)
        
        // Clear input URL once queued
        _urlInput.value = ""
        
        // Jump to progress tab
        _activeTab.value = AppTab.QUEUE

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting URL resolution for: $targetUrl")
                
                val result = CobaltClient.resolveUrl(
                    inputUrl = targetUrl,
                    isAudioOnly = initialTask.isAudio,
                    format = _audioFormatOption.value,
                    quality = _videoQualityOption.value
                )

                if (result.status == "error" || result.url == null) {
                    val errorText = result.errorMessage ?: "Erro desconhecido ao processar link."
                    Log.e(TAG, "Resolution failed: $errorText")
                    updateTaskState(taskId, initialTask.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = errorText,
                        title = "Falha na extração"
                    ))
                    return@launch
                }

                // Extract or generate a clean title
                var finalTitle = result.filename ?: ""
                if (finalTitle.isBlank()) {
                    finalTitle = "Faixa_${System.currentTimeMillis()}"
                } else {
                    // Strip file extension from title label if present
                    if (finalTitle.contains(".")) {
                        finalTitle = finalTitle.substringBeforeLast(".")
                    }
                }

                Log.d(TAG, "URL resolved successfully. Direct Link: ${result.url}, File: $finalTitle")

                // Update task status to downloading
                val directUrl = result.url
                val downloadingTask = initialTask.copy(
                    status = DownloadStatus.DOWNLOADING,
                    directDownloadUrl = directUrl,
                    title = finalTitle
                )
                updateTaskState(taskId, downloadingTask)

                val fileExt = if (initialTask.isAudio) initialTask.format else "mp4"
                val finalFileName = "${finalTitle}.${fileExt}"

                // 2. Perform direct streaming download onto local private storage
                FileDownloader.download(
                    context = context,
                    url = directUrl,
                    suggestedFileName = finalFileName,
                    onProgress = { downloadedBytes, totalBytes, speedKbps ->
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
                        updateTaskState(taskId, downloadingTask.copy(
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            speedKbps = speedKbps,
                            status = DownloadStatus.DOWNLOADING
                        ))
                    },
                    onSuccess = { file ->
                        Log.d(TAG, "File downloaded successfully to private storage: ${file.absolutePath}")
                        
                        viewModelScope.launch {
                            // 3. Save download file to Room database library
                            val track = DownloadedTrack(
                                title = finalTitle,
                                localPath = file.absolutePath,
                                url = targetUrl,
                                fileSize = file.length(),
                                duration = 0, // Duration will extract dynamically upon reading
                                artist = "Baixado",
                                format = fileExt
                            )
                            repository.insert(track)
                            
                            // Complete the task state
                            updateTaskState(taskId, downloadingTask.copy(
                                status = DownloadStatus.COMPLETED,
                                progress = 1.0f,
                                downloadedBytes = file.length(),
                                totalBytes = file.length()
                            ))
                            
                            showToast("Download completo: $finalTitle!")
                        }
                    },
                    onFailure = { errorMsg ->
                        Log.e(TAG, "Streaming failed: $errorMsg")
                        updateTaskState(taskId, downloadingTask.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = errorMsg
                        ))
                        showToast("Falha no download: $errorMsg")
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Resolution crash", e)
                updateTaskState(taskId, initialTask.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Erro de processamento interno"
                ))
            }
        }
    }

    private fun updateTaskState(id: String, task: DownloadTask) {
        _activeTasks.value = _activeTasks.value.toMutableMap().apply {
            put(id, task)
        }
    }

    fun removeTask(taskId: String) {
        _activeTasks.value = _activeTasks.value.toMutableMap().apply {
            remove(taskId)
        }
    }

    fun clearCompletedTasks() {
        _activeTasks.value = _activeTasks.value.filterValues { it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.FAILED }
    }

    /**
     * Delete track from local Room database and securely erase files from private storage
     */
    fun deleteTrack(track: DownloadedTrack) {
        viewModelScope.launch {
            try {
                // If it is current playing track, stop player first
                if (currentPlayingTrack.value?.id == track.id) {
                    player.stop()
                }

                // Delete file
                val file = File(track.localPath)
                if (file.exists()) {
                    val deleted = withContext(Dispatchers.IO) { file.delete() }
                    Log.d(TAG, "File deletion result for ${track.localPath}: $deleted")
                }

                // Delete entry from DB
                repository.delete(track)
                showToast("Arquivo removido: ${track.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete track files", e)
                showToast("Erro ao excluir arquivo.")
            }
        }
    }

    /**
     * Player bridging controls
     */
    fun playTrack(track: DownloadedTrack) {
        val tracksList = downloadedTracks.value
        // Filters just playable audio
        val audioTracks = tracksList.filter { it.format != "mp4" }
        player.play(track, audioTracks)
        _activeTab.value = AppTab.PLAYER // Jump to player view for immersive control!
    }

    fun pauseSong() = player.pause()
    fun resumeSong() = player.resume()
    fun seekSong(ms: Long) = player.seekTo(ms)
    fun nextSong() = player.next()
    fun previousSong() = player.previous()
    fun toggleRepeat() = player.toggleRepeat()
    fun toggleShuffle() = player.toggleShuffle()
    fun playFromQueue(track: DownloadedTrack, queue: List<DownloadedTrack>) = player.play(track, queue)

    // Configuration methods for custom Cobalt instance
    fun getCustomInstanceUrl(): String? = InstanceConfig.customInstanceUrl
    fun setCustomInstanceUrl(url: String?) {
        InstanceConfig.customInstanceUrl = url?.trim()
        showToast(if (url.isNullOrBlank()) "Instância padrão restaurada" else "Instância customizada definida")
    }

    fun getApiKey(): String? = InstanceConfig.apiKey
    fun setApiKey(key: String?) {
        InstanceConfig.apiKey = key?.trim()
        showToast(if (key.isNullOrBlank()) "Chave API removida" else "Chave API salva")
    }

    fun getAuthScheme(): String? = InstanceConfig.authScheme
    fun setAuthScheme(scheme: String?) {
        InstanceConfig.authScheme = scheme
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Player is cleared inside MainActivity onDestroy to prevent memory leaks
    }
}
