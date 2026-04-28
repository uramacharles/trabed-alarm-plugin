package com.trabedalarm.alarmplugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        val msg = intent.getStringExtra(EXTRA_ALARM_MESSAGE) ?: "Alarm Triggered!"
        val durationMinutes = intent.getIntExtra(
            EXTRA_ALARM_DURATION_MINUTES,
            DEFAULT_ALARM_DURATION_MINUTES
        )

        if (alarmId.isNullOrBlank()) {
            return
        }

        val serviceIntent = Intent(context, AlarmPlaybackService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_MESSAGE, msg)
            putExtra(EXTRA_ALARM_DURATION_MINUTES, durationMinutes)
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
