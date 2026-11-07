package com.trabedalarm.alarmplugin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)

        if (alarmId.isNullOrBlank()) {
            return
        }

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled() ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED)
        ) {
            removeTrackedAlarmId(context, alarmId)
            return
        }

        // Keep the resource name stable across app upgrades, when numeric IDs may change.
        val soundName = context.resources.getResourceEntryName(R.raw.trabed_reminder)
        val sound = Uri.parse("android.resource://${context.packageName}/raw/$soundName")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(ALARM_CHANNEL_ID, ALARM_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Sounds and notifications for task reminders"
                setSound(
                    sound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            manager.createNotificationChannel(channel)
        }

        val dismissIntent = Intent(context, DismissAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, requestCodeFor(alarmId), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Reminder")
            .setContentText(intent.getStringExtra(EXTRA_ALARM_MESSAGE) ?: "Task reminder")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(sound)
            .setAutoCancel(true)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        try {
            manager.notify(notificationIdFor(alarmId), notification)
        } catch (_: SecurityException) {
            // Permission can be revoked while the receiver is running.
            removeTrackedAlarmId(context, alarmId)
        }
    }
}
