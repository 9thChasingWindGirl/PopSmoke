import { getSupabaseClient } from './apiService';
import { User, AuthState, AuthError } from '../types';
import EventHandle from '../event/EventHandle';
import { EventType } from '../event/EventType';

// 类型守卫函数
function isSupabaseError(error: unknown): error is { code: string; message: string } {
  return error !== null && typeof error === 'object' && 'code' in error && 'message' in error;
}

// 错误处理函数
function handleAuthError(error: unknown): AuthError {
  if (error instanceof Error) {
    // 网络错误
    if (error.name === 'TypeError' && (error.message.includes('fetch') || error.message.includes('Network'))) {
      return {
        code: 'NETWORK_ERROR',
        message: '网络连接失败，请检查网络设置',
        type: 'network'
      };
    }
    
    // 超时错误
    if (error.name === 'AbortError') {
      return {
        code: 'TIMEOUT',
        message: '请求超时，请检查网络连接',
        type: 'network'
      };
    }
    
    // Supabase 错误
    if (isSupabaseError(error)) {
      return {
        code: error.code,
        message: error.message,
        type: 'auth'
      };
    }
    
    // 其他错误
    return {
      code: 'UNKNOWN_ERROR',
      message: error.message,
      type: 'unknown'
    };
  }
  
  return {
    code: 'UNKNOWN_ERROR',
    message: '未知错误',
    type: 'unknown'
  };
}

// 获取当前使用的Supabase客户端
const getClient = async () => {
  return await getSupabaseClient();
};

// 认证服务单例管理（简化版）
class AuthServiceManager {
  private static instance: AuthServiceManager;
  private subscriptions: Set<{ unsubscribe: () => void }> = new Set();

  private constructor() {}

  public static getInstance(): AuthServiceManager {
    if (!AuthServiceManager.instance) {
      AuthServiceManager.instance = new AuthServiceManager();
    }
    return AuthServiceManager.instance;
  }

  public addSubscription(subscription: { unsubscribe: () => void }): void {
    this.subscriptions.add(subscription);
  }

  public removeSubscription(subscription: { unsubscribe: () => void }): void {
    this.subscriptions.delete(subscription);
  }

  public clearSubscriptions(): void {
    this.subscriptions.forEach(sub => {
      try {
        sub.unsubscribe();
      } catch (e) {
        console.warn('Error unsubscribing:', e);
      }
    });
    this.subscriptions.clear();
  }

  public getSubscriptionCount(): number {
    return this.subscriptions.size;
  }
}

const authManager = AuthServiceManager.getInstance();

export const authService = {
  // 注册新用户
  async signUp(email: string, password: string): Promise<AuthState> {
    try {
      const client = await getClient();
      const { data, error } = await client.auth.signUp({
        email,
        password,
      });

      if (error) {
        EventHandle.publish({
          type: EventType.AUTH_SIGNUP,
          category: 'auth',
          data: { success: false, error: error.message, email },
          timestamp: Date.now()
        });
        return {
          status: 'error',
          user: null,
          error: handleAuthError(error)
        };
      }

      EventHandle.publish({
        type: EventType.AUTH_SIGNUP,
        category: 'auth',
        data: { success: true, user: data.user, email },
        timestamp: Date.now()
      });

      return {
        status: 'authenticated',
        user: data.user as User,
        error: null,
      };
    } catch (error) {
      const authError = handleAuthError(error);
      EventHandle.publish({
        type: EventType.AUTH_SIGNUP,
        category: 'auth',
        data: { success: false, error: authError.message },
        timestamp: Date.now()
      });
      return {
        status: 'error',
        user: null,
        error: authError
      };
    }
  },

  // 用户登录
  async signIn(email: string, password: string): Promise<AuthState> {
    try {
      const client = await getClient();
      const { data, error } = await client.auth.signInWithPassword({
        email,
        password,
      });

      if (error) {
        EventHandle.publish({
          type: EventType.AUTH_SIGNIN,
          category: 'auth',
          data: { success: false, error: error.message, email },
          timestamp: Date.now()
        });
        return {
          status: 'error',
          user: null,
          error: handleAuthError(error)
        };
      }

      EventHandle.publish({
        type: EventType.AUTH_SIGNIN,
        category: 'auth',
        data: { success: true, user: data.user, email },
        timestamp: Date.now()
      });

      return {
        status: 'authenticated',
        user: data.user as User,
        error: null,
      };
    } catch (error) {
      const authError = handleAuthError(error);
      EventHandle.publish({
        type: EventType.AUTH_SIGNIN,
        category: 'auth',
        data: { success: false, error: authError.message, email },
        timestamp: Date.now()
      });
      if (authError.type === 'network') {
        return {
          status: 'error',
          user: null,
          error: authError // 使用原始错误信息
        };
      }
      return {
        status: 'error',
        user: null,
        error: authError
      };
    }
  },

  // 用户登出
  async signOut(): Promise<AuthState> {
    try {
      const client = await getClient();
      const { error } = await client.auth.signOut();

      if (error) {
        EventHandle.publish({
          type: EventType.AUTH_LOGOUT,
          category: 'auth',
          data: { success: false, error: error.message },
          timestamp: Date.now()
        });
        return {
          status: 'error',
          user: null,
          error: handleAuthError(error)
        };
      }

      EventHandle.publish({
        type: EventType.AUTH_LOGOUT,
        category: 'auth',
        data: { success: true },
        timestamp: Date.now()
      });

      return {
        status: 'unauthenticated',
        user: null,
        error: null,
      };
    } catch (error) {
      const authError = handleAuthError(error);
      EventHandle.publish({
        type: EventType.AUTH_LOGOUT,
        category: 'auth',
        data: { success: false, error: authError.message },
        timestamp: Date.now()
      });
      return {
        status: 'error',
        user: null,
        error: authError
      };
    }
  },

  // 获取当前用户（带超时）
  async getCurrentUser(): Promise<AuthState> {
    try {
      const client = await getClient();
      
      // 使用 Promise.race 实现超时
      const timeoutPromise = new Promise<never>((_, reject) => {
        setTimeout(() => reject(new Error('TIMEOUT')), 10000);
      });
      
      const { data: { user } } = await Promise.race([
        client.auth.getUser(),
        timeoutPromise
      ]) as { data: { user: User | null } };

      return {
        status: user ? 'authenticated' : 'unauthenticated',
        user: user as User,
        error: null,
      };
    } catch (error) {
      const authError = handleAuthError(error);
      if (authError.type === 'network' || authError.code === 'TIMEOUT') {
        return {
          status: 'error',
          user: null,
          error: authError
        };
      }
      return {
        status: 'error',
        user: null,
        error: authError
      };
    }
  },

  // 监听认证状态变化（合并版本）
  async onAuthStateChange(callback: (event: string, session: any, user: User | null) => void) {
    const client = await getClient();
    const { data: { subscription } } = client.auth.onAuthStateChange((event, session) => {
      callback(event, session, session?.user as User | null);
    });
    
    authManager.addSubscription(subscription);
    
    return {
      data: { subscription },
      unsubscribe: () => {
        authManager.removeSubscription(subscription);
        subscription.unsubscribe();
      }
    };
  },

  // 清理所有活跃的订阅
  cleanup() {
    authManager.clearSubscriptions();
  },

  // 更新用户密码
  async updatePassword(newPassword: string): Promise<AuthState> {
    try {
      const client = await getClient();
      const { error } = await client.auth.updateUser({
        password: newPassword
      });

      if (error) {
        EventHandle.publish({
          type: EventType.AUTH_PASSWORD_RESET,
          category: 'auth',
          data: { success: false, error: error.message },
          timestamp: Date.now()
        });
        return {
          status: 'error',
          user: null,
          error: handleAuthError(error)
        };
      }

      EventHandle.publish({
        type: EventType.AUTH_PASSWORD_RESET,
        category: 'auth',
        data: { success: true },
        timestamp: Date.now()
      });

      return {
        status: 'authenticated',
        user: null,
        error: null
      };
    } catch (error) {
      const authError = handleAuthError(error);
      EventHandle.publish({
        type: EventType.AUTH_PASSWORD_RESET,
        category: 'auth',
        data: { success: false, error: authError.message },
        timestamp: Date.now()
      });
      return {
        status: 'error',
        user: null,
        error: authError
      };
    }
  },
};
