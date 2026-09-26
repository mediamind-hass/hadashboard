package com.example.myapplication.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi
import kotlinx.coroutines.delay

suspend fun handleButtonAction(context: Context, glanceId: GlanceId, buttonIndex: Int) {
    try {
        Log.d("ApiPerf", "handleButtonAction started for button index $buttonIndex")
        val prefs = HaPreferences(context)
        val entityId = when (buttonIndex) {
            1 -> prefs.button1Entity
            2 -> prefs.button2Entity
            3 -> prefs.button3Entity
            4 -> prefs.button4Entity
            else -> ""
        }
        Log.d("ApiPerf", "handleButtonAction entityId: $entityId")
        if (entityId.isNotBlank()) {
            val api = HomeAssistantApi(prefs)
            val success = api.callEntityService(entityId, "toggle")
            Log.d("ApiPerf", "handleButtonAction callEntityService success: $success")
            delay(500)
        }
        HaCompositeWidget().updateAll(context)
        Log.d("ApiPerf", "handleButtonAction updateAll completed successfully")
    } catch (e: Exception) {
        Log.e("ApiPerf", "handleButtonAction crashed with exception", e)
    }
}

class Button1Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("ApiPerf", "Button1Action onAction clicked")
        handleButtonAction(context, glanceId, 1)
    }
}

class Button2Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("ApiPerf", "Button2Action onAction clicked")
        handleButtonAction(context, glanceId, 2)
    }
}

class Button3Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("ApiPerf", "Button3Action onAction clicked")
        handleButtonAction(context, glanceId, 3)
    }
}

class Button4Action : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("ApiPerf", "Button4Action onAction clicked")
        handleButtonAction(context, glanceId, 4)
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            Log.d("ApiPerf", "RefreshWidgetAction onAction clicked")
            delay(100)
            val prefs = HaPreferences(context)
            val api = HomeAssistantApi(prefs)
            api.testConnectionDetailed()
            HaCompositeWidget().updateAll(context)
            Log.d("ApiPerf", "RefreshWidgetAction completed successfully")
        } catch (e: Exception) {
            Log.e("ApiPerf", "RefreshWidgetAction crashed with exception", e)
        }
    }
}
