package com.example.myapplication.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters.Key
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi

class ToggleEntityAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entityId = parameters[KEY_ENTITY_ID] ?: return
        val prefs = HaPreferences(context)
        val api = HomeAssistantApi(prefs)

        // Call Home Assistant service toggle
        api.callEntityService(entityId, "toggle")

        // Update widget UI
        HaCompositeWidget().update(context, glanceId)
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
        HaCompositeWidget().update(context, glanceId)
    }
}
