package com.example.myapplication.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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

    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

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
            client.newCall(reqBuilder.get().build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val state = json.optString("state", "N/A")
                val attributes = json.optJSONObject("attributes")
                val unit = attributes?.optString("unit_of_measurement")
                val friendlyName = attributes?.optString("friendly_name")
                EntityStateResult(entityId, state, unit, friendlyName)
            }
        } catch (e: Exception) {
            Log.w("HomeAssistantApi", "Error fetching entity state for $entityId: ${e.message}")
            null
        }
    }

    suspend fun callEntityService(rawEntityId: String, actionService: String = "toggle"): Boolean = withContext(Dispatchers.IO) {
        val trimmed = rawEntityId.trim()
        if (trimmed.isBlank()) return@withContext false
        val entityId = if (!trimmed.contains(".")) "switch.$trimmed" else trimmed
        val parts = entityId.split(".", limit = 2)
        if (parts.size < 2) return@withContext false
        val domain = parts[0]

        val serviceToCall = when (domain) {
            "button" -> "press"
            "script", "scene" -> "turn_on"
            else -> actionService
        }

        try {
            val jsonBody = JSONObject().apply { put("entity_id", entityId) }
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val reqBuilder = buildRequest("/api/services/$domain/$serviceToCall") ?: return@withContext false
            client.newCall(reqBuilder.post(requestBody).build()).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("HomeAssistantApi", "Error calling service for $entityId: ${e.message}")
            false
        }
    }

    suspend fun fetchCameraSnapshot(rawCameraEntityId: String): Bitmap? = withContext(Dispatchers.IO) {
        val cameraEntityId = rawCameraEntityId.trim().let {
            if (it.isNotBlank() && !it.contains(".")) "camera.$it" else it
        }
        if (cameraEntityId.isBlank()) return@withContext null

        try {
            val reqBuilder = buildRequest("/api/camera_proxy/$cameraEntityId") ?: return@withContext null
            client.newCall(reqBuilder.get().build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bytes = response.body?.bytes() ?: return@withContext null
                if (bytes.isEmpty()) return@withContext null
                val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null

                // Downscale for RemoteViews limit (max 400x250)
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
            Log.w("HomeAssistantApi", "Error fetching camera snapshot for $cameraEntityId: ${e.message}")
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
            client.newCall(reqBuilder.get().build()).execute().use { response ->
                if (response.isSuccessful) "OK" else "Errore HTTP ${response.code}"
            }
        } catch (e: Exception) {
            "Errore di Connessione: ${e.localizedMessage ?: e.message}"
        }
    }
}
