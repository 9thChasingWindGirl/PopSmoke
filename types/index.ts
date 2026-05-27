export * from './auth';
export * from './storage';
export * from './logs';
export * from './common';

// 显式重新导出常用类型，解决循环引用问题
export type { SmokeLog, AppSettings, EncryptedApiSettings, Language } from './storage';
export type { User } from './auth';

// 导入 SmokeLog 用于接口定义（避免循环引用）
import type { SmokeLog, AppSettings, EncryptedApiSettings } from './storage';
import type { User } from './auth';

export type Platform = 'web' | 'android';

export enum ViewState {
  DASHBOARD = 'DASHBOARD',
  ANALYSIS = 'ANALYSIS',
  HISTORY = 'HISTORY',
  API = 'API',
  SETTINGS = 'SETTINGS',
}

// ============ 同步相关类型 (使用 storage.ts 中的基础类型) ============

/** 同步差异结果 */
export interface SyncDiffResult {
  diff: DataDiff;
  source: 'feishu' | 'supabase';
  timestamp: number;
  message: string;
}

/** 数据差异详情 */
export interface DataDiff {
  localOnly: SmokeLog[];
  cloudOnly: SmokeLog[];
  conflicting: SmokeLog[];
  fieldsToUpdate: SmokeLog[];
  totalLocal: number;
  totalCloud: number;
}

/** 云端记录（通用） */
export interface CloudRecord {
  id: string;
  [key: string]: unknown;
}

// ============ API 设置扩展类型 ============

/** 飞书 API 配置 */
export interface FeishuApiConfig {
  apiUrl: string;
  writeAccessKey?: string;
  [key: string]: unknown;
}

/** Supabase 运行时配置 */
export interface SupabaseRuntimeConfig {
  apiUrl: string;
  anonKey: string;
  [key: string]: unknown;
}

// ============ 初始化回调类型 ============

/** 应用初始化回调 */
export interface InitializationCallbacks {
  onSettingsLoaded?: (settings: AppSettings) => void;
  onLogsLoaded?: (logs: SmokeLog[]) => void;
  onAuthStateChange?: (user: User | null, status: import('./auth').AuthStatus, error?: Error) => void;
  onInitComplete?: () => void;
  onCheckCloudData?: (localLogsCount: number, savedApiSettings: EncryptedApiSettings | null, hasLoggedIn: boolean) => Promise<void>;
}

// ============ 操作日志类型 ============

export interface DaySummary {
  date: string;
  count: number;
  logs: SmokeLog[];
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
  type: 'create' | 'update' | 'delete' | 'clear' | 'sync' | 'system';
  action?: string;
  data?: Partial<SmokeLog>;
  syncStatus?: 'pending' | 'synced' | 'failed';
  timestamp: number;
  message?: string;
  details?: string;
  apiFetchedCount?: number;
}

export interface SyncStatus {
  type: 'upload' | 'download' | 'sync' | 'create' | 'update' | 'delete' | 'system';
  status: 'pending' | 'success' | 'error';
  message: string;
  timestamp: number;
}

export interface DataStorageAdapter {
  getLogs: () => Promise<SmokeLog[]>;
  saveLogs: (logs: SmokeLog[]) => Promise<void>;
  getSettings: () => Promise<AppSettings>;
  saveSettings: (settings: AppSettings) => Promise<void>;
  getApiSettings: () => Promise<EncryptedApiSettings | null>;
  saveApiSettings: (settings: EncryptedApiSettings) => Promise<void>;
  deleteApiSettings: () => Promise<void>;
  clearAll: () => Promise<void>;
  clearLogsOnly: () => Promise<void>;
  // 安卓端扩展方法
  getAuthItem?: (key: string) => Promise<string | null>;
  setAuthItem?: (key: string, value: string) => Promise<void>;
  removeAuthItem?: (key: string) => Promise<void>;
  getSystemLogs?: () => Promise<string | null>;
  saveSystemLogs?: (value: string) => Promise<void>;
  getNativeRuntimeLogs?: () => Promise<string | null>;
  saveNativeRuntimeLogs?: (value: string) => Promise<void>;
  // 连接管理方法
  closeConnection?: () => Promise<void>;
}
