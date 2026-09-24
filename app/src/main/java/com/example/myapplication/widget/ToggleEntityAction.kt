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
        val now = System.currentTimeMillis()

        if (prefs.requireConfirmation) {
            val isPendingConfirm = (prefs.confirmingEntityId == entityId) && ((now - prefs.confirmingTimestamp) < 8000)
            if (isPendingConfirm) {
                // Confirmed on second tap! Clear confirmation state and call Home Assistant
                prefs.confirmingEntityId = ""
                prefs.confirmingTimestamp = 0L

                val api = HomeAssistantApi(prefs)
                api.callEntityService(entityId, "toggle")
            } else {
                // First tap: Set entity in confirmation state immediately
                prefs.confirmingEntityId = entityId
                prefs.confirmingTimestamp = now
            }
        } else {
            // Confirmation disabled: Direct execution
            val api = HomeAssistantApi(prefs)
            api.callEntityService(entityId, "toggle")
        }

        // Force Glance UI update
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
        val prefs = HaPreferences(context)
        prefs.confirmingEntityId = ""
        prefs.confirmingTimestamp = 0L
        HaCompositeWidget().update(context, glanceId)
    }
}
