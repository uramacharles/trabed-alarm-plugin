import { registerPlugin } from '@capacitor/core';

import type { AlarmPluginPlugin } from './definitions';

const AlarmPlugin = registerPlugin<AlarmPluginPlugin>('AlarmPlugin', {
  web: () => import('./web').then((m) => new m.AlarmPluginWeb()),
});

export * from './definitions';
export { AlarmPlugin };
