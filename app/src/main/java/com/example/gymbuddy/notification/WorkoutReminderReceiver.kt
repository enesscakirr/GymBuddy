package com.example.gymbuddy.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.gymbuddy.data.preferences.AppPreferences

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Bildirimi göster
        NotificationHelper.showWorkoutReminder(context)

        // Ertesi gün için tekrar planla (exact alarm tek seferlik)
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = AppPreferences(context)
            val hour = prefs.notifHour.first()
            val minute = prefs.notifMin.first()
            NotificationHelper.scheduleWorkoutReminder(context, hour, minute)
        }
    }
}
