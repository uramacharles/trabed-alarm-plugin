package com.trabedalarm.alarmplugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android drops every alarm when the phone restarts (and when the app is updated). This sets the saved ones again:
 * future alarms at their time; one missed while the phone was off (up to an hour ago) rings a few seconds after
 * start-up; older ones are forgotten.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, "android.intent.action.QUICKBOOT_POWERON" -> Unit
            else -> return
        }
        val now = System.currentTimeMillis()
        savedAlarms(context).forEach { alarm ->
            when {
                alarm.time > now -> scheduleAlarm(context, alarm.id, alarm.time, alarm.message)
                now - alarm.time <= MISSED_GRACE_MS -> scheduleAlarm(context, alarm.id, now + 10_000L, alarm.message)
                else -> removeTrackedAlarmId(context, alarm.id)
            }
        }
    }

    companion object {
        const val MISSED_GRACE_MS = 60L * 60L * 1000L
    }
}
