export * from './auth';
export * from './storage';
export * from './logs';

export type Platform = 'web' | 'android';

export enum ViewState {
  DASHBOARD = 'DASHBOARD',
  ANALYSIS = 'ANALYSIS',
  HISTORY = 'HISTORY',
  API = 'API',
  SETTINGS = 'SETTINGS',
}

export interface DaySummary {
  date: string;
  count: number;
  logs: import('./storage').SmokeLog[];
}

export interface FeishuTableInfo {
  table_id: string;
  table_name: string;
}

export interface FeishuRecord {
  [key: string]: unknown;
  '记录日期': string;
}

export interface FeishuTableData {
  table_info: FeishuTableInfo;
  records: FeishuRecord[];
}

export interface FeishuApiResponse {
  code: number;
  msg: string;
  data: unknown;
}

export interface HistoryFilters {
  startDate?: string;
  endDate?: string;
  minCount?: number;
  maxCount?: number;
}

export interface HistorySort {
  field: 'date' | 'count';
  direction: 'asc' | 'desc';
}

export interface HistoryPagination {
  currentPage: number;
  pageSize: number;
  totalCount: number;
}

export interface OperationLog {
  id: string;
  type: 'create' | 'update' | 'delete' | 'clear' | 'sync';
  data: import('./storage').SmokeLog;
  syncStatus?: 'pending' | 'synced' | 'failed';
  timestamp: number;
  message?: string;
  apiFetchedCount?: number;
}

export interface SyncStatus {
  type: 'upload' | 'download' | 'sync' | 'create' | 'update' | 'delete';
  status: 'pending' | 'success' | 'error';
  message: string;
  timestamp: number;
}

export interface DataStorageAdapter {
  getLogs: () => Promise<import('./storage').SmokeLog[]>;
  saveLogs: (logs: import('./storage').SmokeLog[]) => Promise<void>;
  getSettings: () => Promise<import('./storage').AppSettings>;
  saveSettings: (settings: import('./storage').AppSettings) => Promise<void>;
  getApiSettings: () => Promise<import('./storage').EncryptedApiSettings | null>;
  saveApiSettings: (settings: import('./storage').EncryptedApiSettings) => Promise<void>;
  deleteApiSettings: () => Promise<void>;
  clearAll: () => Promise<void>;
  clearLogsOnly: () => Promise<void>;
}
