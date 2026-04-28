export interface AlarmPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;

  setAlarm(options: {
    id: string;
    time: number;
    message: string;
    durationMinutes?: number;
  }): Promise<void>;
  cancelAlarm(options: { id: string }): Promise<void>;
  cancelAllAlarms(): Promise<void>;
}
