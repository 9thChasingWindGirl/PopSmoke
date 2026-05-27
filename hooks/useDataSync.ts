import { useState, useEffect, useCallback } from 'react';
import { SmokeLog, AppSettings, SyncDiffResult } from '../types';
import { getStorageAdapter } from '../services/storageAdapter';
import { apiService } from '../services/apiService';
import EventHandle from '../event/EventHandle';
import { EventType } from '../event/EventType';
import { systemLogService } from '../services/systemLogService';

export interface UseDataSyncReturn {
  logs: SmokeLog[];
  setLogs: React.Dispatch<React.SetStateAction<SmokeLog[]>>;
  isSyncing: boolean;
  setIsSyncing: (syncing: boolean) => void;
  syncDiff: SyncDiffResult | null;
  setSyncDiff: React.Dispatch<React.SetStateAction<SyncDiffResult | null>>;
  syncOptions: { upload: boolean; download: boolean };
  setSyncOptions: (options: { upload: boolean; download: boolean }) => void;
  loadingStatus: 'loading' | 'syncing';
  setLoadingStatus: (status: 'loading' | 'syncing') => void;
  handleSync: (source: 'feishu' | 'supabase' | null, password?: string) => Promise<void>;
  loadUserData: (user: { id: string }) => Promise<void>;
  loadMoreCloudLogs: (userId: string, page: number) => Promise<SmokeLog[]>;
  refreshLogs: () => Promise<void>;
}

export const useDataSync = (settings: AppSettings): UseDataSyncReturn => {
  const [logs, setLogs] = useState<SmokeLog[]>([]);
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncDiff, setSyncDiff] = useState<SyncDiffResult | null>(null);
  const [syncOptions, setSyncOptions] = useState<{ upload: boolean; download: boolean }>({ upload: true, download: true });
  const [loadingStatus, setLoadingStatus] = useState<'loading' | 'syncing'>('loading');

  useEffect(() => {
    const syncSuccessUnsubscribe = EventHandle.subscribe(EventType.SYNC_SUCCESS, async (event) => {
      const data = event.data as { success?: boolean };
      if (data?.success) {
        try {
          const adapter = getStorageAdapter();
          const updatedLogs = await adapter.getLogs();
          setLogs(updatedLogs);
          systemLogService.info('sync', '同步成功，更新本地日志', { count: updatedLogs.length });
        } catch (error) {
          systemLogService.error('sync', '同步后更新日志失败', error as Error);
        }
      }
    });

    const logCreateUnsubscribe = EventHandle.subscribe(EventType.LOG_CREATE, (event) => {
      const data = event.data as { success?: boolean; logs?: SmokeLog[] };
      if (data?.success && data?.logs) {
        setLogs(data.logs);
      }
    });

    const logUpdateUnsubscribe = EventHandle.subscribe(EventType.LOG_UPDATE, (event) => {
      const data = event.data as { success?: boolean; logs?: SmokeLog[] };
      if (data?.success && data?.logs) {
        setLogs(data.logs);
      }
    });

    const logDeleteUnsubscribe = EventHandle.subscribe(EventType.LOG_DELETE, (event) => {
      const data = event.data as { success?: boolean; logs?: SmokeLog[] };
      if (data?.success && data?.logs) {
        setLogs(data.logs);
      }
    });

    return () => {
      syncSuccessUnsubscribe.unsubscribe();
      logCreateUnsubscribe.unsubscribe();
      logUpdateUnsubscribe.unsubscribe();
      logDeleteUnsubscribe.unsubscribe();
    };
  }, []);

  const refreshLogs = useCallback(async () => {
    try {
      const adapter = getStorageAdapter();
      const updatedLogs = await adapter.getLogs();
      setLogs(updatedLogs);
    } catch (error) {
      systemLogService.error('storage', '刷新日志失败', error as Error);
    }
  }, []);

  const handleSync = useCallback(async (source: 'feishu' | 'supabase' | null, password?: string) => {
    setIsSyncing(true);
    setLoadingStatus('syncing');

    try {
      let diffResult: SyncDiffResult;

      if (source === 'feishu') {
        const userId = 'local';
        diffResult = await apiService.getSyncDiff('feishu', userId, password, settings.language);
        systemLogService.info('sync', '飞书同步差异计算完成', diffResult as unknown as Record<string, unknown>);
      } else if (source === 'supabase') {
        const userId = '';
        if (!userId) {
          throw new Error('用户未登录');
        }
        diffResult = await apiService.getSyncDiff('supabase', userId, undefined, settings.language);
        systemLogService.info('sync', 'Supabase 同步差异计算完成', diffResult as unknown as Record<string, unknown>);
      } else {
        throw new Error('未指定同步源');
      }

      setSyncDiff(diffResult);

      if (diffResult.diff.totalLocal > 0 || diffResult.diff.totalCloud > 0) {
        systemLogService.info('sync', '发现需要同步的数据', {
          localOnly: diffResult.diff.localOnly.length,
          cloudOnly: diffResult.diff.cloudOnly.length,
          conflicting: diffResult.diff.conflicting.length
        });
      }
    } catch (error) {
      systemLogService.error('sync', '同步失败', error as Error);
      setSyncDiff(null);
    } finally {
      setIsSyncing(false);
      setLoadingStatus('loading');
    }
  }, [settings.language]);

  const loadUserData = useCallback(async (user: { id: string }) => {
    try {
      systemLogService.info('data', '开始加载用户数据', { userId: user.id });
      
      const adapter = getStorageAdapter();
      const [localLogs, localSettings] = await Promise.all([
        adapter.getLogs(),
        adapter.getSettings()
      ]);

      if (localLogs && localLogs.length > 0) {
        setLogs(localLogs);
        systemLogService.debug('data', '加载本地日志', { count: localLogs.length });
      }

      if (localSettings) {
        systemLogService.debug('data', '加载本地设置', localSettings as unknown as Record<string, unknown>);
      }

      systemLogService.info('data', '用户数据加载完成', { 
        logsCount: localLogs?.length || 0,
        hasSettings: !!localSettings
      });
    } catch (error) {
      systemLogService.error('data', '加载用户数据失败', error as Error);
    }
  }, []);

  const loadMoreCloudLogs = useCallback(async (userId: string, page: number): Promise<SmokeLog[]> => {
    try {
      systemLogService.info('cloud', '加载更多云端日志', { userId, page });
      
      const logs = await apiService.getLogs(userId, page, 20);
      
      if (logs && logs.length > 0) {
        systemLogService.info('cloud', '云端日志加载成功', { count: logs.length });
        return logs;
      }

      return [];
    } catch (error) {
      systemLogService.error('cloud', '加载云端日志失败', error as Error);
      return [];
    }
  }, []);

  return {
    logs,
    setLogs,
    isSyncing,
    setIsSyncing,
    syncDiff,
    setSyncDiff,
    syncOptions,
    setSyncOptions,
    loadingStatus,
    setLoadingStatus,
    handleSync,
    loadUserData,
    loadMoreCloudLogs,
    refreshLogs
  };
};
