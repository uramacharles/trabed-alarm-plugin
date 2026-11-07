export interface AlarmPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;

  /**
   * Schedule a reminder at a Unix timestamp in milliseconds.
   * Android plays a notification sound once; durationMinutes is ignored there.
   * Without exact-alarm access, Android delivery may be delayed.
   */
  setAlarm(options: { id: string; time: number; message: string; durationMinutes?: number }): Promise<void>;
  cancelAlarm(options: { id: string }): Promise<void>;
  cancelAllAlarms(): Promise<void>;

  /** Android only. Check access to exact alarm scheduling. */
  checkExactAlarmPermission(): Promise<{ granted: boolean }>;
  /** Android only. Open alarm access settings if needed and recheck on return. */
  requestExactAlarmPermission(): Promise<{ granted: boolean }>;
  /** Android only. Check whether app notifications are allowed. */
  checkNotificationPermission(): Promise<{ granted: boolean }>;
  /** Android only. Request notification permission on Android 13+. */
  requestNotificationPermission(): Promise<{ granted: boolean }>;
}
