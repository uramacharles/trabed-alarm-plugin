import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(AlarmPluginPlugin)
public class AlarmPluginPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "AlarmPluginPlugin"
    public let jsName = "AlarmPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "echo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setAlarm", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "cancelAlarm", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "cancelAllAlarms", returnType: CAPPluginReturnPromise)
    ]
    private let implementation = AlarmPlugin()

    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": implementation.echo(value)
        ])
    }

    @objc func setAlarm(_ call: CAPPluginCall) {
        guard let id = call.getString("id"), !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            call.reject("Alarm id is required")
            return
        }

        guard let time = call.getDouble("time") else {
            call.reject("Time is required")
            return
        }

        let message = call.getString("message") ?? "Alarm!"
        let requestedDuration = call.getInt("durationMinutes")
        let durationMinutes = (requestedDuration != nil && requestedDuration! > 0) ? requestedDuration! : 3

        implementation.setAlarm(id: id, time: time, message: message, durationMinutes: durationMinutes) { error in
            if let error {
                call.reject(error.localizedDescription)
            } else {
                call.resolve()
            }
        }
    }

    @objc func cancelAlarm(_ call: CAPPluginCall) {
        guard let id = call.getString("id"), !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            call.reject("Alarm id is required")
            return
        }

        implementation.cancelAlarm(id: id) {
            call.resolve()
        }
    }

    @objc func cancelAllAlarms(_ call: CAPPluginCall) {
        implementation.cancelAllAlarms {
            call.resolve()
        }
    }
}
