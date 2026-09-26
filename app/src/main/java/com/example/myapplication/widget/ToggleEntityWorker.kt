package com.example.myapplication.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myapplication.data.HaPreferences
import com.example.myapplication.data.HomeAssistantApi
import kotlinx.coroutines.delay

class ToggleEntityWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val KEY_BUTTON_INDEX = "button_index"

        fun enqueue(context: Context, buttonIndex: Int) {
            val data = Data.Builder()
                .putInt(KEY_BUTTON_INDEX, buttonIndex)
                .build()
            val request = OneTimeWorkRequestBuilder<ToggleEntityWorker>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val buttonIndex = inputData.getInt(KEY_BUTTON_INDEX, 0)
            val prefs = HaPreferences(applicationContext)
            val entityId = when (buttonIndex) {
                1 -> prefs.button1Entity
                2 -> prefs.button2Entity
                3 -> prefs.button3Entity
                4 -> prefs.button4Entity
                else -> ""
            }
            if (entityId.isNotBlank()) {
                val api = HomeAssistantApi(prefs)
                api.callEntityService(entityId, "toggle")
                delay(500)
            }
            HaCompositeWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
