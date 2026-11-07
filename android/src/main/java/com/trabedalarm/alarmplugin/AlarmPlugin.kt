package com.trabedalarm.alarmplugin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.getcapacitor.PluginCall

class AlarmPlugin {
    private val preferencesName = "trabed_alarm_plugin"
    private val alarmIdsKey = "scheduled_alarm_ids"

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

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_MESSAGE, message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(alarmId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        scheduleAlarm(alarmManager, time, pendingIntent)
        saveAlarmId(context, alarmId)

        call.resolve()
    }

    fun cancelAlarm(context: Context, call: PluginCall) {
        val alarmId = call.getString("id")?.trim().takeUnless { it.isNullOrEmpty() }
            ?: return call.reject("Alarm id is required")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(alarmId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
        NotificationManagerCompat.from(context).cancel(notificationIdFor(alarmId))
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
                requestCodeFor(alarmId),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            alarmManager.cancel(pendingIntent)
            NotificationManagerCompat.from(context).cancel(notificationIdFor(alarmId))
        }

        clearAlarmIds(context)
        call.resolve()
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, time: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                return
            } catch (_: SecurityException) {
                // Access may have been revoked between the check and scheduling.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }

    private fun saveAlarmId(context: Context, alarmId: String) {
        val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val updatedIds = prefs.getStringSet(alarmIdsKey, emptySet()).orEmpty().toMutableSet()
        updatedIds.add(alarmId)
        prefs.edit().putStringSet(alarmIdsKey, updatedIds).apply()
    }

    private fun removeAlarmId(context: Context, alarmId: String) {
        val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val updatedIds = prefs.getStringSet(alarmIdsKey, emptySet()).orEmpty().toMutableSet()
        updatedIds.remove(alarmId)
        prefs.edit().putStringSet(alarmIdsKey, updatedIds).apply()
    }

    private fun clearAlarmIds(context: Context) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .remove(alarmIdsKey)
            .apply()
    }

    private fun getAlarmIds(context: Context): Set<String> {
        return context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getStringSet(alarmIdsKey, emptySet())
            .orEmpty()
            .toSet()
    }
}
