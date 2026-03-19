import { getSupabaseClient, getSupabase } from './apiService';
import { User, AuthState } from '../types';
import EventHandle from '../event/EventHandle';
import { EventType } from '../event/EventType';

// 获取当前使用的Supabase客户端
const getClient = async () => {
  return await getSupabaseClient();
};

// 认证服务单例管理
class AuthServiceManager {
  private static instance: AuthServiceManager;
  private activeSubscriptions: Set<{ unsubscribe: () => void }> = new Set();

  private constructor() {}

  public static getInstance(): AuthServiceManager {
    if (!AuthServiceManager.instance) {
      AuthServiceManager.instance = new AuthServiceManager();
    }
    return AuthServiceManager.instance;
  }

  public addSubscription(subscription: { unsubscribe: () => void }): void {
    this.activeSubscriptions.add(subscription);
  }

  public removeSubscription(subscription: { unsubscribe: () => void }): void {
    this.activeSubscriptions.delete(subscription);
  }

  public unsubscribeAll(): void {
    this.activeSubscriptions.forEach(sub => {
      try {
        sub.unsubscribe();
      } catch (e) {
        console.warn('Error unsubscribing:', e);
      }
    });
    this.activeSubscriptions.clear();
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
          type: EventType.AUTH_LOGIN,
          category: 'auth',
          data: { success: false, error: error.message, email },
          timestamp: Date.now()
        });
        return {
          user: null,
          loading: false,
          error: error.message,
        };
      }

      EventHandle.publish({
        type: EventType.AUTH_LOGIN,
        category: 'auth',
        data: { success: true, user: data.user, email },
        timestamp: Date.now()
      });

      return {
        user: data.user as User,
        loading: false,
        error: null,
      };
    } catch (error) {
      EventHandle.publish({
        type: EventType.AUTH_LOGIN,
        category: 'auth',
        data: { success: false, error: error instanceof Error ? error.message : '注册失败' },
        timestamp: Date.now()
      });
      return {
        user: null,
        loading: false,
        error: error instanceof Error ? error.message : '注册失败',
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
          type: EventType.AUTH_LOGIN,
          category: 'auth',
          data: { success: false, error: error.message, email },
          timestamp: Date.now()
        });
        return {
          user: null,
          loading: false,
          error: error.message,
        };
      }

      EventHandle.publish({
        type: EventType.AUTH_LOGIN,
        category: 'auth',
        data: { success: true, user: data.user, email },
        timestamp: Date.now()
      });

      return {
        user: data.user as User,
        loading: false,
        error: null,
      };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '登录失败';
      EventHandle.publish({
        type: EventType.AUTH_LOGIN,
        category: 'auth',
        data: { success: false, error: errorMessage, email },
        timestamp: Date.now()
      });
      if (errorMessage.includes('failed to fetch') || errorMessage.includes('Network error')) {
        return {
          user: null,
          loading: false,
          error: 'API配置无效，请先进入API管理页面验证安全密码并查看已保存的API设置',
        };
      }
      return {
        user: null,
        loading: false,
        error: errorMessage,
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
          user: null,
          loading: false,
          error: error.message,
        };
      }

      EventHandle.publish({
        type: EventType.AUTH_LOGOUT,
        category: 'auth',
        data: { success: true },
        timestamp: Date.now()
      });

      return {
        user: null,
        loading: false,
        error: null,
      };
    } catch (error) {
      EventHandle.publish({
        type: EventType.AUTH_LOGOUT,
        category: 'auth',
        data: { success: false, error: error instanceof Error ? error.message : '登出失败' },
        timestamp: Date.now()
      });
      return {
        user: null,
        loading: false,
        error: error instanceof Error ? error.message : '登出失败',
      };
    }
  },

  // 获取当前用户（带超时）
  async getCurrentUser(): Promise<AuthState> {
    try {
      const client = await getClient();
      
      // 添加超时处理
      const timeoutPromise = new Promise<{ user: any }>((_, reject) => {
        setTimeout(() => reject(new Error('Request timeout')), 10000);
      });
      
      const userPromise = client.auth.getUser();
      
      const { data: { user } } = await Promise.race([userPromise, timeoutPromise]) as { data: { user: any } };

      return {
        user: user as User,
        loading: false,
        error: null,
      };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取用户信息失败';
      // 检查是否是网络错误或超时
      if (errorMessage.includes('failed to fetch') || errorMessage.includes('Network error') || errorMessage.includes('timeout')) {
        return {
          user: null,
          loading: false,
          error: '网络连接超时，请检查网络设置或API配置',
        };
      }
      return {
        user: null,
        loading: false,
        error: errorMessage,
      };
    }
  },

  // 监听认证状态变化
  onAuthStateChange(callback: (user: User | null) => void) {
    const { data: { subscription } } = getSupabase().auth.onAuthStateChange((event, session) => {
      callback(session?.user as User | null);
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

  // 监听认证事件（包括密码重置等）
  onAuthEvent(callback: (event: string, session: any) => void) {
    const { data: { subscription } } = getSupabase().auth.onAuthStateChange((event, session) => {
      callback(event, session);
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
    authManager.unsubscribeAll();
  },

  // 更新用户密码
  async updatePassword(newPassword: string): Promise<{ success: boolean; error?: string }> {
    try {
      const client = await getClient();
      const { error } = await client.auth.updateUser({
        password: newPassword
      });

      if (error) {
        return {
          success: false,
          error: error.message
        };
      }

      return {
        success: true
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : '密码更新失败'
      };
    }
  },
};
