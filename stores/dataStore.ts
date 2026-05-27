import { create } from 'zustand';
import { SmokeLog, SyncDiffResult, OperationLog } from '../types';
import { getStorageAdapter } from '../services/storageAdapter';
import { apiService } from '../services/apiService';
import { systemLogService } from '../services/systemLogService';

interface DataState {
  logs: SmokeLog[];
  isSyncing: boolean;
  isLoading: boolean;
  syncDiff: SyncDiffResult | null;
  cloudLogs: SmokeLog[];
  operationLogs: OperationLog[];
  cloudPage: number;
  loadingMore: boolean;
  hasMoreCloudLogs: boolean;
}

interface DataActions {
  setLogs: (logs: SmokeLog[]) => void;
  addLog: (log: SmokeLog) => void;
  updateLog: (id: string, updates: Partial<SmokeLog>) => void;
  removeLog: (id: string) => void;
  clearLogs: () => void;
  setIsSyncing: (isSyncing: boolean) => void;
  setIsLoading: (isLoading: boolean) => void;
  setSyncDiff: (diff: SyncDiffResult | null) => void;
  setCloudLogs: (logs: SmokeLog[]) => void;
  appendCloudLogs: (logs: SmokeLog[]) => void;
  setCloudPage: (page: number) => void;
  setLoadingMore: (loading: boolean) => void;
  setHasMoreCloudLogs: (hasMore: boolean) => void;
  setOperationLogs: (logs: OperationLog[]) => void;
  
  // 异步操作
  loadLogs: () => Promise<void>;
  saveLogs: (logs: SmokeLog[]) => Promise<void>;
  refreshLogs: () => Promise<void>;
  loadCloudLogs: (userId: string, page: number) => Promise<void>;
  syncWithCloud: (
    source: 'feishu' | 'supabase',
    userId: string,
    options?: { upload: boolean; download: boolean },
    language?: string
  ) => Promise<{ success: boolean; message: string }>;
}

export const useDataStore = create<DataState & DataActions>((set, get) => ({
  // 初始状态
  logs: [],
  isSyncing: false,
  isLoading: false,
  syncDiff: null,
  cloudLogs: [],
  operationLogs: [],
  cloudPage: 0,
  loadingMore: false,
  hasMoreCloudLogs: false,

  // 同步操作
  setLogs: (logs) => set({ logs }),
  
  addLog: (log) => set((state) => ({ 
    logs: [log, ...state.logs] 
  })),
  
  updateLog: (id, updates) => set((state) => ({
    logs: state.logs.map(log => 
      log.id === id ? { ...log, ...updates } : log
    )
  })),
  
  removeLog: (id) => set((state) => ({
    logs: state.logs.filter(log => log.id !== id)
  })),
  
  clearLogs: () => set({ logs: [] }),
  setIsSyncing: (isSyncing) => set({ isSyncing }),
  setIsLoading: (isLoading) => set({ isLoading }),
  setSyncDiff: (syncDiff) => set({ syncDiff }),
  setCloudLogs: (cloudLogs) => set({ cloudLogs }),
  
  appendCloudLogs: (newLogs) => set((state) => ({
    cloudLogs: [...state.cloudLogs, ...newLogs]
  })),
  
  setCloudPage: (cloudPage) => set({ cloudPage }),
  setLoadingMore: (loadingMore) => set({ loadingMore }),
  setHasMoreCloudLogs: (hasMoreCloudLogs) => set({ hasMoreCloudLogs }),
  setOperationLogs: (operationLogs) => set({ operationLogs }),

  // 异步操作
  loadLogs: async () => {
    try {
      const adapter = getStorageAdapter();
      const logs = await adapter.getLogs();
      set({ logs });
      systemLogService.info('data', '日志加载完成', { count: logs.length });
    } catch (error) {
      systemLogService.error('data', '加载日志失败', error as Error);
    }
  },

  saveLogs: async (logs) => {
    try {
      const adapter = getStorageAdapter();
      await adapter.saveLogs(logs);
      set({ logs });
      systemLogService.info('data', '日志保存完成', { count: logs.length });
    } catch (error) {
      systemLogService.error('data', '保存日志失败', error as Error);
      throw error;
    }
  },

  refreshLogs: async () => {
    await get().loadLogs();
  },

  loadCloudLogs: async (userId, page) => {
    try {
      set({ loadingMore: true });
      
      const result = await apiService.getCloudLogs(userId, page, 20);
      
      if (result.success && result.logs) {
        if (page === 0) {
          set({ cloudLogs: result.logs });
        } else {
          get().appendCloudLogs(result.logs);
        }
        
        set({ 
          cloudPage: page,
          hasMoreCloudLogs: result.logs.length >= 20 
        });
        
        systemLogService.info('cloud', '云端日志加载完成', { 
          count: result.logs.length, 
          page 
        });
      }
    } catch (error) {
      systemLogService.error('cloud', '加载云端日志失败', error as Error);
    } finally {
      set({ loadingMore: false });
    }
  },

  syncWithCloud: async (source, userId, options = { upload: true, download: true }, language = 'en') => {
    try {
      set({ isSyncing: true });
      
      let result;
      
      if (source === 'feishu') {
        result = await apiService.syncFromFeishu({}, undefined, undefined);
      } else if (source === 'supabase') {
        result = await apiService.syncFromFeishu({}, userId);
      } else {
        throw new Error('无效的同步源');
      }

      if (result.success) {
        // 重新加载本地日志
        await get().loadLogs();
        
        systemLogService.info('sync', '同步完成', {
          source,
          count: result.count || 0
        });
        
        return { 
          success: true, 
          message: result.message || '同步成功'
        };
      } else {
        return { 
          success: false, 
          message: result.message || '同步失败' 
        };
      }
    } catch (error) {
      systemLogService.error('sync', '同步失败', error as Error);
      return { 
        success: false, 
        message: error instanceof Error ? error.message : '同步过程中发生错误' 
      };
    } finally {
      set({ isSyncing: false });
    }
  },
}));
