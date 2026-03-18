export interface SmokeLog {
  id: string;
  user_id: string;
  table_id?: string | null;
  table_name?: string | null;
  record_date: string;
  record_time: string;
  record_index: number;
  timestamp: number;
  created_at?: string;
  date?: string;
  [key: string]: unknown;
}

export interface AppSettings {
  user_id: string;
  dailyLimit: number;
  warningLimit: number;
  themeColor: string;
  language: Language;
  avatarUrl?: string;
  allowCloudSync?: boolean;
  daily_limit?: number;
  warning_limit?: number;
  theme_color?: string;
  avatar_url?: string;
}

export type Language = 'en' | 'zh' | 'ja' | 'ko';

export interface EncryptedApiSettings {
  encryptedData?: string;
  updatedAt?: number;
  avatarCache?: string;
  feishu?: string;
  supabase?: string;
  securityPassword?: string;
  authData?: Record<string, string>;
}

export interface FeishuApiSettings {
  apiUrl: string;
  writeAccessKey?: string;
}

export interface SupabaseApiSettings {
  apiUrl: string;
  anonKey: string;
}

export interface ApiSettings {
  feishu: FeishuApiSettings;
  supabase: SupabaseApiSettings;
}

export interface StorageUsage {
  used: number;
  total: number;
  percent: number;
}

export interface StorageKey {
  logs: string;
  settings: string;
  apiSettings: string;
}

export interface LocalData {
  logs: SmokeLog[];
  settings: AppSettings;
  apiSettings: EncryptedApiSettings | null;
  hasLoggedIn: boolean;
}

export interface CloudData {
  logs: SmokeLog[];
  settings: AppSettings;
}

export interface SyncOptions {
  tableName?: string;
  tableId?: string;
  startDate?: string;
  endDate?: string;
  refresh?: boolean;
}

export interface SubmitOptions {
  tableName: string;
  recordIds?: string[];
  startDate?: string;
  endDate?: string;
}
