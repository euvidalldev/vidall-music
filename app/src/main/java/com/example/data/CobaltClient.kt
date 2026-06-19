package com.example.data

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object CobaltClient {
    private const val TAG = "MediaResolver"

    private val DEFAULT_PIPED_INSTANCE = "https://pipedapi.kavin.rocks"

    private fun getBaseUrl(): String {
        val custom = InstanceConfig.customInstanceUrl
        return if (!custom.isNullOrBlank()) custom.trimEnd('/') else DEFAULT_PIPED_INSTANCE
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    data class CobaltResponse(
        val status: String,
        val url: String? = null,
        val filename: String? = null,
        val text: String? = null
    ) {
        val errorMessage: String? get() = text
    }

    data class PipedStream(
        val url: String? = null,
        val format: String? = null,
        val quality: String? = null,
        val mimeType: String? = null,
        val videoOnly: Boolean? = null
    )

    data class PipedStreamsResponse(
        val title: String? = null,
        val thumbnailUrl: String? = null,
        val duration: Long? = null,
        val audioStreams: List<PipedStream>? = null,
        val videoStreams: List<PipedStream>? = null,
        val error: String? = null
    )

    fun extractYoutubeVideoId(url: String): String? {
        val patterns = listOf(
            "v=([a-zA-Z0-9_-]{11})",
            "youtu\\.be/([a-zA-Z0-9_-]{11})",
            "shorts/([a-zA-Z0-9_-]{11})",
            "embed/([a-zA-Z0-9_-]{11})",
            "live/([a-zA-Z0-9_-]{11})"
        )
        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern).matcher(url)
            if (matcher.find()) return matcher.group(1)
        }
        return null
    }

    suspend fun resolveUrl(
        inputUrl: String,
        isAudioOnly: Boolean,
        format: String = "mp3",
        quality: String = "720"
    ): CobaltResponse = withContext(Dispatchers.IO) {
        // First try Piped API
        val videoId = extractYoutubeVideoId(inputUrl)
        if (videoId != null) {
            val pipedResult = resolveViaPiped(videoId, isAudioOnly, quality)
            if (pipedResult != null) return@withContext pipedResult
        } else {
            return@withContext CobaltResponse(
                status = "error",
                text = "URL do YouTube inválida. Use um link do tipo youtube.com/watch?v=..."
            )
        }

        // If no custom instance configured and Piped failed, return error
        val customUrl = InstanceConfig.customInstanceUrl
        if (customUrl.isNullOrBlank()) {
            return@withContext CobaltResponse(
                status = "error",
                text = "Não foi possível processar o vídeo. A instância Piped pode estar temporariamente indisponível."
            )
        }

        return@withContext CobaltResponse(
            status = "error",
            text = "Todas as tentativas de resolução falharam."
        )
    }

    private suspend fun resolveViaPiped(
        videoId: String,
        isAudioOnly: Boolean,
        quality: String
    ): CobaltResponse? {
        val baseUrl = getBaseUrl()
        val apiUrl = "$baseUrl/streams/$videoId"

        return try {
            Log.d(TAG, "Fetching streams from Piped API: $apiUrl")
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                Log.d(TAG, "Piped Response Code: ${response.code}")

                if (!response.isSuccessful || body == null) {
                    Log.e(TAG, "Piped API error: ${response.code}")
                    return null
                }

                val adapter = moshi.adapter(PipedStreamsResponse::class.java)
                val streams = adapter.fromJson(body)

                if (streams == null) {
                    Log.e(TAG, "Failed to parse Piped response")
                    return null
                }

                if (streams.error != null) {
                    Log.e(TAG, "Piped returned error: ${streams.error}")
                    return null
                }

                val title = streams.title ?: "Video_${videoId}"

                if (isAudioOnly) {
                    val audioStreams = streams.audioStreams
                    if (audioStreams.isNullOrEmpty()) {
                        Log.e(TAG, "No audio streams available")
                        return null
                    }
                    val bestAudio = audioStreams.maxByOrNull {
                        parseBitrate(it.quality ?: "0")
                    }
                    val streamUrl = bestAudio?.url
                    if (streamUrl.isNullOrBlank()) return null
                    val ext = bestAudio.format ?: "webm"
                    return CobaltResponse(
                        status = "redirect",
                        url = streamUrl,
                        filename = "$title.$ext"
                    )
                } else {
                    val videoStreams = streams.videoStreams
                    if (videoStreams.isNullOrEmpty()) {
                        Log.e(TAG, "No video streams available")
                        return null
                    }
                    val targetQuality = quality.toIntOrNull() ?: 720
                    val bestVideo = videoStreams
                        .filter { it.videoOnly != true }
                        .minByOrNull {
                            val q = it.quality?.filter { c -> c.isDigit() || c == 'p' }
                                ?.replace("p", "")?.toIntOrNull() ?: 0
                            Math.abs(q - targetQuality)
                        } ?: videoStreams.firstOrNull { it.videoOnly != true }
                    val streamUrl = bestVideo?.url
                    if (streamUrl.isNullOrBlank()) return null
                    val ext = when {
                        bestVideo.mimeType?.contains("mp4") == true -> "mp4"
                        bestVideo.mimeType?.contains("webm") == true -> "webm"
                        else -> "mp4"
                    }
                    return CobaltResponse(
                        status = "redirect",
                        url = streamUrl,
                        filename = "$title.$ext"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Piped request failed: ${e.message}", e)
            null
        }
    }

    private fun parseBitrate(quality: String): Int {
        val digits = quality.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }
}
