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
            when (buttonIndex) {
                1 -> this[HaCompositeWidget.B1_STATE_KEY] = "loading"
                2 -> this[HaCompositeWidget.B2_STATE_KEY] = "loading"
                3 -> this[HaCompositeWidget.B3_STATE_KEY] = "loading"
                4 -> this[HaCompositeWidget.B4_STATE_KEY] = "loading"
            }
        }
    }
    HaCompositeWidget().update(context, glanceId)
    Log.d("ApiPerf", "Passo 1: Set state to LOADING (RED) and triggered update(glanceId)")

    // Pausa di 350ms garantita affinché il frame ROSSO rimanga visibile a schermo
    delay(350)

    // =========================================================================
    // PASSO 2: Esecuzione del comando HTTP verso Home Assistant
    // =========================================================================
    val api = HomeAssistantApi(prefs)
    val success = api.callEntityService(entityId, "toggle")
    Log.d("ApiPerf", "Passo 2: callEntityService success=$success")

    // =========================================================================
    // PASSO 3: Applicazione immediata dello stato Invertito (BLU se "on", GRIGIO se "off")
    // =========================================================================
    val newState = if (success) targetState else previousState
    when (buttonIndex) {
        1 -> prefs.cachedButton1State = newState
        2 -> prefs.cachedButton2State = newState
        3 -> prefs.cachedButton3State = newState
        4 -> prefs.cachedButton4State = newState
    }

    updateAppWidgetState(context, glanceId) { state ->
        state.toMutablePreferences().apply {
            when (buttonIndex) {
                1 -> this[HaCompositeWidget.B1_STATE_KEY] = newState
                2 -> this[HaCompositeWidget.B2_STATE_KEY] = newState
                3 -> this[HaCompositeWidget.B3_STATE_KEY] = newState
                4 -> this[HaCompositeWidget.B4_STATE_KEY] = newState
            }
        }
    }
    HaCompositeWidget().update(context, glanceId)
    Log.d("ApiPerf", "Passo 3: Set state to $newState and triggered update(glanceId)")

    // =========================================================================
    // PASSO 4: Pausa di assestamento hardware (1000ms) e verifica finale stato HA
    // =========================================================================
    if (success) {
        delay(1000)
        val activeStates = listOf("on", "active", "playing", "true")
        val widgetStates = api.fetchWidgetStates()
        Log.d("ApiPerf", "Passo 4: fetchWidgetStates returned b1=${widgetStates.b1}, b2=${widgetStates.b2}, b3=${widgetStates.b3}, b4=${widgetStates.b4}")

        if (widgetStates.s1 != "---") {
            val s1Val = formatSensorVal(widgetStates.s1, prefs.sensor1Unit)
            val s2Val = formatSensorVal(widgetStates.s2, prefs.sensor2Unit)
            val s3Val = formatSensorVal(widgetStates.s3, prefs.sensor3Unit)

            prefs.cachedSensor1Val = s1Val
            prefs.cachedSensor2Val = s2Val
            prefs.cachedSensor3Val = s3Val

            val isButtonDomain = entityId.startsWith("button.") || entityId.startsWith("script.") || entityId.startsWith("scene.")
            val confirmedB1 = if (!isButtonDomain && activeStates.contains(widgetStates.b1.lowercase())) "on" else "off"
            val confirmedB2 = if (!isButtonDomain && activeStates.contains(widgetStates.b2.lowercase())) "on" else "off"
            val confirmedB3 = if (!isButtonDomain && activeStates.contains(widgetStates.b3.lowercase())) "on" else "off"
            val confirmedB4 = if (!isButtonDomain && activeStates.contains(widgetStates.b4.lowercase())) "on" else "off"

            prefs.cachedButton1State = confirmedB1
            prefs.cachedButton2State = confirmedB2
            prefs.cachedButton3State = confirmedB3
            prefs.cachedButton4State = confirmedB4

            updateAppWidgetState(context, glanceId) { state ->
                state.toMutablePreferences().apply {
                    this[HaCompositeWidget.S1_VAL_KEY] = s1Val
                    this[HaCompositeWidget.S2_VAL_KEY] = s2Val
                    this[HaCompositeWidget.S3_VAL_KEY] = s3Val
                    this[HaCompositeWidget.B1_STATE_KEY] = confirmedB1
                    this[HaCompositeWidget.B2_STATE_KEY] = confirmedB2
                    this[HaCompositeWidget.B3_STATE_KEY] = confirmedB3
                    this[HaCompositeWidget.B4_STATE_KEY] = confirmedB4
                }
            }
        }

        val freshCam = api.fetchCameraSnapshot(prefs.cameraEntity)
        if (freshCam != null) {
            HaCompositeWidget.saveCachedCameraBitmap(context, freshCam)
            updateAppWidgetState(context, glanceId) { state ->
                state.toMutablePreferences().apply {
                    this[HaCompositeWidget.CAM_UPDATE_KEY] = System.currentTimeMillis()
                }
            }
            Log.d("ApiPerf", "Passo 4: freshCam saved")
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
            val s1Val = formatSensorVal(widgetStates.s1, prefs.sensor1Unit)
            val s2Val = formatSensorVal(widgetStates.s2, prefs.sensor2Unit)
            val s3Val = formatSensorVal(widgetStates.s3, prefs.sensor3Unit)

            val confirmedB1 = if (activeStates.contains(widgetStates.b1.lowercase())) "on" else "off"
            val confirmedB2 = if (activeStates.contains(widgetStates.b2.lowercase())) "on" else "off"
            val confirmedB3 = if (activeStates.contains(widgetStates.b3.lowercase())) "on" else "off"
            val confirmedB4 = if (activeStates.contains(widgetStates.b4.lowercase())) "on" else "off"

            prefs.cachedSensor1Val = s1Val
            prefs.cachedSensor2Val = s2Val
            prefs.cachedSensor3Val = s3Val

            prefs.cachedButton1State = confirmedB1
            prefs.cachedButton2State = confirmedB2
            prefs.cachedButton3State = confirmedB3
            prefs.cachedButton4State = confirmedB4

            updateAppWidgetState(context, glanceId) { state ->
                state.toMutablePreferences().apply {
                    this[HaCompositeWidget.S1_VAL_KEY] = s1Val
                    this[HaCompositeWidget.S2_VAL_KEY] = s2Val
                    this[HaCompositeWidget.S3_VAL_KEY] = s3Val
                    this[HaCompositeWidget.B1_STATE_KEY] = confirmedB1
                    this[HaCompositeWidget.B2_STATE_KEY] = confirmedB2
                    this[HaCompositeWidget.B3_STATE_KEY] = confirmedB3
                    this[HaCompositeWidget.B4_STATE_KEY] = confirmedB4
                }
            }
        }
        
        val freshCam = api.fetchCameraSnapshot(prefs.cameraEntity)
        if (freshCam != null) {
            HaCompositeWidget.saveCachedCameraBitmap(context, freshCam)
            updateAppWidgetState(context, glanceId) { state ->
                state.toMutablePreferences().apply {
                    this[HaCompositeWidget.CAM_UPDATE_KEY] = System.currentTimeMillis()
                }
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
