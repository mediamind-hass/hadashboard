package com.example.myapplication.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi
import kotlinx.coroutines.delay

class Button1Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        handleButtonToggle(context, glanceId, 1)
    }
}

class Button2Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        handleButtonToggle(context, glanceId, 2)
    }
}

class Button3Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        handleButtonToggle(context, glanceId, 3)
    }
}

class Button4Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        handleButtonToggle(context, glanceId, 4)
    }
}

private suspend fun handleButtonToggle(context: Context, glanceId: GlanceId, buttonIndex: Int) {
    val prefs = HaPreferences(context)
    val entityId = when (buttonIndex) {
        1 -> prefs.button1Entity
        2 -> prefs.button2Entity
        3 -> prefs.button3Entity
        4 -> prefs.button4Entity
        else -> ""
    }

    // 1. Optimistic UI update: instantly toggle cached button state for zero-latency visual response
    when (buttonIndex) {
        1 -> prefs.cachedButton1On = !prefs.cachedButton1On
        2 -> prefs.cachedButton2On = !prefs.cachedButton2On
        3 -> prefs.cachedButton3On = !prefs.cachedButton3On
        4 -> prefs.cachedButton4On = !prefs.cachedButton4On
    }
    HaCompositeWidget.resetLastFetchTimestamp()
    HaCompositeWidget().update(context, glanceId)

    // 2. Perform network call directly in the Glance action thread
    if (entityId.isNotBlank()) {
        val api = HomeAssistantApi(prefs)
        val success = api.callEntityService(entityId, "toggle")
        
        // 3. Sync confirmed true states from Home Assistant
        if (success) {
            delay(200)
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

    // 4. Update widget UI with confirmed states
    HaCompositeWidget.resetLastFetchTimestamp()
    HaCompositeWidget().update(context, glanceId)
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = HaPreferences(context)
        val api = HomeAssistantApi(prefs)
        
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
        
        val freshCam = api.fetchCameraSnapshot(prefs.cameraEntity)
        if (freshCam != null) {
            HaCompositeWidget.saveCachedCameraBitmap(context, freshCam)
        }

        HaCompositeWidget.resetLastFetchTimestamp()
        HaCompositeWidget().update(context, glanceId)
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
