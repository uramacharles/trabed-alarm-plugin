export interface AlarmPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;

  setAlarm(options: {
    id: number;
    time: number;
    message: string;
    durationMinutes?: number;
  }): Promise<void>;
  cancelAlarm(options: { id: number }): Promise<void>;
  cancelAllAlarms(): Promise<void>;
}
