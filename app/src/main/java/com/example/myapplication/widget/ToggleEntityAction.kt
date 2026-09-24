package com.example.myapplication.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.ActionParameters.Key
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi

object WidgetKeys {
    val PREF_CONFIRM_ENTITY = stringPreferencesKey("confirm_entity_id")
    val PREF_CONFIRM_TS = longPreferencesKey("confirm_ts")
}

class ToggleEntityAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entityId = parameters[KEY_ENTITY_ID] ?: ""
        val prefs = HaPreferences(context)
        val now = System.currentTimeMillis()

        var triggerService = false
        var targetEntityToTrigger = entityId

        updateAppWidgetState(context, glanceId) { glancePrefs ->
            val activeEntity = glancePrefs[WidgetKeys.PREF_CONFIRM_ENTITY] ?: ""
            val activeTs = glancePrefs[WidgetKeys.PREF_CONFIRM_TS] ?: 0L
            
            // Check if confirmation is pending (within 6 seconds)
            val isPendingConfirm = prefs.requireConfirmation &&
                    activeEntity.isNotBlank() &&
                    ((now - activeTs) < 6000)

            if (!prefs.requireConfirmation) {
                // Confirmation disabled: Direct execution
                triggerService = true
                glancePrefs[WidgetKeys.PREF_CONFIRM_ENTITY] = ""
                glancePrefs[WidgetKeys.PREF_CONFIRM_TS] = 0L
            } else if (isPendingConfirm) {
                // Second tap: Confirmed! Clear confirmation and trigger action
                triggerService = true
                targetEntityToTrigger = activeEntity
                glancePrefs[WidgetKeys.PREF_CONFIRM_ENTITY] = ""
                glancePrefs[WidgetKeys.PREF_CONFIRM_TS] = 0L
            } else {
                // First tap: Set confirmation target
                val keyToStore = if (entityId.isNotBlank()) entityId else "btn_generic_action"
                glancePrefs[WidgetKeys.PREF_CONFIRM_ENTITY] = keyToStore
                glancePrefs[WidgetKeys.PREF_CONFIRM_TS] = now
            }
        }

        if (triggerService && targetEntityToTrigger.isNotBlank()) {
            val api = HomeAssistantApi(prefs)
            api.callEntityService(targetEntityToTrigger, "toggle")
        }

        // Re-render Glance UI
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

        updateAppWidgetState(context, glanceId) { glancePrefs ->
            glancePrefs[WidgetKeys.PREF_CONFIRM_ENTITY] = ""
            glancePrefs[WidgetKeys.PREF_CONFIRM_TS] = 0L
        }

        HaCompositeWidget().update(context, glanceId)
    }
}
