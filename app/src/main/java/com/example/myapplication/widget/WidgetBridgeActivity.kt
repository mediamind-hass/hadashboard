package com.example.myapplication.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WidgetBridgeActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BUTTON_INDEX = "extra_button_index"
        const val EXTRA_IS_REFRESH = "extra_is_refresh"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val buttonIndex = intent.getIntExtra(EXTRA_BUTTON_INDEX, 0)
        val isRefresh = intent.getBooleanExtra(EXTRA_IS_REFRESH, false)

        lifecycleScope.launch {
            try {
                val prefs = HaPreferences(applicationContext)
                val api = HomeAssistantApi(prefs)

                if (isRefresh) {
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
                        HaCompositeWidget.saveCachedCameraBitmap(applicationContext, freshCam)
                    }
                    HaCompositeWidget().updateAll(applicationContext)
                } else if (buttonIndex in 1..4) {
                    val entityId = when (buttonIndex) {
                        1 -> prefs.button1Entity
                        2 -> prefs.button2Entity
                        3 -> prefs.button3Entity
                        4 -> prefs.button4Entity
                        else -> ""
                    }

                    if (entityId.isNotBlank()) {
                        val previousState = when (buttonIndex) {
                            1 -> prefs.cachedButton1State
                            2 -> prefs.cachedButton2State
                            3 -> prefs.cachedButton3State
                            4 -> prefs.cachedButton4State
                            else -> "off"
                        }
                        val targetState = if (previousState == "on") "off" else "on"

                        // Optimistic UI update in foreground context
                        when (buttonIndex) {
                            1 -> prefs.cachedButton1State = targetState
                            2 -> prefs.cachedButton2State = targetState
                            3 -> prefs.cachedButton3State = targetState
                            4 -> prefs.cachedButton4State = targetState
                        }
                        HaCompositeWidget().updateAll(applicationContext)

                        // Execute network service call
                        val success = api.callEntityService(entityId, "toggle")

                        if (success) {
                            delay(400)
                            val widgetStates = api.fetchWidgetStates()
                            val activeStates = listOf("on", "active", "playing", "true")
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
                                HaCompositeWidget.saveCachedCameraBitmap(applicationContext, freshCam)
                            }
                        } else {
                            // Revert on failure
                            when (buttonIndex) {
                                1 -> prefs.cachedButton1State = previousState
                                2 -> prefs.cachedButton2State = previousState
                                3 -> prefs.cachedButton3State = previousState
                                4 -> prefs.cachedButton4State = previousState
                            }
                        }
                        HaCompositeWidget().updateAll(applicationContext)
                    }
                }
            } catch (_: Exception) {
            } finally {
                finish()
            }
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
