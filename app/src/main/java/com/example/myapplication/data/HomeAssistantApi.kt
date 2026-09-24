package com.example.myapplication.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class EntityStateResult(
    val entityId: String,
    val state: String,
    val unitOfMeasurement: String? = null,
    val friendlyName: String? = null
)

class HomeAssistantApi(private val prefs: HaPreferences) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private fun buildRequest(path: String): Request.Builder? {
        val serverUrl = prefs.serverUrl
        val token = prefs.token
        if (serverUrl.isBlank() || token.isBlank()) return null

        val url = if (path.startsWith("http")) path else "$serverUrl$path"
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
    }

    suspend fun fetchEntityState(rawEntityId: String): EntityStateResult? = withContext(Dispatchers.IO) {
        val trimmed = rawEntityId.trim()
        if (trimmed.isBlank()) return@withContext null
        val entityId = if (!trimmed.contains(".")) "sensor.$trimmed" else trimmed
        try {
            val reqBuilder = buildRequest("/api/states/$entityId") ?: return@withContext null
            val response = client.newCall(reqBuilder.get().build()).execute()
            response.use {
                if (!it.isSuccessful) return@withContext null
                val bodyStr = it.body?.string() ?: return@withContext null
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

            val jsonBody = JSONObject().apply {
                put("entity_id", entityId)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            val reqBuilder = buildRequest("/api/services/$domain/$actionService") ?: return@withContext false
            val response = client.newCall(reqBuilder.post(requestBody).build()).execute()
            response.use { it.isSuccessful }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchCameraSnapshot(rawCameraEntityId: String): Bitmap? = withContext(Dispatchers.IO) {
        val cameraEntityId = rawCameraEntityId.trim().let {
            if (it.isNotBlank() && !it.contains(".")) "camera.$it" else it
        }
        if (cameraEntityId.isBlank()) return@withContext null

        // Attempt 1: Direct /api/camera_proxy/$cameraEntityId
        val directBitmap = downloadImageFromPath("/api/camera_proxy/$cameraEntityId")
        if (directBitmap != null) return@withContext directBitmap

        // Attempt 2: Fetch /api/states/$cameraEntityId to read 'entity_picture' attribute
        try {
            val reqBuilder = buildRequest("/api/states/$cameraEntityId") ?: return@withContext null
            val response = client.newCall(reqBuilder.get().build()).execute()
            response.use {
                if (it.isSuccessful) {
                    val bodyStr = it.body?.string() ?: return@use
                    val json = JSONObject(bodyStr)
                    val attributes = json.optJSONObject("attributes")
                    val entityPicture = if (attributes?.has("entity_picture") == true) attributes.optString("entity_picture") else null
                    if (!entityPicture.isNullOrBlank()) {
                        return@withContext downloadImageFromPath(entityPicture)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    private fun downloadImageFromPath(path: String): Bitmap? {
        return try {
            val reqBuilder = buildRequest(path) ?: return null
            val response = client.newCall(reqBuilder.get().build()).execute()
            response.use {
                if (!it.isSuccessful) return null
                val bytes = it.body?.bytes() ?: return null
                if (bytes.isEmpty()) return null
                val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

                // Downscale bitmap for RemoteViews Binder transaction limits (max 400x250)
                val maxWidth = 400
                val maxHeight = 250
                if (rawBitmap.width > maxWidth || rawBitmap.height > maxHeight) {
                    val scale = minOf(maxWidth.toFloat() / rawBitmap.width, maxHeight.toFloat() / rawBitmap.height)
                    val scaledW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
                    val scaledH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(rawBitmap, scaledW, scaledH, true)
                } else {
                    rawBitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun testConnectionDetailed(): String = withContext(Dispatchers.IO) {
        val serverUrl = prefs.serverUrl
        val token = prefs.token
        if (serverUrl.isBlank()) return@withContext "URL Server vuoto"
        if (token.isBlank()) return@withContext "Token Bearer vuoto"

        try {
            val reqBuilder = buildRequest("/api/") ?: return@withContext "URL non valido"
            val response = client.newCall(reqBuilder.get().build()).execute()
            response.use {
                if (it.isSuccessful) {
                    "OK"
                } else {
                    "Errore HTTP ${it.code}: ${it.message}"
                }
            }
        } catch (e: Exception) {
            "Eccezione di Rete: ${e.localizedMessage ?: e.message}"
        }
    }
}
