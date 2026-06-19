package com.example.data

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object CobaltClient {
    private const val TAG = "CobaltClient"

    // Default public instances as fallback
    // Note: v7 API (api/json) shut down Nov 2024. Now using POST / (root path).
    private val DEFAULT_ENDPOINTS = listOf(
        "https://api.cobalt.tools/",
        "https://co.wuk.sh/",
        "https://cobalt.api.ryder.rip/"
    )

    private fun getEndpoints(): List<String> {
        val customUrl = InstanceConfig.customInstanceUrl
        return if (!customUrl.isNullOrBlank()) {
            listOf(customUrl.trimEnd('/') + "/")
        } else {
            DEFAULT_ENDPOINTS
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val responseAdapter = moshi.adapter(CobaltResponse::class.java)

    data class CobaltRequest(
        val url: String,
        val downloadMode: String, // "audio" or "video"
        val audioFormat: String = "mp3", // "mp3" or "best"
        val videoQuality: String = "720", // "720" or "1080"
        val filenameStyle: String = "classic"
    )

    data class CobaltError(
        val code: String? = null,
        val context: Map<String, Any>? = null
    )

    data class CobaltResponse(
        val status: String, // "tunnel", "redirect", "picker", "error", "local-processing"
        val url: String? = null,
        val filename: String? = null,
        val text: String? = null, // legacy error message
        val error: CobaltError? = null // error object (v8+)
    ) {
        val errorMessage: String?
            get() = text ?: error?.code
    }

    /**
     * Resolves the media download link using Cobalt API endpoints with automatic fallback.
     */
    suspend fun resolveUrl(
        inputUrl: String,
        isAudioOnly: Boolean,
        format: String = "mp3",
        quality: String = "720"
    ): CobaltResponse = withContext(Dispatchers.IO) {
        val requestBodyMap = mapOf(
            "url" to inputUrl,
            "downloadMode" to (if (isAudioOnly) "audio" else "auto"),
            "audioFormat" to format,
            "videoQuality" to quality,
            "filenameStyle" to "classic"
        )
        
        val requestJson = moshi.adapter(Map::class.java).toJson(requestBodyMap)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toRequestBody(mediaType)

        var lastException: Exception? = null

        val endpoints = getEndpoints()

        for (endpoint in endpoints) {
            try {
                Log.d(TAG, "Attempting to resolve URL on: $endpoint")
                val requestBuilder = Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")

                // Add auth header if configured
                val scheme = InstanceConfig.authScheme
                val key = InstanceConfig.apiKey
                if (!scheme.isNullOrBlank() && !key.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "$scheme $key")
                }

                val request = requestBuilder.build()

                client.newCall(request).execute().use { response ->
                    val responseBodyString = response.body?.string()
                    Log.d(TAG, "Cobalt Response Code: ${response.code}, Body: $responseBodyString")

                    if (response.isSuccessful && responseBodyString != null) {
                        val resolved = responseAdapter.fromJson(responseBodyString)
                        if (resolved != null) {
                            if (resolved.status == "error") {
                                Log.e(TAG, "Cobalt returned error: ${resolved.errorMessage}")
                                return@withContext resolved
                            }
                            return@withContext resolved
                        }
                    } else if (response.code == 400 || response.code == 422) {
                        if (responseBodyString != null) {
                            val resolved = responseAdapter.fromJson(responseBodyString)
                            if (resolved != null) return@withContext resolved
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Filed resolving with $endpoint: ${e.message}", e)
                lastException = e
            }
        }

        // If we reach here, all instances failed
        return@withContext CobaltResponse(
            status = "error",
            text = "Não foi possível conectar aos servidores de download. Erro: ${lastException?.localizedMessage ?: "Conexão perdida"}"
        )
    }
}
