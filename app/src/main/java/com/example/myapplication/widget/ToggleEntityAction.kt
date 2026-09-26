package com.example.myapplication.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi

class Button1Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ToggleEntityWorker.enqueue(context, 1)
    }
}

class Button2Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ToggleEntityWorker.enqueue(context, 2)
    }
}

class Button3Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ToggleEntityWorker.enqueue(context, 3)
    }
}

class Button4Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ToggleEntityWorker.enqueue(context, 4)
    }
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
        
        api.fetchCameraSnapshot(prefs.cameraEntity)
        HaCompositeWidget().updateAll(context)
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
