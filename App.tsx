import React, { useState, useEffect, useRef } from 'react';
import { builder, BuilderComponent } from '@builder.io/react';
import { ViewState, SmokeLog, AppSettings, User, AuthState, OperationLog as OperationLogType, Language } from './types';
import { authService } from './services/authService';
import { systemLogService } from './services/systemLogService';
import { PopDashboard } from './components/pages/PopDashboard';
import { PopAnalysis } from './components/pages/PopAnalysis';
import { PopHistory } from './components/pages/PopHistory';
import { PopSettings } from './components/pages/PopSettings';
import { PopAPI } from './components/pages/PopAPI';
import { PopButton } from './components/ui/PopButton';
import { PopNotification } from './components/ui/PopNotification';
import { PopStorageErrorDialog } from './components/ui/PopStorageErrorDialog';
import { PopAuthModal } from './components/ui/PopAuthModal';
import { PopPasswordResetDialog } from './components/ui/PopPasswordResetDialog';
import { PopCloudDataDialog } from './components/ui/PopCloudDataDialog';
import { PopColorPicker } from './components/ui/PopColorPicker';
import { PopLoading } from './components/ui/PopNotification';
import PopNav from './components/ui/PopNav';
import { ErrorBoundary } from './components/ErrorBoundary';
import { THEME_PRESETS } from './constants';
import { TRANSLATIONS } from './i18n';
import { getStorageAdapter, getAuthStorageAdapter, getSyncQueueManager, simpleDecrypt, isAndroidPlatform, hasLoggedInBefore, setLoggedInFlag, getSupabaseRuntimeConfig, setSupabaseRuntimeConfig, clearSupabaseRuntimeConfig, createSupabaseAuthStorage } from './services/storageAdapter';
import { apiService, createSupabaseClient, setSupabaseClient, initializeSupabaseClient, isSupabaseClientInitialized, persistSupabaseRuntimeConfig, fetchAllTables, convertToSmokeLogs, getSupabase } from './services/apiService';
import EventHandle from './event/EventHandle';
import { EventType } from './event/EventType';
import { getStorageKeys } from './utils/logUtils';
import { safeSetItem, getStorageUsage, cleanupOldData } from './utils/storageUtils';

let effectIdCounter = 0;

interface PopEffectItem {
  id: number;
  x: number;
  y: number;
}

export default function App() {
  const [view, setView] = useState<ViewState>(ViewState.DASHBOARD);
  const [logs, setLogs] = useState<SmokeLog[]>([]);
  const [settings, setSettingsState] = useState<AppSettings>({
    user_id: '',
    dailyLimit: 10,
    warningLimit: 7,
    themeColor: '#FFD700',
    language: 'en'
  });
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    loading: true,
    error: null
  });
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [authMode, setAuthMode] = useState<'signin' | 'signup'>('signin');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLocalMode, setIsLocalMode] = useState(false);
  const [showPasswordReset, setShowPasswordReset] = useState(false);
  const [resetEmail, setResetEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [resetStatus, setResetStatus] = useState<{ success?: boolean; message?: string } | null>(null);
  const [isResetting, setIsResetting] = useState(false);
  const [showCloudDataDialog, setShowCloudDataDialog] = useState(false);
  const [isClosingDialog, setIsClosingDialog] = useState(false);
  const [cloudRecordCount, setCloudRecordCount] = useState(0);
  const [cloudRecords, setCloudRecords] = useState<any[]>([]);
  const [showPreviousLoginDialog, setShowPreviousLoginDialog] = useState(false);
  const [showRestorePasswordDialog, setShowRestorePasswordDialog] = useState(false);
  const [restorePassword, setRestorePassword] = useState('');
  const [restoreError, setRestoreError] = useState<string | null>(null);
  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [showStorageErrorDialog, setShowStorageErrorDialog] = useState(false);
  const [storageError, setStorageError] = useState<string | null>(null);
  const [showAuthErrorNotification, setShowAuthErrorNotification] = useState(false);
  
  const [popEffects, setPopEffects] = useState<PopEffectItem[]>([]);
  const [operationLogs, setOperationLogs] = useState<OperationLogType[]>([]);
  const touchStartX = useRef<number | null>(null);
  const initStartedRef = useRef(false);
  const syncStateManager = getSyncQueueManager();
  

  
  // 云端分页加载状态
  const [cloudPage, setCloudPage] = useState(0);
  const [cloudLogs, setCloudLogs] = useState<SmokeLog[]>([]);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMoreCloudLogs, setHasMoreCloudLogs] = useState(false);
  
  // 云端数据发现对话框状态
  const [cloudDataSource, setCloudDataSource] = useState<'supabase' | 'feishu' | 'none'>('none');
  const [cloudDataCount, setCloudDataCount] = useState(0);
  const [cloudDataRecords, setCloudDataRecords] = useState<SmokeLog[]>([]);
  const [cloudDialogMode, setCloudDialogMode] = useState<'download' | 'login'>('download');
  
  const isAndroid = isAndroidPlatform();

  useEffect(() => {
    // 初始化Builder.io
    builder.init('6c1e65c12fc04d159fa60bbc198d7183');

    const authUnsubscribe = EventHandle.subscribe(EventType.AUTH_LOGIN, (event) => {
      const data = event.data as { success?: boolean; user?: User; error?: string };
      if (data?.success && data?.user) {
        setAuthState({ user: data.user, loading: false, error: null });
      } else if (data?.success === false) {
        setAuthState({ user: null, loading: false, error: data?.error || '登录失败' });
      }
    });

    const logoutUnsubscribe = EventHandle.subscribe(EventType.AUTH_LOGOUT, (event) => {
      const data = event.data as { success?: boolean };
      if (data?.success) {
        setAuthState({ user: null, loading: false, error: null });
        setLogs([]);
      }
    });

    const syncSuccessUnsubscribe = EventHandle.subscribe(EventType.SYNC_SUCCESS, async (event) => {
      const data = event.data as { success?: boolean };
      if (data?.success) {
        const adapter = getStorageAdapter();
        const updatedLogs = await adapter.getLogs();
        setLogs(updatedLogs);
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
      authUnsubscribe.unsubscribe();
      logoutUnsubscribe.unsubscribe();
      syncSuccessUnsubscribe.unsubscribe();
      logCreateUnsubscribe.unsubscribe();
      logUpdateUnsubscribe.unsubscribe();
      logDeleteUnsubscribe.unsubscribe();
    };
  }, []);

  useEffect(() => {
    // 使用 ref 确保初始化只执行一次（防止 React StrictMode 双重渲染）
    if (initStartedRef.current) return;
    initStartedRef.current = true;
    
    setIsLocalMode(true);
    setAuthState({ user: null, loading: false, error: null });

    const initializeApp = async () => {
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
          setSettingsState(localSettings);
          systemLogService.debug('settings', '应用本地设置', { settings: localSettings });
        }
        
        if (localLogs && localLogs.length > 0) {
          setLogs(localLogs);
          systemLogService.debug('storage', '应用本地日志数据');
        }
        
        const shouldTryRestore = hasLoggedIn;

        if (shouldTryRestore) {
          systemLogService.info('auth', '尝试恢复会话', {
            hasLoggedIn,
            hasRuntimeSupabaseConfig: !!runtimeSupabaseConfig
          });

          const currentUser = await authService.getCurrentUser();
          if (currentUser.user) {
            systemLogService.info('auth', '会话恢复成功', { userId: currentUser.user.id });
            setShowAuthModal(false);
            setShowPasswordReset(false);
            setShowPreviousLoginDialog(false);
            setShowRestorePasswordDialog(false);
            setIsLocalMode(false);
            setLoggedInFlag(true);
            loadUserData(currentUser.user);
          } else {
            systemLogService.info('auth', '无活跃会话，检查保存的API设置');
            if (savedApiSettings && localLogs?.length > 0) {
              setShowPreviousLoginDialog(true);
            } else {
              checkCloudDataAndShowDialog(
                localLogs?.length || 0,
                savedApiSettings,
                false
              );
            }
          }
        } else {
          systemLogService.info('init', '首次启动，检查云端数据');
          checkCloudDataAndShowDialog(
            localLogs?.length || 0,
            savedApiSettings,
            false
          );
        }
      } catch (error) {
        systemLogService.error('init', '应用初始化失败', error as Error);
      } finally {
        setIsLoading(false);
        systemLogService.info('init', '应用初始化完成');
      }
    };

    initializeApp();

    const { unsubscribe } = authService.onAuthEvent((event, session) => {
      systemLogService.info('auth', `认证事件: ${event}`);
      
      if (event === 'PASSWORD_RECOVERY') {
        systemLogService.info('auth', '密码恢复检测');
        setShowPasswordReset(true);
        setShowAuthModal(false);
        if (session?.user?.email) {
          setResetEmail(session.user.email);
          systemLogService.debug('auth', '设置密码重置邮箱', { email: session.user.email });
        }
        return;
      }
      
      if (session?.user && session.user.email_confirmed_at) {
        systemLogService.info('auth', '用户登录成功', { userId: session.user.id });
        
        // 更新认证状态
        setAuthState({
          user: session.user as User,
          loading: false,
          error: null
        });
        
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
          } as SmokeLog,
          syncStatus: 'synced',
          timestamp: Date.now(),
          message: `登录成功: ${session.user.email}`
        };
        setOperationLogs(prev => [loginLog, ...prev]);
        
        setShowAuthModal(false);
        setShowPasswordReset(false);
        setShowPreviousLoginDialog(false);
        setShowRestorePasswordDialog(false);
        setIsLocalMode(false);
        setLoggedInFlag(true);
        loadUserData(session.user as User);
      } else if (event === 'SIGNED_OUT') {
        systemLogService.info('auth', '用户登出');
        
        // 添加登出成功的操作日志
        const logoutLog: OperationLogType = {
          id: `logout_${Date.now()}`,
          type: 'sync',
          data: {
            id: '',
            user_id: 'local',
            record_date: new Date().toISOString().split('T')[0],
            record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
            timestamp: Date.now()
          } as SmokeLog,
          syncStatus: 'synced',
          timestamp: Date.now(),
          message: '已登出，切换到本地模式'
        };
        setOperationLogs(prev => [logoutLog, ...prev]);
        
        setAuthState({ user: null, loading: false, error: null });
        setIsLocalMode(true);
        
        const loadLocalData = async () => {
          systemLogService.info('storage', '加载本地数据');
          const adapter = getStorageAdapter();
          const localSettings = await adapter.getSettings();
          if (localSettings) {
            setSettingsState(localSettings);
            systemLogService.debug('settings', '应用本地设置');
          } else {
            setSettingsState({
              user_id: '',
              dailyLimit: 10,
              warningLimit: 7,
              themeColor: '#FFD700',
              language: 'en'
            });
            systemLogService.debug('settings', '应用默认设置');
          }
          const localLogs = await adapter.getLogs();
          if (localLogs) {
            setLogs(localLogs);
            systemLogService.debug('storage', '应用本地日志数据');
          } else {
            setLogs([]);
            systemLogService.debug('storage', '无本地日志数据');
          }
        };
        loadLocalData();
      }
    });

    return () => {
      unsubscribe();
    };
  }, [isInitialized]);

  const loadUserData = async (user: User) => {
    systemLogService.info('auth', '开始加载用户数据', { userId: user.id });
    await setLoggedInFlag(true);

    try {
      const result = await apiService.loadUserData(user.id);

      if (result.settings) {
        setSettingsState(result.settings);
      }

      if (result.needCloudSync && result.cloudLogs) {
        systemLogService.info('storage', `发现 ${result.cloudLogs.length} 条云端日志，显示同步对话框`);
        setCloudDataSource('supabase');
        setCloudDialogMode('download');
        setCloudDataCount(result.cloudLogs.length);
        setCloudDataRecords(result.cloudLogs);
        setShowCloudDataDialog(true);
        setLogs([]);
      } else {
        setLogs(result.localLogs);
      }

      systemLogService.info('auth', '用户数据加载成功');
    } catch (error) {
      systemLogService.error('auth', '加载用户数据失败', error as Error);

      const adapter = getStorageAdapter();
      const localLogs = await adapter.getLogs();
      setLogs(localLogs || []);
    }
  };

  // 从云端加载更多历史记录
  const loadMoreCloudLogs = async (userId: string, page: number): Promise<SmokeLog[]> => {
    try {
      setLoadingMore(true);
      const moreLogs = await apiService.getLogs(userId, page, 1000);
      
      if (moreLogs.length > 0) {
        setCloudLogs(prev => [...prev, ...moreLogs]);
        setLogs(prev => [...prev, ...moreLogs]);
        setCloudPage(page);
        setHasMoreCloudLogs(moreLogs.length === 1000);
      } else {
        setHasMoreCloudLogs(false);
      }
      
      return moreLogs;
    } catch (error) {
      console.error('Failed to load more cloud logs:', error);
      setHasMoreCloudLogs(false);
      return [];
    } finally {
      setLoadingMore(false);
    }
  };

  const checkCloudDataAndShowDialog = async (
    localLogsCount: number,
    savedApiSettings: any,
    isLoggedIn: boolean
  ) => {
    if (localLogsCount > 0) {
      console.log('Local data exists, skip cloud data dialog');
      return;
    }

    if (savedApiSettings?.feishu) {
      console.log('Feishu API configured (encrypted), showing download dialog...');
      setCloudDataSource('feishu');
      setCloudDialogMode('download');
      setShowCloudDataDialog(true);
      return;
    }

    if (savedApiSettings?.supabase) {
      console.log('Supabase API configured, showing login dialog...');
      setCloudDataSource('supabase');
      setCloudDialogMode('login');
      setShowCloudDataDialog(true);
      return;
    }
  };

  const handleDownloadCloudData = async (password?: string) => {
    if (cloudDataSource === 'none') {
      return { needPassword: false };
    }

    try {
      const result = await apiService.downloadCloudData(
        cloudDataSource,
        authState.user?.id,
        password
      );

      if (result.needPassword) {
        return { needPassword: true };
      }

      if (result.logs.length === 0) {
        return { needPassword: false };
      }

      setLogs(result.logs);
      return { needPassword: false };
    } catch (error) {
      console.error('Failed to download cloud data:', error);
      return { needPassword: false };
    }
  };

  const handleLoginForCloudData = () => {
    setShowAuthModal(true);
    setAuthMode('signin');
  };

  const handleSkipCloudData = () => {
    console.log('User skipped cloud data download');
  };

  const handleAuth = async () => {
    let result: AuthState;
    
    if (authMode === 'signin') {
      result = await authService.signIn(email, password);
      if (result.user && result.user.email_confirmed_at) {
        const loginLog: OperationLogType = {
          id: `login_${Date.now()}`,
          type: 'sync',
          data: { id: '', user_id: result.user.id, record_date: new Date().toISOString().split('T')[0], record_time: new Date().toTimeString().split(' ')[0].substring(0, 5), timestamp: Date.now() } as SmokeLog,
          syncStatus: 'synced',
          timestamp: Date.now(),
          message: `登录成功: ${email}`
        };
        setOperationLogs(prev => [loginLog, ...prev]);
        setShowAuthModal(false);
        setIsLocalMode(false);
      }
    } else {
      result = await authService.signUp(email, password);
      if (result.user) {
        const signUpLog: OperationLogType = {
          id: `signup_${Date.now()}`,
          type: 'sync',
          data: { id: '', user_id: result.user.id, record_date: new Date().toISOString().split('T')[0], record_time: new Date().toTimeString().split(' ')[0].substring(0, 5), timestamp: Date.now() } as SmokeLog,
          syncStatus: 'pending',
          timestamp: Date.now(),
          message: `注册成功: ${email}，请验证邮箱`
        };
        setOperationLogs(prev => [signUpLog, ...prev]);
        setAuthState({ user: null, loading: false, error: t.verifyEmail || '注册成功！请查收验证邮件并点击链接完成验证。' });
        setShowAuthErrorNotification(true);
        return;
      }
    }
    
    if (result.error) {
      setShowAuthErrorNotification(true);
    }
  };

  const handleSignOut = async () => {
    const userEmail = authState.user?.email;
    const result = await authService.signOut();
    
    // 添加登出成功的操作日志
    const logoutLog: OperationLogType = {
      id: `logout_${Date.now()}`,
      type: 'sync',
      data: {
        id: '',
        user_id: 'local',
        record_date: new Date().toISOString().split('T')[0],
        record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
        timestamp: Date.now()
      } as SmokeLog,
      syncStatus: 'synced',
      timestamp: Date.now(),
      message: userEmail ? `已登出: ${userEmail}` : '已登出，切换到本地模式'
    };
    setOperationLogs(prev => [logoutLog, ...prev]);
    
    setAuthState(result);
    setIsLocalMode(true);
    setLoggedInFlag(false);
  };

  const handleRestoreLogin = async () => {
    setRestoreError(null);
    
    try {
      const adapter = getStorageAdapter();
      const savedApiSettings = await adapter.getApiSettings();
      
      if (!savedApiSettings) {
        setRestoreError('没有找到保存的API设置');
        return;
      }
      
      const decryptedSecurityPassword = simpleDecrypt(savedApiSettings.securityPassword || '', restorePassword);
      if (decryptedSecurityPassword !== restorePassword) {
        setRestoreError(t.currentPasswordIncorrect || '当前密码不正确');
        return;
      }
      
      const decryptedSupabase = JSON.parse(simpleDecrypt(savedApiSettings.supabase || '', restorePassword));
      
      if (isAndroidPlatform() && decryptedSupabase.apiUrl && decryptedSupabase.anonKey) {
        const client = createSupabaseClient(decryptedSupabase.apiUrl, decryptedSupabase.anonKey);
        setSupabaseClient(client, decryptedSupabase);
        await persistSupabaseRuntimeConfig(decryptedSupabase.apiUrl, decryptedSupabase.anonKey);
      }
      
      setShowRestorePasswordDialog(false);
      setShowPreviousLoginDialog(false);
      setRestorePassword('');
      
      const result = await authService.getCurrentUser();
      setAuthState(result);
      
      if (result.user && result.user.email_confirmed_at) {
        setShowAuthModal(false);
        setIsLocalMode(false);
        setLoggedInFlag(true);
        await loadUserData(result.user);
      } else {
        setIsLocalMode(true);
      }
      
    } catch (error) {
      setRestoreError(error instanceof Error ? error.message : '恢复登录失败');
    }
  };

  const handleRecord = async () => {
    systemLogService.info('ui', '开始记录吸烟数据');
    
    const usage = getStorageUsage();
    if (usage.percent > 0.8) {
      systemLogService.warn('storage', '存储使用超过80%，清理旧数据');
      cleanupOldData(authState.user?.id);
    }
    
    try {
      // 等待记录创建完成
      await apiService.createRecord(authState.user?.id, logs);
      
      // 记录创建完成后触发POW动画
      const newEffectId = effectIdCounter++;
      const randomX = 50 + (Math.random() * 10 - 5); 
      const randomY = 50 + (Math.random() * 10 - 5);
      
      setPopEffects(prev => [...prev, { id: newEffectId, x: randomX, y: randomY }]);
      
      setTimeout(() => {
        setPopEffects(prev => prev.filter(e => e.id !== newEffectId));
      }, 600);
      
      const operationLog: OperationLogType = {
        id: crypto.randomUUID(),
        type: 'create',
        data: {} as SmokeLog,
        syncStatus: 'pending',
        timestamp: Date.now(),
        message: `新增记录`
      };
      setOperationLogs(prev => [operationLog, ...prev]);
      
      syncStateManager.addOperation(operationLog);
    } catch (error) {
      systemLogService.error('ui', '记录失败', error as Error);
      setStorageError(error instanceof Error ? error.message : 'Record error');
      setShowStorageErrorDialog(true);
    }
  };

  const handleDelete = async (id: string) => {
    const logToDelete = logs.find(l => l.id === id);
    
    try {
      await apiService.deleteLog(id, authState.user?.id || 'local', logs);
      
      if (logToDelete) {
        const operationLog: OperationLogType = {
          id: crypto.randomUUID(),
          type: 'delete',
          data: logToDelete,
          syncStatus: 'synced',
          timestamp: Date.now(),
          message: `删除记录: ${logToDelete.record_date} ${logToDelete.record_time}`
        };
        setOperationLogs(prev => [operationLog, ...prev]);
        syncStateManager.addOperation(operationLog);
      }
    } catch (error) {
      console.error('Failed to delete log:', error);
      setStorageError(error instanceof Error ? error.message : 'Delete error');
      setShowStorageErrorDialog(true);
    }
  };

  const handleUpdate = async (updatedLog: SmokeLog) => {
    try {
      await apiService.updateLog(updatedLog, logs);
      
      const operationLog: OperationLogType = {
        id: crypto.randomUUID(),
        type: 'update',
        data: updatedLog,
        syncStatus: 'pending',
        timestamp: Date.now(),
        message: `编辑${updatedLog.record_date}第 ${updatedLog.record_index} 条记录`
      };
      setOperationLogs(prev => [operationLog, ...prev]);
      syncStateManager.addOperation(operationLog);
    } catch (error) {
      console.error('Failed to update log:', error);
      setStorageError(error instanceof Error ? error.message : 'Update error');
      setShowStorageErrorDialog(true);
    }
  };

  const handleUpdateSettings = async (newSettings: AppSettings) => {
    const updatedSettings = { ...newSettings, user_id: authState.user?.id || 'local' };
    setSettingsState(updatedSettings);
    try {
      await apiService.saveSettingsComplete(updatedSettings);
    } catch (error) {
      console.error('Failed to save settings:', error);
      setStorageError(error instanceof Error ? error.message : 'Storage error');
      setShowStorageErrorDialog(true);
    }
  };

  const handleThemeChange = async (color: string) => {
    const newSettings = { ...settings, themeColor: color, user_id: authState.user?.id || 'local' };
    setSettingsState(newSettings);
    try {
      await apiService.saveSettingsComplete(newSettings);
    } catch (error) {
      console.error('Failed to save theme:', error);
      setStorageError(error instanceof Error ? error.message : 'Storage error');
      setShowStorageErrorDialog(true);
    }
  };

  const handleDownloadFromCloud = async () => {
    const adapter = getStorageAdapter();
    setLogs(cloudRecords);
    try {
      await adapter.saveLogs(cloudRecords);
    } catch (error) {
      console.error('Failed to save cloud records locally', error);
      setStorageError(error instanceof Error ? error.message : 'Storage error');
      setShowStorageErrorDialog(true);
      return;
    }
    
    setIsClosingDialog(true);
    setTimeout(() => {
      setShowCloudDataDialog(false);
      setIsClosingDialog(false);
    }, 300);
  };

  const handleSkipDownload = () => {
    setIsClosingDialog(true);
    setTimeout(() => {
      setShowCloudDataDialog(false);
      setIsClosingDialog(false);
    }, 300);
  };

  const handleCloseDialog = () => {
    setIsClosingDialog(true);
    setTimeout(() => {
      setShowCloudDataDialog(false);
      setIsClosingDialog(false);
    }, 300);
  };

  const handleLanguageChange = async (language: string) => {
    const newSettings = { ...settings, language: language as Language, user_id: authState.user?.id || 'local' };
    setSettingsState(newSettings);
    try {
      await apiService.saveSettingsComplete(newSettings);
    } catch (error) {
      console.error('Failed to save language:', error);
      setStorageError(error instanceof Error ? error.message : 'Storage error');
      setShowStorageErrorDialog(true);
    }
  };

  const t = TRANSLATIONS[settings.language];

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' });
  }, [view]);

  const viewsOrder = [ViewState.DASHBOARD, ViewState.ANALYSIS, ViewState.HISTORY, ViewState.API, ViewState.SETTINGS];
  
  const navigate = (direction: 'next' | 'prev') => {
    const currentIndex = viewsOrder.indexOf(view);
    let newIndex = direction === 'next' ? currentIndex + 1 : currentIndex - 1;
    
    if (newIndex < 0) newIndex = 0;
    if (newIndex >= viewsOrder.length) newIndex = viewsOrder.length - 1;
    
    setView(viewsOrder[newIndex]);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleTouchStart = (e: React.TouchEvent) => {
    const target = e.target as HTMLElement;
    if (target.tagName === 'INPUT' && target.getAttribute('type') === 'range') {
      touchStartX.current = null;
      return;
    }
    touchStartX.current = e.touches[0].clientX;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    const target = e.target as HTMLElement;
    if (target.tagName === 'INPUT' && target.getAttribute('type') === 'range') {
      touchStartX.current = null;
      return;
    }
    
    if (touchStartX.current === null) return;
    const touchEndX = e.changedTouches[0].clientX;
    const diff = touchStartX.current - touchEndX;

    if (diff > 50) {
      navigate('next');
    }
    else if (diff < -50) {
      navigate('prev');
    }
    touchStartX.current = null;
  };

  if (isLoading) {
    return <PopLoading settings={settings} />;
  }

  const handlePasswordReset = async () => {
    if (!newPassword.trim()) {
      setResetStatus({
        success: false,
        message: t.newPassword || '请输入新密码'
      });
      return;
    }

    if (newPassword.length < 6) {
      setResetStatus({
        success: false,
        message: t.passwordMinLength || '密码至少需要6位字符'
      });
      return;
    }

    if (newPassword !== confirmPassword) {
      setResetStatus({
        success: false,
        message: t.passwordMismatch || '两次输入的密码不一致'
      });
      return;
    }

    setIsResetting(true);
    setResetStatus(null);

    try {
      const result = await authService.updatePassword(newPassword);
      
      if (result.success) {
        setResetStatus({
          success: true,
          message: t.passwordResetSuccess || '密码重置成功'
        });
        
        setTimeout(() => {
          setShowPasswordReset(false);
          setView(ViewState.DASHBOARD);
        }, 2000);
      } else {
        setResetStatus({
          success: false,
          message: result.error || t.passwordResetFailed || '密码重置失败'
        });
      }
    } catch (error) {
      setResetStatus({
        success: false,
        message: error instanceof Error ? error.message : '密码重置失败'
      });
    } finally {
      setIsResetting(false);
    }
  };

  if (showPasswordReset) {
    return (
      <PopPasswordResetDialog
        isOpen={showPasswordReset}
        resetEmail={resetEmail}
        newPassword={newPassword}
        confirmPassword={confirmPassword}
        resetStatus={resetStatus}
        isResetting={isResetting}
        onNewPasswordChange={setNewPassword}
        onConfirmPasswordChange={setConfirmPassword}
        onReset={handlePasswordReset}
        onClose={() => setShowPasswordReset(false)}
        themeColor={settings.themeColor}
        language={settings.language}
        t={t}
      />
    );
  }

  if (showPreviousLoginDialog) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-paper p-4">
        <div className="bg-white border-4 border-black p-8 max-w-md w-full shadow-pop">
          <h1 className="font-display text-3xl mb-6 text-center">
            POP<span style={{ color: settings.themeColor }}>SMOKE</span>
          </h1>
          
          <h2 className="font-bold text-xl mb-4">
            {t.previousLoginFound}
          </h2>
          
          <p className="mb-6 text-sm text-gray-600">
            {t.previousLoginFoundMessage}
          </p>
          
          <div className="space-y-4">
            <button
              onClick={() => {
                setShowPreviousLoginDialog(false);
                setShowRestorePasswordDialog(true);
              }}
              className="w-full bg-black text-white font-bold py-3 border-4 border-black hover:bg-white hover:text-black transition-colors"
            >
              {t.restoreLogin}
            </button>
            <button
              onClick={() => {
                setShowPreviousLoginDialog(false);
                setIsLocalMode(true);
              }}
              className="w-full bg-gray-200 text-black font-bold py-3 border-4 border-gray-400 hover:bg-gray-300 transition-colors"
            >
              {t.continueLocalMode}
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (showRestorePasswordDialog) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-paper p-4">
        <div className="bg-white border-4 border-black p-8 max-w-md w-full shadow-pop">
          <h1 className="font-display text-3xl mb-6 text-center">
            POP<span style={{ color: settings.themeColor }}>SMOKE</span>
          </h1>
          
          <h2 className="font-bold text-xl mb-4">
            {t.securityPassword}
          </h2>
          
          <p className="mb-4 text-sm text-gray-600">
            {t.enterSecurityPasswordForRestore}
          </p>
          
          {restoreError && (
            <div className="mb-4 p-3 border-2 border-red-500 rounded bg-red-50 text-red-700">
              {restoreError}
            </div>
          )}
          
          <div className="space-y-4">
            <div>
              <label className="block font-bold mb-1">{t.enterPassword}</label>
              <input
                type="password"
                value={restorePassword}
                onChange={(e) => setRestorePassword(e.target.value)}
                className="w-full border-4 border-black p-2 font-display"
                placeholder={t.enterPassword}
              />
            </div>
            
            <div className="flex space-x-3">
              <button
                onClick={handleRestoreLogin}
                className="flex-1 bg-black text-white font-bold py-3 border-4 border-black hover:bg-white hover:text-black transition-colors"
              >
                {t.confirm}
              </button>
              <button
                onClick={() => {
                  setShowRestorePasswordDialog(false);
                  setShowPreviousLoginDialog(true);
                  setRestorePassword('');
                  setRestoreError(null);
                }}
                className="flex-1 bg-gray-200 text-black font-bold py-3 border-4 border-gray-400 hover:bg-gray-300 transition-colors"
              >
                {t.cancel}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (showAuthModal) {
    return (
      <PopAuthModal
        isOpen={showAuthModal}
        mode={authMode}
        email={email}
        password={password}
        error={authState.error}
        loading={authState.loading}
        showErrorNotification={showAuthErrorNotification}
        onEmailChange={setEmail}
        onPasswordChange={setPassword}
        onSubmit={handleAuth}
        onClose={() => setShowAuthModal(false)}
        onSwitchMode={setAuthMode}
        onSkipLogin={() => {
          setShowAuthModal(false);
          setIsLocalMode(true);
        }}
        onCloseError={() => {
          setShowAuthErrorNotification(false);
          setAuthState({ user: null, loading: false, error: null });
        }}
        themeColor={settings.themeColor}
        t={t}
      />
    );
  }



  if (showStorageErrorDialog) {
    return (
      <PopStorageErrorDialog
        isOpen={showStorageErrorDialog}
        errorMessage={storageError || ''}
        onDismiss={() => setShowStorageErrorDialog(false)}
        onGoToSettings={() => {
          setShowStorageErrorDialog(false);
          setView(ViewState.SETTINGS);
        }}
        themeColor={settings.themeColor}
        t={t}
      />
    );
  }

  return (
    <ErrorBoundary>
      <div 
        className="min-h-screen md:h-auto pb-[calc(70px+env(safe-area-inset-bottom))] md:pb-0 font-body text-black overflow-y-auto md:overflow-visible selection:bg-black selection:text-white flex flex-col"
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
      >
        
        <div className={`fixed inset-0 pointer-events-none transition-colors duration-500 opacity-10`} style={{ backgroundColor: settings.themeColor }}></div>

        <PopNav
          view={view}
          setView={setView}
          settings={settings}
          handleUpdateSettings={handleUpdateSettings}
          t={t}
          isAndroid={isAndroid}
        />

      <main className="flex-1 w-full max-w-4xl mx-auto px-2 relative flex justify-start md:justify-center gap-2 md:gap-4 overflow-y-auto md:overflow-visible py-4 md:py-8">
        
        <div className="hidden md:flex flex-col justify-center shrink-0 z-40 fixed left-4 top-1/2 -translate-y-1/2">
           <button 
             onClick={() => navigate('prev')} 
             disabled={view === ViewState.DASHBOARD}
             className={`p-4 bg-white border-4 border-black shadow-pop hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px] disabled:opacity-20 disabled:shadow-none disabled:translate-x-1 disabled:translate-y-1 disabled:cursor-not-allowed transition-all rounded-lg`}
           >
             <span className="font-display text-2xl">&larr;</span>
           </button>
        </div>

        <div className="flex-1 w-full max-w-3xl relative flex flex-col justify-start md:justify-center md:block">
            {popEffects.map(effect => (
                <div 
                    key={effect.id}
                    className="fixed z-[100] pointer-events-none animate-pop-blast"
                    style={{ left: `${effect.x}%`, top: `${effect.y}%` }}
                >
                   <div className="bg-white border-4 border-black p-4 rotate-12 shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
                       <span className="font-display text-6xl text-red-500 whitespace-nowrap">POW!</span>
                   </div>
                </div>
            ))}

            <div key={view} className="animate-slide-in w-full flex flex-col justify-start md:justify-center md:block">
              {view === ViewState.DASHBOARD && (
                <div className="md:min-h-[calc(100vh-140px)] md:flex md:flex-col md:justify-center">
                    <PopDashboard 
                      logs={logs} 
                      settings={settings} 
                      onRecord={handleRecord}
                      onDelete={handleDelete}
                      onUpdate={handleUpdate}
                    />
                </div>
              )}
              
              {view === ViewState.ANALYSIS && (
                <div>
                    <PopAnalysis 
                      logs={logs} 
                      settings={settings} 
                      user={authState.user}
                      onNavigateToSettings={() => setView(ViewState.SETTINGS)}
                      onRefreshLogs={(newLogs) => setLogs(newLogs)}
                      operationLogs={operationLogs}
                      onClearOperationLogs={() => setOperationLogs([])}
                      onAddOperationLog={(log) => setOperationLogs(prev => [log, ...prev])}
                    />
                </div>
              )}
              
              {view === ViewState.HISTORY && (
                <div>
                    <PopHistory 
                      logs={logs} 
                      settings={settings} 
                      userId={authState.user?.id}
                      onLoadMore={authState.user ? (page) => loadMoreCloudLogs(authState.user!.id, page) : undefined}
                      hasMore={hasMoreCloudLogs}
                      loading={loadingMore}
                    />
                </div>
              )}
              
              {view === ViewState.SETTINGS && (
                <div>
                    <PopSettings 
                      settings={settings} 
                      onSave={handleUpdateSettings} 
                      user={authState.user}
                      onSignOut={handleSignOut}
                      onNavigateToAuth={() => setShowAuthModal(true)}
                      onNavigateToDashboard={() => setView(ViewState.DASHBOARD)}
                      onRefreshLogs={(newLogs) => setLogs(newLogs)}
                      onAddOperationLog={(log) => setOperationLogs(prev => [log, ...prev])}
                    />
                </div>
              )}
              
              {view === ViewState.API && (
                <div>
                    <PopAPI 
                      settings={settings} 
                      onSave={handleUpdateSettings} 
                      refreshLogs={() => authState.user && loadUserData(authState.user)}
                      user={authState.user}
                    />
                </div>
              )}
            </div>
        </div>

        <div className="hidden md:flex flex-col justify-center shrink-0 z-40 fixed right-4 top-1/2 -translate-y-1/2">
           <button 
             onClick={() => navigate('next')} 
             disabled={view === ViewState.SETTINGS}
             className={`p-4 bg-white border-4 border-black shadow-pop hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px] disabled:opacity-20 disabled:shadow-none disabled:translate-x-1 disabled:translate-y-1 disabled:cursor-not-allowed transition-all rounded-lg`}
           >
             <span className="font-display text-2xl">&rarr;</span>
           </button>
        </div>

      </main>

      <div className="md:hidden fixed bottom-0 left-0 w-full bg-white border-t-4 border-black flex justify-around p-3 z-50 shadow-[0px_-4px_0px_0px_rgba(0,0,0,0.1)] h-[calc(60px+env(safe-area-inset-bottom))] items-start pb-[env(safe-area-inset-bottom)] box-content">
        <button 
            onClick={() => setView(ViewState.DASHBOARD)}
            className={`flex flex-col items-center mt-1 ${view === ViewState.DASHBOARD ? 'text-black opacity-100' : 'text-gray-400 opacity-60'}`}
        >
            <span className="font-display text-lg tracking-wide">{t.tracker}</span>
        </button>
        <button 
            onClick={() => setView(ViewState.ANALYSIS)}
            className={`flex flex-col items-center mt-1 ${view === ViewState.ANALYSIS ? 'text-black opacity-100' : 'text-gray-400 opacity-60'}`}
        >
            <span className="font-display text-lg tracking-wide">{t.analysis}</span>
        </button>
        <button 
            onClick={() => setView(ViewState.HISTORY)}
            className={`flex flex-col items-center mt-1 ${view === ViewState.HISTORY ? 'text-black opacity-100' : 'text-gray-400 opacity-60'}`}
        >
            <span className="font-display text-lg tracking-wide">{t.history}</span>
        </button>
        <button 
            onClick={() => setView(ViewState.API)}
            className={`flex flex-col items-center mt-1 ${view === ViewState.API ? 'text-black opacity-100' : 'text-gray-400 opacity-60'}`}
        >
            <span className="font-display text-lg tracking-wide">{t.api}</span>
        </button>
        <button 
            onClick={() => setView(ViewState.SETTINGS)}
            className={`flex flex-col items-center mt-1 ${view === ViewState.SETTINGS ? 'text-black opacity-100' : 'text-gray-400 opacity-60'}`}
        >
            <span className="font-display text-lg tracking-wide">{t.settings}</span>
        </button>
      </div>

      <PopCloudDataDialog
        visible={showCloudDataDialog}
        mode={cloudDialogMode}
        recordCount={cloudDataCount}
        onDownload={handleDownloadCloudData}
        onLogin={handleLoginForCloudData}
        onSkip={handleSkipCloudData}
        onClose={() => setShowCloudDataDialog(false)}
        themeColor={settings.themeColor}
        title={cloudDialogMode === 'login' ? t.loginRequired : t.cloudDataFound}
        message={cloudDialogMode === 'login' ? t.loginRequiredMessage : t.cloudDataFoundMessage}
        downloadText={t.downloadFromCloud}
        loginText={t.loginNow}
        skipText={t.skipDownload}
        requirePassword={!!cloudDataSource}
        passwordPlaceholder={t.enterPassword || 'Enter password'}
      />

      </div>
    </ErrorBoundary>
  );
}
