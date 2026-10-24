package com.example.myapplication.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters.Key
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ToggleEntityAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entityId = parameters[KEY_ENTITY_ID] ?: return
        val prefs = HaPreferences(context)
        val api = HomeAssistantApi(prefs, context)

        // 1. Call Home Assistant service toggle/turn_on immediately
        api.callEntityService(entityId, "toggle")

        // 2. Update widget UI immediately
        try {
            HaCompositeWidget().update(context, glanceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Delayed background re-update in separate job so Glance SessionWorker isn't cancelled
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            try {
                HaCompositeWidget().update(context, glanceId)
            } catch (_: Exception) {}
        }
    }

    companion object {
        val KEY_ENTITY_ID = Key<String>("entity_id")
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            HaCompositeWidget().update(context, glanceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
