package com.trabedalarm.alarmplugin

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Run in the library's test host, not an installed production app. */
@RunWith(AndroidJUnit4::class)
class ReminderNotificationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val preferences = context.getSharedPreferences("trabed_alarm_plugin", Context.MODE_PRIVATE)
    private val implementation = AlarmPlugin()

    @Before
    fun prepare() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        manager.cancelAll()
        preferences.edit().clear().commit()
    }

    @After
    fun cleanUp() {
        implementation.cancelAllAlarms(context, RecordingCall(JSObject()))
        manager.cancelAll()
    }

    @Test
    fun manifestDoesNotRequestRestrictedPermissionsOrPlaybackService() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES)
        val permissions = info.requestedPermissions.orEmpty().toSet()
        assertTrue(permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(permissions.contains(Manifest.permission.SCHEDULE_EXACT_ALARM))
        assertFalse(permissions.contains("android.permission.USE_EXACT_ALARM"))
        assertFalse(permissions.contains("android.permission.FOREGROUND_SERVICE"))
        assertFalse(permissions.contains("android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"))
        assertFalse(info.services.orEmpty().any { it.name.endsWith("AlarmPlaybackService") })
    }

    @Test
    fun reminderUsesAudibleChannelEvenWhenOldChannelIsSilent() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())
        manager.createNotificationChannel(NotificationChannel("trabed_alarm_channel", "Old alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)
        })

        post("first")
        val notification = manager.activeNotifications.single().notification
        val channel = manager.getNotificationChannel(notification.channelId)
        assertEquals(ALARM_CHANNEL_ID, channel.id)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertNotNull(channel.sound)
        assertEquals("android.resource", channel.sound.scheme)
        assertEquals("/raw/trabed_reminder", channel.sound.path)
        assertEquals(Notification.CATEGORY_ALARM, notification.category)
        assertEquals("Task first", notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals("Dismiss", notification.actions.single().title.toString())
        assertNotNull(notification.deleteIntent)
        assertEquals(0, notification.flags and Notification.FLAG_INSISTENT)
        assertEquals(0, notification.flags and Notification.FLAG_ONGOING_EVENT)
        assertEquals(0, notification.flags and Notification.FLAG_FOREGROUND_SERVICE)
    }

    @Test
    fun dismissalAndCancellationRemoveOnlyTheirOwnReminders() {
        post("first")
        post("second")
        preferences.edit().putStringSet("scheduled_alarm_ids", setOf("first", "second")).commit()

        DismissAlarmReceiver().onReceive(context, Intent().putExtra(EXTRA_ALARM_ID, "first"))
        assertEquals(setOf(notificationIdFor("second")), manager.activeNotifications.map { it.id }.toSet())
        assertEquals(setOf("second"), preferences.getStringSet("scheduled_alarm_ids", emptySet()))

        val call = RecordingCall(JSObject().put("id", "second"))
        implementation.cancelAlarm(context, call)
        assertTrue(call.resolved)
        assertTrue(manager.activeNotifications.isEmpty())
        assertTrue(preferences.getStringSet("scheduled_alarm_ids", emptySet())!!.isEmpty())
    }

    @Test
    fun cancelAllRemovesEveryDeliveredReminder() {
        post("first")
        post("second")
        preferences.edit().putStringSet("scheduled_alarm_ids", setOf("first", "second")).commit()
        val call = RecordingCall(JSObject())
        implementation.cancelAllAlarms(context, call)
        assertTrue(call.resolved)
        assertTrue(manager.activeNotifications.isEmpty())
        assertTrue(preferences.getStringSet("scheduled_alarm_ids", emptySet())!!.isEmpty())
    }

    @Test
    fun schedulingWithoutExactAccessStillSucceeds() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assumeFalse(alarms.canScheduleExactAlarms())
        val call = RecordingCall(JSObject()
            .put("id", "approximate")
            .put("time", System.currentTimeMillis() + 600_000L)
            .put("message", "Approximate task reminder"))
        implementation.setAlarm(context, call)
        assertTrue(call.resolved)
        assertNull(call.error)
        assertTrue(preferences.getStringSet("scheduled_alarm_ids", emptySet())!!.contains("approximate"))
    }

    @Test
    fun restartSetsFutureAlarmsAgainAndForgetsOldOnes() {
        val call = RecordingCall(JSObject()
            .put("id", "after_restart")
            .put("time", System.currentTimeMillis() + 3_600_000L)
            .put("message", "Still due after a restart"))
        implementation.setAlarm(context, call)
        assertTrue(call.resolved)
        assertTrue(savedAlarms(context).any { it.id == "after_restart" && it.message == "Still due after a restart" })
        trackAlarm(context, "long_gone", System.currentTimeMillis() - 2 * 3_600_000L, "Missed long ago")

        // A restart: Android drops the alarm (the pending intent goes with it).
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = alarmPendingIntent(context, "after_restart", null)
        alarms.cancel(pending)
        pending.cancel()

        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        val again = android.app.PendingIntent.getBroadcast(context, requestCodeFor("after_restart"),
            Intent(context, AlarmReceiver::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_NO_CREATE)
        assertNotNull(again)
        assertFalse(savedAlarms(context).any { it.id == "long_gone" })
        assertFalse(trackedAlarmIds(context).contains("long_gone"))
    }

    @Test
    fun aRungAlarmIsNotSetAgainAfterARestart() {
        trackAlarm(context, "rang", System.currentTimeMillis() + 60_000L, "Rings once")
        post("rang")
        assertFalse(savedAlarms(context).any { it.id == "rang" })
    }

    private fun post(id: String) {
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())
        AlarmReceiver().onReceive(context, Intent()
            .putExtra(EXTRA_ALARM_ID, id)
            .putExtra(EXTRA_ALARM_MESSAGE, "Task $id"))
    }

    private class RecordingCall(data: JSObject) : PluginCall(null, "AlarmPlugin", "test", "test", data) {
        var resolved = false
        var error: String? = null

        override fun resolve() { resolved = true }
        override fun reject(message: String) { error = message }
    }
}
