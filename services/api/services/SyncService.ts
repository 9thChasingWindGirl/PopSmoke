import { SmokeLog, AppSettings } from '../../../types';
import { getSupabaseClient } from '../clients/SupabaseClient';
import { feishuClient, getFeishuApiSettings } from '../clients/FeishuClient';
import { logService } from './LogService';
import { systemLogService } from '../../systemLogService';
import EventHandle from '../../../event/EventHandle';
import { EventType } from '../../../event/EventType';

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
  uploadedLogs?: SmokeLog[];
  downloadedLogs?: SmokeLog[];
}

export interface DataDiff {
  localOnly: SmokeLog[];
  cloudOnly: SmokeLog[];
  conflicting: SmokeLog[];
  fieldsToUpdate: SmokeLog[];
  totalLocal: number;
  totalCloud: number;
}

export interface SyncDiffResult {
  diff: DataDiff;
  source: 'feishu' | 'supabase';
  timestamp: number;
  message: string;
}

export class SyncService {

  public async getSyncDiff(
    source: 'feishu' | 'supabase',
    userId: string,
    password?: string,
    language: string = 'en'
  ): Promise<SyncDiffResult> {
    try {
      const { getStorageAdapter } = await import('../../storageAdapter');
      const adapter = getStorageAdapter();
      const localLogs = await adapter.getLogs();

      let cloudLogs: SmokeLog[] = [];

      if (source === 'feishu') {
        const feishuConfig = await getFeishuApiSettings();
        if (!feishuConfig) {
          throw new Error('飞书 API 未配置');
        }

        const result = await feishuClient.fetchRecords(userId, password);
        if (!result.success || !result.logs) {
          throw new Error(result.error || '获取飞书数据失败');
        }
        
        cloudLogs = result.logs;
      } else if (source === 'supabase') {
        const client = await getSupabaseClient();
        
        const { data, error, count } = await client
          .from('smoke_logs')
          .select('*', { count: 'exact' })
          .eq('user_id', userId);

        if (error) {
          throw error;
        }

        cloudLogs = data || [];
        (systemLogService as any).info('sync', `获取云端数据完成`, {
          source: 'supabase',
          count: cloudLogs.length,
          total: count
        });
      }

      const diff = this.calculateDiff(localLogs, cloudLogs);

      return {
        diff,
        source,
        timestamp: Date.now(),
        message: this.generateDiffMessage(diff)
      };
    } catch (error) {
      (systemLogService as any).error('sync', '计算同步差异失败', error as Error);
      return {
        diff: {
          localOnly: [],
          cloudOnly: [],
          conflicting: [],
          fieldsToUpdate: [],
          totalLocal: 0,
          totalCloud: 0
        },
        source,
        timestamp: Date.now(),
        message: error instanceof Error ? error.message : '计算同步差异失败'
      };
    }
  }

  private calculateDiff(localLogs: SmokeLog[], cloudLogs: SmokeLog[]): DataDiff {
    const localMap = new Map(localLogs.map(log => [log.id, log]));
    const cloudMap = new Map(cloudLogs.map(log => [log.id, log]));

    const localOnly: SmokeLog[] = [];
    const cloudOnly: SmokeLog[] = [];
    const conflicting: SmokeLog[] = [];
    const fieldsToUpdate: SmokeLog[] = [];

    for (const [id, localLog] of localMap) {
      const cloudLog = cloudMap.get(id);
      
      if (!cloudLog) {
        localOnly.push(localLog);
      } else if (localLog.timestamp !== cloudLog.timestamp ||
                 JSON.stringify(localLog.notes) !== JSON.stringify(cloudLog.notes)) {
        conflicting.push({
          ...localLog,
          _conflictVersion: cloudLog
        } as any);
      }
    }

    for (const [id, cloudLog] of cloudMap) {
      if (!localMap.has(id)) {
        cloudOnly.push(cloudLog);
      }
    }

    return {
      localOnly,
      cloudOnly,
      conflicting,
      fieldsToUpdate,
      totalLocal: localLogs.length,
      totalCloud: cloudLogs.length
    };
  }

  private generateDiffMessage(diff: DataDiff): string {
    const parts: string[] = [];
    
    if (diff.totalLocal > 0) parts.push(`本地 ${diff.totalLocal} 条`);
    if (diff.totalCloud > 0) parts.push(`云端 ${diff.totalCloud} 条`);
    
    let changes = '';
    if (diff.localOnly.length > 0) changes += `需上传 ${diff.localOnly.length} 条`;
    if (diff.cloudOnly.length > 0) changes += `${changes ? '，' : ''}需下载 ${diff.cloudOnly.length} 条`;
    if (diff.conflicting.length > 0) changes += `${changes ? '，' : ''}冲突 ${diff.conflicting.length} 条`;

    return `数据对比完成: ${parts.join(' / ')}. ${changes || '数据已完全同步'}`;
  }

  public async syncFromSupabase(
    userId: string,
    options: { upload: boolean; download: boolean },
    getLanguage: () => string
  ): Promise<CloudSyncResult> {
    try {
      const { getStorageAdapter } = await import('../../storageAdapter');
      const adapter = getStorageAdapter();
      const localLogs = await adapter.getLogs();
      const client = await getSupabaseClient();

      let uploadedCount = 0;
      let downloadedCount = 0;
      const uploadedLogs: SmokeLog[] = [];
      const downloadedLogs: SmokeLog[] = [];

      // 上传本地独有的记录
      if (options.upload && localLogs.length > 0) {
        for (const log of localLogs) {
          const { data, error } = await client
            .from('smoke_logs')
            .upsert({ ...log, user_id: userId }, { onConflict: 'id' })
            .select()
            .single();

          if (!error && data) {
            uploadedCount++;
            uploadedLogs.push(data);
          }
        }
      }

      // 下载云端独有的记录
      if (options.download) {
        const { data: cloudData } = await client
          .from('smoke_logs')
          .select('*')
          .eq('user_id', userId)
          .order('timestamp', { ascending: false });

        if (cloudData) {
          const localIds = new Set(localLogs.map(l => l.id));
          const newFromCloud = cloudData.filter((log: SmokeLog) => !localIds.has(log.id));
          
          if (newFromCloud.length > 0) {
            const mergedLogs = [...newFromCloud, ...localLogs];
            await adapter.saveLogs(mergedLogs);
            downloadedLogs.push(...newFromCloud);
            downloadedCount = newFromCloud.length;

            (EventHandle as any).publish(EventType.SYNC_SUCCESS, {
              success: true
            });
          }
        }
      }

      (systemLogService as any).info('sync', 'Supabase 同步完成', {
        uploaded: uploadedCount,
        downloaded: downloadedCount
      });

      return {
        success: true,
        message: `同步成功: 上传${uploadedCount}条, 下载${downloadedCount}条`,
        localCount: localLogs.length,
        cloudCount: downloadedLogs.length + uploadedLogs.length,
        uploadedCount,
        downloadedCount,
        totalCount: localLogs.length + downloadedCount,
        uploadedLogs,
        downloadedLogs
      };
    } catch (error) {
      (systemLogService as any).error('sync', 'Supabase 同步失败', error as Error);
      return {
        success: false,
        message: error instanceof Error ? error.message : '同步失败',
        localCount: 0,
        cloudCount: 0,
        uploadedCount: 0,
        downloadedCount: 0,
        totalCount: 0
      };
    }
  }

  public async syncFromFeishu(
    password?: string,
    language: string = 'en'
  ): Promise<CloudSyncResult> {
    try {
      const feishuConfig = await getFeishuApiSettings();
      if (!feishuConfig) {
        throw new Error('飞书 API 未配置');
      }

      const { getStorageAdapter } = await import('../../storageAdapter');
      const adapter = getStorageAdapter();
      const localLogs = await adapter.getLogs();

      // 获取飞书数据（使用固定用户ID或从配置中读取）
      const userId = 'local';
      const result = await feishuClient.fetchRecords(userId, password);

      if (!result.success || !result.logs) {
        throw new Error(result.error || '获取飞书数据失败');
      }

      const cloudLogs = result.logs;
      const localIds = new Set(localLogs.map(l => l.id));
      const newFromCloud = cloudLogs.filter(log => !localIds.has(log.id));

      let downloadedCount = 0;
      const downloadedLogs: SmokeLog[] = [];

      if (newFromCloud.length > 0) {
        const mergedLogs = [...newFromCloud, ...localLogs];
        await adapter.saveLogs(mergedLogs);
        downloadedLogs.push(...newFromCloud);
        downloadedCount = newFromCloud.length;

        (EventHandle as any).publish(EventType.SYNC_SUCCESS, {
          success: true
        });
      }

      (systemLogService as any).info('sync', '飞书同步完成', {
        downloaded: downloadedCount
      });

      return {
        success: true,
        message: `飞书同步成功: 下载${downloadedCount}条`,
        localCount: localLogs.length,
        cloudCount: cloudLogs.length,
        uploadedCount: 0,
        downloadedCount,
        totalCount: localLogs.length + downloadedCount,
        downloadedLogs
      };
    } catch (error) {
      (systemLogService as any).error('sync', '飞书同步失败', error as Error);
      return {
        success: false,
        message: error instanceof Error ? error.message : '飞书同步失败',
        localCount: 0,
        cloudCount: 0,
        uploadedCount: 0,
        downloadedCount: 0,
        totalCount: 0
      };
    }
  }

  public async checkSupabaseDataExists(): Promise<{ exists: boolean; count?: number }> {
    try {
      const client = await getSupabaseClient();

      const { count, error } = await client
        .from('smoke_logs')
        .select('*', { count: 'exact', head: true })
        .limit(1);

      if (error) {
        throw error;
      }

      return {
        exists: (count || 0) > 0,
        count: count || 0
      };
    } catch (error) {
      (systemLogService as any).warn('sync', '检查 Supabase 数据失败', { message: (error as Error).message });
      return { exists: false };
    }
  }

  public async checkFeishuDataExists(apiUrl: string): Promise<{ exists: boolean; count?: number }> {
    try {
      feishuClient.setConfig({ apiUrl });
      return await feishuClient.checkDataExists();
    } catch (error) {
      (systemLogService as any).warn('sync', '检查飞书数据失败', { message: (error as Error).message });
      return { exists: false };
    }
  }
}

export const syncService = new SyncService();
export const apiService = {
  ...logService,
  ...syncService,
  getSyncDiff: (source: 'feishu' | 'supabase', userId: string, password?: string, language?: string) => 
    syncService.getSyncDiff(source, userId, password, language),
  syncFromSupabase: (userId: string, options: any, getLanguage: any) => 
    syncService.syncFromSupabase(userId, options, getLanguage),
  syncFromFeishu: (options: any, userId?: string, password?: string) => 
    syncService.syncFromFeishu(password as any, userId as any) as any,
  checkSupabaseDataExists: () => 
    syncService.checkSupabaseDataExists(),
  checkFeishuDataExists: (apiUrl: string) => 
    syncService.checkFeishuDataExists(apiUrl),
  getCloudLogs: (userId: string, page: number = 0, pageSize: number = 20) => 
    logService.getCloudLogs(userId, page, pageSize),
  saveLog: (log: SmokeLog) => 
    logService.saveLog(log),
  updateLog: (log: SmokeLog, currentLogs: SmokeLog[]) => 
    logService.updateLog(log, currentLogs),
  deleteLog: (id: string, userId: string, currentLogs: SmokeLog[]) => 
    logService.deleteLog(id, userId, currentLogs)
};
