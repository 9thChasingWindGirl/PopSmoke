import { createClient, SupabaseClient, User } from '@supabase/supabase-js';

const DEFAULT_SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL || '';
const DEFAULT_SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY || '';

export interface SupabaseConfig {
  apiUrl: string;
  anonKey: string;
}

class SupabaseClientManager {
  private static instance: SupabaseClientManager;
  private client: SupabaseClient | null = null;
  private config: SupabaseConfig | null = null;
  private defaultClient: SupabaseClient;
  private isInitialized = false;

  private constructor() {
    this.defaultClient = createClient(
      DEFAULT_SUPABASE_URL || 'https://your-project.supabase.co',
      DEFAULT_SUPABASE_ANON_KEY || 'your-anon-key',
      { auth: { persistSession: true, autoRefreshToken: true } }
    );
    this.client = this.defaultClient;
  }

  public static getInstance(): SupabaseClientManager {
    if (!SupabaseClientManager.instance) {
      SupabaseClientManager.instance = new SupabaseClientManager();
    }
    return SupabaseClientManager.instance;
  }

  public async getClient(): Promise<SupabaseClient> {
    if (!this.isInitialized) {
      try {
        const userConfig = await this.getUserConfig();
        if (userConfig) {
          this.setClient(userConfig.apiUrl, userConfig.anonKey);
        }
      } catch (error) {
        console.error('[SupabaseClient] 初始化失败:', error);
      }
      this.isInitialized = true;
    }
    return this.client || this.defaultClient;
  }

  public setClient(apiUrl: string, anonKey: string): SupabaseClient {
    if (this.config && this.config.apiUrl === apiUrl && this.config.anonKey === anonKey) {
      return this.client!;
    }
    
    this.client = createClient(apiUrl, anonKey, {
      auth: { persistSession: true, autoRefreshToken: true }
    });
    this.config = { apiUrl, anonKey };
    return this.client;
  }

  public hasUserConfig(): boolean {
    return this.config !== null;
  }

  public resetToDefault(): void {
    this.client = this.defaultClient;
    this.config = null;
  }

  public getConfig(): SupabaseConfig | null {
    return this.config;
  }

  public setInitialized(value: boolean): void {
    this.isInitialized = value;
  }

  public getInitialized(): boolean {
    return this.isInitialized;
  }

  private async getUserConfig(): Promise<SupabaseConfig | null> {
    try {
      // Web端优先使用环境变量
      const { isWebPlatform } = await import('../../storageAdapter');
      if (isWebPlatform()) {
        const envUrl = (import.meta.env.VITE_SUPABASE_URL || '').trim();
        const envKey = (import.meta.env.VITE_SUPABASE_ANON_KEY || '').trim();
        
        if (envUrl && envKey) {
          return { apiUrl: envUrl, anonKey: envKey };
        }
      }
      
      // 尝试从运行时配置获取
      const { getSupabaseRuntimeConfig } = await import('../../storageAdapter');
      const runtimeConfig = await getSupabaseRuntimeConfig();
      if (runtimeConfig?.apiUrl && runtimeConfig?.anonKey) {
        return runtimeConfig;
      }

      // 从存储的API设置获取
      const { getStorageAdapter } = await import('../../storageAdapter');
      const adapter = getStorageAdapter();
      const savedSettings = await adapter.getApiSettings();

      if (!savedSettings?.supabase) {
        return null;
      }

      try {
        const supabaseConfig = JSON.parse(savedSettings.supabase);
        if (supabaseConfig.apiUrl && supabaseConfig.anonKey) {
          return { apiUrl: supabaseConfig.apiUrl, anonKey: supabaseConfig.anonKey };
        }
      } catch {
        // 配置是加密的，跳过
      }

      return null;
    } catch (error) {
      console.error('[SupabaseClient] 获取用户配置失败:', error);
      return null;
    }
  }
}

export const supabaseClientManager = SupabaseClientManager.getInstance();

export const getSupabaseClient = (): Promise<SupabaseClient> => 
  supabaseClientManager.getClient();

export const initializeSupabaseClient = async (): Promise<SupabaseClient> => {
  const client = await getSupabaseClient();
  supabaseClientManager.setInitialized(true);
  return client;
};

export const setSupabaseClient = (apiUrl: string, anonKey: string): SupabaseClient => 
  supabaseClientManager.setClient(apiUrl, anonKey);

export const isSupabaseClientInitialized = (): boolean => 
  supabaseClientManager.getInitialized();
