import { create } from 'zustand';
import { User, AuthState, AuthError, OperationLog as OperationLogType } from '../types';
import { authService } from '../services/authService';
import { systemLogService } from '../services/systemLogService';
import { getSupabaseClient } from '../services/apiService';

interface AuthStoreState {
  user: User | null;
  status: AuthState['status'];
  error: AuthError | null;
  isLoading: boolean;
  showAuthModal: boolean;
  authMode: 'signin' | 'signup';
  showPasswordReset: boolean;
  resetEmail: string;
  hasLoggedInSuccess: boolean;
  operationLogs: OperationLogType[];
  
  // 表单状态
  email: string;
  password: string;
}

interface AuthStoreActions {
  setUser: (user: User | null) => void;
  setStatus: (status: AuthState['status']) => void;
  setError: (error: AuthError | null) => void;
  setLoading: (loading: boolean) => void;
  setShowAuthModal: (show: boolean) => void;
  setAuthMode: (mode: 'signin' | 'signup') => void;
  setShowPasswordReset: (show: boolean, email?: string) => void;
  setResetEmail: (email: string) => void;
  setHasLoggedInSuccess: (success: boolean) => void;
  addOperationLog: (log: OperationLogType) => void;
  clearOperationLogs: () => void;
  
  // 表单操作
  setEmail: (email: string) => void;
  setPassword: (password: string) => void;
  
  // 异步操作
  login: (email: string, password: string) => Promise<{ success: boolean; error?: string }>;
  signup: (email: string, password: string) => Promise<{ success: boolean; error?: string }>;
  logout: () => Promise<void>;
  checkSession: () => Promise<User | null>;
  resetPassword: (newPassword: string, confirmPassword: string) => Promise<{ success: boolean; message?: string }>;
}

export const useAuthStore = create<AuthStoreState & AuthStoreActions>((set, get) => ({
  // 初始状态
  user: null,
  status: 'loading',
  error: null,
  isLoading: false,
  showAuthModal: false,
  authMode: 'signin',
  showPasswordReset: false,
  resetEmail: '',
  hasLoggedInSuccess: false,
  operationLogs: [],
  
  // 表单状态
  email: '',
  password: '',

  // 同步操作
  setUser: (user) => set({ user }),
  setStatus: (status) => set({ status }),
  setError: (error) => set({ error }),
  setLoading: (isLoading) => set({ isLoading }),
  setShowAuthModal: (showAuthModal) => set({ showAuthModal }),
  setAuthMode: (authMode) => set({ authMode }),
  setShowPasswordReset: (showPasswordReset, email) => set({ 
    showPasswordReset, 
    ...(email !== undefined && { resetEmail: email })
  }),
  setResetEmail: (resetEmail) => set({ resetEmail }),
  setHasLoggedInSuccess: (hasLoggedInSuccess) => set({ hasLoggedInSuccess }),
  addOperationLog: (log) => set((state) => ({ 
    operationLogs: [log, ...state.operationLogs] 
  })),
  clearOperationLogs: () => set({ operationLogs: [] }),
  
  // 表单操作
  setEmail: (email) => set({ email }),
  setPassword: (password) => set({ password }),

  // 异步操作
  login: async (email, password) => {
    try {
      set({ isLoading: true, error: null });
      
      const result = await authService.signIn(email, password);
      
      if (result.status === 'authenticated' && result.user) {
        const loginLog: OperationLogType = {
          id: `login_${Date.now()}`,
          type: 'sync',
          data: {
            id: '',
            user_id: result.user.id,
            record_date: new Date().toISOString().split('T')[0],
            record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
            timestamp: Date.now()
          },
          syncStatus: 'synced',
          timestamp: Date.now(),
          message: `登录成功: ${result.user.email}`
        };

        set({
          user: result.user,
          status: 'authenticated',
          error: null,
          showAuthModal: false,
          hasLoggedInSuccess: true,
          isLoading: false,
        });

        get().addOperationLog(loginLog);
        (systemLogService as any).info('auth', '用户登录成功', { userId: result.user.id });

        return { success: true };
      } else {
        set({
          user: null,
          status: 'error',
          error: { code: 'AUTH_ERROR', message: result.error?.message || '登录失败', type: 'auth' },
          isLoading: false,
        });

        return { success: false, error: result.error?.message || '登录失败' };
      }
    } catch (error) {
      systemLogService.error('auth', '登录失败', error as Error);
      set({
        user: null,
        status: 'error',
        error: { code: 'AUTH_ERROR', message: '登录过程中发生错误', type: 'auth' },
        isLoading: false,
      });

      return { success: false, error: '登录过程中发生错误' };
    }
  },

  signup: async (email, password) => {
    try {
      set({ isLoading: true, error: null });
      
      const result = await authService.signUp(email, password);
      
      if (result.status === 'authenticated' && result.user) {
        set({
          user: result.user,
          status: 'authenticated',
          error: null,
          showAuthModal: false,
          hasLoggedInSuccess: true,
          isLoading: false,
        });

        (systemLogService as any).info('auth', '用户注册成功', { userId: result.user.id });
        return { success: true };
      } else {
        set({
          user: null,
          status: 'error',
          error: { code: 'AUTH_ERROR', message: result.error?.message || '注册失败', type: 'auth' },
          isLoading: false,
        });

        return { success: false, error: result.error?.message || '注册失败' };
      }
    } catch (error) {
      systemLogService.error('auth', '注册失败', error as Error);
      set({
        user: null,
        status: 'error',
        error: { code: 'AUTH_ERROR', message: '注册过程中发生错误', type: 'auth' },
        isLoading: false,
      });

      return { success: false, error: '注册过程中发生错误' };
    }
  },

  logout: async () => {
    try {
      await authService.signOut();

      const logoutLog: OperationLogType = {
        id: `logout_${Date.now()}`,
        type: 'sync',
        data: {
          id: '',
          user_id: get().user?.id || 'local',
          record_date: new Date().toISOString().split('T')[0],
          record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
          timestamp: Date.now()
        },
        syncStatus: 'synced',
        timestamp: Date.now(),
        message: '已登出，切换到本地模式'
      };

      set({
        user: null,
        status: 'unauthenticated',
        error: null,
      });

      get().addOperationLog(logoutLog);
      systemLogService.info('auth', '用户登出');
    } catch (error) {
      systemLogService.error('auth', '登出失败', error as Error);
    }
  },

  checkSession: async () => {
    try {
      const currentUser = await authService.getCurrentUser();
      
      if (currentUser.user) {
        set({
          user: currentUser.user,
          status: 'authenticated',
          error: null,
        });
        
        return currentUser.user;
      } else {
        set({
          user: null,
          status: 'unauthenticated',
          error: null,
        });
        
        return null;
      }
    } catch (error) {
      systemLogService.error('auth', '检查会话失败', error as Error);
      set({
        user: null,
        status: 'unauthenticated',
        error: { code: 'AUTH_ERROR', message: '检查会话失败', type: 'auth' },
      });
      
      return null;
    }
  },

  resetPassword: async (newPassword, confirmPassword) => {
    if (!newPassword.trim()) {
      return { success: false, message: '请输入新密码' };
    }

    if (newPassword.length < 6) {
      return { success: false, message: '密码长度至少6个字符' };
    }

    if (newPassword !== confirmPassword) {
      return { success: false, message: '两次输入的密码不一致' };
    }

    try {
      set({ isLoading: true });
      
      const client = await getSupabaseClient();
      const { error } = await client.auth.updateUser({ password: newPassword });

      if (error) {
        if (error.message.includes('New password should be different')) {
          throw new Error('新密码不能与当前密码相同');
        }
        throw error;
      }

      set({
        showPasswordReset: false,
        isLoading: false,
      });

      systemLogService.info('auth', '密码重置成功');
      return { success: true, message: '密码重置成功' };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '密码重置失败';
      set({ isLoading: false });
      systemLogService.error('auth', '密码重置失败', error as Error);
      return { success: false, message: errorMessage };
    }
  },
}));
