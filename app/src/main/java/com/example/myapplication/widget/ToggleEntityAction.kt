package com.example.myapplication.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi

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

    if (entityId.isBlank()) return

    val api = HomeAssistantApi(prefs)
    val activeStates = listOf("on", "active", "playing", "true")

    // =========================================================================
    // PASSO 1: Leggo lo stato attuale da Home Assistant
    // =========================================================================
    val initialStates = api.fetchWidgetStates()
    if (initialStates.s1 != "---") {
        prefs.cachedSensor1Val = formatSensorVal(initialStates.s1, prefs.sensor1Unit)
        prefs.cachedSensor2Val = formatSensorVal(initialStates.s2, prefs.sensor2Unit)
        prefs.cachedSensor3Val = formatSensorVal(initialStates.s3, prefs.sensor3Unit)
        prefs.cachedButton1State = if (activeStates.contains(initialStates.b1.lowercase())) "on" else "off"
        prefs.cachedButton2State = if (activeStates.contains(initialStates.b2.lowercase())) "on" else "off"
        prefs.cachedButton3State = if (activeStates.contains(initialStates.b3.lowercase())) "on" else "off"
        prefs.cachedButton4State = if (activeStates.contains(initialStates.b4.lowercase())) "on" else "off"
    }

    val currentButtonState = when (buttonIndex) {
        1 -> prefs.cachedButton1State
        2 -> prefs.cachedButton2State
        3 -> prefs.cachedButton3State
        4 -> prefs.cachedButton4State
        else -> "off"
    }

    // =========================================================================
    // PASSO 2: Inverto lo stato grafico e aggiorno subito il Widget (BLU/GRIGIO)
    // =========================================================================
    val invertedState = if (currentButtonState == "off") "on" else "off"
    when (buttonIndex) {
        1 -> prefs.cachedButton1State = invertedState
        2 -> prefs.cachedButton2State = invertedState
        3 -> prefs.cachedButton3State = invertedState
        4 -> prefs.cachedButton4State = invertedState
    }

    updateAppWidgetState(context, glanceId) { state ->
        state.toMutablePreferences().apply {
            this[HaCompositeWidget.UPDATE_TIME_KEY] = System.currentTimeMillis()
        }
    }
    HaCompositeWidget().update(context, glanceId)

    // =========================================================================
    // PASSO 3: Eseguo il comando verso Home Assistant e aggiornato la Cam
    // =========================================================================
    val success = api.callEntityService(entityId, "toggle")

    if (!success) {
        // Se il comando di rete fallisce, ripristino lo stato grafico precedente
        when (buttonIndex) {
            1 -> prefs.cachedButton1State = currentButtonState
            2 -> prefs.cachedButton2State = currentButtonState
            3 -> prefs.cachedButton3State = currentButtonState
            4 -> prefs.cachedButton4State = currentButtonState
        }
    }

    // Download dello snapshot fotocamera aggiornato
    val freshCam = api.fetchCameraSnapshot(prefs.cameraEntity)
    if (freshCam != null) {
        HaCompositeWidget.saveCachedCameraBitmap(context, freshCam)
    }

    // Aggiornamento grafico finale di conferma
    updateAppWidgetState(context, glanceId) { state ->
        state.toMutablePreferences().apply {
            this[HaCompositeWidget.UPDATE_TIME_KEY] = System.currentTimeMillis()
        }
    }
    HaCompositeWidget().update(context, glanceId)
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = HaPreferences(context)
        val api = HomeAssistantApi(prefs)
        
        // Full "Salva e Verifica" pipeline on Reload (🔄) button
        api.testConnectionDetailed() // Wakes up socket / VPN / portforward route
        
        val widgetStates = api.fetchWidgetStates()
        val activeStates = listOf("on", "active", "playing", "true")
        if (widgetStates.s1 != "---") {
            prefs.cachedSensor1Val = formatSensorVal(widgetStates.s1, prefs.sensor1Unit)
            prefs.cachedSensor2Val = formatSensorVal(widgetStates.s2, prefs.sensor2Unit)
            prefs.cachedSensor3Val = formatSensorVal(widgetStates.s3, prefs.sensor3Unit)

            // Update verified button states: "on" -> BLUE, "off" -> GREY
            prefs.cachedButton1State = if (activeStates.contains(widgetStates.b1.lowercase())) "on" else "off"
            prefs.cachedButton2State = if (activeStates.contains(widgetStates.b2.lowercase())) "on" else "off"
            prefs.cachedButton3State = if (activeStates.contains(widgetStates.b3.lowercase())) "on" else "off"
            prefs.cachedButton4State = if (activeStates.contains(widgetStates.b4.lowercase())) "on" else "off"
        }
        
        val freshCam = api.fetchCameraSnapshot(prefs.cameraEntity)
        if (freshCam != null) {
            HaCompositeWidget.saveCachedCameraBitmap(context, freshCam)
        }

        // Single clean Glance update
        updateAppWidgetState(context, glanceId) { state ->
            state.toMutablePreferences().apply {
                this[HaCompositeWidget.UPDATE_TIME_KEY] = System.currentTimeMillis()
            }
        }
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
