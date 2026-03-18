import { EventType, EventPayload, EventHandler, LogLevel, EventCategory } from './EventType';
import EventHandle from './EventHandle';

class EventAssist {
  private static instance: EventAssist;
  private debugMode = false;

  private constructor() {}

  public static getInstance(): EventAssist {
    if (!EventAssist.instance) {
      EventAssist.instance = new EventAssist();
    }
    return EventAssist.instance;
  }

  public emit(eventType: string, data?: unknown): void {
    const category = this.getCategoryForEvent(eventType);
    const payload: EventPayload = {
      type: eventType as EventType,
      category,
      data: data || {},
      timestamp: Date.now()
    };
    EventHandle.publish(payload);
  }

  public once(eventType: string, handler: EventHandler): void {
    EventHandle.once(eventType, handler);
  }

  public log(level: LogLevel, category: EventCategory, message: string, data?: Record<string, unknown>): void {
    const payload: EventPayload = {
      type: EventType.DEBUG_LOG,
      category,
      data: { level, message, ...data },
      timestamp: Date.now()
    };
    EventHandle.publish(payload);

    if (this.debugMode) {
      this.outputToConsole(level, message, data);
    }
  }

  public info(category: EventCategory, message: string, data?: Record<string, unknown>): void {
    this.log(LogLevel.INFO, category, message, data);
  }

  public debug(category: EventCategory, message: string, data?: Record<string, unknown>): void {
    this.log(LogLevel.DEBUG, category, message, data);
  }

  public warn(category: EventCategory, message: string, data?: Record<string, unknown>): void {
    this.log(LogLevel.WARN, category, message, data);
  }

  public error(category: EventCategory, message: string, data?: Record<string, unknown>): void {
    this.log(LogLevel.ERROR, category, message, data);
  }

  public trace(flowId: string, step: string, data?: Record<string, unknown>): void {
    const payload: EventPayload = {
      type: EventType.DEBUG_TRACE,
      category: 'debug',
      data: { flowId, step, ...data },
      timestamp: Date.now()
    };
    EventHandle.publish(payload);

    if (this.debugMode) {
      console.log(`[TRACE] ${flowId} > ${step}`, data || '');
    }
  }

  public record(operation: string, data?: Record<string, unknown>): void {
    const payload: EventPayload = {
      type: EventType.UI_CLICK,
      category: 'ui',
      data: { operation, ...data },
      timestamp: Date.now()
    };
    EventHandle.publish(payload);

    if (this.debugMode) {
      console.log(`[RECORD] ${operation}`, data || '');
    }
  }

  public setDebugMode(enabled: boolean): void {
    this.debugMode = enabled;
    if (enabled) {
      console.log('[EventAssist] Debug mode enabled');
    }
  }

  public isDebugMode(): boolean {
    return this.debugMode;
  }

  private getCategoryForEvent(eventType: string): EventCategory {
    if (eventType.startsWith('SESSION_')) return 'session';
    if (eventType.startsWith('AUTH_')) return 'auth';
    if (eventType.startsWith('LOG_')) return 'data';
    if (eventType.startsWith('SYNC_') || eventType.startsWith('CLOUD_')) return 'sync';
    if (eventType.startsWith('SETTINGS_')) return 'settings';
    if (eventType.startsWith('THEME_')) return 'theme';
    if (eventType.startsWith('VIEW_')) return 'view';
    if (eventType.startsWith('STORAGE_')) return 'storage';
    if (eventType.startsWith('UI_')) return 'ui';
    if (eventType.startsWith('DEBUG_') || eventType.startsWith('HELPER_')) return 'debug';
    return 'helper';
  }

  private outputToConsole(level: LogLevel, message: string, data?: Record<string, unknown>): void {
    const prefix = `[${level.toUpperCase()}]`;
    switch (level) {
      case LogLevel.DEBUG:
        console.debug(prefix, message, data || '');
        break;
      case LogLevel.INFO:
        console.info(prefix, message, data || '');
        break;
      case LogLevel.WARN:
        console.warn(prefix, message, data || '');
        break;
      case LogLevel.ERROR:
        console.error(prefix, message, data || '');
        break;
    }
  }
}

export { EventAssist };
export default EventAssist.getInstance();
