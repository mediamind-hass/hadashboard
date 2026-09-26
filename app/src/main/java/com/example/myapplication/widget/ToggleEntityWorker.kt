package com.example.myapplication.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi
import kotlinx.coroutines.delay

class ToggleEntityWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val KEY_BUTTON_INDEX = "button_index"

        fun enqueue(context: Context, buttonIndex: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val data = Data.Builder()
                .putInt(KEY_BUTTON_INDEX, buttonIndex)
                .build()
            val request = OneTimeWorkRequestBuilder<ToggleEntityWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val buttonIndex = inputData.getInt(KEY_BUTTON_INDEX, 0)
            val prefs = HaPreferences(applicationContext)
            val entityId = when (buttonIndex) {
                1 -> prefs.button1Entity
                2 -> prefs.button2Entity
                3 -> prefs.button3Entity
                4 -> prefs.button4Entity
                else -> ""
            }

            // 1. Optimistic UI update: instantly toggle cached state for zero-latency feedback on 5G
            when (buttonIndex) {
                1 -> prefs.cachedButton1On = !prefs.cachedButton1On
                2 -> prefs.cachedButton2On = !prefs.cachedButton2On
                3 -> prefs.cachedButton3On = !prefs.cachedButton3On
                4 -> prefs.cachedButton4On = !prefs.cachedButton4On
            }
            HaCompositeWidget.resetLastFetchTimestamp()
            HaCompositeWidget().updateAll(applicationContext)

            // 2. Perform remote service call
            if (entityId.isNotBlank()) {
                val api = HomeAssistantApi(prefs)
                val success = api.callEntityService(entityId, "toggle")
                delay(300)

                // 3. Fetch true updated states from HA in 1 fast request to ensure accuracy
                if (success) {
                    val widgetStates = api.fetchWidgetStates()
                    val activeStates = listOf("on", "active", "playing", "true")
                    if (widgetStates.s1 != "---") {
                        prefs.cachedSensor1Val = formatSensorVal(widgetStates.s1, prefs.sensor1Unit)
                        prefs.cachedSensor2Val = formatSensorVal(widgetStates.s2, prefs.sensor2Unit)
                        prefs.cachedSensor3Val = formatSensorVal(widgetStates.s3, prefs.sensor3Unit)
                        prefs.cachedButton1On = activeStates.contains(widgetStates.b1.lowercase())
                        prefs.cachedButton2On = activeStates.contains(widgetStates.b2.lowercase())
                        prefs.cachedButton3On = activeStates.contains(widgetStates.b3.lowercase())
                        prefs.cachedButton4On = activeStates.contains(widgetStates.b4.lowercase())
                    }
                }
            }
            HaCompositeWidget.resetLastFetchTimestamp()
            HaCompositeWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun formatSensorVal(raw: String?, unit: String): String {
        if (raw == null || raw == "N/A" || raw == "unavailable") return "---"
        val trimmedRaw = raw.trim()
        val trimmedUnit = unit.trim()
        if (trimmedUnit.isEmpty()) return trimmedRaw
        if (trimmedRaw.endsWith(trimmedUnit, ignoreCase = true)) return trimmedRaw
        return "$trimmedRaw $trimmedUnit"
    }
}
