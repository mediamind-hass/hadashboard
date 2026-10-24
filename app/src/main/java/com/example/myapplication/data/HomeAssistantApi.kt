package com.example.myapplication.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class EntityStateResult(
    val entityId: String,
    val state: String,
    val unitOfMeasurement: String? = null,
    val friendlyName: String? = null
)

sealed class ConnectionResult {
    object Success : ConnectionResult()
    data class Timeout(val url: String, val suggestedUrl: String? = null) : ConnectionResult()
    data class Unauthorized(val code: Int) : ConnectionResult()
    data class Error(val message: String, val suggestedUrl: String? = null) : ConnectionResult()
}

class HomeAssistantApi(
    private val prefs: HaPreferences,
    private val context: Context? = null
) {
    init {
        if (context != null) {
            NetworkUtils.bindProcessToLocalNetwork(context)
        }
    }

    private val client: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(okhttp3.ConnectionPool(0, 1, TimeUnit.NANOSECONDS)) // Disable connection pooling to prevent One UI 9 stale socket reuse issues
                .build()
        } catch (e: Exception) {
            Log.e("HA_API", "Error building OkHttpClient: ${e.localizedMessage}")
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }

    private fun buildUrl(path: String): String {
        var serverUrl = prefs.serverUrl.trim().trimEnd('/')
        if (serverUrl.isBlank()) return ""
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            serverUrl = "http://$serverUrl"
        }
        val formattedPath = if (path.startsWith("http")) path else if (path.startsWith("/")) path else "/$path"
        return if (path.startsWith("http")) path else "$serverUrl$formattedPath"
    }

    private fun executeCallOkHttp(path: String, isPost: Boolean = false, jsonBodyStr: String? = null): Pair<Int, String?> {
        val fullUrl = buildUrl(path)
        val token = prefs.token.trim()
        if (fullUrl.isBlank() || token.isBlank()) return Pair(-1, null)

        Log.d("HA_API", "OkHttp executing: $fullUrl (isPost=$isPost)")

        val maxRetries = 2
        for (attempt in 1..maxRetries) {
            try {
                val requestBuilder = Request.Builder()
                    .url(fullUrl)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 15; SM-F761B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")

                if (isPost) {
                    val bodyData = (jsonBodyStr ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType())
                    requestBuilder.post(bodyData)
                } else {
                    requestBuilder.get()
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    val code = response.code
                    val bodyStr = response.body?.string()
                    Log.d("HA_API", "OkHttp response code: $code for $fullUrl")
                    return Pair(code, bodyStr)
                }
            } catch (e: java.net.SocketTimeoutException) {
                Log.w("HA_API", "OkHttp SocketTimeoutException attempt $attempt/$maxRetries for $fullUrl: ${e.localizedMessage}")
                if (attempt >= maxRetries) {
                    Log.e("HA_API", "OkHttp timeout exception for $fullUrl after $maxRetries attempts", e)
                    return Pair(-1, null)
                }
                try { Thread.sleep(500) } catch (_: Exception) {}
            } catch (e: Exception) {
                Log.e("HA_API", "OkHttp exception for $fullUrl: ${e.localizedMessage}", e)
                return Pair(-1, null)
            }
        }
        return Pair(-1, null)
    }

    suspend fun fetchAllStatesMap(): Map<String, EntityStateResult> = withContext(Dispatchers.IO) {
        try {
            val (code, bodyStr) = executeCallOkHttp("/api/states")
            if (code !in 200..299 || bodyStr == null) {
                Log.e("HA_API", "fetchAllStatesMap failed with HTTP code: $code")
                return@withContext emptyMap()
            }
            val resultMap = mutableMapOf<String, EntityStateResult>()
            val jsonArray = JSONArray(bodyStr)
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.optJSONObject(i) ?: continue
                val entityId = json.optString("entity_id", "")
                if (entityId.isBlank()) continue
                val state = json.optString("state", "N/A")
                val attributes = json.optJSONObject("attributes")
                val unit = if (attributes?.has("unit_of_measurement") == true) attributes.optString("unit_of_measurement") else null
                val friendlyName = if (attributes?.has("friendly_name") == true) attributes.optString("friendly_name") else null
                resultMap[entityId] = EntityStateResult(
                    entityId = entityId,
                    state = state,
                    unitOfMeasurement = unit,
                    friendlyName = friendlyName
                )
            }
            Log.d("HA_API", "fetchAllStatesMap SUCCESS! Loaded ${resultMap.size} entities.")
            resultMap
        } catch (e: Exception) {
            Log.e("HA_API", "Exception in fetchAllStatesMap: ${e.localizedMessage}", e)
            emptyMap()
        }
    }

    suspend fun fetchEntityState(rawEntityId: String): EntityStateResult? = withContext(Dispatchers.IO) {
        val trimmed = rawEntityId.trim()
        if (trimmed.isBlank()) return@withContext null
        val entityId = if (!trimmed.contains(".")) "sensor.$trimmed" else trimmed
        try {
            val (code, bodyStr) = executeCallOkHttp("/api/states/$entityId")
            if (code !in 200..299 || bodyStr == null) {
                Log.e("HA_API", "fetchEntityState($entityId) failed with HTTP code: $code")
                return@withContext null
            }
            val json = JSONObject(bodyStr)
            val state = json.optString("state", "N/A")
            val attributes = json.optJSONObject("attributes")
            val unit = if (attributes?.has("unit_of_measurement") == true) attributes.optString("unit_of_measurement") else null
            val friendlyName = if (attributes?.has("friendly_name") == true) attributes.optString("friendly_name") else null
            EntityStateResult(
                entityId = entityId,
                state = state,
                unitOfMeasurement = unit,
                friendlyName = friendlyName
            )
        } catch (e: Exception) {
            Log.e("HA_API", "Exception in fetchEntityState($entityId): ${e.localizedMessage}", e)
            null
        }
    }

    suspend fun callEntityService(rawEntityId: String, actionService: String = "toggle"): Boolean = withContext(Dispatchers.IO) {
        val trimmed = rawEntityId.trim()
        if (trimmed.isBlank()) return@withContext false
        val entityId = if (!trimmed.contains(".")) "switch.$trimmed" else trimmed
        try {
            val parts = entityId.split(".", limit = 2)
            if (parts.size < 2) return@withContext false
            val domain = parts[0]
            val entityName = parts[1]

            val jsonBody = JSONObject().apply {
                put("entity_id", entityId)
            }
            val jsonStr = jsonBody.toString()

            val actualService = if (actionService == "toggle") {
                when (domain) {
                    "script" -> "turn_on"
                    "automation" -> "trigger"
                    "scene" -> "turn_on"
                    "button", "input_button" -> "press"
                    else -> "toggle"
                }
            } else {
                actionService
            }

            // Attempt 1: /api/services/domain/actualService
            val (code, _) = executeCallOkHttp("/api/services/$domain/$actualService", isPost = true, jsonBodyStr = jsonStr)
            if (code in 200..299) return@withContext true

            // Attempt 2: /api/services/homeassistant/turn_on
            val (code2, _) = executeCallOkHttp("/api/services/homeassistant/turn_on", isPost = true, jsonBodyStr = jsonStr)
            if (code2 in 200..299) return@withContext true

            // Attempt 3 for scripts
            if (domain == "script") {
                val (fallbackCode, _) = executeCallOkHttp("/api/services/script/$entityName", isPost = true, jsonBodyStr = jsonStr)
                if (fallbackCode in 200..299) return@withContext true
            }

            false
        } catch (e: Exception) {
            Log.e("HA_API", "Exception in callEntityService($entityId): ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun fetchCameraSnapshot(rawCameraEntityId: String): Bitmap? = withContext(Dispatchers.IO) {
        val cameraEntityId = rawCameraEntityId.trim().let {
            if (it.isNotBlank() && !it.contains(".")) "camera.$it" else it
        }
        if (cameraEntityId.isBlank()) return@withContext null

        val directBitmap = downloadImageFromPath("/api/camera_proxy/$cameraEntityId")
        if (directBitmap != null) return@withContext directBitmap

        try {
            val (code, bodyStr) = executeCallOkHttp("/api/states/$cameraEntityId")
            if (code in 200..299 && bodyStr != null) {
                val json = JSONObject(bodyStr)
                val attributes = json.optJSONObject("attributes")
                val entityPicture = if (attributes?.has("entity_picture") == true) attributes.optString("entity_picture") else null
                if (!entityPicture.isNullOrBlank()) {
                    return@withContext downloadImageFromPath(entityPicture)
                }
            }
        } catch (e: Exception) {
            Log.e("HA_API", "Exception in fetchCameraSnapshot($cameraEntityId): ${e.localizedMessage}", e)
        }
        null
    }

    private fun downloadImageFromPath(path: String): Bitmap? {
        val fullUrl = buildUrl(path)
        val token = prefs.token.trim()
        if (fullUrl.isBlank() || token.isBlank()) return null

        val maxRetries = 2
        for (attempt in 1..maxRetries) {
            try {
                val request = Request.Builder()
                    .url(fullUrl)
                    .header("Authorization", "Bearer $token")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 15; SM-F761B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return null
                    val bytes = response.body?.bytes() ?: return null
                    if (bytes.isEmpty()) return null
                    val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

                    val maxWidth = 400
                    val maxHeight = 250
                    return if (rawBitmap.width > maxWidth || rawBitmap.height > maxHeight) {
                        val scale = minOf(maxWidth.toFloat() / rawBitmap.width, maxHeight.toFloat() / rawBitmap.height)
                        val scaledW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
                        val scaledH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(rawBitmap, scaledW, scaledH, true)
                    } else {
                        rawBitmap
                    }
                }
            } catch (e: Exception) {
                Log.e("HA_API", "downloadImageFromPath exception for $fullUrl: ${e.localizedMessage}", e)
                if (attempt >= maxRetries) return null
                try { Thread.sleep(500) } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun probeUrl(urlStr: String): Boolean {
        return try {
            val token = prefs.token.trim()
            if (token.isBlank()) return false
            val request = Request.Builder()
                .url(urlStr)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 401
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun testConnectionDetails(): ConnectionResult = withContext(Dispatchers.IO) {
        val mainResult = try {
            val (code, _) = executeCallOkHttp("/api/")
            if (code in 200..299) {
                ConnectionResult.Success
            } else if (code == 401) {
                ConnectionResult.Unauthorized(code)
            } else {
                ConnectionResult.Error("HTTP $code")
            }
        } catch (_: Exception) {
            ConnectionResult.Timeout(prefs.serverUrl)
        }

        if (mainResult is ConnectionResult.Timeout || mainResult is ConnectionResult.Error) {
            val host = try { java.net.URI(prefs.serverUrl).host } catch (_: Exception) { null }
            if (!host.isNullOrBlank()) {
                val candidatePorts = listOf(8123, 18888, 80, 443, 8443, 30103)
                val currentPort = try { java.net.URI(prefs.serverUrl).port.let { if (it != -1) it else 80 } } catch (_: Exception) { 80 }
                for (port in candidatePorts) {
                    if (port == currentPort) continue
                    val scheme = if (port == 443 || port == 8443) "https" else "http"
                    val testUrl = "$scheme://$host:$port/api/"
                    if (probeUrl(testUrl)) {
                        val suggested = "$scheme://$host:$port"
                        return@withContext ConnectionResult.Timeout(prefs.serverUrl, suggestedUrl = suggested)
                    }
                }
            }
        }
        return@withContext mainResult
    }

    suspend fun testConnection(): Boolean {
        return testConnectionDetails() == ConnectionResult.Success
    }
}
