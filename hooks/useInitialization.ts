import { useState, useEffect, useRef, useCallback } from 'react';
import { AppSettings, SmokeLog, User, OperationLog as OperationLogType, EncryptedApiSettings, AuthStatus } from '../types';
import { getStorageAdapter, hasLoggedInBefore, setLoggedInFlag, getSupabaseRuntimeConfig, simpleEncrypt } from '../services/storageAdapter';
import { authService } from '../services/authService';
import { systemLogService } from '../services/systemLogService';

export interface UseInitializationReturn {
  isInitialized: boolean;
  setIsInitialized: (initialized: boolean) => void;
  isLoading: boolean;
  setIsLoading: (loading: boolean) => void;
  isConnecting: boolean;
  setIsConnecting: (connecting: boolean) => void;
  isLocalMode: boolean;
  setIsLocalMode: (localMode: boolean) => void;
  showStorageErrorDialog: boolean;
  setShowStorageErrorDialog: (show: boolean) => void;
  storageError: string | null;
  setStorageError: (error: string | null) => void;
  isLoadingUserData: boolean;
  setIsLoadingUserData: (loading: boolean) => void;
  initStartedRef: React.RefObject<boolean>;
  initializeApp: (
    callbacks: InitializationCallbacks
  ) => Promise<void>;
}

export interface InitializationCallbacks {
  onSettingsLoaded?: (settings: AppSettings) => void;
  onLogsLoaded?: (logs: SmokeLog[]) => void;
  onAuthStateChange?: (user: User | null, status: AuthStatus, error?: Error) => void;
  onShowAuthModal?: () => void;
  onHideAuthModal?: () => void;
  onShowPasswordReset?: (email?: string) => void;
  onHidePasswordReset?: () => void;
  onShowPreviousLoginDialog?: () => void;
  onHidePreviousLoginDialog?: () => void;
  onShowRestorePasswordDialog?: () => void;
  onHideRestorePasswordDialog?: () => void;
  onCheckCloudData?: (localLogsCount: number, savedApiSettings: EncryptedApiSettings | null, hasLoggedIn: boolean) => Promise<void>;
  onLoadUserData?: (user: User) => Promise<void>;
  onAddOperationLog?: (log: OperationLogType) => void;
}

export const useInitialization = (): UseInitializationReturn => {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isLocalMode, setIsLocalMode] = useState(false);
  const [showStorageErrorDialog, setShowStorageErrorDialog] = useState(false);
  const [storageError, setStorageError] = useState<string | null>(null);
  const [isLoadingUserData, setIsLoadingUserData] = useState(false);
  
  const initStartedRef = useRef(false);

  const initializeApp = useCallback(async (callbacks: InitializationCallbacks) => {
    if (initStartedRef.current) return;
    initStartedRef.current = true;

    setIsLocalMode(true);
    callbacks.onAuthStateChange?.(null, 'unauthenticated');

    systemLogService.info('init', '开始初始化应用');
    
    try {
      const adapter = getStorageAdapter();
      
      // 并行执行所有初始化操作
      const [localSettings, localLogs, hasLoggedIn, savedApiSettings, runtimeSupabaseConfig] = await Promise.all([
        adapter.getSettings(),
        adapter.getLogs(),
        hasLoggedInBefore(),
        adapter.getApiSettings(),
        getSupabaseRuntimeConfig()
      ]);
      
      systemLogService.info('storage', `加载本地设置: ${localSettings ? '成功' : '失败'}`);
      systemLogService.info('storage', `加载本地日志: ${localLogs?.length || 0} 条记录`);
      
      if (localSettings && localSettings.user_id !== undefined) {
        callbacks.onSettingsLoaded?.(localSettings);
        systemLogService.debug('settings', '应用本地设置', { settings: localSettings });
      }
      
      if (localLogs && localLogs.length > 0) {
        callbacks.onLogsLoaded?.(localLogs);
        systemLogService.debug('storage', '应用本地日志数据');
      }
      
      // 检查API配置，确定本地/云端模式
      const hasFeishu = !!savedApiSettings?.feishu;
      const hasSupabase = !!savedApiSettings?.supabase;
      
      if (!hasFeishu && !hasSupabase) {
        systemLogService.info('init', '未配置任何API，使用本地模式');
        setIsLocalMode(true);
      } else if (hasFeishu && !hasSupabase) {
        systemLogService.info('init', '仅配置飞书API，使用本地模式 + 飞书远端获取数据');
        setIsLocalMode(true);
      } else {
        systemLogService.info('init', '配置了Supabase API，使用云端模式');
        setIsLocalMode(false);
      }

      // 检查本地数据后，根据登录状态和API配置决定后续操作
      if (hasLoggedIn) {
        systemLogService.info('auth', '尝试恢复会话', {
          hasLoggedIn,
          hasRuntimeSupabaseConfig: !!runtimeSupabaseConfig
        });

        setIsConnecting(true);
        try {
          const currentUser = await authService.getCurrentUser();
          if (currentUser.user) {
            systemLogService.info('auth', '会话恢复成功', { userId: currentUser.user.id });
            
            callbacks.onHideAuthModal?.();
            callbacks.onHidePasswordReset?.();
            callbacks.onHidePreviousLoginDialog?.();
            callbacks.onHideRestorePasswordDialog?.();
            setIsLocalMode(false);
            setLoggedInFlag(true);
            
            if (localLogs?.length === 0) {
              await callbacks.onCheckCloudData?.(
                localLogs?.length || 0,
                savedApiSettings,
                true
              );
            } else {
              await callbacks.onLoadUserData?.(currentUser.user);
            }
          } else {
            systemLogService.info('auth', '无活跃会话，检查保存的API设置');
            if (savedApiSettings && localLogs?.length > 0) {
              callbacks.onShowPreviousLoginDialog?.();
            } else {
              await callbacks.onCheckCloudData?.(
                localLogs?.length || 0,
                savedApiSettings,
                false
              );
            }
          }
        } finally {
          setIsConnecting(false);
        }
      } else {
        systemLogService.info('init', '检查云端数据');
        setIsConnecting(true);
        try {
          await callbacks.onCheckCloudData?.(
            localLogs?.length || 0,
            savedApiSettings,
            false
          );
        } finally {
          setIsConnecting(false);
        }
      }

      // 设置认证状态订阅
      setupAuthSubscription(callbacks);

    } catch (error) {
      systemLogService.error('init', '应用初始化失败', error as Error);
      
      // 显示存储错误对话框
      if (error instanceof Error && error.message.includes('storage')) {
        setStorageError(error.message);
        setShowStorageErrorDialog(true);
      }
    } finally {
      setIsLoading(false);
      setIsInitialized(true);
      systemLogService.info('init', '应用初始化完成');
    }
  }, []);

  const setupAuthSubscription = async (callbacks: InitializationCallbacks) => {
    try {
      await authService.onAuthStateChange((event, session, user) => {
        systemLogService.info('auth', `认证事件: ${event}`);
        
        if (event === 'PASSWORD_RECOVERY') {
          systemLogService.info('auth', '密码恢复检测');
          callbacks.onShowPasswordReset?.(session?.user?.email);
          callbacks.onHideAuthModal?.();
          return;
        }
        
        if (session?.user && session.user.email_confirmed_at) {
          // 更新认证状态
          callbacks.onAuthStateChange?.(
            session.user as User,
            'authenticated'
          );
          
          // 添加登录成功的操作日志
          const loginLog: OperationLogType = {
            id: `login_${Date.now()}`,
            type: 'sync',
            data: {
              id: '',
              user_id: session.user.id,
              record_date: new Date().toISOString().split('T')[0],
              record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
              timestamp: Date.now()
            } as any,
            syncStatus: 'synced',
            timestamp: Date.now(),
            message: `登录成功: ${session.user.email}`
          };
          callbacks.onAddOperationLog?.(loginLog);
          
          callbacks.onHideAuthModal?.();
          callbacks.onHidePasswordReset?.();
          callbacks.onHidePreviousLoginDialog?.();
          callbacks.onHideRestorePasswordDialog?.();
          setIsLocalMode(false);
          setLoggedInFlag(true);
          
          // 保存API设置到SQLite（异步执行，不阻塞）
          saveApiSettingsOnLogin(session.user.email);
        } else if (event === 'SIGNED_OUT') {
          systemLogService.info('auth', '用户登出');
          
          const logoutLog: OperationLogType = {
            id: `logout_${Date.now()}`,
            type: 'sync',
            data: {
              id: '',
              user_id: 'local',
              record_date: new Date().toISOString().split('T')[0],
              record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
              timestamp: Date.now()
            } as any,
            syncStatus: 'synced',
            timestamp: Date.now(),
            message: '已登出，切换到本地模式'
          };
          callbacks.onAddOperationLog?.(logoutLog);
          
          callbacks.onAuthStateChange?.(null, 'unauthenticated');
          setIsLocalMode(true);
        }
      });
    } catch (error) {
      systemLogService.error('auth', 'Failed to set up auth subscription:', error as Error);
    }
  };

  const saveApiSettingsOnLogin = async (userEmail?: string) => {
    try {
      const adapter = getStorageAdapter();
      const existingSettings = await adapter.getApiSettings();
      
      if (existingSettings && existingSettings.supabase) {
        systemLogService.debug('auth', 'API设置已存在，跳过保存');
        return;
      }
      
      const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || '';
      const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || '';
      
      if (!supabaseUrl || !supabaseAnonKey) {
        systemLogService.warn('auth', '环境变量中未找到Supabase配置');
        return;
      }
      
      const password = userEmail || 'default';
      const encryptedSettings = {
        supabase: simpleEncrypt(JSON.stringify({ apiUrl: supabaseUrl, anonKey: supabaseAnonKey }), password),
        securityPassword: simpleEncrypt(password, password)
      };
      
      await adapter.saveApiSettings(encryptedSettings);
      systemLogService.info('auth', '登录成功，已保存API设置到SQLite');
    } catch (error) {
      systemLogService.error('auth', '保存API设置失败:', error as Error);
    }
  };

  return {
    isInitialized,
    setIsInitialized,
    isLoading,
    setIsLoading,
    isConnecting,
    setIsConnecting,
    isLocalMode,
    setIsLocalMode,
    showStorageErrorDialog,
    setShowStorageErrorDialog,
    storageError,
    setStorageError,
    isLoadingUserData,
    setIsLoadingUserData,
    initStartedRef,
    initializeApp
  };
};
