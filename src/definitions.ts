export interface AlarmPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
