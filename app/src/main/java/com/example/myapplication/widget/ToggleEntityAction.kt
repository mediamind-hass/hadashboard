package com.example.myapplication.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi
import kotlinx.coroutines.delay

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
        delay(100)
        val prefs = HaPreferences(context)
        val api = HomeAssistantApi(prefs)
        api.testConnectionDetailed()
        HaCompositeWidget().updateAll(context)
    }
}
