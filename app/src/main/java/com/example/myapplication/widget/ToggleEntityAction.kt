package com.example.myapplication.widget

import android.content.Context
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

    // Determine current state before toggle to calculate intended target state
    val previousState = when (buttonIndex) {
        1 -> prefs.cachedButton1State
        2 -> prefs.cachedButton2State
        3 -> prefs.cachedButton3State
        4 -> prefs.cachedButton4State
        else -> "off"
    }
    val targetState = if (previousState == "on") "off" else "on"

    // 1. Instant visual feedback upon tap: highlight pressed button in RED ("loading" state)
    when (buttonIndex) {
        1 -> prefs.cachedButton1State = "loading"
        2 -> prefs.cachedButton2State = "loading"
        3 -> prefs.cachedButton3State = "loading"
        4 -> prefs.cachedButton4State = "loading"
    }
    
    // Notify Glance State DataStore -> Renders RED background immediately on button tap
    updateAppWidgetState(context, glanceId) { state ->
        state.toMutablePreferences().apply {
            this[HaCompositeWidget.UPDATE_TIME_KEY] = System.currentTimeMillis()
        }
    }
    HaCompositeWidget().update(context, glanceId)

    // 2. Perform network service call and run the "Salva e Verifica" pipeline
    if (entityId.isNotBlank()) {
        val api = HomeAssistantApi(prefs)
        
        // 2a. Execute service toggle command
        val success = api.callEntityService(entityId, "toggle")
        
        if (success) {
            // Apply intended target state immediately (BLUE if ON, GREY if OFF)
            when (buttonIndex) {
                1 -> prefs.cachedButton1State = targetState
                2 -> prefs.cachedButton2State = targetState
                3 -> prefs.cachedButton3State = targetState
                4 -> prefs.cachedButton4State = targetState
            }
            updateAppWidgetState(context, glanceId) { state ->
                state.toMutablePreferences().apply {
                    this[HaCompositeWidget.UPDATE_TIME_KEY] = System.currentTimeMillis()
                }
            }
            HaCompositeWidget().update(context, glanceId)

            // 2b. Wait 800ms for HA integration database state to settle, then verify with HA
            delay(800)
            api.testConnectionDetailed() // Wakes up socket / VPN / portforward route
            
            val widgetStates = api.fetchWidgetStates()
            val activeStates = listOf("on", "active", "playing", "true")
            if (widgetStates.s1 != "---") {
                prefs.cachedSensor1Val = formatSensorVal(widgetStates.s1, prefs.sensor1Unit)
                prefs.cachedSensor2Val = formatSensorVal(widgetStates.s2, prefs.sensor2Unit)
                prefs.cachedSensor3Val = formatSensorVal(widgetStates.s3, prefs.sensor3Unit)

                // Confirm verified button states: "on" -> BLUE, "off" -> GREY
                prefs.cachedButton1State = if (activeStates.contains(widgetStates.b1.lowercase())) "on" else "off"
                prefs.cachedButton2State = if (activeStates.contains(widgetStates.b2.lowercase())) "on" else "off"
                prefs.cachedButton3State = if (activeStates.contains(widgetStates.b3.lowercase())) "on" else "off"
                prefs.cachedButton4State = if (activeStates.contains(widgetStates.b4.lowercase())) "on" else "off"
            }

            val freshCam = api.fetchCameraSnapshot(prefs.cameraEntity)
            if (freshCam != null) {
                HaCompositeWidget.saveCachedCameraBitmap(context, freshCam)
            }
        } else {
            // Service call failed -> Revert to previous state
            when (buttonIndex) {
                1 -> prefs.cachedButton1State = previousState
                2 -> prefs.cachedButton2State = previousState
                3 -> prefs.cachedButton3State = previousState
                4 -> prefs.cachedButton4State = previousState
            }
        }
    } else {
        // Reset to off/inactive if entityId is blank
        when (buttonIndex) {
            1 -> prefs.cachedButton1State = "off"
            2 -> prefs.cachedButton2State = "off"
            3 -> prefs.cachedButton3State = "off"
            4 -> prefs.cachedButton4State = "off"
        }
    }

    // 3. Final confirmed update
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
