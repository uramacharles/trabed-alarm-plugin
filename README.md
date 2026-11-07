# trabed-alarm-plugin

This is an alarm system that tends to interact with react apps and set alarms for users

## Install

```bash
npm install trabed-alarm-plugin
npx cap sync
```

## Android changes in 0.0.2

Task reminders now post a high-priority notification with a bundled short chime and
a Dismiss button. Sound plays once, subject to the user's volume, notification
channel settings, and Do Not Disturb settings. There is no looping playback or
foreground service. `durationMinutes` remains accepted for compatibility but is
ignored on Android. iOS and web reminder behavior is unchanged.

The plugin declares only `POST_NOTIFICATIONS` and `SCHEDULE_EXACT_ALARM`. It no
longer declares `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, or
`USE_EXACT_ALARM`. A new sound channel is used because Android cannot change the
sound of the old, silent channel after it has been created.

### Permissions and scheduling

The four permission methods below are Android-only. Guard them with
`Capacitor.getPlatform()`; they are not implemented on iOS or web.

```typescript
import { Capacitor } from '@capacitor/core';
import { AlarmPlugin } from 'trabed-alarm-plugin';

async function scheduleTaskReminder(id: string, time: number, message: string) {
  if (Capacitor.getPlatform() === 'android') {
    // Call this from a user action, after explaining why reminders need permission.
    const notifications = await AlarmPlugin.requestNotificationPermission();
    if (!notifications.granted) {
      throw new Error('Enable notifications in app settings to receive task reminders.');
    }

    const exact = await AlarmPlugin.checkExactAlarmPermission();
    if (!exact.granted) {
      // Opens Android's special access settings and returns the state on return.
      const result = await AlarmPlugin.requestExactAlarmPermission();
      if (!result.granted) {
        console.info('The reminder will use approximate timing and may arrive late.');
      }
    }
  }

  await AlarmPlugin.setAlarm({ id, time, message });
}
```

`setAlarm` does not open permission prompts itself. On Android 12+, it checks
`canScheduleExactAlarms()` and uses `setExactAndAllowWhileIdle()` when allowed;
otherwise it uses `setAndAllowWhileIdle()`. On older Android versions it uses
exact scheduling. Missing exact access does not reject the reminder. Missing
notification permission prevents delivery. On Android 12 and below, or after a
notification permission is permanently denied, notifications must be enabled in
the app's system settings. The check methods report app-level access; a user can
also disable the reminder channel separately.

Cancellation removes both pending reminders and delivered notifications. Dismiss
and swipe-away remove the notification from the plugin's tracked IDs. Scheduled
reminders are not restored after device reboot. Revoking exact-alarm access can
also cancel already scheduled exact alarms; the app must schedule those again.

### Updating the consuming app

After publishing 0.0.2, install it in the app and sync native dependencies:

```bash
npm install trabed-alarm-plugin@0.0.2
npx cap sync android
```

Update the app's reminder permission flow using the example above. Build a new
AAB and inspect its merged manifest to confirm the removed permissions and
`AlarmPlaybackService` are absent. Other dependencies or the app manifest can
still declare those permissions. Submit that new app bundle to Google Play;
updating the npm package alone does not update the live Play Store bundle.

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`setAlarm(...)`](#setalarm)
* [`cancelAlarm(...)`](#cancelalarm)
* [`cancelAllAlarms()`](#cancelallalarms)
* [`checkExactAlarmPermission()`](#checkexactalarmpermission)
* [`requestExactAlarmPermission()`](#requestexactalarmpermission)
* [`checkNotificationPermission()`](#checknotificationpermission)
* [`requestNotificationPermission()`](#requestnotificationpermission)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### setAlarm(...)

```typescript
setAlarm(options: { id: string; time: number; message: string; durationMinutes?: number; }) => Promise<void>
```

Schedule a reminder at a Unix timestamp in milliseconds.
Android plays a notification sound once; durationMinutes is ignored there.
Without exact-alarm access, Android delivery may be delayed.

| Param         | Type                                                                                  |
| ------------- | ------------------------------------------------------------------------------------- |
| **`options`** | <code>{ id: string; time: number; message: string; durationMinutes?: number; }</code> |

--------------------


### cancelAlarm(...)

```typescript
cancelAlarm(options: { id: string; }) => Promise<void>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: string; }</code> |

--------------------


### cancelAllAlarms()

```typescript
cancelAllAlarms() => Promise<void>
```

--------------------


### checkExactAlarmPermission()

```typescript
checkExactAlarmPermission() => Promise<{ granted: boolean; }>
```

Android only. Check access to exact alarm scheduling.

**Returns:** <code>Promise&lt;{ granted: boolean; }&gt;</code>

--------------------


### requestExactAlarmPermission()

```typescript
requestExactAlarmPermission() => Promise<{ granted: boolean; }>
```

Android only. Open alarm access settings if needed and recheck on return.

**Returns:** <code>Promise&lt;{ granted: boolean; }&gt;</code>

--------------------


### checkNotificationPermission()

```typescript
checkNotificationPermission() => Promise<{ granted: boolean; }>
```

Android only. Check whether app notifications are allowed.

**Returns:** <code>Promise&lt;{ granted: boolean; }&gt;</code>

--------------------


### requestNotificationPermission()

```typescript
requestNotificationPermission() => Promise<{ granted: boolean; }>
```

Android only. Request notification permission on Android 13+.

**Returns:** <code>Promise&lt;{ granted: boolean; }&gt;</code>

--------------------

</docgen-api>
