export enum EventType {
  SESSION_CREATE = 'SESSION_CREATE',
  SESSION_EXPIRED = 'SESSION_EXPIRED',
  SESSION_VALIDATE = 'SESSION_VALIDATE',
  AUTH_LOGIN = 'AUTH_LOGIN',
  AUTH_SIGNUP = 'AUTH_SIGNUP',
  AUTH_SIGNIN = 'AUTH_SIGNIN',
  AUTH_LOGOUT = 'AUTH_LOGOUT',
  AUTH_PASSWORD_RESET = 'AUTH_PASSWORD_RESET',
  LOG_CREATE = 'LOG_CREATE',
  LOG_UPDATE = 'LOG_UPDATE',
  LOG_DELETE = 'LOG_DELETE',
  SYNC_START = 'SYNC_START',
  SYNC_SUCCESS = 'SYNC_SUCCESS',
  SYNC_ERROR = 'SYNC_ERROR',
  CLOUD_DOWNLOAD = 'CLOUD_DOWNLOAD',
  CLOUD_UPLOAD = 'CLOUD_UPLOAD',
  SETTINGS_CHANGE = 'SETTINGS_CHANGE',
  THEME_CHANGE = 'THEME_CHANGE',
  VIEW_CHANGE = 'VIEW_CHANGE',
  STORAGE_WARNING = 'STORAGE_WARNING',
  STORAGE_CLEARED = 'STORAGE_CLEARED',
  UI_CLICK = 'UI_CLICK',
  UI_INPUT = 'UI_INPUT',
  UI_NAVIGATE = 'UI_NAVIGATE',
  UI_MODAL_OPEN = 'UI_MODAL_OPEN',
  UI_MODAL_CLOSE = 'UI_MODAL_CLOSE',
  DEBUG_LOG = 'DEBUG_LOG',
  DEBUG_TRACE = 'DEBUG_TRACE',
  HELPER_INIT = 'HELPER_INIT',
  HELPER_CANCEL = 'HELPER_CANCEL',
}

export type EventCategory = 'session' | 'auth' | 'data' | 'sync' | 'settings' | 'theme' | 'view' | 'storage' | 'ui' | 'debug' | 'helper';

export interface EventPayload<T = unknown> {
  type: EventType;
  category: EventCategory;
  data: T;
  timestamp: number;
}

export interface EventHandler {
  (event: EventPayload): void;
}

export interface Subscription {
  unsubscribe(): void;
}

export interface SubscribeOptions {
  once?: boolean;
  priority?: number;
}

export enum LogLevel {
  DEBUG = 'debug',
  INFO = 'info',
  WARN = 'warn',
  ERROR = 'error',
}
