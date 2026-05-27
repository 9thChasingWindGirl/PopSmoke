import { useState, useEffect, useCallback } from 'react';
import { User, AuthState, AuthError, OperationLog as OperationLogType } from '../types';
import { authService } from '../services/authService';
import { systemLogService } from '../services/systemLogService';
import { getSupabaseClient } from '../services/apiService';
import EventHandle from '../event/EventHandle';
import { EventType } from '../event/EventType';
import { getStorageAdapter, hasLoggedInBefore, setLoggedInFlag, getSupabaseRuntimeConfig, simpleEncrypt } from '../services/storageAdapter';

export interface UseAuthReturn {
  authState: AuthState;
  setAuthState: React.Dispatch<React.SetStateAction<AuthState>>;
  showAuthModal: boolean;
  setShowAuthModal: (show: boolean) => void;
  authMode: 'signin' | 'signup';
  setAuthMode: (mode: 'signin' | 'signup') => void;
  email: string;
  setEmail: (email: string) => void;
  password: string;
  setPassword: (password: string) => void;
  showPasswordReset: boolean;
  setShowPasswordReset: (show: boolean) => void;
  resetEmail: string;
  setResetEmail: (email: string) => void;
  newPassword: string;
  setNewPassword: (password: string) => void;
  confirmPassword: string;
  setConfirmPassword: (password: string) => void;
  resetStatus: { success?: boolean; message?: string } | null;
  setResetStatus: (status: { success?: boolean; message?: string } | null) => void;
  isResetting: boolean;
  setIsResetting: (resetting: boolean) => void;
  hasLoggedInSuccess: boolean;
  setHasLoggedInSuccess: (success: boolean) => void;
  operationLogs: OperationLogType[];
  setOperationLogs: React.Dispatch<React.SetStateAction<OperationLogType[]>>;
  handleLogin: (email: string, password: string) => Promise<void>;
  handleLogout: () => Promise<void>;
  handlePasswordReset: (newPassword: string, confirmPassword: string) => Promise<void>;
  checkExistingSession: () => Promise<{ user: User | null; hasSession: boolean }>;
}

export const useAuth = (): UseAuthReturn => {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    status: 'loading',
    error: null
  });

  const [showAuthModal, setShowAuthModal] = useState(false);
  const [authMode, setAuthMode] = useState<'signin' | 'signup'>('signin');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPasswordReset, setShowPasswordReset] = useState(false);
  const [resetEmail, setResetEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [resetStatus, setResetStatus] = useState<{ success?: boolean; message?: string } | null>(null);
  const [isResetting, setIsResetting] = useState(false);
  const [hasLoggedInSuccess, setHasLoggedInSuccess] = useState(false);
  const [operationLogs, setOperationLogs] = useState<OperationLogType[]>([]);

  useEffect(() => {
    const authUnsubscribe = EventHandle.subscribe(EventType.AUTH_LOGIN, (event) => {
      const data = event.data as { success?: boolean; user?: User; error?: string | { code: string; message: string; type: string } };
      if (data?.success && data?.user) {
        setAuthState({ user: data.user, status: 'authenticated', error: null });
      } else if (data?.success === false) {
        if (typeof data?.error === 'string') {
          setAuthState({ user: null, status: 'error', error: { code: 'AUTH_ERROR', message: data?.error || '登录失败', type: 'auth' } });
        } else if (data?.error) {
          setAuthState({ user: null, status: 'error', error: data.error as AuthError });
        } else {
          setAuthState({ user: null, status: 'error', error: { code: 'AUTH_ERROR', message: '登录失败', type: 'auth' } });
        }
      }
    });

    const logoutUnsubscribe = EventHandle.subscribe(EventType.AUTH_LOGOUT, (event) => {
      const data = event.data as { success?: boolean };
      if (data?.success) {
        setAuthState({ user: null, status: 'unauthenticated', error: null });
      }
    });

    return () => {
      authUnsubscribe.unsubscribe();
      logoutUnsubscribe.unsubscribe();
    };
  }, []);

  const handleLogin = useCallback(async (loginEmail: string, loginPassword: string) => {
    try {
      setIsResetting(true);
      const result = await authService.signIn(loginEmail, loginPassword);
      
      if (result.status === 'authenticated' && result.user) {
        setAuthState({ user: result.user, status: 'authenticated', error: null });
        setHasLoggedInSuccess(true);
        setShowAuthModal(false);
        setLoggedInFlag(true);

        const loginLog: OperationLogType = {
          id: `login_${Date.now()}`,
          type: 'sync',
          data: {
            id: '',
            user_id: result.user.id,
            record_date: new Date().toISOString().split('T')[0],
            record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
            timestamp: Date.now()
          } as any,
          syncStatus: 'synced',
          timestamp: Date.now(),
          message: `登录成功: ${result.user.email}`
        };
        setOperationLogs(prev => [loginLog, ...prev]);

        systemLogService.info('auth', '用户登录成功', { userId: result.user.id });
      } else {
        setAuthState({
          user: null,
          status: 'error',
          error: { code: 'AUTH_ERROR', message: result.error?.message || '登录失败', type: 'auth' }
        });
      }
    } catch (error) {
      systemLogService.error('auth', '登录失败', error as Error);
      setAuthState({
        user: null,
        status: 'error',
        error: { code: 'AUTH_ERROR', message: '登录过程中发生错误', type: 'auth' }
      });
    } finally {
      setIsResetting(false);
    }
  }, []);

  const handleLogout = useCallback(async () => {
    try {
      await authService.signOut();

      const logoutLog: OperationLogType = {
        id: `logout_${Date.now()}`,
        type: 'sync',
        data: {
          id: '',
          user_id: authState.user?.id || 'local',
          record_date: new Date().toISOString().split('T')[0],
          record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
          timestamp: Date.now()
        } as any,
        syncStatus: 'synced',
        timestamp: Date.now(),
        message: '已登出，切换到本地模式'
      };
      setOperationLogs(prev => [logoutLog, ...prev]);

      setAuthState({ user: null, status: 'unauthenticated', error: null });
      systemLogService.info('auth', '用户登出');
    } catch (error) {
      systemLogService.error('auth', '登出失败', error as Error);
    }
  }, [authState.user]);

  const handlePasswordReset = useCallback(async (newPwd: string, confirmPwd: string) => {
    if (!newPwd.trim()) {
      setResetStatus({ success: false, message: '请输入新密码' });
      return;
    }

    if (newPwd.length < 6) {
      setResetStatus({ success: false, message: '密码长度至少6个字符' });
      return;
    }

    if (newPwd !== confirmPwd) {
      setResetStatus({ success: false, message: '两次输入的密码不一致' });
      return;
    }

    setIsResetting(true);
    try {
      const client = await getSupabaseClient();
      const { error } = await client.auth.updateUser({ password: newPwd });

      if (error) {
        if (error.message.includes('New password should be different')) {
          throw new Error('新密码不能与当前密码相同');
        }
        throw error;
      }

      setResetStatus({ success: true, message: '密码重置成功' });
      setShowPasswordReset(false);
      setNewPassword('');
      setConfirmPassword('');
      systemLogService.info('auth', '密码重置成功');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '密码重置失败';
      setResetStatus({ success: false, message: errorMessage });
      systemLogService.error('auth', '密码重置失败', error as Error);
    } finally {
      setIsResetting(false);
    }
  }, []);

  const checkExistingSession = useCallback(async (): Promise<{ user: User | null; hasSession: boolean }> => {
    try {
      const currentUser = await authService.getCurrentUser();
      return {
        user: currentUser.user,
        hasSession: !!currentUser.user
      };
    } catch (error) {
      systemLogService.error('auth', '检查会话失败', error as Error);
      return { user: null, hasSession: false };
    }
  }, []);

  return {
    authState,
    setAuthState,
    showAuthModal,
    setShowAuthModal,
    authMode,
    setAuthMode,
    email,
    setEmail,
    password,
    setPassword,
    showPasswordReset,
    setShowPasswordReset,
    resetEmail,
    setResetEmail,
    newPassword,
    setNewPassword,
    confirmPassword,
    setConfirmPassword,
    resetStatus,
    setResetStatus,
    isResetting,
    setIsResetting,
    hasLoggedInSuccess,
    setHasLoggedInSuccess,
    operationLogs,
    setOperationLogs,
    handleLogin,
    handleLogout,
    handlePasswordReset,
    checkExistingSession
  };
};
