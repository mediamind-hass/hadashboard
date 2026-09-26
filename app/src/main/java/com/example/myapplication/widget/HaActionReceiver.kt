package com.example.myapplication.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class HaActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TOGGLE = "com.example.myapplication.ACTION_TOGGLE"
        const val ACTION_REFRESH = "com.example.myapplication.ACTION_REFRESH"
        const val EXTRA_BUTTON_INDEX = "button_index"

        private val isBusy = AtomicBoolean(false)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = HaPreferences(context)
                val api = HomeAssistantApi(prefs)

                when (intent.action) {
                    ACTION_TOGGLE -> {
                        val buttonIndex = intent.getIntExtra(EXTRA_BUTTON_INDEX, 0)
                        val entityId = when (buttonIndex) {
                            1 -> prefs.button1Entity
                            2 -> prefs.button2Entity
                            3 -> prefs.button3Entity
                            4 -> prefs.button4Entity
                            else -> ""
                        }
                        if (entityId.isNotBlank()) {
                            api.callEntityService(entityId, "toggle")
                        }
                    }
                    ACTION_REFRESH -> {
                        if (!isBusy.compareAndSet(false, true)) {
                            return@launch
                        }
                        try {
                            api.testConnectionDetailed()
                        } finally {
                            isBusy.set(false)
                        }
                    }
                }
                HaCompositeWidget().updateAll(context)
            } catch (e: Exception) {
                Log.e("HaActionReceiver", "Error handling action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
