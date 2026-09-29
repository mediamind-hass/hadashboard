package com.example.myapplication.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
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

    Log.d("ApiPerf", ">>> TAP Action on Button $buttonIndex ($entityId)")
    if (entityId.isBlank()) return

    val previousState = when (buttonIndex) {
        1 -> prefs.cachedButton1State
        2 -> prefs.cachedButton2State
        3 -> prefs.cachedButton3State
        4 -> prefs.cachedButton4State
        else -> "off"
    }
    val targetState = if (previousState == "on") "off" else "on"
    Log.d("ApiPerf", "Previous state: $previousState -> Target state: $targetState")

    // =========================================================================
    // PASSO 1: Riscontro visivo immediato a 0 ms (Pulsante ROSSO "loading")
    // =========================================================================
    when (buttonIndex) {
        1 -> prefs.cachedButton1State = "loading"
        2 -> prefs.cachedButton2State = "loading"
        3 -> prefs.cachedButton3State = "loading"
        4 -> prefs.cachedButton4State = "loading"
    }

    updateAppWidgetState(context, glanceId) { state ->
        state.toMutablePreferences().apply {
            this[HaCompositeWidget.UPDATE_TIME_KEY] = System.currentTimeMillis()
        }
    }
    HaCompositeWidget().update(context, glanceId)
    Log.d("ApiPerf", "Passo 1: Set state to LOADING (RED) and triggered update(glanceId)")

    // =========================================================================
    // PASSO 2: Esecuzione del comando HTTP verso Home Assistant
    // =========================================================================
    val api = HomeAssistantApi(prefs)
    val success = api.callEntityService(entityId, "toggle")
    Log.d("ApiPerf", "Passo 2: callEntityService success=$success")

    // =========================================================================
    // PASSO 3: Applicazione immediata dello stato Invertito (BLU se "on", GRIGIO se "off")
    // =========================================================================
    if (success) {
        when (buttonIndex) {
            1 -> prefs.cachedButton1State = targetState
            2 -> prefs.cachedButton2State = targetState
            3 -> prefs.cachedButton3State = targetState
            4 -> prefs.cachedButton4State = targetState
        }
    } else {
        when (buttonIndex) {
            1 -> prefs.cachedButton1State = previousState
            2 -> prefs.cachedButton2State = previousState
            3 -> prefs.cachedButton3State = previousState
            4 -> prefs.cachedButton4State = previousState
        }
    }

    updateAppWidgetState(context, glanceId) { state ->
        state.toMutablePreferences().apply {
            this[HaCompositeWidget.UPDATE_TIME_KEY] = System.currentTimeMillis()
        }
    }
    HaCompositeWidget().update(context, glanceId)
    Log.d("ApiPerf", "Passo 3: Set state to $targetState and triggered update(glanceId)")

    // =========================================================================
    // PASSO 4: Pausa di assestamento hardware (1000ms) e verifica finale stato HA
    // =========================================================================
    if (success) {
        delay(1000)
        val activeStates = listOf("on", "active", "playing", "true")
        val widgetStates = api.fetchWidgetStates()
        Log.d("ApiPerf", "Passo 4: fetchWidgetStates returned b1=${widgetStates.b1}, b2=${widgetStates.b2}, b3=${widgetStates.b3}, b4=${widgetStates.b4}")

        if (widgetStates.s1 != "---") {
            prefs.cachedSensor1Val = formatSensorVal(widgetStates.s1, prefs.sensor1Unit)
            prefs.cachedSensor2Val = formatSensorVal(widgetStates.s2, prefs.sensor2Unit)
            prefs.cachedSensor3Val = formatSensorVal(widgetStates.s3, prefs.sensor3Unit)

            val isButtonDomain = entityId.startsWith("button.") || entityId.startsWith("script.") || entityId.startsWith("scene.")
            if (!isButtonDomain) {
                prefs.cachedButton1State = if (activeStates.contains(widgetStates.b1.lowercase())) "on" else "off"
                prefs.cachedButton2State = if (activeStates.contains(widgetStates.b2.lowercase())) "on" else "off"
                prefs.cachedButton3State = if (activeStates.contains(widgetStates.b3.lowercase())) "on" else "off"
                prefs.cachedButton4State = if (activeStates.contains(widgetStates.b4.lowercase())) "on" else "off"
            } else {
                when (buttonIndex) {
                    1 -> prefs.cachedButton1State = "off"
                    2 -> prefs.cachedButton2State = "off"
                    3 -> prefs.cachedButton3State = "off"
                    4 -> prefs.cachedButton4State = "off"
                }
            }
        }

        val freshCam = api.fetchCameraSnapshot(prefs.cameraEntity)
        if (freshCam != null) {
            HaCompositeWidget.saveCachedCameraBitmap(context, freshCam)
            Log.d("ApiPerf", "Passo 4: freshCam saved")
        }

        updateAppWidgetState(context, glanceId) { state ->
            state.toMutablePreferences().apply {
                this[HaCompositeWidget.UPDATE_TIME_KEY] = System.currentTimeMillis()
            }
        }
        HaCompositeWidget().update(context, glanceId)
        Log.d("ApiPerf", "Passo 4: Final update(glanceId) complete")
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("ApiPerf", ">>> TAP Action on RefreshWidgetAction (🔄)")
        val prefs = HaPreferences(context)
        val api = HomeAssistantApi(prefs)
        
        api.testConnectionDetailed()
        
        val widgetStates = api.fetchWidgetStates()
        val activeStates = listOf("on", "active", "playing", "true")
        if (widgetStates.s1 != "---") {
            prefs.cachedSensor1Val = formatSensorVal(widgetStates.s1, prefs.sensor1Unit)
            prefs.cachedSensor2Val = formatSensorVal(widgetStates.s2, prefs.sensor2Unit)
            prefs.cachedSensor3Val = formatSensorVal(widgetStates.s3, prefs.sensor3Unit)

            prefs.cachedButton1State = if (activeStates.contains(widgetStates.b1.lowercase())) "on" else "off"
            prefs.cachedButton2State = if (activeStates.contains(widgetStates.b2.lowercase())) "on" else "off"
            prefs.cachedButton3State = if (activeStates.contains(widgetStates.b3.lowercase())) "on" else "off"
            prefs.cachedButton4State = if (activeStates.contains(widgetStates.b4.lowercase())) "on" else "off"
        }
        
        val freshCam = api.fetchCameraSnapshot(prefs.cameraEntity)
        if (freshCam != null) {
            HaCompositeWidget.saveCachedCameraBitmap(context, freshCam)
        }

        updateAppWidgetState(context, glanceId) { state ->
            state.toMutablePreferences().apply {
                this[HaCompositeWidget.UPDATE_TIME_KEY] = System.currentTimeMillis()
            }
        }
        HaCompositeWidget().update(context, glanceId)
        Log.d("ApiPerf", "RefreshWidgetAction complete")
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
