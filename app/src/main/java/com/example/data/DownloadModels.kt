package com.example.data

enum class DownloadStatus {
    QUEUED,
    RESOLVING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadTask(
    val id: String,
    val title: String,
    val originalUrl: String,
    val directDownloadUrl: String? = null,
    val progress: Float = 0f, // 0.0 to 1.0
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val speedKbps: Double = 0.0,
    val errorMessage: String? = null,
    val isAudio: Boolean = true,
    val format: String = "mp3",
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0
)
