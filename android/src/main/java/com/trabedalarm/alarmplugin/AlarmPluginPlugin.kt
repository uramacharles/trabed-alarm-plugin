package com.trabedalarm.alarmplugin

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "AlarmPlugin")
class AlarmPluginPlugin : Plugin() {

    private val implementation = AlarmPlugin()

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
