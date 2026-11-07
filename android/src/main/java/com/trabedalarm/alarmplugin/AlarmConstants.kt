package com.trabedalarm.alarmplugin

// Use a new channel because the original channel's silent sound cannot be changed.
const val ALARM_CHANNEL_ID = "trabed_reminder_channel_v2"
const val ALARM_CHANNEL_NAME = "Trabed Alarms"
const val ALARM_NOTIFICATION_ID_BASE = 1000
const val ACTION_DISMISS_ALARM = "com.trabedalarm.alarmplugin.ACTION_DISMISS_ALARM"
const val EXTRA_ALARM_ID = "alarmId"
const val EXTRA_ALARM_MESSAGE = "message"

fun requestCodeFor(alarmId: String): Int {
    return alarmId.hashCode()
}

fun notificationIdFor(alarmId: String): Int {
    return ALARM_NOTIFICATION_ID_BASE + (alarmId.hashCode() and 0x0FFFFFFF)
}
