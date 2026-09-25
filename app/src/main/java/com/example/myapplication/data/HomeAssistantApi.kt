package com.example.myapplication.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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
            .dispatcher(Dispatcher().apply {
                maxRequests = 32
                maxRequestsPerHost = 16
            })
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

    /**
     * Fetches all entity states in a single request (/api/states),
     * matching Home Assistant API best practices for dashboards and apps.
     */
    suspend fun fetchAllStates(): Map<String, EntityStateResult> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val result = mutableMapOf<String, EntityStateResult>()
        try {
            val reqBuilder = buildRequest("/api/states") ?: return@withContext result
            client.newCall(reqBuilder.get().build()).execute().use { response ->
                val duration = System.currentTimeMillis() - start
                if (!response.isSuccessful) {
                    Log.w("ApiPerf", "fetchAllStates failed: HTTP ${response.code} in ${duration}ms")
                    return@withContext result
                }
                val bodyStr = response.body?.string() ?: return@withContext result
                val jsonArray = JSONArray(bodyStr)
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    val entityId = json.optString("entity_id")
                    val state = json.optString("state", "N/A")
                    val attributes = json.optJSONObject("attributes")
                    val unit = attributes?.optString("unit_of_measurement")
                    val friendlyName = attributes?.optString("friendly_name")
                    if (entityId.isNotBlank()) {
                        result[entityId] = EntityStateResult(entityId, state, unit, friendlyName)
                    }
                }
                Log.d("ApiPerf", "fetchAllStates succeeded in ${duration}ms (${result.size} entities loaded)")
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            Log.e("ApiPerf", "fetchAllStates threw ${e.javaClass.simpleName}: ${e.message} after ${duration}ms")
        }
        result
    }

    suspend fun fetchEntityState(rawEntityId: String): EntityStateResult? = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val trimmed = rawEntityId.trim()
        if (trimmed.isBlank()) return@withContext null
        val entityId = if (!trimmed.contains(".")) "sensor.$trimmed" else trimmed
        try {
            val reqBuilder = buildRequest("/api/states/$entityId") ?: return@withContext null
            client.newCall(reqBuilder.get().build()).execute().use { response ->
                val duration = System.currentTimeMillis() - start
                if (!response.isSuccessful) {
                    Log.w("ApiPerf", "fetchEntityState $entityId failed: HTTP ${response.code} in ${duration}ms")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val state = json.optString("state", "N/A")
                val attributes = json.optJSONObject("attributes")
                val unit = attributes?.optString("unit_of_measurement")
                val friendlyName = attributes?.optString("friendly_name")
                Log.d("ApiPerf", "fetchEntityState $entityId succeeded in ${duration}ms (state: $state)")
                EntityStateResult(entityId, state, unit, friendlyName)
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            Log.e("ApiPerf", "fetchEntityState $entityId threw ${e.javaClass.simpleName}: ${e.message} after ${duration}ms")
            null
        }
    }

    suspend fun callEntityService(rawEntityId: String, actionService: String = "toggle"): Boolean = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
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
            client.newCall(reqBuilder.post(requestBody).build()).execute().use { response ->
                val duration = System.currentTimeMillis() - start
                Log.d("ApiPerf", "callEntityService $domain/$serviceToCall for $entityId result: ${response.isSuccessful} in ${duration}ms")
                response.isSuccessful
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            Log.e("ApiPerf", "callEntityService for $entityId threw ${e.javaClass.simpleName}: ${e.message} after ${duration}ms")
            false
        }
    }

    suspend fun fetchCameraSnapshot(rawCameraEntityId: String): Bitmap? = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val cameraEntityId = rawCameraEntityId.trim().let {
            if (it.isNotBlank() && !it.contains(".")) "camera.$it" else it
        }
        if (cameraEntityId.isBlank()) return@withContext null

        try {
            val reqBuilder = buildRequest("/api/camera_proxy/$cameraEntityId") ?: return@withContext null
            client.newCall(reqBuilder.get().build()).execute().use { response ->
                val duration = System.currentTimeMillis() - start
                if (!response.isSuccessful) {
                    Log.w("ApiPerf", "fetchCameraSnapshot $cameraEntityId failed: HTTP ${response.code} in ${duration}ms")
                    return@withContext null
                }
                val bytes = response.body?.bytes() ?: return@withContext null
                if (bytes.isEmpty()) {
                    Log.w("ApiPerf", "fetchCameraSnapshot $cameraEntityId empty bytes in ${duration}ms")
                    return@withContext null
                }
                val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
                Log.d("ApiPerf", "fetchCameraSnapshot $cameraEntityId succeeded in ${duration}ms (${bytes.size} bytes)")

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
            val duration = System.currentTimeMillis() - start
            Log.e("ApiPerf", "fetchCameraSnapshot $cameraEntityId threw ${e.javaClass.simpleName}: ${e.message} after ${duration}ms")
            null
        }
    }

    suspend fun testConnectionDetailed(): String = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val serverUrl = prefs.serverUrl
        val token = prefs.token
        if (serverUrl.isBlank()) return@withContext "URL Server vuoto"
        if (token.isBlank()) return@withContext "Token Bearer vuoto"

        try {
            val reqBuilder = buildRequest("/api/") ?: return@withContext "URL non valido"
            client.newCall(reqBuilder.get().build()).execute().use { response ->
                val duration = System.currentTimeMillis() - start
                if (response.isSuccessful) {
                    Log.d("ApiPerf", "testConnectionDetailed succeeded in ${duration}ms")
                    "OK"
                } else {
                    Log.w("ApiPerf", "testConnectionDetailed failed: HTTP ${response.code} in ${duration}ms")
                    "Errore HTTP ${response.code}"
                }
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            Log.e("ApiPerf", "testConnectionDetailed threw ${e.javaClass.simpleName}: ${e.message} after ${duration}ms")
            "Errore di Connessione: ${e.localizedMessage ?: e.message}"
        }
    }
}
