import { SmokeLog, AppSettings, FeishuApiSettings, SyncOptions, SubmitOptions } from '../types';
import { getStorageAdapter, simpleEncrypt, simpleDecrypt } from './storageAdapter';
import { Capacitor } from '@capacitor/core';
import { createClient, SupabaseClient } from '@supabase/supabase-js';
import EventHandle from '../event/EventHandle';
import { EventType } from '../event/EventType';
import { TRANSLATIONS } from '../i18n';

const DEFAULT_SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL || '';
const DEFAULT_SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY || '';

export interface SyncResult {
  success: boolean;
  message: string;
  count?: number;
  totalCount?: number;
  newCount?: number;
  duplicateCount?: number;
}

export interface CloudSyncResult {
  success: boolean;
  message: string;
  localCount: number;
  cloudCount: number;
  uploadedCount: number;
  downloadedCount: number;
  totalCount: number;
}

export interface DataDiff {
  localOnly: SmokeLog[];     // 仅本地有（需要上传）
  cloudOnly: SmokeLog[];     // 仅云端有（需要下载）
  conflicting: SmokeLog[];    // 冲突记录
  totalLocal: number;
  totalCloud: number;
}

export interface SyncDiffResult {
  diff: DataDiff;
  source: 'feishu' | 'supabase';
  timestamp: number;
  message: string;
}

export interface ApiServiceConfig {
  feishu?: {
    apiUrl: string;
    writeAccessKey: string;
  };
  supabase?: {
    apiUrl: string;
    anonKey: string;
  };
}

class SupabaseClientManager {
  private static instance: SupabaseClientManager;
  private client: SupabaseClient | null = null;
  private config: { apiUrl: string; anonKey: string } | null = null;
  private defaultClient: SupabaseClient;
  private isInitialized = false;

  private constructor() {
    this.defaultClient = createClient(
      DEFAULT_SUPABASE_URL || 'https://your-project.supabase.co',
      DEFAULT_SUPABASE_ANON_KEY || 'your-anon-key',
      { auth: { persistSession: true, autoRefreshToken: true } }
    );
    this.client = this.defaultClient;
  }

  public static getInstance(): SupabaseClientManager {
    if (!SupabaseClientManager.instance) {
      SupabaseClientManager.instance = new SupabaseClientManager();
    }
    return SupabaseClientManager.instance;
  }

  public getClient(): SupabaseClient {
    return this.client || this.defaultClient;
  }

  public setClient(apiUrl: string, anonKey: string): SupabaseClient {
    if (this.config && this.config.apiUrl === apiUrl && this.config.anonKey === anonKey) {
      return this.client!;
    }
    const newClient = createClient(apiUrl, anonKey, { auth: { persistSession: true, autoRefreshToken: true } });
    this.client = newClient;
    this.config = { apiUrl, anonKey };
    return newClient;
  }

  public hasUserConfig(): boolean {
    return this.config !== null;
  }

  public resetToDefault(): void {
    this.client = this.defaultClient;
    this.config = null;
  }

  public getConfig(): { apiUrl: string; anonKey: string } | null {
    return this.config;
  }

  public setInitialized(value: boolean): void {
    this.isInitialized = value;
  }

  public getInitialized(): boolean {
    return this.isInitialized;
  }
}

const clientManager = SupabaseClientManager.getInstance();

const getClient = async (): Promise<SupabaseClient> => {
  if (!clientManager.getInitialized()) {
    try {
      const userConfig = await getUserSupabaseConfig();
      if (userConfig) {
        clientManager.setClient(userConfig.apiUrl, userConfig.anonKey);
        clientManager.setInitialized(true);
      }
    } catch (error) {
      console.error('Failed to initialize Supabase client:', error);
    }
    clientManager.setInitialized(true);
  }
  return clientManager.getClient();
};

export const getUserSupabaseConfig = async (): Promise<{ apiUrl: string; anonKey: string } | null> => {
  try {
    // Web 端优先使用环境变量配置
    const { isWebPlatform } = await import('./storageAdapter');
    if (isWebPlatform()) {
      const envUrl = (import.meta.env.VITE_SUPABASE_URL || '').trim();
      const envKey = (import.meta.env.VITE_SUPABASE_ANON_KEY || '').trim();
      
      if (envUrl && envKey) {
        console.log('[getUserSupabaseConfig] Using environment variable config for Web platform');
        return { apiUrl: envUrl, anonKey: envKey };
      }
    }
    
    const { getSupabaseRuntimeConfig } = await import('./storageAdapter');
    const runtimeConfig = await getSupabaseRuntimeConfig();
    if (runtimeConfig?.apiUrl && runtimeConfig?.anonKey) {
      return runtimeConfig;
    }

    const adapter = getStorageAdapter();
    const savedSettings = await adapter.getApiSettings();

    if (!savedSettings || !savedSettings.supabase) {
      return null;
    }

    try {
      const supabaseConfig = JSON.parse(savedSettings.supabase);
      if (supabaseConfig.apiUrl && supabaseConfig.anonKey) {
        return { apiUrl: supabaseConfig.apiUrl, anonKey: supabaseConfig.anonKey };
      }
    } catch (e) {
      console.log('Supabase config is encrypted');
    }

    return null;
  } catch (error) {
    console.error('Failed to get user Supabase config:', error);
    return null;
  }
};

export const isApiConfigEncrypted = async (source: 'feishu' | 'supabase'): Promise<boolean> => {
  try {
    const adapter = getStorageAdapter();
    const savedSettings = await adapter.getApiSettings();

    if (!savedSettings) {
      return false;
    }

    const configField = source === 'feishu' ? savedSettings.feishu : savedSettings.supabase;
    if (!configField) {
      return false;
    }

    // Web 端特殊处理：检查 Supabase 配置是否来自环境变量
    if (source === 'supabase') {
      const { isWebPlatform } = await import('./storageAdapter');
      if (isWebPlatform()) {
        // Web 端：检查环境变量中是否有 Supabase 配置
        const envUrl = (import.meta.env.VITE_SUPABASE_URL || '').trim();
        const envKey = (import.meta.env.VITE_SUPABASE_ANON_KEY || '').trim();
        
        // 如果环境变量中有配置，说明可能是自动保存的配置，不需要密码
        if (envUrl && envKey) {
          console.log('[isApiConfigEncrypted] Web platform with env config, treating as unencrypted');
          return false;
        }
      }
    }

    try {
      JSON.parse(configField);
      return false;
    } catch (e) {
      return true;
    }
  } catch (error) {
    console.error(`Failed to check ${source} config encryption:`, error);
    return false;
  }
};

export const decryptSupabaseConfig = async (password: string): Promise<{ apiUrl: string; anonKey: string } | null> => {
  try {
    const adapter = getStorageAdapter();
    const savedSettings = await adapter.getApiSettings();

    if (!savedSettings) return null;

    const decryptedPassword = simpleDecrypt(savedSettings.securityPassword || '', password);
    if (decryptedPassword !== password) {
      return null;
    }

    const decryptedSupabase = JSON.parse(simpleDecrypt(savedSettings.supabase || '', password));
    return { apiUrl: decryptedSupabase.apiUrl || '', anonKey: decryptedSupabase.anonKey || '' };
  } catch (error) {
    console.error('Failed to decrypt Supabase config:', error);
    return null;
  }
};

export const createSupabaseClient = (apiUrl: string, anonKey: string): SupabaseClient => {
  return clientManager.setClient(apiUrl, anonKey);
};

export const getSupabaseClient = async (): Promise<SupabaseClient> => {
  return await getClient();
};

export const hasUserSupabaseConfig = (): boolean => {
  return clientManager.hasUserConfig();
};

export const initializeSupabaseClient = async (apiUrl: string, anonKey: string): Promise<SupabaseClient> => {
  return clientManager.setClient(apiUrl, anonKey);
};

export const isSupabaseClientInitialized = (): boolean => {
  return clientManager.getInitialized();
};

export const persistSupabaseRuntimeConfig = async (apiUrl: string, anonKey: string): Promise<void> => {
  const { setSupabaseRuntimeConfig } = await import('./storageAdapter');
  await setSupabaseRuntimeConfig({ apiUrl, anonKey });
};

export const clearPersistedSupabaseRuntimeConfig = async (): Promise<void> => {
  const { clearSupabaseRuntimeConfig } = await import('./storageAdapter');
  await clearSupabaseRuntimeConfig();
};

export const resetSupabaseClient = (): void => {
  clientManager.resetToDefault();
};

export const setSupabaseClient = (client: SupabaseClient, config: { apiUrl: string; anonKey: string }): void => {
  clientManager.setClient(config.apiUrl, config.anonKey);
};

const generateLogId = (userId: string, recordDate: string, recordTime: string, index?: number): string => {
  return crypto.randomUUID();
};

const getUniqueKey = (log: SmokeLog): string => `${log.user_id}_${log.record_date}_${log.record_time}`;

const compareData = (localLogs: SmokeLog[], cloudLogs: SmokeLog[]): DataDiff => {
  const localKeys = new Map<string, SmokeLog>();
  const cloudKeys = new Map<string, SmokeLog>();
  
  localLogs.forEach(log => {
    localKeys.set(getUniqueKey(log), log);
  });
  
  cloudLogs.forEach(log => {
    cloudKeys.set(getUniqueKey(log), log);
  });
  
  const localOnly: SmokeLog[] = [];
  const cloudOnly: SmokeLog[] = [];
  const conflicting: SmokeLog[] = [];
  
  // 检查仅本地有的记录
  localKeys.forEach((log, key) => {
    if (!cloudKeys.has(key)) {
      localOnly.push(log);
    }
  });
  
  // 检查仅云端有的记录
  cloudKeys.forEach((log, key) => {
    if (!localKeys.has(key)) {
      cloudOnly.push(log);
    } else {
      // 检查冲突记录（相同键但内容不同）
      const localLog = localKeys.get(key);
      if (localLog) {
        // 比较关键属性，忽略可能不同的属性（如id、sync_status等）
        const isConflicting = localLog.record_date !== log.record_date ||
                            localLog.record_time !== log.record_time ||
                            localLog.category !== log.category ||
                            localLog.operation !== log.operation ||
                            localLog.duration !== log.duration ||
                            localLog.content !== log.content;
        
        if (isConflicting) {
          conflicting.push(log);
        }
      }
    }
  });
  
  return {
    localOnly,
    cloudOnly,
    conflicting,
    totalLocal: localLogs.length,
    totalCloud: cloudLogs.length
  };
};

const formatTimestamp = (date: Date): string => {
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, '0');
  const day = String(date.getUTCDate()).padStart(2, '0');
  const hours = String(date.getUTCHours()).padStart(2, '0');
  const minutes = String(date.getUTCMinutes()).padStart(2, '0');
  const seconds = String(date.getUTCSeconds()).padStart(2, '0');
  const milliseconds = String(date.getUTCMilliseconds()).padStart(3, '0');
  const microseconds = '000'; // JavaScript Date only provides millisecond precision, so we add 3 zeros for microseconds
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.${milliseconds}${microseconds}+00`;
};

const completeLogFields = (log: SmokeLog): SmokeLog => ({
  ...log,
  user_id: log.user_id || 'local',
  table_id: log.table_id || null,
  table_name: log.table_name || null,
  record_date: log.record_date || '',
  record_time: log.record_time || '',
  record_index: log.record_index || 1,
  timestamp: log.timestamp || Date.now(),
  created_at: log.created_at || formatTimestamp(new Date())
});

export const getFeishuApiSettings = async (): Promise<FeishuApiSettings> => {
  try {
    const adapter = getStorageAdapter();
    const apiSettings = await adapter.getApiSettings();
    if (apiSettings && apiSettings.feishu) {
      return { apiUrl: '__ENCRYPTED__' };
    }
  } catch (error) {
    console.error('Failed to get Feishu API settings:', error);
  }
  return { apiUrl: '' };
};

export const getFeishuApiSettingsWithPassword = async (password: string): Promise<FeishuApiSettings> => {
  try {
    const adapter = getStorageAdapter();
    const apiSettings = await adapter.getApiSettings();
    if (apiSettings && apiSettings.feishu) {
      const decrypted = simpleDecrypt(apiSettings.feishu, password);
      const parsed = JSON.parse(decrypted);
      return { apiUrl: parsed.apiUrl || '', writeAccessKey: parsed.writeAccessKey || '' };
    }
  } catch (error) {
    console.error('Failed to decrypt Feishu API settings:', error);
  }
  return { apiUrl: '' };
};

export const saveFeishuApiSettingsWithPassword = async (settings: FeishuApiSettings, password: string, existingData?: { supabaseUrl?: string; supabaseKey?: string }): Promise<void> => {
  try {
    const adapter = getStorageAdapter();
    const dataToEncrypt = {
      feishuUrl: settings.apiUrl,
      feishuWriteAccessKey: settings.writeAccessKey || '',
      supabaseUrl: existingData?.supabaseUrl || '',
      supabaseKey: existingData?.supabaseKey || ''
    };
    const encryptedData = simpleEncrypt(JSON.stringify(dataToEncrypt), password);
    await adapter.saveApiSettings({ encryptedData, updatedAt: Date.now() });
  } catch (error) {
    console.error('Failed to save Feishu API settings:', error);
    throw error;
  }
};

export const saveFeishuApiSettings = async (settings: FeishuApiSettings): Promise<void> => {
  console.warn('Use saveFeishuApiSettingsWithPassword for all platforms');
};

export const syncFromFeishu = async (options: SyncOptions, userId?: string, password?: string): Promise<SyncResult> => {
  try {
    let feishuSettings: FeishuApiSettings;
    if (password) {
      feishuSettings = await getFeishuApiSettingsWithPassword(password);
    } else {
      feishuSettings = await getFeishuApiSettings();
    }

    if (!feishuSettings.apiUrl) {
      return { success: false, message: 'Feishu API URL is not configured' };
    }

    if (feishuSettings.apiUrl === '__ENCRYPTED__') {
      return { success: false, message: 'Feishu API settings are encrypted. Please provide password.' };
    }

    const tables = await fetchAllTables(feishuSettings, options.refresh || false);
    const allNewLogs: SmokeLog[] = [];

    for (const table of tables) {
      const logs = convertToSmokeLogs(table, userId || 'local');
      allNewLogs.push(...logs);
    }

    const storageService = getStorageAdapter();
    const existingLogs = await storageService.getLogs();
    const existingKeys = new Set(existingLogs.map(getUniqueKey));
    const uniqueNewLogs = allNewLogs.filter((log) => !existingKeys.has(getUniqueKey(log)));
    const duplicateCount = allNewLogs.length - uniqueNewLogs.length;

    if (uniqueNewLogs.length === 0) {
      return { success: true, message: 'No new records to sync', count: 0, totalCount: allNewLogs.length, newCount: 0, duplicateCount };
    }

    const mergedLogs = [...uniqueNewLogs, ...existingLogs].sort((a, b) => b.timestamp - a.timestamp);
    await storageService.saveLogs(mergedLogs);

    return {
      success: true,
      message: `Synced ${uniqueNewLogs.length} new records from Feishu`,
      count: uniqueNewLogs.length,
      totalCount: allNewLogs.length,
      newCount: uniqueNewLogs.length,
      duplicateCount
    };
  } catch (error) {
    return { success: false, message: error instanceof Error ? error.message : 'Failed to sync from Feishu' };
  }
};

interface FeishuTableData {
  table_info: { table_id: string; table_name: string };
  records: Record<string, any>[];
}

interface FeishuApiResponse {
  code: number;
  data?: FeishuTableData[] | FeishuTableData;
  msg?: string;
}

export const fetchAllTables = async (settings: FeishuApiSettings, refresh: boolean = false): Promise<FeishuTableData[]> => {
  try {
    if (!settings.apiUrl) throw new Error('飞书API地址未配置');

    let baseUrl = settings.apiUrl.replace(/\/(tableall|table|alltable)(\/json)?$/i, '');
    let url = `${baseUrl}/tableall${refresh ? '?refresh=1' : ''}`;
    url += (url.includes('?') ? '&' : '?') + `_t=${Date.now()}`;

    const response = await fetch(url, { cache: 'no-store' });
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    const data: FeishuApiResponse = await response.json();

    if (data.code !== 200) {
      throw new Error(String(data.msg) || 'Failed to fetch tables');
    }

    return (data.data as FeishuTableData[]) || [];
  } catch (error) {
    console.error('Failed to fetch all tables:', error);
    
    if (error instanceof TypeError && error.message.includes('Failed to fetch')) {
      throw new Error('网络连接失败，请检查网络连接或API地址是否正确');
    }
    
    if (error instanceof Error && error.message.includes('ERR_CONNECTION')) {
      throw new Error('网络连接被关闭，请检查网络连接或API地址是否正确');
    }
    
    throw error;
  }
};

export const fetchTableData = async (settings: FeishuApiSettings, tableId?: string, tableName?: string): Promise<FeishuTableData> => {
  try {
    if (!settings.apiUrl) throw new Error('飞书API地址未配置');

    let baseUrl = settings.apiUrl.replace(/\/(tableall|table|alltable)(\/json)?$/i, '');
    let url = `${baseUrl}/table/json?`;
    if (tableId) url += `table_id=${encodeURIComponent(tableId)}`;
    else if (tableName) url += `table_name=${encodeURIComponent(tableName)}`;
    else throw new Error('Either tableId or tableName is required');

    const response = await fetch(url);
    const data: FeishuApiResponse = await response.json();

    if (data.code !== 200) throw new Error(String(data.msg) || 'Failed to fetch table data');
    return (data.data as FeishuTableData) || { table_info: { table_id: null, table_name: null }, records: [] };
  } catch (error) {
    console.error('Failed to fetch table data:', error);
    throw error;
  }
};

export const convertToSmokeLogs = (tableData: FeishuTableData, userId?: string): SmokeLog[] => {
  const logs: SmokeLog[] = [];
  const tableId = tableData.table_info.table_id || null;
  const tableName = tableData.table_info.table_name || null;

  tableData.records.forEach((record) => {
    const recordDate = record['记录日期'];
    if (!recordDate) return;

    const dateParts = recordDate.split('-');
    if (dateParts.length !== 3) return;

    const year = parseInt(dateParts[0]);
    const month = parseInt(dateParts[1]);
    const day = parseInt(dateParts[2]);

    for (let recordIndex = 1; recordIndex <= 50; recordIndex++) {
      const recordValue = record[String(recordIndex)];
      if (recordValue !== undefined && recordValue !== null && recordValue !== '') {
        const timeStr = String(recordValue);
        const timeParts = timeStr.split(':');

        if (timeParts.length === 2) {
          const hour = parseInt(timeParts[0]);
          const minute = parseInt(timeParts[1]);
          if (!isNaN(hour) && !isNaN(minute)) {
            const timestamp = new Date(year, month - 1, day, hour, minute, 0).getTime();
            const recordTime = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;

            logs.push({
              id: generateLogId(userId || 'local', recordDate, recordTime, recordIndex),
              user_id: userId || 'local',
              table_id: tableId,
              table_name: tableName,
              record_date: recordDate,
              record_time: recordTime,
              record_index: recordIndex,
              timestamp,
              created_at: formatTimestamp(new Date())
            });
          }
        }
      }
    }
  });

  return logs;
};

export const submitToFeishu = async (options: SubmitOptions, writeAccessKey: string): Promise<{ success: boolean; message: string; submittedCount: number; failedCount: number }> => {
  try {
    const adapter = getStorageAdapter();
    let localLogs = await adapter.getLogs();

    if (options.recordIds) {
      localLogs = localLogs.filter((log) => options.recordIds?.includes(log.id));
    } else if (options.startDate || options.endDate) {
      const startTimestamp = options.startDate ? new Date(options.startDate).getTime() : 0;
      const endTimestamp = options.endDate ? new Date(options.endDate + 'T23:59:59').getTime() : Infinity;
      localLogs = localLogs.filter((log) => log.timestamp >= startTimestamp && log.timestamp <= endTimestamp);
    }

    if (localLogs.length === 0) {
      return { success: false, message: '没有符合条件的记录可提交', submittedCount: 0, failedCount: 0 };
    }

    const settings = await getFeishuApiSettings();
    if (!settings.apiUrl) {
      return { success: false, message: '飞书API地址未配置', submittedCount: 0, failedCount: 0 };
    }

    let baseUrl = settings.apiUrl.replace(/\/(tableall|table|alltable)(\/json)?$/i, '');
    const recordsByDate = new Map<string, SmokeLog[]>();

    localLogs.forEach((log) => {
      const date = log.record_date;
      if (!recordsByDate.has(date)) recordsByDate.set(date, []);
      recordsByDate.get(date)!.push(log);
    });

    const records: Record<string, { '记录日期': string; [key: string]: string | number }> = {};
    recordsByDate.forEach((dayLogs, date) => {
      const hourCounts: Record<string, number> = {};
      for (let i = 1; i <= 24; i++) hourCounts[String(i)] = 0;
      dayLogs.forEach((log) => {
        const hour = parseInt(log.record_time.split(':')[0]) + 1;
        hourCounts[String(hour)] = (hourCounts[String(hour)] || 0) + 1;
      });
      records[date] = { '记录日期': date, ...hourCounts };
    });

    const submitUrl = `${baseUrl}/table/${encodeURIComponent(options.tableName)}/json`;
    const response = await fetch(submitUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${writeAccessKey}` },
      body: JSON.stringify({ records })
    });

    const result = await response.json();
    if (result.code === 200) {
      return { success: true, message: `成功提交 ${localLogs.length} 条记录`, submittedCount: localLogs.length, failedCount: 0 };
    } else {
      return { success: false, message: String(result.msg) || '提交失败', submittedCount: 0, failedCount: localLogs.length };
    }
  } catch (error) {
    return { success: false, message: error instanceof Error ? error.message : '提交失败', submittedCount: 0, failedCount: 0 };
  }
};

export const apiService = {
  async getLogs(userId: string, page: number = 0, pageSize: number = 1000): Promise<SmokeLog[]> {
    try {
      const client = await getClient();
      const { data, error } = await client
        .from('smoke_logs')
        .select('*')
        .eq('user_id', userId)
        .order('timestamp', { ascending: false })
        .range(page * pageSize, (page + 1) * pageSize - 1);
      if (error) return [];
      return data || [];
    } catch (error) {
      console.error('Failed to load logs:', error);
      return [];
    }
  },

  async getAllLogs(userId: string): Promise<SmokeLog[]> {
    try {
      const allLogs: SmokeLog[] = [];
      let page = 0;
      const pageSize = 1000;
      let hasMore = true;

      while (hasMore) {
        const logs = await this.getLogs(userId, page, pageSize);
        if (logs.length > 0) {
          allLogs.push(...logs);
          hasMore = logs.length === pageSize;
          page++;
        } else {
          hasMore = false;
        }
      }
      return allLogs;
    } catch (error) {
      console.error('Failed to load all logs:', error);
      return [];
    }
  },

  async saveLog(log: SmokeLog): Promise<SmokeLog> {
    if (log.user_id === 'local') throw new Error('Cannot save log with user_id "local"');
    const completedLog = completeLogFields(log);
    const client = await getClient();
    const { data, error } = await client.from('smoke_logs').upsert(completedLog, { onConflict: 'user_id, record_date, record_time' }).select().single();
    if (error) {
      EventHandle.publish({
        type: EventType.LOG_CREATE,
        category: 'data',
        data: { success: false, error: error.message },
        timestamp: Date.now()
      });
      throw error;
    }
    EventHandle.publish({
      type: EventType.LOG_CREATE,
      category: 'data',
      data: { success: true, log: data },
      timestamp: Date.now()
    });
    return data;
  },

  async saveLogs(logs: SmokeLog[]): Promise<{ success: boolean; count: number; error?: string }> {
    if (!logs || logs.length === 0) return { success: true, count: 0 };
    const validLogs = logs.filter((log) => log.user_id !== 'local');
    if (validLogs.length === 0) return { success: true, count: 0 };

    const completedLogs = validLogs.map(completeLogFields);
    const uniqueLogs = new Map<string, SmokeLog>();
    completedLogs.forEach((log) => uniqueLogs.set(`${log.user_id}_${log.record_date}_${log.record_time}`, log));
    const deduplicatedLogs = Array.from(uniqueLogs.values());

    // 查询已存在的记录
    const client = await getClient();
    const userIds = [...new Set(deduplicatedLogs.map(log => log.user_id))];
    const dates = [...new Set(deduplicatedLogs.map(log => log.record_date))];
    
    const { data: existing } = await client
      .from('smoke_logs')
      .select('user_id, record_date, record_time')
      .in('user_id', userIds)
      .in('record_date', dates);
    
    const existingKeys = new Set((existing || []).map(r => `${r.user_id}_${r.record_date}_${r.record_time}`));
    const newLogs = deduplicatedLogs.filter(log => !existingKeys.has(getUniqueKey(log)));

    if (newLogs.length === 0) {
      return { success: true, count: 0 };
    }

    const BATCH_SIZE = 500;
    const batches: SmokeLog[][] = [];
    for (let i = 0; i < newLogs.length; i += BATCH_SIZE) {
      batches.push(newLogs.slice(i, i + BATCH_SIZE));
    }

    let successCount = 0;
    let lastError: string | undefined;

    for (const batch of batches) {
      const { error } = await client.from('smoke_logs').insert(batch);
      if (error) lastError = error.message;
      else successCount += batch.length;
    }

    return { success: successCount === newLogs.length, count: successCount, error: lastError };
  },

  async updateLog(updatedLog: SmokeLog, existingLogs: SmokeLog[]): Promise<SmokeLog> {
    const adapter = getStorageAdapter();
    const updatedLogs = existingLogs.map(l => l.id === updatedLog.id ? updatedLog : l);
    await adapter.saveLogs(updatedLogs);

    if (updatedLog.user_id && updatedLog.user_id !== 'local') {
      const completedLog = completeLogFields(updatedLog);
      try {
        const client = await getClient();
        const { data, error } = await client.from('smoke_logs').update(completedLog).eq('id', updatedLog.id).select().single();
        if (error) {
          EventHandle.publish({
            type: EventType.LOG_UPDATE,
            category: 'data',
            data: { success: false, error: error.message },
            timestamp: Date.now()
          });
          throw error;
        }
        EventHandle.publish({
          type: EventType.LOG_UPDATE,
          category: 'data',
          data: { success: true, log: data, logs: updatedLogs },
          timestamp: Date.now()
        });
        return data;
      } catch (error) {
        console.error('Failed to update in cloud, local update succeeded:', error);
      }
    }

    EventHandle.publish({
      type: EventType.LOG_UPDATE,
      category: 'data',
      data: { success: true, log: updatedLog, logs: updatedLogs },
      timestamp: Date.now()
    });

    return updatedLog;
  },

  async deleteLog(id: string, userId: string, existingLogs: SmokeLog[]): Promise<string> {
    const adapter = getStorageAdapter();
    const updatedLogs = existingLogs.filter(l => l.id !== id);
    await adapter.saveLogs(updatedLogs);

    if (userId && userId !== 'local') {
      try {
        const client = await getClient();
        const { error } = await client.from('smoke_logs').delete().eq('id', id);
        if (error) {
          EventHandle.publish({
            type: EventType.LOG_DELETE,
            category: 'data',
            data: { success: false, error: error.message },
            timestamp: Date.now()
          });
          throw error;
        }
      } catch (error) {
        console.error('Failed to delete from cloud, local deletion succeeded:', error);
      }
    }

    EventHandle.publish({
      type: EventType.LOG_DELETE,
      category: 'data',
      data: { success: true, id, logs: updatedLogs },
      timestamp: Date.now()
    });

    return id;
  },

  async getSyncDiff(source: 'feishu' | 'supabase', userId?: string, password?: string, language: string = 'zh'): Promise<SyncDiffResult> {
    try {
      const adapter = getStorageAdapter();
      const localLogs = await adapter.getLogs();
      let cloudLogs: SmokeLog[] = [];

      if (source === 'feishu') {
        // 从飞书获取数据
        let feishuSettings = await getFeishuApiSettings();
        if (password) {
          feishuSettings = await getFeishuApiSettingsWithPassword(password);
        }

        if (feishuSettings.apiUrl === '__ENCRYPTED__') {
          throw new Error('Feishu API settings are encrypted. Please provide password.');
        }

        if (!feishuSettings.apiUrl) {
          throw new Error('Feishu API URL is not configured');
        }

        const tables = await fetchAllTables(feishuSettings, true);
        for (const table of tables) {
          const logs = convertToSmokeLogs(table, userId || 'local');
          cloudLogs.push(...logs);
        }
      } else if (source === 'supabase') {
        // 从supabase获取数据
        cloudLogs = await this.getAllLogs(userId || 'local');
      }

      const diff = compareData(localLogs, cloudLogs);
      
      const t = TRANSLATIONS[language as keyof typeof TRANSLATIONS];
      let message = '';
      if (source === 'feishu') {
        message = t.foundNewRecords.replace('{count}', String(diff.cloudOnly.length)) + '，' + t.foundConflictingRecords.replace('{count}', String(diff.conflicting.length));
      } else {
        message = t.needUploadRecords.replace('{count}', String(diff.localOnly.length)) + '，' + t.needDownloadRecords.replace('{count}', String(diff.cloudOnly.length)) + '，' + t.foundConflictingRecords.replace('{count}', String(diff.conflicting.length));
      }

      return {
        diff,
        source,
        timestamp: Date.now(),
        message
      };
    } catch (error) {
      throw new Error(error instanceof Error ? error.message : '获取同步差异失败');
    }
  },

  async executeSync(source: 'feishu' | 'supabase', diff: DataDiff, options: { upload?: boolean; download?: boolean } = {}, language: string = 'zh'): Promise<CloudSyncResult> {
    try {
      EventHandle.publish({
        type: EventType.SYNC_START,
        category: 'sync',
        data: { source, options },
        timestamp: Date.now()
      });

      const adapter = getStorageAdapter();
      const localLogs = await adapter.getLogs();
      let uploadedCount = 0;
      let downloadedCount = 0;

      // 执行上传（仅supabase）
      if (source === 'supabase' && options.upload && diff.localOnly.length > 0) {
        const uploadResult = await this.saveLogs(diff.localOnly);
        uploadedCount = uploadResult.count;
        
        EventHandle.publish({
          type: EventType.CLOUD_UPLOAD,
          category: 'sync',
          data: { count: uploadedCount },
          timestamp: Date.now()
        });
      }

      // 执行下载
      if (options.download && diff.cloudOnly.length > 0) {
        // 使用insert、replace模式合并数据
        const existingKeys = new Set(localLogs.map(getUniqueKey));
        const newLogs = diff.cloudOnly.filter(log => !existingKeys.has(getUniqueKey(log)));
        
        if (newLogs.length > 0) {
          const mergedLogs = [...newLogs, ...localLogs].sort((a, b) => b.timestamp - a.timestamp);
          await adapter.saveLogs(mergedLogs);
          downloadedCount = newLogs.length;
          
          EventHandle.publish({
            type: EventType.CLOUD_DOWNLOAD,
            category: 'sync',
            data: { count: downloadedCount },
            timestamp: Date.now()
          });
        }
      }

      const t = TRANSLATIONS[language as keyof typeof TRANSLATIONS];
      const result = {
        success: true,
        message: t.uploadCount.replace('{count}', String(uploadedCount)) + '，' + t.downloadCount.replace('{count}', String(downloadedCount)),
        localCount: localLogs.length,
        cloudCount: diff.totalCloud,
        uploadedCount,
        downloadedCount,
        totalCount: localLogs.length + downloadedCount - uploadedCount
      };

      EventHandle.publish({
        type: EventType.SYNC_SUCCESS,
        category: 'sync',
        data: result,
        timestamp: Date.now()
      });

      return result;
    } catch (error) {
      const errorResult = { success: false, message: error instanceof Error ? error.message : '同步失败', localCount: 0, cloudCount: 0, uploadedCount: 0, downloadedCount: 0, totalCount: 0 };
      EventHandle.publish({
        type: EventType.SYNC_ERROR,
        category: 'sync',
        data: errorResult,
        timestamp: Date.now()
      });
      return errorResult;
    }
  },

  async syncWithCloud(userId: string): Promise<CloudSyncResult> {
    try {
      const diffResult = await this.getSyncDiff('supabase', userId);
      return await this.executeSync('supabase', diffResult.diff, { upload: true, download: true });
    } catch (error) {
      const errorResult = { success: false, message: error instanceof Error ? error.message : '同步失败', localCount: 0, cloudCount: 0, uploadedCount: 0, downloadedCount: 0, totalCount: 0 };
      EventHandle.publish({
        type: EventType.SYNC_ERROR,
        category: 'sync',
        data: errorResult,
        timestamp: Date.now()
      });
      return errorResult;
    }
  },

  async getSettings(userId: string): Promise<AppSettings> {
    try {
      const client = await getClient();
      const { data, error } = await client.from('app_settings').select('*').eq('user_id', userId).maybeSingle();
      if (error || !data) return { user_id: userId, dailyLimit: 20, warningLimit: 15, themeColor: '#FF6B6B', language: 'zh', allowCloudSync: true };

      return {
        ...data,
        dailyLimit: data.daily_limit || data.dailyLimit || 20,
        warningLimit: data.warning_limit || data.warningLimit || 15,
        themeColor: data.theme_color || data.themeColor || '#FF6B6B',
        avatarUrl: data.avatar_url || data.avatarUrl,
        user_id: userId,
      };
    } catch (error) {
      return { user_id: userId, dailyLimit: 20, warningLimit: 15, themeColor: '#FF6B6B', language: 'zh', allowCloudSync: true };
    }
  },

  async saveSettings(settings: AppSettings): Promise<void> {
    try {
      const client = await getClient();
      const dbSettings = {
        user_id: settings.user_id,
        daily_limit: settings.dailyLimit,
        warning_limit: settings.warningLimit,
        theme_color: settings.themeColor,
        language: settings.language,
        avatar_url: settings.avatarUrl,
      };

      const { data: existing } = await client.from('app_settings').select('id').eq('user_id', settings.user_id).single();
      if (existing) {
        await client.from('app_settings').update(dbSettings).eq('user_id', settings.user_id);
      } else {
        await client.from('app_settings').insert(dbSettings);
      }
      EventHandle.publish({
        type: EventType.SETTINGS_CHANGE,
        category: 'settings',
        data: { success: true, settings },
        timestamp: Date.now()
      });
    } catch (error) {
      EventHandle.publish({
        type: EventType.SETTINGS_CHANGE,
        category: 'settings',
        data: { success: false, error: error instanceof Error ? error.message : 'Failed to save settings' },
        timestamp: Date.now()
      });
      throw error;
    }
  },

  async clearAllData(userId: string): Promise<void> {
    try {
      const client = await getClient();
      await client.from('smoke_logs').delete().eq('user_id', userId);
    } catch (error) {
      throw error;
    }
  },

  async createRecord(userId: string | undefined, existingLogs: SmokeLog[]): Promise<SmokeLog> {
    const now = new Date();
    const recordDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    const recordTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
    
    const allTodayLogs = existingLogs.filter(log => 
      (log.record_date || log.date) === recordDate
    );
    const recordIndex = allTodayLogs.length + 1;
    
    const newLog: SmokeLog = {
      id: crypto.randomUUID(),
      user_id: userId || 'local',
      record_date: recordDate,
      record_time: recordTime,
      record_index: recordIndex,
      timestamp: Date.now()
    };

    const completedLog = completeLogFields(newLog);
    const updatedLogs = [completedLog, ...existingLogs];

    // 立即发布事件，通知UI更新
    EventHandle.publish({
      type: EventType.LOG_CREATE,
      category: 'data',
      data: { success: true, log: completedLog, logs: updatedLogs },
      timestamp: Date.now()
    });

    // 在后台异步执行存储操作，不阻塞返回
    setTimeout(async () => {
      try {
        const adapter = getStorageAdapter();
        await adapter.saveLogs(updatedLogs);

        // 在后台异步进行Supabase同步，不阻塞返回
        if (userId) {
          this.saveLog(newLog).catch(error => {
            console.error('Failed to sync log to cloud:', error);
          });
        }
      } catch (error) {
        console.error('Failed to save log locally:', error);
      }
    }, 0);

    return completedLog;
  },

  async saveSettingsComplete(settings: AppSettings): Promise<void> {
    const adapter = getStorageAdapter();
    await adapter.saveSettings(settings);

    if (settings.user_id && settings.user_id !== 'local') {
      try {
        await this.saveSettings(settings);
      } catch (error) {
        console.error('Failed to save settings to cloud, local save succeeded:', error);
      }
    }

    EventHandle.publish({
      type: EventType.SETTINGS_CHANGE,
      category: 'settings',
      data: { success: true, settings },
      timestamp: Date.now()
    });
  },

  async downloadCloudData(
    dataSource: 'feishu' | 'supabase',
    userId: string | undefined,
    password?: string
  ): Promise<{ logs: SmokeLog[], needPassword: boolean }> {
    const adapter = getStorageAdapter();
    let cloudLogs: SmokeLog[] = [];

    if (dataSource === 'feishu') {
      const savedApiSettings = await adapter.getApiSettings();
      if (savedApiSettings?.feishu) {
        const decryptedText = simpleDecrypt(savedApiSettings.feishu, password || '');
        if (!decryptedText || decryptedText.startsWith('JPAXdC]')) {
          return { logs: [], needPassword: true };
        }
        try {
          const decryptedFeishu = JSON.parse(decryptedText);
          const tables = await fetchAllTables(decryptedFeishu, true);
          for (const table of tables) {
            const logs = convertToSmokeLogs(table, userId || 'local');
            cloudLogs.push(...logs);
          }
        } catch (parseError) {
          console.error('Failed to parse Feishu settings:', parseError);
          return { logs: [], needPassword: true };
        }
      }
    } else if (dataSource === 'supabase' && userId) {
      cloudLogs = await this.getAllLogs(userId);
    }

    if (cloudLogs.length === 0) {
      return { logs: [], needPassword: false };
    }

    await adapter.saveLogs(cloudLogs);
    return { logs: cloudLogs, needPassword: false };
  },

  async loadUserData(userId: string): Promise<{
    settings: AppSettings | null;
    localLogs: SmokeLog[];
    cloudLogs?: SmokeLog[];
    needCloudSync?: boolean;
  }> {
    const adapter = getStorageAdapter();

    try {
      const userSettings = await this.getSettings(userId);
      await adapter.saveSettings(userSettings);

      const localLogs = await adapter.getLogs();

      if (!localLogs || localLogs.length === 0) {
        try {
          const cloudLogs = await this.getAllLogs(userId);
          if (cloudLogs.length > 0) {
            return {
              settings: userSettings,
              localLogs: [],
              cloudLogs,
              needCloudSync: true
            };
          }
        } catch (cloudError) {
          console.warn('Check cloud data failed:', cloudError);
        }
      }

      return {
        settings: userSettings,
        localLogs: localLogs || []
      };
    } catch (error) {
      const localLogs = await adapter.getLogs();
      let settings: AppSettings | null = null;
      try {
        settings = await this.getSettings(userId);
        await adapter.saveSettings(settings);
      } catch (settingsError) {
        console.warn('Load settings failed:', settingsError);
      }

      return {
        settings,
        localLogs: localLogs || []
      };
    }
  }
};
