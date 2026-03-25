import { Capacitor } from '@capacitor/core';
import { Preferences } from '@capacitor/preferences';
import { LogEntry, LogLevel, LogCategory, LogFilter, LogStats, DiagnosticConfig } from '../types';
import { getStorageAdapter, isAndroidPlatform } from './storageAdapter';

const NATIVE_LOGS_KEY = 'popsmoke_runtime_logs';
const NATIVE_LOGS_MAX_CHARS = 200000;

class SystemLogService {
  private logs: LogEntry[] = [];
  private config: DiagnosticConfig = {
    maxLogs: 1000,
    enableConsole: true,
    enableStorage: true,
    logLevel: 'info'
  };

  private static instance: SystemLogService;

  static getInstance(): SystemLogService {
    if (!SystemLogService.instance) {
      SystemLogService.instance = new SystemLogService();
    }
    return SystemLogService.instance;
  }

  private constructor() {
    void this.loadLogsFromStorage();
  }

  private isNativePlatform(): boolean {
    return Capacitor.isNativePlatform();
  }

  private async loadLogsFromStorage(): Promise<void> {
    try {
      if (isAndroidPlatform()) {
        // 安卓端使用SQLite存储
        const adapter = getStorageAdapter();
        if ('getSystemLogs' in adapter) {
          const value = await adapter.getSystemLogs();
          if (value) {
            this.logs = JSON.parse(value);
          }
        }
        return;
      } else if (this.isNativePlatform()) {
        // 其他原生平台使用Preferences
        const { value } = await Preferences.get({ key: 'system_logs' });
        if (value) {
          this.logs = JSON.parse(value);
        }
        return;
      }

      // Web端使用localStorage
      const stored = localStorage.getItem('system_logs');
      if (stored) {
        this.logs = JSON.parse(stored);
      }
    } catch (error) {
      console.error('Failed to load logs from storage:', error);
    }
  }

  private async appendNativeRuntimeLog(entry: LogEntry): Promise<void> {
    if (!this.isNativePlatform()) return;

    try {
      const timestamp = new Date(entry.timestamp).toISOString();
      const line = `[${timestamp}] [${entry.level.toUpperCase()}] [${entry.category}] ${entry.message}`;
      const payload = `${line}${entry.data ? ` | data=${JSON.stringify(entry.data)}` : ''}${entry.error ? ` | error=${entry.error.message}` : ''}\n`;

      if (isAndroidPlatform()) {
        // 安卓端使用SQLite存储
        const adapter = getStorageAdapter();
        if ('getNativeRuntimeLogs' in adapter && 'saveNativeRuntimeLogs' in adapter) {
          const existing = await adapter.getNativeRuntimeLogs();
          const merged = `${existing || ''}${payload}`;
          const trimmed = merged.length > NATIVE_LOGS_MAX_CHARS
            ? merged.slice(merged.length - NATIVE_LOGS_MAX_CHARS)
            : merged;

          await adapter.saveNativeRuntimeLogs(trimmed);
        }
      } else {
        // 其他原生平台使用Preferences
        const { value: existing } = await Preferences.get({ key: NATIVE_LOGS_KEY });
        const merged = `${existing || ''}${payload}`;
        const trimmed = merged.length > NATIVE_LOGS_MAX_CHARS
          ? merged.slice(merged.length - NATIVE_LOGS_MAX_CHARS)
          : merged;

        await Preferences.set({ key: NATIVE_LOGS_KEY, value: trimmed });
      }
    } catch (error) {
      console.error('Failed to append native runtime log:', error);
    }
  }

  private saveLogsToStorage(): void {
    try {
      if (!this.config.enableStorage) return;

      const serialized = JSON.stringify(this.logs);
      if (isAndroidPlatform()) {
        // 安卓端使用SQLite存储
        const adapter = getStorageAdapter();
        if ('saveSystemLogs' in adapter) {
          void adapter.saveSystemLogs(serialized);
        }
      } else if (this.isNativePlatform()) {
        // 其他原生平台使用Preferences
        void Preferences.set({ key: 'system_logs', value: serialized });
      } else {
        // Web端使用localStorage
        localStorage.setItem('system_logs', serialized);
      }
    } catch (error) {
      console.error('Failed to save logs to storage:', error);
    }
  }

  private addLog(entry: LogEntry): void {
    // Check log level
    const levelOrder = { debug: 0, info: 1, warn: 2, error: 3 };
    const configLevel = levelOrder[this.config.logLevel];
    const entryLevel = levelOrder[entry.level];

    if (entryLevel < configLevel) {
      return;
    }

    // Add to logs
    this.logs.unshift(entry);

    // Limit logs
    if (this.logs.length > this.config.maxLogs) {
      this.logs = this.logs.slice(0, this.config.maxLogs);
    }

    // Save to storage
    this.saveLogsToStorage();
    void this.appendNativeRuntimeLog(entry);

    // Log to console
    if (this.config.enableConsole) {
      this.logToConsole(entry);
    }
  }

  private logToConsole(entry: LogEntry): void {
    const timestamp = new Date(entry.timestamp).toISOString();
    const message = `[${entry.level.toUpperCase()}] [${entry.category}] ${entry.message}`;

    switch (entry.level) {
      case 'debug':
        console.debug(`[${timestamp}] ${message}`, entry.data || '');
        break;
      case 'info':
        console.info(`[${timestamp}] ${message}`, entry.data || '');
        break;
      case 'warn':
        console.warn(`[${timestamp}] ${message}`, entry.data || '');
        break;
      case 'error':
        console.error(`[${timestamp}] ${message}`, entry.data || '', entry.error || '');
        break;
    }
  }

  debug(category: LogCategory, message: string, data?: Record<string, unknown>): void {
    this.addLog({
      id: crypto.randomUUID(),
      timestamp: Date.now(),
      level: 'debug',
      category,
      message,
      data
    });
  }

  info(category: LogCategory, message: string, data?: Record<string, unknown>): void {
    this.addLog({
      id: crypto.randomUUID(),
      timestamp: Date.now(),
      level: 'info',
      category,
      message,
      data
    });
  }

  warn(category: LogCategory, message: string, data?: Record<string, unknown>): void {
    this.addLog({
      id: crypto.randomUUID(),
      timestamp: Date.now(),
      level: 'warn',
      category,
      message,
      data
    });
  }

  error(category: LogCategory, message: string, error?: Error, data?: Record<string, unknown>): void {
    this.addLog({
      id: crypto.randomUUID(),
      timestamp: Date.now(),
      level: 'error',
      category,
      message,
      data,
      error
    });
  }

  getLogs(filter?: LogFilter): LogEntry[] {
    let filtered = [...this.logs];

    if (filter) {
      if (filter.level) {
        filtered = filtered.filter(log => log.level === filter.level);
      }

      if (filter.category) {
        filtered = filtered.filter(log => log.category === filter.category);
      }

      if (filter.startDate !== undefined) {
        filtered = filtered.filter(log => log.timestamp >= filter.startDate!);
      }

      if (filter.endDate !== undefined) {
        filtered = filtered.filter(log => log.timestamp <= filter.endDate!);
      }

      if (filter.search) {
        const searchLower = filter.search.toLowerCase();
        filtered = filtered.filter(log => 
          log.message.toLowerCase().includes(searchLower) ||
          (log.data && JSON.stringify(log.data).toLowerCase().includes(searchLower))
        );
      }
    }

    return filtered;
  }

  getStats(): LogStats {
    const stats: LogStats = {
      total: this.logs.length,
      byLevel: { debug: 0, info: 0, warn: 0, error: 0 },
      byCategory: { init: 0, auth: 0, storage: 0, api: 0, sync: 0, ui: 0, error: 0, settings: 0 }
    };

    this.logs.forEach(log => {
      stats.byLevel[log.level]++;
      stats.byCategory[log.category]++;
    });

    return stats;
  }

  clearLogs(): void {
    this.logs = [];
    this.saveLogsToStorage();
  }

  setConfig(config: Partial<DiagnosticConfig>): void {
    this.config = { ...this.config, ...config };
  }

  getConfig(): DiagnosticConfig {
    return { ...this.config };
  }


  async getNativeRuntimeLogText(): Promise<string> {
    try {
      const { value } = await Preferences.get({ key: NATIVE_LOGS_KEY });
      return value || '';
    } catch {
      return '';
    }
  }
}


export const systemLogService = SystemLogService.getInstance();