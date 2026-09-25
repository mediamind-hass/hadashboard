package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences

class HaPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ha_widget_prefs", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("server_url", "") ?: ""
        set(value) {
            var trimmed = value.trim().trimEnd('/')
            if (trimmed.isNotBlank() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                trimmed = "http://$trimmed"
            }
            prefs.edit().putString("server_url", trimmed).apply()
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

    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && token.isNotBlank()

    // Cached values for offline/cold-start resilience
    var cachedSensor1Val: String
        get() = prefs.getString("cached_s1", "---") ?: "---"
        set(value) = prefs.edit().putString("cached_s1", value).apply()

    var cachedSensor2Val: String
        get() = prefs.getString("cached_s2", "---") ?: "---"
        set(value) = prefs.edit().putString("cached_s2", value).apply()

    var cachedSensor3Val: String
        get() = prefs.getString("cached_s3", "---") ?: "---"
        set(value) = prefs.edit().putString("cached_s3", value).apply()

    var cachedButton1On: Boolean
        get() = prefs.getBoolean("cached_b1", false)
        set(value) = prefs.edit().putBoolean("cached_b1", value).apply()

    var cachedButton2On: Boolean
        get() = prefs.getBoolean("cached_b2", false)
        set(value) = prefs.edit().putBoolean("cached_b2", value).apply()

    var cachedButton3On: Boolean
        get() = prefs.getBoolean("cached_b3", false)
        set(value) = prefs.edit().putBoolean("cached_b3", value).apply()

    var cachedButton4On: Boolean
        get() = prefs.getBoolean("cached_b4", false)
        set(value) = prefs.edit().putBoolean("cached_b4", value).apply()
}
