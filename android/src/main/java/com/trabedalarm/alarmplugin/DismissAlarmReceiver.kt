package com.trabedalarm.alarmplugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class DismissAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        if (alarmId.isNullOrBlank()) {
            return
        }

        NotificationManagerCompat.from(context).cancel(notificationIdFor(alarmId))
        removeTrackedAlarmId(context, alarmId)
    }
}
