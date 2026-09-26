package com.example.myapplication.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
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

data class WidgetStatesResult(
    val s1: String = "---",
    val s2: String = "---",
    val s3: String = "---",
    val b1: String = "off",
    val b2: String = "off",
    val b3: String = "off",
    val b4: String = "off"
)

class HomeAssistantApi(private val prefs: HaPreferences) {

    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 1, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .dispatcher(Dispatcher().apply {
                maxRequests = 32
                maxRequestsPerHost = 16
            })
            .build()

        // Dedicated fast-failing client for cameras with fresh connections for mobile/foldable network switches
        private val cameraClient = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
            .retryOnConnectionFailure(true)
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

    private fun formatEntity(raw: String, defaultDomain: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "$defaultDomain.unknown"
        return if (!trimmed.contains(".")) "$defaultDomain.$trimmed" else trimmed
    }

    /**
     * Fetches all widget entity states in a single request using Home Assistant's Template API (/api/template).
     * Includes automatic retry on cold-start background VPN block.
     */
    suspend fun fetchWidgetStates(): WidgetStatesResult = withContext(Dispatchers.IO) {
        val maxAttempts = 2
        for (attempt in 0 until maxAttempts) {
            val start = System.currentTimeMillis()
            try {
                val reqBuilder = buildRequest("/api/template") ?: return@withContext WidgetStatesResult()
                
                val s1Id = formatEntity(prefs.sensor1Entity, "sensor")
                val s2Id = formatEntity(prefs.sensor2Entity, "sensor")
                val s3Id = formatEntity(prefs.sensor3Entity, "sensor")
                val b1Id = formatEntity(prefs.button1Entity, "switch")
                val b2Id = formatEntity(prefs.button2Entity, "switch")
                val b3Id = formatEntity(prefs.button3Entity, "switch")
                val b4Id = formatEntity(prefs.button4Entity, "switch")

                val templateStr = """
                    {
                      "s1": "{{ states('$s1Id') }}",
                      "s2": "{{ states('$s2Id') }}",
                      "s3": "{{ states('$s3Id') }}",
                      "b1": "{{ states('$b1Id') }}",
                      "b2": "{{ states('$b2Id') }}",
                      "b3": "{{ states('$b3Id') }}",
                      "b4": "{{ states('$b4Id') }}"
                    }
                """.trimIndent()

                val jsonBody = JSONObject().apply { put("template", templateStr) }
                val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val result = client.newCall(reqBuilder.post(requestBody).build()).execute().use { response ->
                    val duration = System.currentTimeMillis() - start
                    if (!response.isSuccessful) {
                        Log.w("ApiPerf", "fetchWidgetStates failed: HTTP ${response.code} in ${duration}ms")
                        null
                    } else {
                        val bodyStr = response.body?.string() ?: return@use null
                        val json = JSONObject(bodyStr)
                        Log.d("ApiPerf", "fetchWidgetStates succeeded in ${duration}ms")
                        WidgetStatesResult(
                            s1 = json.optString("s1", "---"),
                            s2 = json.optString("s2", "---"),
                            s3 = json.optString("s3", "---"),
                            b1 = json.optString("b1", "off"),
                            b2 = json.optString("b2", "off"),
                            b3 = json.optString("b3", "off"),
                            b4 = json.optString("b4", "off")
                        )
                    }
                }
                if (result != null) return@withContext result
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - start
                Log.w("ApiPerf", "fetchWidgetStates attempt ${attempt + 1} failed: ${e.message} in ${duration}ms")
            }
            if (attempt < maxAttempts - 1) {
                try { Thread.sleep(1000) } catch (_: InterruptedException) {}
            }
        }
        WidgetStatesResult()
    }

    suspend fun fetchEntityState(rawEntityId: String): EntityStateResult? = withContext(Dispatchers.IO) {
        val entityId = formatEntity(rawEntityId, "sensor")
        val maxAttempts = 2
        for (attempt in 0 until maxAttempts) {
            val start = System.currentTimeMillis()
            try {
                val reqBuilder = buildRequest("/api/states/$entityId") ?: return@withContext null
                val result = client.newCall(reqBuilder.get().build()).execute().use { response ->
                    val duration = System.currentTimeMillis() - start
                    if (!response.isSuccessful) {
                        Log.w("ApiPerf", "fetchEntityState $entityId failed: HTTP ${response.code} in ${duration}ms")
                        null
                    } else {
                        val bodyStr = response.body?.string() ?: return@use null
                        val json = JSONObject(bodyStr)
                        val state = json.optString("state", "N/A")
                        val attributes = json.optJSONObject("attributes")
                        val unit = attributes?.optString("unit_of_measurement")
                        val friendlyName = attributes?.optString("friendly_name")
                        Log.d("ApiPerf", "fetchEntityState $entityId succeeded in ${duration}ms (state: $state)")
                        EntityStateResult(entityId, state, unit, friendlyName)
                    }
                }
                if (result != null) return@withContext result
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - start
                Log.w("ApiPerf", "fetchEntityState $entityId attempt ${attempt + 1} failed: ${e.message} after ${duration}ms")
            }
            if (attempt < maxAttempts - 1) {
                try { Thread.sleep(500) } catch (_: InterruptedException) {}
            }
        }
        null
    }

    suspend fun callEntityService(rawEntityId: String, actionService: String = "toggle"): Boolean = withContext(Dispatchers.IO) {
        val maxAttempts = 2
        val entityId = formatEntity(rawEntityId, "switch")
        val parts = entityId.split(".", limit = 2)
        if (parts.size < 2) return@withContext false
        val domain = parts[0]

        val serviceToCall = when (domain) {
            "button" -> "press"
            "script", "scene" -> "turn_on"
            else -> actionService
        }

        for (attempt in 0 until maxAttempts) {
            val start = System.currentTimeMillis()
            try {
                Log.d("ApiPerf", "callEntityService started for rawEntityId: $rawEntityId (attempt ${attempt + 1})")
                Log.d("ApiPerf", "callEntityService domain: $domain, serviceToCall: $serviceToCall, entityId: $entityId")
                val jsonBody = JSONObject().apply { put("entity_id", entityId) }
                val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val reqBuilder = buildRequest("/api/services/$domain/$serviceToCall") ?: return@withContext false
                
                Log.d("ApiPerf", "callEntityService executing network call")
                val isSuccess = client.newCall(reqBuilder.post(requestBody).build()).execute().use { response ->
                    val duration = System.currentTimeMillis() - start
                    Log.d("ApiPerf", "callEntityService result: ${response.isSuccessful} in ${duration}ms")
                    response.isSuccessful
                }
                if (isSuccess) return@withContext true
            } catch (e: Throwable) {
                val duration = System.currentTimeMillis() - start
                Log.w("ApiPerf", "callEntityService attempt ${attempt + 1} failed (${e.javaClass.simpleName}): ${e.message} after ${duration}ms")
            }
            if (attempt < maxAttempts - 1) {
                try { Thread.sleep(500) } catch (_: InterruptedException) {}
            }
        }
        false
    }

    suspend fun fetchCameraSnapshot(rawCameraEntityId: String): Bitmap? = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val cameraEntityId = formatEntity(rawCameraEntityId, "camera")
        if (cameraEntityId.isBlank()) return@withContext null

        try {
            val reqBuilder = buildRequest("/api/camera_proxy/$cameraEntityId") ?: return@withContext null
            cameraClient.newCall(reqBuilder.get().build()).execute().use { response ->
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
            Log.w("ApiPerf", "fetchCameraSnapshot $cameraEntityId skipped/unavailable (${e.javaClass.simpleName}) after ${duration}ms")
            null
        }
    }

    suspend fun testConnectionDetailed(): String = withContext(Dispatchers.IO) {
        val maxAttempts = 2
        for (attempt in 0 until maxAttempts) {
            val start = System.currentTimeMillis()
            val serverUrl = prefs.serverUrl
            val token = prefs.token
            if (serverUrl.isBlank()) return@withContext "URL Server vuoto"
            if (token.isBlank()) return@withContext "Token Bearer vuoto"

            try {
                val reqBuilder = buildRequest("/api/") ?: return@withContext "URL non valido"
                val resMsg = client.newCall(reqBuilder.get().build()).execute().use { response ->
                    val duration = System.currentTimeMillis() - start
                    if (response.isSuccessful) {
                        Log.d("ApiPerf", "testConnectionDetailed succeeded in ${duration}ms")
                        "OK"
                    } else {
                        Log.w("ApiPerf", "testConnectionDetailed failed: HTTP ${response.code} in ${duration}ms")
                        null
                    }
                }
                if (resMsg == "OK") return@withContext "OK"
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - start
                Log.w("ApiPerf", "testConnectionDetailed attempt ${attempt + 1} failed: ${e.message} in ${duration}ms")
            }
            if (attempt < maxAttempts - 1) {
                try { Thread.sleep(1000) } catch (_: InterruptedException) {}
            }
        }
        "Errore di Connessione"
    }
}
