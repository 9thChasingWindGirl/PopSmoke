import { useState, useCallback } from 'react';
import { SmokeLog, EncryptedApiSettings } from '../types';
import { getStorageAdapter } from '../services/storageAdapter';
import { apiService } from '../services/api';
import { systemLogService } from '../services/systemLogService';

export interface UseCloudDataReturn {
  showCloudDataDialog: boolean;
  setShowCloudDataDialog: (show: boolean) => void;
  isClosingDialog: boolean;
  setIsClosingDialog: (closing: boolean) => void;
  cloudRecordCount: number;
  setCloudRecordCount: (count: number) => void;
  cloudRecords: SmokeLog[];
  setCloudRecords: (records: SmokeLog[]) => void;
  showPreviousLoginDialog: boolean;
  setShowPreviousLoginDialog: (show: boolean) => void;
  showRestorePasswordDialog: boolean;
  setShowRestorePasswordDialog: (show: boolean) => void;
  restorePassword: string;
  setRestorePassword: (password: string) => void;
  restoreError: string | null;
  setRestoreError: (error: string | null) => void;
  cloudPage: number;
  setCloudPage: (page: number) => void;
  cloudLogs: SmokeLog[];
  setCloudLogs: React.Dispatch<React.SetStateAction<SmokeLog[]>>;
  loadingMore: boolean;
  setLoadingStatus: (loading: boolean) => void;
  hasMoreCloudLogs: boolean;
  setHasMoreCloudLogs: (hasMore: boolean) => void;
  cloudDataSource: 'supabase' | 'feishu' | 'none' | 'both';
  setCloudDataSource: (source: 'supabase' | 'feishu' | 'none' | 'both') => void;
  cloudDataCount: number;
  setCloudDataCount: (count: number) => void;
  cloudDataRecords: SmokeLog[];
  setCloudDataRecords: (records: SmokeLog[]) => void;
  cloudDialogMode: 'download' | 'login' | 'select';
  setCloudDialogMode: (mode: 'download' | 'login' | 'select') => void;
  checkCloudDataAndShowDialog: (
    localLogsCount: number,
    savedApiSettings: EncryptedApiSettings | null,
    hasLoggedIn: boolean
  ) => Promise<void>;
  handleDownloadCloudData: (password?: string) => Promise<void>;
}

export const useCloudData = (): UseCloudDataReturn => {
  const [showCloudDataDialog, setShowCloudDataDialog] = useState(false);
  const [isClosingDialog, setIsClosingDialog] = useState(false);
  const [cloudRecordCount, setCloudRecordCount] = useState(0);
  const [cloudRecords, setCloudRecords] = useState<SmokeLog[]>([]);
  const [showPreviousLoginDialog, setShowPreviousLoginDialog] = useState(false);
  const [showRestorePasswordDialog, setShowRestorePasswordDialog] = useState(false);
  const [restorePassword, setRestorePassword] = useState('');
  const [restoreError, setRestoreError] = useState<string | null>(null);
  
  const [cloudPage, setCloudPage] = useState(0);
  const [cloudLogs, setCloudLogs] = useState<SmokeLog[]>([]);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMoreCloudLogs, setHasMoreCloudLogs] = useState(false);
  
  const [cloudDataSource, setCloudDataSource] = useState<'supabase' | 'feishu' | 'none' | 'both'>('none');
  const [cloudDataCount, setCloudDataCount] = useState(0);
  const [cloudDataRecords, setCloudDataRecords] = useState<SmokeLog[]>([]);
  const [cloudDialogMode, setCloudDialogMode] = useState<'download' | 'login' | 'select'>('download');

  const checkCloudDataAndShowDialog = useCallback(async (
    localLogsCount: number,
    savedApiSettings: EncryptedApiSettings | null,
    hasLoggedIn: boolean
  ) => {
    try {
      systemLogService.info('cloud', '检查云端数据', {
        localLogsCount,
        hasSavedApiSettings: !!savedApiSettings,
        hasLoggedIn
      });

      let supabaseDataExists = false;
      let feishuDataExists = false;

      if (savedApiSettings?.supabase && !hasLoggedIn) {
        try {
          const result = await apiService.checkSupabaseDataExists();
          supabaseDataExists = result.exists;
          if (result.count && result.count > 0) {
            setCloudDataCount(result.count);
          }
          systemLogService.info('cloud', 'Supabase 数据检查完成', { exists: supabaseDataExists, count: result.count });
        } catch (error) {
          systemLogService.warn('cloud', '检查 Supabase 数据失败', { message: (error as Error).message });
        }
      }

      if (savedApiSettings?.feishu) {
        try {
          const feishuConfig = savedApiSettings.feishu;
          if (typeof feishuConfig === 'string') {
            // 加密配置，需要密码解密
          } else if ((feishuConfig as Record<string, unknown>).apiUrl) {
            const result = await apiService.checkFeishuDataExists((feishuConfig as Record<string, unknown>).apiUrl as string);
            feishuDataExists = result.exists;
            systemLogService.info('cloud', '飞书数据检查完成', { exists: feishuDataExists });
          }
        } catch (error) {
          systemLogService.warn('cloud', '检查飞书数据失败', { message: (error as Error).message });
        }
      }

      if (supabaseDataExists || feishuDataExists) {
        if (supabaseDataExists && feishuDataExists) {
          setCloudDataSource('both');
        } else if (supabaseDataExists) {
          setCloudDataSource('supabase');
        } else {
          setCloudDataSource('feishu');
        }

        if (!hasLoggedIn && (supabaseDataExists || feishuDataExists)) {
          setCloudDialogMode('login');
          setShowCloudDataDialog(true);
          systemLogService.info('cloud', '显示云端数据登录对话框');
        } else if (hasLoggedIn && localLogsCount === 0) {
          setCloudDialogMode('download');
          setShowCloudDataDialog(true);
          systemLogService.info('cloud', '显示云端数据下载对话框');
        }
      } else {
        setCloudDataSource('none');
        systemLogService.info('cloud', '未发现云端数据');
      }
    } catch (error) {
      systemLogService.error('cloud', '检查云端数据过程出错', error as Error);
      setCloudDataSource('none');
    }
  }, []);

  const handleDownloadCloudData = useCallback(async (password?: string) => {
    try {
      setIsClosingDialog(false);
      
      const adapter = getStorageAdapter();
      const savedApiSettings = await adapter.getApiSettings();
      
      if (!savedApiSettings) {
        throw new Error('未找到API配置');
      }

      let downloadedLogs: SmokeLog[] = [];

      if (cloudDataSource === 'supabase' || cloudDataSource === 'both') {
        systemLogService.info('cloud', '开始从 Supabase 下载数据');
        
        const userId = '';
        
        const syncResult = await apiService.syncFromSupabase(
          userId,
          { upload: false, download: true },
          () => 'en'
        );

        if (syncResult.success) {
          downloadedLogs = syncResult.downloadedLogs || [];
          systemLogService.info('cloud', 'Supabase 数据下载成功', { count: downloadedLogs.length });
        }
      }

      if (cloudDataSource === 'feishu' || cloudDataSource === 'both') {
        systemLogService.info('cloud', '开始从飞书下载数据');
        
        const feishuResult = await apiService.syncFromFeishu({} as any, password as any);
        
        if (feishuResult.success) {
          const feishuLogs = feishuResult.downloadedLogs || [];
          downloadedLogs = [...downloadedLogs, ...feishuLogs];
          systemLogService.info('cloud', '飞书数据下载成功', { count: feishuLogs.length });
        }
      }

      if (downloadedLogs.length > 0) {
        await adapter.saveLogs(downloadedLogs);
        setCloudDataRecords(downloadedLogs);
        setCloudRecordCount(downloadedLogs.length);
        
        systemLogService.info('cloud', '云端数据下载并保存成功', { 
          total: downloadedLogs.length 
        });
      }

      setShowCloudDataDialog(false);
    } catch (error) {
      systemLogService.error('cloud', '下载数据失败', error as Error);
      setRestoreError(error instanceof Error ? error.message : '下载数据失败');
    }
  }, [cloudDataSource]);

  return {
    showCloudDataDialog,
    setShowCloudDataDialog,
    isClosingDialog,
    setIsClosingDialog,
    cloudRecordCount,
    setCloudRecordCount,
    cloudRecords,
    setCloudRecords,
    showPreviousLoginDialog,
    setShowPreviousLoginDialog,
    showRestorePasswordDialog,
    setShowRestorePasswordDialog,
    restorePassword,
    setRestorePassword,
    restoreError,
    setRestoreError,
    cloudPage,
    setCloudPage,
    cloudLogs,
    setCloudLogs,
    loadingMore,
    setLoadingStatus: setLoadingMore,
    hasMoreCloudLogs,
    setHasMoreCloudLogs,
    cloudDataSource,
    setCloudDataSource,
    cloudDataCount,
    setCloudDataCount,
    cloudDataRecords,
    setCloudDataRecords,
    cloudDialogMode,
    setCloudDialogMode,
    checkCloudDataAndShowDialog,
    handleDownloadCloudData
  };
};
