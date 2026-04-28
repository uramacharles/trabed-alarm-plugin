import Foundation
import UserNotifications

@objc public class AlarmPlugin: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }

    private let notificationCenter = UNUserNotificationCenter.current()
    private let identifierPrefix = "trabed_alarm_"

    @objc public func setAlarm(
        id: String,
        time: Double,
        message: String,
        durationMinutes: Int,
        completion: @escaping (Error?) -> Void
    ) {
        if id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            completion(AlarmPluginError.invalidAlarmId)
            return
        }

        let fireDate = Date(timeIntervalSince1970: time / 1000)
        if fireDate.timeIntervalSinceNow <= 0 {
            completion(AlarmPluginError.timeMustBeInFuture)
            return
        }

        notificationCenter.getNotificationSettings { settings in
            switch settings.authorizationStatus {
            case .authorized, .provisional, .ephemeral:
                self.scheduleNotification(
                    id: id,
                    fireDate: fireDate,
                    message: message,
                    durationMinutes: durationMinutes,
                    completion: completion
                )
            case .notDetermined:
                self.notificationCenter.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
                    if let error {
                        completion(error)
                        return
                    }

                    guard granted else {
                        completion(AlarmPluginError.notificationsNotAllowed)
                        return
                    }

                    self.scheduleNotification(
                        id: id,
                        fireDate: fireDate,
                        message: message,
                        durationMinutes: durationMinutes,
                        completion: completion
                    )
                }
            case .denied:
                completion(AlarmPluginError.notificationsNotAllowed)
            @unknown default:
                completion(AlarmPluginError.notificationsNotAllowed)
            }
        }
    }

    @objc public func cancelAlarm(id: String, completion: @escaping () -> Void) {
        let identifier = alarmIdentifier(for: id)
        notificationCenter.removePendingNotificationRequests(withIdentifiers: [identifier])
        notificationCenter.removeDeliveredNotifications(withIdentifiers: [identifier])
        completion()
    }

    @objc public func cancelAllAlarms(completion: @escaping () -> Void) {
        notificationCenter.getPendingNotificationRequests { requests in
            let identifiers = requests
                .map(\.identifier)
                .filter { $0.hasPrefix(self.identifierPrefix) }

            self.notificationCenter.removePendingNotificationRequests(withIdentifiers: identifiers)
            self.notificationCenter.getDeliveredNotifications { notifications in
                let deliveredIdentifiers = notifications
                    .map(\.request.identifier)
                    .filter { $0.hasPrefix(self.identifierPrefix) }
                self.notificationCenter.removeDeliveredNotifications(withIdentifiers: deliveredIdentifiers)
                completion()
            }
        }
    }

    private func scheduleNotification(
        id: String,
        fireDate: Date,
        message: String,
        durationMinutes: Int,
        completion: @escaping (Error?) -> Void
    ) {
        let content = UNMutableNotificationContent()
        content.title = "Alarm"
        content.body = message
        content.sound = .default
        content.userInfo = [
            "alarmId": id,
            "durationMinutes": durationMinutes,
        ]

        let dateComponents = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: fireDate
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: false)
        let request = UNNotificationRequest(
            identifier: alarmIdentifier(for: id),
            content: content,
            trigger: trigger
        )

        notificationCenter.add(request) { error in
            completion(error)
        }
    }

    private func alarmIdentifier(for id: String) -> String {
        "\(identifierPrefix)\(id)"
    }
}

enum AlarmPluginError: LocalizedError {
    case invalidAlarmId
    case timeMustBeInFuture
    case notificationsNotAllowed

    var errorDescription: String? {
        switch self {
        case .invalidAlarmId:
            return "Alarm id must be a non-empty string"
        case .timeMustBeInFuture:
            return "Time must be in the future"
        case .notificationsNotAllowed:
            return "Notifications are not allowed on this device/app"
        }
    }
}
