import { WebPlugin } from '@capacitor/core';

import type { AlarmPluginPlugin } from './definitions';

export class AlarmPluginWeb extends WebPlugin implements AlarmPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
