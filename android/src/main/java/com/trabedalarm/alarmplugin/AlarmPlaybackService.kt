package com.trabedalarm.alarmplugin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var activeAlarmId: String? = null
    private val stopHandler = Handler(Looper.getMainLooper())
    private val stopAlarmRunnable = Runnable {
        val alarmId = activeAlarmId ?: return@Runnable
        NotificationManagerCompat.from(this).cancel(notificationIdFor(alarmId))
        removeTrackedAlarmId(this, alarmId)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getStringExtra(EXTRA_ALARM_ID)
        if (intent?.action == ACTION_STOP_ALARM_PLAYBACK) {
            if (!alarmId.isNullOrBlank() && alarmId == activeAlarmId) {
                stopHandler.removeCallbacks(stopAlarmRunnable)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        val message = intent?.getStringExtra(EXTRA_ALARM_MESSAGE) ?: "Alarm Triggered!"
        val durationMinutes = intent?.getIntExtra(
            EXTRA_ALARM_DURATION_MINUTES,
            DEFAULT_ALARM_DURATION_MINUTES
        ) ?: DEFAULT_ALARM_DURATION_MINUTES

        if (alarmId.isNullOrBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        activeAlarmId = alarmId

        createNotificationChannel()
        startAlarmForeground(alarmId, message)
        startAlarmSound()
        scheduleAutoStop(durationMinutes)

        return START_STICKY
    }

    override fun onDestroy() {
        stopHandler.removeCallbacks(stopAlarmRunnable)
        activeAlarmId = null
        stopAlarmSound()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAlarmForeground(alarmId: String, message: String) {
        val dismissIntent = Intent(this, DismissAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            requestCodeFor(alarmId),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(resolveSmallIcon())
            .setContentTitle("Alarm")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationIdFor(alarmId),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(notificationIdFor(alarmId), notification)
        }
    }

    private fun startAlarmSound() {
        if (mediaPlayer?.isPlaying == true) {
            return
        }

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@AlarmPlaybackService, alarmUri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            prepare()
            start()
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.run {
            if (isPlaying) {
                stop()
            }
            reset()
            release()
        }
        mediaPlayer = null
    }

    private fun scheduleAutoStop(durationMinutes: Int) {
        stopHandler.removeCallbacks(stopAlarmRunnable)
        stopHandler.postDelayed(stopAlarmRunnable, durationMinutes * 60_000L)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            ALARM_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for scheduled alarms"
            setSound(null, null)
        }

        manager.createNotificationChannel(channel)
    }

    private fun resolveSmallIcon(): Int {
        return applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_lock_idle_alarm
    }
}
