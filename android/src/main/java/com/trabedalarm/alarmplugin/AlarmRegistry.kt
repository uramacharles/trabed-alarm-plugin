package com.trabedalarm.alarmplugin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONObject

private const val PREFERENCES_NAME = "trabed_alarm_plugin"
private const val ALARM_IDS_KEY = "scheduled_alarm_ids"
private const val ALARM_DATA_PREFIX = "alarm_data_"

/** A scheduled alarm as saved on the phone, so it can be set again after a restart. */
data class SavedAlarm(val id: String, val time: Long, val message: String)

private fun prefs(context: Context) = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

fun trackedAlarmIds(context: Context): Set<String> =
    prefs(context).getStringSet(ALARM_IDS_KEY, emptySet()).orEmpty().toSet()

fun trackAlarm(context: Context, alarmId: String, time: Long, message: String) {
    val p = prefs(context)
    val ids = p.getStringSet(ALARM_IDS_KEY, emptySet()).orEmpty().toMutableSet()
    ids.add(alarmId)
    val data = JSONObject().put("time", time).put("message", message).toString()
    p.edit().putStringSet(ALARM_IDS_KEY, ids).putString(ALARM_DATA_PREFIX + alarmId, data).apply()
}

/** The alarm has rung: it no longer needs setting again after a restart (its id stays until dismissed or cancelled). */
fun forgetAlarmTime(context: Context, alarmId: String) {
    prefs(context).edit().remove(ALARM_DATA_PREFIX + alarmId).apply()
}

fun removeTrackedAlarmId(context: Context, alarmId: String) {
    val p = prefs(context)
    val ids = p.getStringSet(ALARM_IDS_KEY, emptySet()).orEmpty().toMutableSet()
    ids.remove(alarmId)
    p.edit().putStringSet(ALARM_IDS_KEY, ids).remove(ALARM_DATA_PREFIX + alarmId).apply()
}

fun clearTrackedAlarms(context: Context) {
    val p = prefs(context)
    val editor = p.edit().remove(ALARM_IDS_KEY)
    p.all.keys.filter { it.startsWith(ALARM_DATA_PREFIX) }.forEach { editor.remove(it) }
    editor.apply()
}

fun savedAlarms(context: Context): List<SavedAlarm> {
    return prefs(context).all.mapNotNull { (key, value) ->
        if (!key.startsWith(ALARM_DATA_PREFIX) || value !is String) return@mapNotNull null
        try {
            val json = JSONObject(value)
            SavedAlarm(key.removePrefix(ALARM_DATA_PREFIX), json.getLong("time"), json.optString("message", "Alarm!"))
        } catch (_: Exception) {
            null
        }
    }
}

fun alarmPendingIntent(context: Context, alarmId: String, message: String?): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra(EXTRA_ALARM_ID, alarmId)
        if (message != null) putExtra(EXTRA_ALARM_MESSAGE, message)
    }
    return PendingIntent.getBroadcast(
        context,
        requestCodeFor(alarmId),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

/** Exact when the phone allows it, otherwise as close as Android allows. */
fun scheduleAlarm(context: Context, alarmId: String, time: Long, message: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = alarmPendingIntent(context, alarmId, message)
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
