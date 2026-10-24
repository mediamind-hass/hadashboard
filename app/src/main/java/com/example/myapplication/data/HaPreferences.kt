package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences

class HaPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ha_widget_prefs", Context.MODE_PRIVATE)

    var serverUrl: String
        get() {
            val raw = prefs.getString("server_url", "http://192.168.16.33:30103") ?: "http://192.168.16.33:30103"
            val trimmed = raw.trim().trimEnd('/')
            val formatted = if (trimmed.isNotBlank() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "http://$trimmed"
            } else {
                trimmed
            }
            return if (formatted.contains("192.168.16.33:8123")) {
                "http://192.168.16.33:30103"
            } else {
                formatted
            }
        }
        set(value) {
            val trimmed = value.trim().trimEnd('/')
            var formatted = if (trimmed.isNotBlank() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "http://$trimmed"
            } else {
                trimmed
            }
            if (formatted.contains("192.168.16.33:8123")) {
                formatted = "http://192.168.16.33:30103"
            }
            prefs.edit().putString("server_url", formatted).apply()
        }

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(value) = prefs.edit().putString("token", value.trim()).apply()

    // Camera
    var cameraEntity: String
        get() = prefs.getString("camera_entity", "camera.ingresso") ?: "camera.ingresso"
        set(value) = prefs.edit().putString("camera_entity", value.trim()).apply()

    // Sensors
    var sensor1Entity: String
        get() = prefs.getString("sensor_1_entity", "sensor.voltage_1") ?: "sensor.voltage_1"
        set(value) = prefs.edit().putString("sensor_1_entity", value.trim()).apply()

    var sensor1Name: String
        get() = prefs.getString("sensor_1_name", "Fase 1") ?: "Fase 1"
        set(value) = prefs.edit().putString("sensor_1_name", value.trim()).apply()

    var sensor1Unit: String
        get() = prefs.getString("sensor_1_unit", "V") ?: "V"
        set(value) = prefs.edit().putString("sensor_1_unit", value.trim()).apply()

    var sensor2Entity: String
        get() = prefs.getString("sensor_2_entity", "sensor.voltage_2") ?: "sensor.voltage_2"
        set(value) = prefs.edit().putString("sensor_2_entity", value.trim()).apply()

    var sensor2Name: String
        get() = prefs.getString("sensor_2_name", "Fase 2") ?: "Fase 2"
        set(value) = prefs.edit().putString("sensor_2_name", value.trim()).apply()

    var sensor2Unit: String
        get() = prefs.getString("sensor_2_unit", "V") ?: "V"
        set(value) = prefs.edit().putString("sensor_2_unit", value.trim()).apply()

    var sensor3Entity: String
        get() = prefs.getString("sensor_3_entity", "sensor.voltage_3") ?: "sensor.voltage_3"
        set(value) = prefs.edit().putString("sensor_3_entity", value.trim()).apply()

    var sensor3Name: String
        get() = prefs.getString("sensor_3_name", "Fase 3") ?: "Fase 3"
        set(value) = prefs.edit().putString("sensor_3_name", value.trim()).apply()

    var sensor3Unit: String
        get() = prefs.getString("sensor_3_unit", "V") ?: "V"
        set(value) = prefs.edit().putString("sensor_3_unit", value.trim()).apply()

    // Buttons
    var button1Entity: String
        get() = prefs.getString("btn_1_entity", "switch.relay_1") ?: "switch.relay_1"
        set(value) = prefs.edit().putString("btn_1_entity", value.trim()).apply()

    var button1Name: String
        get() = prefs.getString("btn_1_name", "Cancello") ?: "Cancello"
        set(value) = prefs.edit().putString("btn_1_name", value.trim()).apply()

    var button2Entity: String
        get() = prefs.getString("btn_2_entity", "switch.relay_2") ?: "switch.relay_2"
        set(value) = prefs.edit().putString("btn_2_entity", value.trim()).apply()

    var button2Name: String
        get() = prefs.getString("btn_2_name", "Luce Est.") ?: "Luce Est."
        set(value) = prefs.edit().putString("btn_2_name", value.trim()).apply()

    var button3Entity: String
        get() = prefs.getString("btn_3_entity", "switch.relay_3") ?: "switch.relay_3"
        set(value) = prefs.edit().putString("btn_3_entity", value.trim()).apply()

    var button3Name: String
        get() = prefs.getString("btn_3_name", "Relè 3") ?: "Relè 3"
        set(value) = prefs.edit().putString("btn_3_name", value.trim()).apply()

    var button4Entity: String
        get() = prefs.getString("btn_4_entity", "switch.relay_4") ?: "switch.relay_4"
        set(value) = prefs.edit().putString("btn_4_entity", value.trim()).apply()

    var button4Name: String
        get() = prefs.getString("btn_4_name", "Allarme") ?: "Allarme"
        set(value) = prefs.edit().putString("btn_4_name", value.trim()).apply()

    // Caching for One UI 9 background widget resilience
    var cachedSensor1Val: String
        get() = prefs.getString("cached_s1_val", "---") ?: "---"
        set(value) = prefs.edit().putString("cached_s1_val", value).apply()

    var cachedSensor2Val: String
        get() = prefs.getString("cached_s2_val", "---") ?: "---"
        set(value) = prefs.edit().putString("cached_s2_val", value).apply()

    var cachedSensor3Val: String
        get() = prefs.getString("cached_s3_val", "---") ?: "---"
        set(value) = prefs.edit().putString("cached_s3_val", value).apply()

    var cachedBtn1On: Boolean
        get() = prefs.getBoolean("cached_b1_on", false)
        set(value) = prefs.edit().putBoolean("cached_b1_on", value).apply()

    var cachedBtn2On: Boolean
        get() = prefs.getBoolean("cached_b2_on", false)
        set(value) = prefs.edit().putBoolean("cached_b2_on", value).apply()

    var cachedBtn3On: Boolean
        get() = prefs.getBoolean("cached_b3_on", false)
        set(value) = prefs.edit().putBoolean("cached_b3_on", value).apply()

    var cachedBtn4On: Boolean
        get() = prefs.getBoolean("cached_b4_on", false)
        set(value) = prefs.edit().putBoolean("cached_b4_on", value).apply()

    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && token.isNotBlank()
}
