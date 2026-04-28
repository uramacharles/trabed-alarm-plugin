package com.trabedalarm.alarmplugin

import android.content.Context

private const val PREFERENCES_NAME = "trabed_alarm_plugin"
private const val ALARM_IDS_KEY = "scheduled_alarm_ids"

fun removeTrackedAlarmId(context: Context, alarmId: String) {
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val updatedIds = prefs.getStringSet(ALARM_IDS_KEY, emptySet()).orEmpty().toMutableSet()
    updatedIds.remove(alarmId)
    prefs.edit().putStringSet(ALARM_IDS_KEY, updatedIds).apply()
}
