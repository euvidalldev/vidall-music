package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object FileDownloader {
    private const val TAG = "FileDownloader"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS) // Large read timeout for large streams
        .build()

    suspend fun download(
        context: Context,
        url: String,
        suggestedFileName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long, speedKbps: Double) -> Unit,
        onSuccess: (file: File) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val downloadDir = File(context.filesDir, "downloads").apply {
                if (!exists()) mkdirs()
            }
            
            // Clean filename
            val sanitized = sanitizeFileName(suggestedFileName)
            val outputFile = File(downloadDir, sanitized)

            Log.d(TAG, "Starting stream download from $url to ${outputFile.absolutePath}")

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    onFailure("Erro HTTP: ${response.code}")
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    onFailure("Corpo de resposta vazio")
                    return@withContext
                }

                val totalBytes = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalDownloaded: Long = 0
                val startTime = System.nanoTime()
                var lastProgressUpdate = System.currentTimeMillis()

                try {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val now = System.currentTimeMillis()
                        // Throttle callback to avoid overrunning Compose state updates
                        if (now - lastProgressUpdate > 200) {
                            val elapsedNano = System.nanoTime() - startTime
                            // Calculated Kbps speed (bytesDownloaded / secondsElapsed / 1024)
                            val elapsedSeconds = elapsedNano.toDouble() / 1_000_000_000.0
                            val speedKbps = if (elapsedSeconds > 0) {
                                (totalDownloaded.toDouble() / 1024.0) / elapsedSeconds
                            } else {
                                0.0
                            }
                            onProgress(totalDownloaded, totalBytes, speedKbps)
                            lastProgressUpdate = now
                        }
                    }
                    outputStream.flush()
                    
                    // Final 100% callback
                    val finalElapsedSeconds = (System.nanoTime() - startTime).toDouble() / 1_000_000_000.0
                    val finalSpeed = (totalDownloaded.toDouble() / 1024.0) / finalElapsedSeconds
                    onProgress(totalDownloaded, totalDownloaded, finalSpeed)
                    
                    onSuccess(outputFile)
                } catch (e: Exception) {
                    Log.e(TAG, "Stream reading failed", e)
                    if (outputFile.exists()) {
                        outputFile.delete() // Cleanup partial
                    }
                    onFailure(e.localizedMessage ?: "Falha ao gravar arquivo")
                } finally {
                    try {
                        outputStream.close()
                    } catch (ignore: Exception) {}
                    try {
                        inputStream.close()
                    } catch (ignore: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed entirely", e)
            onFailure(e.localizedMessage ?: "Erro na rede")
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "download_${System.currentTimeMillis()}" }
    }
}
