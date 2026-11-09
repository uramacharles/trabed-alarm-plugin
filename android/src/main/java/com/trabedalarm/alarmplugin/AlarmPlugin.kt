package com.trabedalarm.alarmplugin

import android.app.AlarmManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.getcapacitor.PluginCall

class AlarmPlugin {
    fun echo(value: String): String {
        return value
    }

    fun setAlarm(context: Context, call: PluginCall) {
        val alarmId = call.getString("id")?.trim().takeUnless { it.isNullOrEmpty() }
            ?: return call.reject("Alarm id is required")
        val time = call.getLong("time") ?: return call.reject("Time is required")
        val message = call.getString("message") ?: "Alarm!"

        if (time <= System.currentTimeMillis()) {
            call.reject("Time must be in the future")
            return
        }

        scheduleAlarm(context, alarmId, time, message)
        // Saved with its time and message, so BootReceiver can set it again after the phone restarts.
        trackAlarm(context, alarmId, time, message)

        call.resolve()
    }

    fun cancelAlarm(context: Context, call: PluginCall) {
        val alarmId = call.getString("id")?.trim().takeUnless { it.isNullOrEmpty() }
            ?: return call.reject("Alarm id is required")

        cancelOne(context, alarmId)
        call.resolve()
    }

    fun cancelAllAlarms(context: Context, call: PluginCall) {
        trackedAlarmIds(context).forEach { cancelOne(context, it, forget = false) }
        clearTrackedAlarms(context)
        call.resolve()
    }

    private fun cancelOne(context: Context, alarmId: String, forget: Boolean = true) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent(context, alarmId, null))
        NotificationManagerCompat.from(context).cancel(notificationIdFor(alarmId))
        if (forget) removeTrackedAlarmId(context, alarmId)
    }
}
