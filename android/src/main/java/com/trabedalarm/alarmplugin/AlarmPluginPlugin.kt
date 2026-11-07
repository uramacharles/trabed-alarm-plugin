package com.trabedalarm.alarmplugin

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.core.app.NotificationManagerCompat
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.PermissionState
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback

@CapacitorPlugin(name = "AlarmPlugin", permissions = [
    Permission(alias = "notifications", strings = [Manifest.permission.POST_NOTIFICATIONS])
])
class AlarmPluginPlugin : Plugin() {

    private val implementation = AlarmPlugin()

    @PluginMethod
    fun checkExactAlarmPermission(call: PluginCall) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        call.resolve(JSObject().put("granted", Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()))
    }

    @PluginMethod
    fun requestExactAlarmPermission(call: PluginCall) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            checkExactAlarmPermission(call)
            return
        }
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        try {
            startActivityForResult(call, intent, "exactAlarmPermissionResult")
        } catch (_: android.content.ActivityNotFoundException) {
            checkExactAlarmPermission(call)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @ActivityCallback
    private fun exactAlarmPermissionResult(call: PluginCall?, result: ActivityResult) {
        // The settings result code is not a grant result; recheck access on return.
        call?.let { checkExactAlarmPermission(it) }
    }

    @PluginMethod
    fun checkNotificationPermission(call: PluginCall) {
        val granted = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                getPermissionState("notifications") == PermissionState.GRANTED)
        call.resolve(JSObject().put("granted", granted))
    }

    @PluginMethod
    fun requestNotificationPermission(call: PluginCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            getPermissionState("notifications") != PermissionState.GRANTED) {
            requestPermissionForAlias("notifications", call, "notificationPermissionResult")
        } else {
            checkNotificationPermission(call)
        }
    }

    @PermissionCallback
    private fun notificationPermissionResult(call: PluginCall) {
        checkNotificationPermission(call)
    }

    @PluginMethod
    fun echo(call: PluginCall) {
        val value = call.getString("value")

        val ret = JSObject().apply {
            put("value", implementation.echo(value ?: ""))
        }

        call.resolve(ret)
    }
    @PluginMethod
    fun setAlarm(call: PluginCall) {
        implementation.setAlarm(context, call)
    }

    @PluginMethod
    fun cancelAlarm(call: PluginCall) {
        implementation.cancelAlarm(context, call)
    }

    @PluginMethod
    fun cancelAllAlarms(call: PluginCall) {
        implementation.cancelAllAlarms(context, call)
    }

}
