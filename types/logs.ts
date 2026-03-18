export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

export type LogCategory = 'init' | 'auth' | 'storage' | 'api' | 'sync' | 'ui' | 'error' | 'settings';

export interface LogEntry {
  id: string;
  timestamp: number;
  level: LogLevel;
  category: LogCategory;
  message: string;
  data?: Record<string, unknown>;
  error?: Error;
}

export interface LogFilter {
  level?: LogLevel;
  category?: LogCategory;
  startDate?: number;
  endDate?: number;
  search?: string;
}

export interface LogStats {
  total: number;
  byLevel: Record<LogLevel, number>;
  byCategory: Record<LogCategory, number>;
}

export interface DiagnosticConfig {
  maxLogs: number;
  enableConsole: boolean;
  enableStorage: boolean;
  logLevel: LogLevel;
}

export interface LogDisplayOptions {
  maxVisible: number;
  showTimestamp: boolean;
  showCategory: boolean;
  showData: boolean;
  compactMode: boolean;
}
