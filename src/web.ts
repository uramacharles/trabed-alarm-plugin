import { WebPlugin } from '@capacitor/core';
import type { AlarmPluginPlugin } from './definitions';

type AlarmOptions = {
  id: number;
  time: number;
  message: string;
  durationMinutes?: number;
};

type ScheduledAlarm = {
  timeoutId: number;
  message: string;
  durationMinutes: number;
};

type ActiveAlarm = {
  stopTimeoutId: number;
  notification?: Notification;
  audioContext?: AudioContext;
  oscillator?: OscillatorNode;
  gainNode?: GainNode;
};

const DEFAULT_ALARM_DURATION_MINUTES = 3;

export class AlarmPluginWeb extends WebPlugin implements AlarmPluginPlugin {
  private scheduledAlarms = new Map<number, ScheduledAlarm>();
  private activeAlarms = new Map<number, ActiveAlarm>();

  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO (web):', options);
    return options;
  }

  async setAlarm(options: AlarmOptions): Promise<void> {
    const { id, time, message } = options;
    const durationMinutes =
      typeof options.durationMinutes === 'number' && options.durationMinutes > 0
        ? options.durationMinutes
        : DEFAULT_ALARM_DURATION_MINUTES;

    if (!Number.isInteger(id) || id < 0) {
      throw new Error('Alarm id must be a non-negative integer');
    }

    if (!Number.isFinite(time) || time <= Date.now()) {
      throw new Error('Time must be in the future');
    }

    this.cancelAlarmInternal(id);

    const delay = time - Date.now();
    const timeoutId = window.setTimeout(() => {
      this.scheduledAlarms.delete(id);
      void this.triggerAlarm(id, message, durationMinutes);
    }, delay);

    this.scheduledAlarms.set(id, {
      timeoutId,
      message,
      durationMinutes,
    });
  }

  async cancelAlarm(options: { id: number }): Promise<void> {
    this.cancelAlarmInternal(options.id);
  }

  async cancelAllAlarms(): Promise<void> {
    Array.from(this.scheduledAlarms.keys()).forEach((id) => this.cancelAlarmInternal(id));
    Array.from(this.activeAlarms.keys()).forEach((id) => this.stopAlarm(id));
  }

  private async triggerAlarm(
    id: number,
    message: string,
    durationMinutes: number,
  ): Promise<void> {
    if (this.activeAlarms.has(id)) {
      this.stopAlarm(id);
    }

    const notification = await this.showNotification(message);
    const audio = this.startWebTone();
    const stopTimeoutId = window.setTimeout(() => {
      this.stopAlarm(id);
    }, durationMinutes * 60_000);

    this.activeAlarms.set(id, {
      stopTimeoutId,
      notification: notification ?? undefined,
      audioContext: audio?.audioContext,
      oscillator: audio?.oscillator,
      gainNode: audio?.gainNode,
    });
  }

  private async showNotification(message: string): Promise<Notification | null> {
    if (typeof Notification === 'undefined') {
      console.warn('Browser notifications are not available.');
      return null;
    }

    let permission = Notification.permission;
    if (permission === 'default') {
      permission = await Notification.requestPermission();
    }

    if (permission !== 'granted') {
      console.warn('Notification permission was not granted for web alarm.');
      return null;
    }

    return new Notification('Alarm', {
      body: message,
      requireInteraction: true,
      tag: `trabed-alarm-${Date.now()}`,
    });
  }

  private startWebTone():
    | {
        audioContext: AudioContext;
        oscillator: OscillatorNode;
        gainNode: GainNode;
      }
    | null {
    const AudioContextCtor =
      window.AudioContext ||
      ((window as typeof window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext);

    if (!AudioContextCtor) {
      console.warn('Web Audio API is not available for alarm tone playback.');
      return null;
    }

    try {
      const audioContext = new AudioContextCtor();
      const oscillator = audioContext.createOscillator();
      const gainNode = audioContext.createGain();

      oscillator.type = 'sine';
      oscillator.frequency.setValueAtTime(880, audioContext.currentTime);
      gainNode.gain.setValueAtTime(0.05, audioContext.currentTime);

      oscillator.connect(gainNode);
      gainNode.connect(audioContext.destination);
      oscillator.start();

      return {
        audioContext,
        oscillator,
        gainNode,
      };
    } catch (error) {
      console.warn('Unable to start web alarm tone.', error);
      return null;
    }
  }

  private stopAlarm(id: number): void {
    const activeAlarm = this.activeAlarms.get(id);
    if (!activeAlarm) {
      return;
    }

    window.clearTimeout(activeAlarm.stopTimeoutId);
    activeAlarm.notification?.close();

    if (activeAlarm.oscillator) {
      activeAlarm.oscillator.stop();
      activeAlarm.oscillator.disconnect();
    }

    if (activeAlarm.gainNode) {
      activeAlarm.gainNode.disconnect();
    }

    if (activeAlarm.audioContext) {
      void activeAlarm.audioContext.close();
    }

    this.activeAlarms.delete(id);
  }

  private cancelAlarmInternal(id: number): void {
    const scheduledAlarm = this.scheduledAlarms.get(id);
    if (scheduledAlarm) {
      window.clearTimeout(scheduledAlarm.timeoutId);
      this.scheduledAlarms.delete(id);
    }

    this.stopAlarm(id);
  }
}
