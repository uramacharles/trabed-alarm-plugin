package com.trabedalarm.alarmplugin

const val ALARM_CHANNEL_ID = "trabed_alarm_channel"
const val ALARM_CHANNEL_NAME = "Trabed Alarms"
const val ALARM_NOTIFICATION_ID_BASE = 1000
const val ACTION_DISMISS_ALARM = "com.trabedalarm.alarmplugin.ACTION_DISMISS_ALARM"
const val ACTION_STOP_ALARM_PLAYBACK = "com.trabedalarm.alarmplugin.ACTION_STOP_ALARM_PLAYBACK"
const val DEFAULT_ALARM_DURATION_MINUTES = 3
const val EXTRA_ALARM_ID = "alarmId"
const val EXTRA_ALARM_MESSAGE = "message"
const val EXTRA_ALARM_DURATION_MINUTES = "durationMinutes"

fun requestCodeFor(alarmId: String): Int {
    return alarmId.hashCode()
}

fun notificationIdFor(alarmId: String): Int {
    return ALARM_NOTIFICATION_ID_BASE + (alarmId.hashCode() and 0x0FFFFFFF)
}
