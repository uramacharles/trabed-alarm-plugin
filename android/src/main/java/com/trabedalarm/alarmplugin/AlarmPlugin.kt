package com.trabedalarm.alarmplugin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.getcapacitor.PluginCall

class AlarmPlugin {
    private val preferencesName = "trabed_alarm_plugin"
    private val alarmIdsKey = "scheduled_alarm_ids"

    fun echo(value: String): String {
        return value
    }

    fun setAlarm(context: Context, call: PluginCall) {
        val alarmId = call.getInt("id") ?: return call.reject("Alarm id is required")
        val time = call.getLong("time") ?: return call.reject("Time is required")
        val message = call.getString("message") ?: "Alarm!"
        val requestedDurationMinutes = call.getInt("durationMinutes")
        val durationMinutes = requestedDurationMinutes
            ?.takeIf { it > 0 }
            ?: DEFAULT_ALARM_DURATION_MINUTES

        if (alarmId < 0) {
            call.reject("Alarm id must be 0 or greater")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_MESSAGE, message)
            putExtra(EXTRA_ALARM_DURATION_MINUTES, durationMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (time <= System.currentTimeMillis()) {
            call.reject("Time must be in the future")
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                call.reject("Exact alarms are not allowed on this device/app")
                return
            }
        }


        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        saveAlarmId(context, alarmId)

        call.resolve()
    }

    fun cancelAlarm(context: Context, call: PluginCall) {
        val alarmId = call.getInt("id") ?: return call.reject("Alarm id is required")

        if (alarmId < 0) {
            call.reject("Alarm id must be 0 or greater")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
        NotificationManagerCompat.from(context).cancel(notificationIdFor(alarmId))
        context.startService(
            Intent(context, AlarmPlaybackService::class.java).apply {
                action = ACTION_STOP_ALARM_PLAYBACK
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
        )
        removeAlarmId(context, alarmId)
        call.resolve()
    }

    fun cancelAllAlarms(context: Context, call: PluginCall) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIds = getAlarmIds(context)

        alarmIds.forEach { alarmId ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            alarmManager.cancel(pendingIntent)
            NotificationManagerCompat.from(context).cancel(notificationIdFor(alarmId))
            context.startService(
                Intent(context, AlarmPlaybackService::class.java).apply {
                    action = ACTION_STOP_ALARM_PLAYBACK
                    putExtra(EXTRA_ALARM_ID, alarmId)
                }
            )
        }

        clearAlarmIds(context)
        call.resolve()
    }

    private fun saveAlarmId(context: Context, alarmId: Int) {
        val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val updatedIds = prefs.getStringSet(alarmIdsKey, emptySet()).orEmpty().toMutableSet()
        updatedIds.add(alarmId.toString())
        prefs.edit().putStringSet(alarmIdsKey, updatedIds).apply()
    }

    private fun removeAlarmId(context: Context, alarmId: Int) {
        val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val updatedIds = prefs.getStringSet(alarmIdsKey, emptySet()).orEmpty().toMutableSet()
        updatedIds.remove(alarmId.toString())
        prefs.edit().putStringSet(alarmIdsKey, updatedIds).apply()
    }

    private fun clearAlarmIds(context: Context) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .remove(alarmIdsKey)
            .apply()
    }

    private fun getAlarmIds(context: Context): Set<Int> {
        return context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getStringSet(alarmIdsKey, emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }
}
