import { SmokeLog, AppSettings, EncryptedApiSettings, DataStorageAdapter, Platform, SyncStatus, OperationLog } from '../types';
import { DEFAULT_SETTINGS, STORAGE_KEY_LOGS, STORAGE_KEY_SETTINGS } from '../constants';
import { Capacitor } from '@capacitor/core';
import { CapacitorSQLite, SQLiteConnection, SQLiteDBConnection, DBSQLiteValues, capSQLiteResult } from '@capacitor-community/sqlite';
import { safeSetItem } from '../utils/storageUtils';
import { Preferences } from '@capacitor/preferences';
import { apiService } from './apiService';

const STORAGE_KEY_API_SETTINGS = 'popsmoke_api_settings';

export const getPlatform = (): Platform => {
  if (Capacitor.isNativePlatform()) {
    if (Capacitor.getPlatform() === 'android') {
      return 'android';
    }
  }
  return 'web';
};

export const isWebPlatform = (): boolean => getPlatform() === 'web';
export const isAndroidPlatform = (): boolean => getPlatform() === 'android';

export const simpleEncrypt = (text: string, password: string): string => {
  let result = '';
  for (let i = 0; i < text.length; i++) {
    const charCode = text.charCodeAt(i) ^ password.charCodeAt(i % password.length);
    result += String.fromCharCode(charCode);
  }
  return btoa(result);
};

export const simpleDecrypt = (encodedText: string, password: string): string => {
  try {
    const text = atob(encodedText);
    let result = '';
    for (let i = 0; i < text.length; i++) {
      const charCode = text.charCodeAt(i) ^ password.charCodeAt(i % password.length);
      result += String.fromCharCode(charCode);
    }
    return result;
  } catch (e) {
    return '';
  }
};

class LocalStorageAdapter implements DataStorageAdapter {
  async getLogs(): Promise<SmokeLog[]> {
    const data = localStorage.getItem(STORAGE_KEY_LOGS);
    return data ? JSON.parse(data) : [];
  }

  async saveLogs(logs: SmokeLog[]): Promise<void> {
    const saveResult = safeSetItem(STORAGE_KEY_LOGS, JSON.stringify(logs));
    if (!saveResult.success) {
      throw new Error(saveResult.error || 'Failed to save logs');
    }
  }

  async getSettings(): Promise<AppSettings> {
    const data = localStorage.getItem(STORAGE_KEY_SETTINGS);
    return data ? JSON.parse(data) : DEFAULT_SETTINGS;
  }

  async saveSettings(settings: AppSettings): Promise<void> {
    localStorage.setItem(STORAGE_KEY_SETTINGS, JSON.stringify(settings));
  }

  async getApiSettings(): Promise<EncryptedApiSettings | null> {
    const data = localStorage.getItem(STORAGE_KEY_API_SETTINGS);
    return data ? JSON.parse(data) : null;
  }

  async saveApiSettings(settings: EncryptedApiSettings): Promise<void> {
    localStorage.setItem(STORAGE_KEY_API_SETTINGS, JSON.stringify(settings));
  }

  async deleteApiSettings(): Promise<void> {
    localStorage.removeItem(STORAGE_KEY_API_SETTINGS);
  }

  async clearAll(): Promise<void> {
    localStorage.removeItem(STORAGE_KEY_LOGS);
    localStorage.removeItem(STORAGE_KEY_SETTINGS);
    localStorage.removeItem(STORAGE_KEY_API_SETTINGS);
  }

  async clearLogsOnly(): Promise<void> {
    localStorage.removeItem(STORAGE_KEY_LOGS);
    console.log('Logs cleared from localStorage (settings and API settings preserved)');
  }
}

class IndexedDBAdapter implements DataStorageAdapter {
  private dbName = 'popsmoke_db';
  private dbVersion = 1;
  private db: IDBDatabase | null = null;
  private dbLogged = false;
  private openingPromise: Promise<IDBDatabase> | null = null;

  private async openDB(): Promise<IDBDatabase> {
    // 如果数据库已打开，直接返回
    if (this.db) {
      if (!this.dbLogged) {
        console.log('[IndexedDBAdapter.openDB] 使用现有数据库连接');
        this.dbLogged = true;
      }
      return this.db;
    }

    // 如果正在打开中，返回现有的 Promise
    if (this.openingPromise) {
      return this.openingPromise;
    }

    console.log('[IndexedDBAdapter.openDB] 打开数据库:', { dbName: this.dbName, version: this.dbVersion });

    this.openingPromise = new Promise((resolve, reject) => {

      const request = indexedDB.open(this.dbName, this.dbVersion);

      request.onerror = () => {
        console.error('[IndexedDBAdapter.openDB] 打开数据库失败');
        this.openingPromise = null;
        reject(new Error('Failed to open IndexedDB'));
      };

      request.onsuccess = () => {
        this.db = request.result;
        this.openingPromise = null;
        console.log('[IndexedDBAdapter.openDB] 数据库打开成功');
        resolve(this.db);
      };

      request.onupgradeneeded = (event) => {
        console.log('[IndexedDBAdapter.openDB] 数据库版本升级，创建对象存储');
        const db = (event.target as IDBOpenDBRequest).result;

        if (!db.objectStoreNames.contains('logs')) {
          console.log('[IndexedDBAdapter.openDB] 创建logs对象存储');
          const logsStore = db.createObjectStore('logs', { keyPath: 'id' });
          logsStore.createIndex('timestamp', 'timestamp');
          logsStore.createIndex('record_date', 'record_date');
        }

        if (!db.objectStoreNames.contains('settings')) {
          console.log('[IndexedDBAdapter.openDB] 创建settings对象存储');
          db.createObjectStore('settings', { keyPath: 'key' });
        }

        if (!db.objectStoreNames.contains('api_settings')) {
          console.log('[IndexedDBAdapter.openDB] 创建api_settings对象存储');
          db.createObjectStore('api_settings', { keyPath: 'key' });
        }
      };
    });
    
    return this.openingPromise;
  }

  async getLogs(): Promise<SmokeLog[]> {
    try {
      const db = await this.openDB();
      return new Promise((resolve, reject) => {
        const transaction = db.transaction('logs', 'readonly');
        const store = transaction.objectStore('logs');
        const index = store.index('timestamp');
        const request = index.openCursor(null, 'prev');
        
        const logs: SmokeLog[] = [];

        request.onsuccess = () => {
          const cursor = request.result;
          if (cursor) {
            logs.push(cursor.value);
            cursor.continue();
          } else {
            resolve(logs);
          }
        };

        request.onerror = () => {
          reject(new Error('Failed to get logs from IndexedDB'));
        };
      });
    } catch (error) {
      console.error('Failed to get logs from IndexedDB:', error);
      return [];
    }
  }

  async saveLogs(logs: SmokeLog[]): Promise<void> {
    try {
      const db = await this.openDB();
      return new Promise((resolve, reject) => {
        // 使用requestAnimationFrame确保操作在浏览器空闲时执行
        requestAnimationFrame(() => {
          try {
            const transaction = db.transaction('logs', 'readwrite');
            const store = transaction.objectStore('logs');

            let completed = 0;
            const total = logs.length;

            if (total === 0) {
              resolve();
              return;
            }

            console.log('[IndexedDBAdapter.saveLogs] 开始保存记录:', { totalLogs: total });

            logs.forEach(log => {
              const request = store.put(log);
              request.onsuccess = () => {
                completed++;
                if (completed === total) {
                  console.log('[IndexedDBAdapter.saveLogs] 所有记录保存成功');
                  resolve();
                }
              };
              request.onerror = () => {
                console.error('[IndexedDBAdapter.saveLogs] 保存记录失败:', log.id);
                reject(new Error(`Failed to save log: ${log.id}`));
              };
            });

            transaction.onerror = () => {
              console.error('[IndexedDBAdapter.saveLogs] 事务错误');
              reject(new Error('Transaction failed'));
            };
          } catch (error) {
            console.error('Failed to save logs to IndexedDB:', error);
            reject(error);
          }
        });
      });
    } catch (error) {
      console.error('Failed to save logs to IndexedDB:', error);
      throw error;
    }
  }

  async getSettings(): Promise<AppSettings> {
    try {
      const db = await this.openDB();
      return new Promise((resolve, reject) => {
        const transaction = db.transaction('settings', 'readonly');
        const store = transaction.objectStore('settings');
        const request = store.get('app_settings');

        request.onsuccess = () => {
          if (request.result) {
            resolve(JSON.parse(request.result.value));
          } else {
            resolve(DEFAULT_SETTINGS);
          }
        };

        request.onerror = () => {
          reject(new Error('Failed to get settings from IndexedDB'));
        };
      });
    } catch (error) {
      console.error('Failed to get settings from IndexedDB:', error);
      return DEFAULT_SETTINGS;
    }
  }

  async saveSettings(settings: AppSettings): Promise<void> {
    try {
      const db = await this.openDB();
      return new Promise((resolve, reject) => {
        const transaction = db.transaction('settings', 'readwrite');
        const store = transaction.objectStore('settings');
        const request = store.put({
          key: 'app_settings',
          value: JSON.stringify(settings)
        });

        request.onsuccess = () => {
          resolve();
        };

        request.onerror = () => {
          reject(new Error('Failed to save settings to IndexedDB'));
        };
      });
    } catch (error) {
      console.error('Failed to save settings to IndexedDB:', error);
      throw error;
    }
  }

  async getApiSettings(): Promise<EncryptedApiSettings | null> {
    try {
      const db = await this.openDB();
      return new Promise((resolve, reject) => {
        const transaction = db.transaction('api_settings', 'readonly');
        const store = transaction.objectStore('api_settings');
        const request = store.get('api_settings');

        request.onsuccess = () => {
          if (request.result) {
            resolve(JSON.parse(request.result.value));
          } else {
            resolve(null);
          }
        };

        request.onerror = () => {
          reject(new Error('Failed to get API settings from IndexedDB'));
        };
      });
    } catch (error) {
      console.error('Failed to get API settings from IndexedDB:', error);
      return null;
    }
  }

  async saveApiSettings(settings: EncryptedApiSettings): Promise<void> {
    try {
      const db = await this.openDB();
      return new Promise((resolve, reject) => {
        const transaction = db.transaction('api_settings', 'readwrite');
        const store = transaction.objectStore('api_settings');
        const request = store.put({
          key: 'api_settings',
          value: JSON.stringify(settings)
        });

        request.onsuccess = () => {
          resolve();
        };

        request.onerror = () => {
          reject(new Error('Failed to save API settings to IndexedDB'));
        };
      });
    } catch (error) {
      console.error('Failed to save API settings to IndexedDB:', error);
      throw error;
    }
  }

  async deleteApiSettings(): Promise<void> {
    try {
      const db = await this.openDB();
      return new Promise((resolve, reject) => {
        const transaction = db.transaction('api_settings', 'readwrite');
        const store = transaction.objectStore('api_settings');
        const request = store.delete('api_settings');

        request.onsuccess = () => {
          resolve();
        };

        request.onerror = () => {
          reject(new Error('Failed to delete API settings from IndexedDB'));
        };
      });
    } catch (error) {
      console.error('Failed to delete API settings from IndexedDB:', error);
      throw error;
    }
  }

  async clearAll(): Promise<void> {
    try {
      const db = await this.openDB();
      return new Promise((resolve, reject) => {
        const transaction = db.transaction(['logs', 'settings'], 'readwrite');
        const logsStore = transaction.objectStore('logs');
        const settingsStore = transaction.objectStore('settings');

        const logsClear = logsStore.clear();
        const settingsClear = settingsStore.clear();

        let completed = 0;
        const total = 2;

        const checkComplete = () => {
          completed++;
          if (completed === total) {
            resolve();
          }
        };

        logsClear.onsuccess = checkComplete;
        settingsClear.onsuccess = checkComplete;

        logsClear.onerror = () => reject(new Error('Failed to clear logs'));
        settingsClear.onerror = () => reject(new Error('Failed to clear settings'));
      });
    } catch (error) {
      console.error('Failed to clear all data from IndexedDB:', error);
      throw error;
    }
  }

  async clearLogsOnly(): Promise<void> {
    try {
      const db = await this.openDB();
      return new Promise((resolve, reject) => {
        const transaction = db.transaction('logs', 'readwrite');
        const store = transaction.objectStore('logs');
        const request = store.clear();

        request.onsuccess = () => {
          console.log('Logs cleared from IndexedDB (settings and API settings preserved)');
          resolve();
        };

        request.onerror = () => {
          reject(new Error('Failed to clear logs from IndexedDB'));
        };
      });
    } catch (error) {
      console.error('Failed to clear logs from IndexedDB:', error);
      throw error;
    }
  }
}

class SQLiteAdapter implements DataStorageAdapter {
  private sqlite!: SQLiteConnection;
  private db: SQLiteDBConnection | null = null;
  private isInitialized = false;
  private initPromise: Promise<void> | null = null;
  
  // 连接池管理
  private connectionPool: {
    maxRetries: number;
    retryDelay: number;
    lastError: Error | null;
    errorCount: number;
    lastRetryTime: number;
  } = {
    maxRetries: 3,
    retryDelay: 1000,
    lastError: null,
    errorCount: 0,
    lastRetryTime: 0
  };

  // 检查数据库连接是否有效
  private async isConnectionValid(): Promise<boolean> {
    if (!this.db || !this.isInitialized) {
      return false;
    }
    try {
      // 尝试执行一个简单的查询来验证连接
      await this.db.query("SELECT 1");
      return true;
    } catch (e) {
      return false;
    }
  }

  // 智能重试机制
  private async withRetry<T>(
    operation: () => Promise<T>,
    operationName: string,
    maxRetries: number = this.connectionPool.maxRetries
  ): Promise<T> {
    let lastError: Error | null = null;
    
    for (let attempt = 0; attempt < maxRetries; attempt++) {
      try {
        // 检查是否需要延迟重试
        const timeSinceLastRetry = Date.now() - this.connectionPool.lastRetryTime;
        if (attempt > 0 && timeSinceLastRetry < this.connectionPool.retryDelay) {
          await new Promise(resolve => 
            setTimeout(resolve, this.connectionPool.retryDelay - timeSinceLastRetry)
          );
        }
        
        const result = await operation();
        
        // 成功时重置错误计数
        this.connectionPool.errorCount = 0;
        this.connectionPool.lastError = null;
        
        return result;
      } catch (error) {
        lastError = error as Error;
        this.connectionPool.lastError = lastError;
        this.connectionPool.errorCount++;
        this.connectionPool.lastRetryTime = Date.now();
        
        console.warn(`[SQLiteAdapter] ${operationName} 失败 (尝试 ${attempt + 1}/${maxRetries}):`, error);
        
        // 如果是连接问题，尝试重新连接
        if (this.isConnectionError(error)) {
          console.log('[SQLiteAdapter] 检测到连接错误，尝试重新连接...');
          await this.resetConnection();
        }
        
        // 最后一次尝试失败，抛出错误
        if (attempt === maxRetries - 1) {
          throw lastError;
        }
        
        // 指数退避延迟
        const delay = this.connectionPool.retryDelay * Math.pow(2, attempt);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
    
    throw lastError || new Error(`${operationName} 失败`);
  }

  // 检查是否是连接相关错误
  private isConnectionError(error: unknown): boolean {
    if (!(error instanceof Error)) return false;
    const errorMessage = error.message.toLowerCase();
    return (
      errorMessage.includes('connection') ||
      errorMessage.includes('no available connection') ||
      errorMessage.includes('already exists') ||
      errorMessage.includes('closed') ||
      errorMessage.includes('database is locked')
    );
  }

  // 重置连接
  private async resetConnection(): Promise<void> {
    console.log('[SQLiteAdapter] 重置数据库连接...');
    
    // 关闭现有连接
    if (this.db) {
      try {
        await this.db.close();
      } catch (e) {
        // 忽略关闭错误
      }
    }
    
    // 重置状态
    this.isInitialized = false;
    this.db = null;
    this.initPromise = null;
    
    // 短暂延迟后重新初始化
    await new Promise(resolve => setTimeout(resolve, 500));
  }

  // 确保数据库连接有效，如果无效则重新初始化
  private async ensureConnection(): Promise<void> {
    const isValid = await this.isConnectionValid();
    if (!isValid) {
      await this.resetConnection();
      await this.initDB();
    }
  }

  private async initDB(): Promise<void> {
    // 如果正在初始化中，等待初始化完成
    if (this.initPromise) {
      return this.initPromise;
    }

    // 如果已经初始化且连接有效，直接返回
    if (this.isInitialized && this.db) {
      const isValid = await this.isConnectionValid();
      if (isValid) {
        return;
      }
      // 连接无效，需要重新初始化
      await this.resetConnection();
    }

    // 创建新的初始化 Promise
    this.initPromise = this.doInitDB();
    
    try {
      await this.initPromise;
    } finally {
      this.initPromise = null;
    }
  }

  private async doInitDB(): Promise<void> {
    if (this.isInitialized && this.db) {
      return;
    }

    try {
      // 确保 sqlite 实例已创建
      if (!this.sqlite) {
        this.sqlite = new SQLiteConnection(CapacitorSQLite);
      }

      // 检查连接一致性
      let ret: capSQLiteResult;
      try {
        ret = await this.sqlite.checkConnectionsConsistency();
      } catch (e) {
        // 如果检查失败，假设没有连接
        ret = { result: false };
      }

      let isConn = false;
      try {
        const connResult = await this.sqlite.isConnection("popsmoke_db", false);
        isConn = connResult.result || false;
      } catch (e) {
        // 如果检查失败，假设没有连接
        isConn = false;
      }

      // 如果连接已存在，先尝试关闭并清理
      if (isConn) {
        try {
          const existingConn = await this.sqlite.retrieveConnection("popsmoke_db", false);
          await existingConn.close();
        } catch (e) {
          // 忽略关闭错误
        }
      }

      // 创建新连接
      try {
        this.db = await this.sqlite.createConnection(
          "popsmoke_db",
          false,
          "no-encryption",
          1,
          false
        );
      } catch (createError: any) {
        // 如果创建失败因为连接已存在，尝试检索现有连接
        if (createError?.message?.includes('already exists')) {
          try {
            this.db = await this.sqlite.retrieveConnection("popsmoke_db", false);
          } catch (retrieveError) {
            console.error('Failed to retrieve existing connection:', retrieveError);
            throw createError;
          }
        } else {
          throw createError;
        }
      }

      if (!this.db) {
        throw new Error('Failed to create or retrieve database connection');
      }

      await this.db.open();
      await this.createTables();
      this.isInitialized = true;
    } catch (error) {
      console.error('Failed to initialize SQLite database:', error);
      // 重置状态以便下次重试
      this.isInitialized = false;
      this.db = null;
      throw error;
    }
  }

  private async createTables(): Promise<void> {
    if (!this.db) return;

    const createLogsTable = `
      CREATE TABLE IF NOT EXISTS logs (
        id TEXT PRIMARY KEY,
        user_id TEXT,
        table_id TEXT,
        table_name TEXT,
        record_date TEXT,
        record_time TEXT,
        record_index INTEGER,
        timestamp INTEGER,
        created_at TEXT
      );
    `;

    const createSettingsTable = `
      CREATE TABLE IF NOT EXISTS settings (
        key TEXT PRIMARY KEY,
        value TEXT
      );
    `;

    const createApiSettingsTable = `
      CREATE TABLE IF NOT EXISTS api_settings (
        key TEXT PRIMARY KEY,
        value TEXT
      );
    `;

    const createRuntimeConfigTable = `
      CREATE TABLE IF NOT EXISTS runtime_config (
        key TEXT PRIMARY KEY,
        value TEXT
      );
    `;

    await this.db.execute(createLogsTable);
    await this.db.execute(createSettingsTable);
    await this.db.execute(createApiSettingsTable);
    await this.db.execute(createRuntimeConfigTable);
  }

  async getLogs(): Promise<SmokeLog[]> {
    return this.withRetry(async () => {
      await this.ensureConnection();
      if (!this.db) return [];
      const result: DBSQLiteValues = await this.db.query("SELECT * FROM logs ORDER BY timestamp DESC");
      return result.values || [];
    }, 'getLogs');
  }

  async saveLogs(logs: SmokeLog[]): Promise<void> {
    // 使用setTimeout确保操作在后台执行，不阻塞UI线程
    return new Promise((resolve, reject) => {
      setTimeout(async () => {
        try {
          await this.withRetry(async () => {
            await this.ensureConnection();
            if (!this.db) throw new Error('Database not initialized');

            // 避免手动 BEGIN/COMMIT 在插件内部事务下触发“no current transaction”异常
            await this.db.execute("DELETE FROM logs");

            if (logs.length > 0) {
              // 分批插入，每批500条
              const batchSize = 500;
              for (let i = 0; i < logs.length; i += batchSize) {
                const batch = logs.slice(i, i + batchSize);

                const values = batch.map(log => [
                  log.id,
                  log.user_id,
                  log.table_id ?? null,
                  log.table_name ?? null,
                  log.record_date || null,
                  log.record_time || null,
                  log.record_index || 0,
                  log.timestamp,
                  log.created_at || null
                ]);

                const placeholders = values.map(() => '(?, ?, ?, ?, ?, ?, ?, ?, ?)').join(', ');
                const flatValues = values.flat();

                const insertQuery = `
                  INSERT INTO logs (id, user_id, table_id, table_name, record_date, record_time, record_index, timestamp, created_at)
                  VALUES ${placeholders}
                `;

                await this.db.run(insertQuery, flatValues);
              }
            }
          }, 'saveLogs');
          resolve();
        } catch (error) {
          console.error('Failed to save logs to SQLite:', error);
          reject(error);
        }
      }, 0);
    });
  }

  async getSettings(): Promise<AppSettings> {
    try {
      await this.ensureConnection();
      if (!this.db) return DEFAULT_SETTINGS;
      const result: DBSQLiteValues = await this.db.query("SELECT value FROM settings WHERE key = 'app_settings'");
      
      if (result.values && result.values.length > 0) {
        return JSON.parse(result.values[0].value);
      }
      
      return DEFAULT_SETTINGS;
    } catch (error) {
      console.error('Failed to get settings from SQLite:', error);
      return DEFAULT_SETTINGS;
    }
  }

  async saveSettings(settings: AppSettings): Promise<void> {
    try {
      await this.ensureConnection();
      if (!this.db) throw new Error('Database not initialized');
      const settingsJson = JSON.stringify(settings);
      
      await this.db.run(
        "INSERT OR REPLACE INTO settings (key, value) VALUES ('app_settings', ?)",
        [settingsJson]
      );
    } catch (error) {
      console.error('Failed to save settings to SQLite:', error);
      throw error;
    }
  }

  async getApiSettings(): Promise<EncryptedApiSettings | null> {
    try {
      await this.ensureConnection();
      if (!this.db) return null;
      const result: DBSQLiteValues = await this.db.query("SELECT value FROM api_settings WHERE key = 'api_settings'");
      
      if (result.values && result.values.length > 0) {
        const value = result.values[0].value || result.values[0];
        return typeof value === 'string' ? JSON.parse(value) : value;
      }
      
      return null;
    } catch (error) {
      console.error('Failed to get API settings from SQLite:', error);
      return null;
    }
  }

  async saveApiSettings(settings: EncryptedApiSettings): Promise<void> {
    try {
      await this.ensureConnection();
      if (!this.db) throw new Error('Database not initialized');
      const settingsJson = JSON.stringify(settings);
      
      console.log('Saving API settings to SQLite:', settingsJson);
      
      const result = await this.db.run(
        "INSERT OR REPLACE INTO api_settings (key, value) VALUES ('api_settings', ?)",
        [settingsJson]
      );
      
      console.log('Save API settings result:', result);
    } catch (error) {
      console.error('Failed to save API settings to SQLite:', error);
      throw error;
    }
  }

  async deleteApiSettings(): Promise<void> {
    try {
      await this.ensureConnection();
      if (!this.db) throw new Error('Database not initialized');
      await this.db.execute("DELETE FROM api_settings");
    } catch (error) {
      console.error('Failed to delete API settings from SQLite:', error);
      throw error;
    }
  }

  async clearAll(): Promise<void> {
    try {
      await this.ensureConnection();
      if (!this.db) throw new Error('Database not initialized');
      await this.db.execute("DELETE FROM logs");
      await this.db.execute("DELETE FROM settings");
      console.log('Cleared logs and settings from SQLite (API settings preserved)');
    } catch (error) {
      console.error('Failed to clear all data from SQLite:', error);
      throw error;
    }
  }

  async clearLogsOnly(): Promise<void> {
    try {
      await this.ensureConnection();
      if (!this.db) throw new Error('Database not initialized');
      await this.db.execute("DELETE FROM logs");
      console.log('Cleared logs from SQLite (settings and API settings preserved)');
    } catch (error) {
      console.error('Failed to clear logs from SQLite:', error);
      throw error;
    }
  }

  async getRuntimeConfig(key: string): Promise<string | null> {
    try {
      await this.ensureConnection();
      if (!this.db) return null;
      const result: DBSQLiteValues = await this.db.query("SELECT value FROM runtime_config WHERE key = ?", [key]);
      
      if (result.values && result.values.length > 0) {
        const value = result.values[0].value || result.values[0];
        return typeof value === 'string' ? value : null;
      }
      
      return null;
    } catch (error) {
      console.error('Failed to get runtime config from SQLite:', error);
      return null;
    }
  }

  async saveRuntimeConfig(key: string, value: string): Promise<void> {
    try {
      await this.ensureConnection();
      if (!this.db) throw new Error('Database not initialized');
      
      await this.db.run(
        "INSERT OR REPLACE INTO runtime_config (key, value) VALUES (?, ?)",
        [key, value]
      );
    } catch (error) {
      console.error('Failed to save runtime config to SQLite:', error);
      throw error;
    }
  }

  async deleteRuntimeConfig(key: string): Promise<void> {
    try {
      await this.ensureConnection();
      if (!this.db) throw new Error('Database not initialized');
      await this.db.run("DELETE FROM runtime_config WHERE key = ?", [key]);
    } catch (error) {
      console.error('Failed to delete runtime config from SQLite:', error);
      throw error;
    }
  }

  // 关闭数据库连接（用于应用清理）
  async closeConnection(): Promise<void> {
    console.log('[SQLiteAdapter] 关闭数据库连接...');
    
    try {
      if (this.db) {
        await this.db.close();
        console.log('[SQLiteAdapter] 数据库连接已关闭');
      }
    } catch (error) {
      console.warn('[SQLiteAdapter] 关闭数据库连接时出错:', error);
    } finally {
      // 重置所有状态
      this.isInitialized = false;
      this.db = null;
      this.initPromise = null;
      this.connectionPool = {
        maxRetries: 3,
        retryDelay: 1000,
        lastError: null,
        errorCount: 0,
        lastRetryTime: 0
      };
    }
  }

  async getAuthItem(key: string): Promise<string | null> {
    try {
      return await this.getRuntimeConfig(key);
    } catch (error) {
      console.error(`[SQLiteAdapter.getAuthItem] Failed to get auth item for key "${key}":`, error);
      return null;
    }
  }

  async setAuthItem(key: string, value: string): Promise<void> {
    try {
      await this.saveRuntimeConfig(key, value);
    } catch (error) {
      console.error(`[SQLiteAdapter.setAuthItem] Failed to set auth item for key "${key}":`, error);
      throw error;
    }
  }

  async removeAuthItem(key: string): Promise<void> {
    try {
      await this.deleteRuntimeConfig(key);
    } catch (error) {
      console.error(`[SQLiteAdapter.removeAuthItem] Failed to remove auth item for key "${key}":`, error);
      throw error;
    }
  }

  async getSystemLogs(): Promise<string | null> {
    try {
      return await this.getRuntimeConfig('system_logs');
    } catch (error) {
      console.error('[SQLiteAdapter.getSystemLogs] Failed to get system logs:', error);
      return null;
    }
  }

  async saveSystemLogs(value: string): Promise<void> {
    try {
      await this.saveRuntimeConfig('system_logs', value);
    } catch (error) {
      console.error('[SQLiteAdapter.saveSystemLogs] Failed to save system logs:', error);
      throw error;
    }
  }

  async getNativeRuntimeLogs(): Promise<string | null> {
    try {
      return await this.getRuntimeConfig('popsmoke_runtime_logs');
    } catch (error) {
      console.error('[SQLiteAdapter.getNativeRuntimeLogs] Failed to get native runtime logs:', error);
      return null;
    }
  }

  async saveNativeRuntimeLogs(value: string): Promise<void> {
    try {
      await this.saveRuntimeConfig('popsmoke_runtime_logs', value);
    } catch (error) {
      console.error('[SQLiteAdapter.saveNativeRuntimeLogs] Failed to save native runtime logs:', error);
      throw error;
    }
  }
}

let storageAdapter: DataStorageAdapter | null = null;
let migrationChecked = false;

// 缓存IndexedDB可用性检查结果
let indexedDBAvailableCache: boolean | null = null;

const isIndexedDBAvailable = (): boolean => {
  // 如果已经检查过，直接返回缓存结果
  if (indexedDBAvailableCache !== null) {
    return indexedDBAvailableCache;
  }
  
  try {
    const available = typeof window !== 'undefined' && 'indexedDB' in window && window.indexedDB !== null;
    // 只在首次检查时输出日志
    console.log('[isIndexedDBAvailable] IndexedDB可用性检查:', {
      windowDefined: typeof window !== 'undefined',
      indexedDBInWindow: typeof window !== 'undefined' && 'indexedDB' in window,
      indexedDBNotNull: typeof window !== 'undefined' && 'indexedDB' in window && window.indexedDB !== null,
      available
    });
    indexedDBAvailableCache = available;
    return available;
  } catch (error) {
    console.warn('[isIndexedDBAvailable] Error checking IndexedDB availability:', error);
    indexedDBAvailableCache = false;
    return false;
  }
};

// 缓存迁移日志状态
let migrationLogged = false;

// 从localStorage迁移数据到IndexedDB
const migrateFromLocalStorage = async (indexedDBAdapter: IndexedDBAdapter): Promise<void> => {
  try {
    if (!migrationLogged) {
      console.log('[migrateFromLocalStorage] 检查是否需要从localStorage迁移数据');
    }
    
    // 迁移日志数据
    const localStorageData = localStorage.getItem(STORAGE_KEY_LOGS);
    if (localStorageData) {
      const oldLogs: SmokeLog[] = JSON.parse(localStorageData);
      if (oldLogs.length > 0) {
        if (!migrationLogged) {
          console.log('[migrateFromLocalStorage] 发现localStorage日志数据:', { count: oldLogs.length });
        }
        
        // 从IndexedDB获取现有数据
        const existingLogs = await indexedDBAdapter.getLogs();
        
        // 合并数据，避免重复
        const existingIds = new Set(existingLogs.map(log => log.id));
        const uniqueOldLogs = oldLogs.filter(log => !existingIds.has(log.id));
        
        if (uniqueOldLogs.length > 0) {
          console.log('[migrateFromLocalStorage] 需要迁移的日志数据:', { count: uniqueOldLogs.length });
          
          // 合并并保存数据
          const mergedLogs = [...uniqueOldLogs, ...existingLogs]
            .sort((a, b) => b.timestamp - a.timestamp);
          
          await indexedDBAdapter.saveLogs(mergedLogs);
          console.log('[migrateFromLocalStorage] 日志数据迁移完成:', { totalCount: mergedLogs.length });
        }
        
        // 清理localStorage中的日志数据
        localStorage.removeItem(STORAGE_KEY_LOGS);
        if (!migrationLogged) {
          console.log('[migrateFromLocalStorage] 已清理localStorage中的旧日志数据');
        }
      }
    }
    
    // 迁移设置数据
    const localSettingsData = localStorage.getItem(STORAGE_KEY_SETTINGS);
    if (localSettingsData) {
      const oldSettings: AppSettings = JSON.parse(localSettingsData);
      const existingSettings = await indexedDBAdapter.getSettings();
      
      // 只有当IndexedDB中没有设置或localStorage中的设置更新时，才迁移
      if (existingSettings.user_id === 'local' && oldSettings.user_id !== 'local') {
        await indexedDBAdapter.saveSettings(oldSettings);
        localStorage.removeItem(STORAGE_KEY_SETTINGS);
        if (!migrationLogged) {
          console.log('[migrateFromLocalStorage] 设置数据迁移完成');
        }
      }
    }
    
    // 迁移API设置数据
    const localApiSettingsData = localStorage.getItem(STORAGE_KEY_API_SETTINGS);
    if (localApiSettingsData) {
      const oldApiSettings: EncryptedApiSettings = JSON.parse(localApiSettingsData);
      const existingApiSettings = await indexedDBAdapter.getApiSettings();
      
      if (!existingApiSettings && oldApiSettings) {
        await indexedDBAdapter.saveApiSettings(oldApiSettings);
        localStorage.removeItem(STORAGE_KEY_API_SETTINGS);
        if (!migrationLogged) {
          console.log('[migrateFromLocalStorage] API设置数据迁移完成');
        }
      }
    }
    
    if (!migrationLogged) {
      console.log('[migrateFromLocalStorage] 数据迁移检查完成');
      migrationLogged = true;
    }
    
  } catch (error) {
    console.error('[migrateFromLocalStorage] 数据迁移失败:', error);
    // 迁移失败不阻止应用继续运行
  }
};

// 缓存适配器选择日志状态
let adapterLogged = false;

export const getStorageAdapter = (): DataStorageAdapter => {
  // 只在首次选择适配器时输出详细日志
  if (!adapterLogged) {
    console.log('[getStorageAdapter] 获取存储适配器:', {
      hasAdapter: !!storageAdapter,
      adapterType: storageAdapter?.constructor.name,
      isAndroid: isAndroidPlatform(),
      isIndexedDBAvailable: isIndexedDBAvailable()
    });
  }

  // 如果已经选择了存储适配器，确保web端使用IndexedDB
  if (storageAdapter) {
    // 对于web端，如果当前不是IndexedDBAdapter且IndexedDB可用，切换到IndexedDBAdapter
    if (!isAndroidPlatform() && !(storageAdapter instanceof IndexedDBAdapter) && isIndexedDBAvailable()) {
      console.log('[getStorageAdapter] 切换到IndexedDB存储');
      storageAdapter = new IndexedDBAdapter();
      // 执行数据迁移
      if (!migrationChecked) {
        migrationChecked = true;
        setTimeout(() => {
          migrateFromLocalStorage(storageAdapter as IndexedDBAdapter);
        }, 100);
      }
    }
    return storageAdapter;
  }

  // 首次选择存储适配器
  if (isAndroidPlatform()) {
    console.log('[getStorageAdapter] 选择SQLite存储适配器 (Android)');
    storageAdapter = new SQLiteAdapter();
  } else {
    // Web端优先使用IndexedDB，即使初始化失败也尝试使用
    try {
      if (isIndexedDBAvailable()) {
        console.log('[getStorageAdapter] 选择IndexedDB存储');
        storageAdapter = new IndexedDBAdapter();
        // 在首次创建IndexedDB适配器时执行数据迁移
        if (!migrationChecked) {
          migrationChecked = true;
          // 延迟执行迁移，确保IndexedDB已初始化
          setTimeout(() => {
            migrateFromLocalStorage(storageAdapter as IndexedDBAdapter);
          }, 100);
        }
      } else {
        console.warn('[getStorageAdapter] IndexedDB不可用，使用localStorage作为回退方案');
        storageAdapter = new LocalStorageAdapter();
      }
    } catch (error) {
      console.error('[getStorageAdapter] IndexedDB初始化失败，使用localStorage作为回退方案:', error);
      storageAdapter = new LocalStorageAdapter();
    }
  }
  
  if (!adapterLogged) {
    console.log('[getStorageAdapter] 最终选择的存储适配器:', storageAdapter.constructor.name);
    adapterLogged = true;
  }
  
  return storageAdapter;
};

export const getStorageType = (): string => {
  if (isAndroidPlatform()) {
    return 'SQLite';
  } else if (isIndexedDBAvailable()) {
    return 'IndexedDB';
  } else {
    return 'localStorage';
  }
};

const STORAGE_KEY_HAS_LOGGED_IN = 'popsmoke_has_logged_in';

const authMemoryStorage: Map<string, string> = new Map();

export const getAuthStorageAdapter = (): DataStorageAdapter & {
  getAuthItem: (key: string) => Promise<string | null>;
  setAuthItem: (key: string, value: string) => Promise<void>;
  removeAuthItem: (key: string) => Promise<void>;
  hasLoggedIn: () => Promise<boolean>;
  setLoggedIn: (value: boolean) => Promise<void>;
} => {
  const adapter = getStorageAdapter();

  return {
    ...adapter,
    async getAuthItem(key: string): Promise<string | null> {
      try {
        if (isAndroidPlatform()) {
          // 安卓端使用SQLite存储
          if (!('getAuthItem' in adapter) || typeof adapter.getAuthItem !== 'function') {
            throw new Error('SQLiteAdapter does not implement getAuthItem');
          }
          return await adapter.getAuthItem(key);
        } else if (Capacitor.isNativePlatform()) {
          // 其他原生平台使用Preferences
          const { value } = await Preferences.get({ key });
          return value;
        } else {
          // Web端使用localStorage
          return localStorage.getItem(key) || authMemoryStorage.get(key) || null;
        }
      } catch (error) {
        console.error('Failed to get auth item:', error);
        return authMemoryStorage.get(key) || null;
      }
    },

    async setAuthItem(key: string, value: string): Promise<void> {
      try {
        if (isAndroidPlatform()) {
          // 安卓端使用SQLite存储
          if (!('setAuthItem' in adapter) || typeof adapter.setAuthItem !== 'function') {
            throw new Error('SQLiteAdapter does not implement setAuthItem');
          }
          await adapter.setAuthItem(key, value);
        } else if (Capacitor.isNativePlatform()) {
          // 其他原生平台使用Preferences
          await Preferences.set({ key, value });
        } else {
          // Web端使用localStorage
          localStorage.setItem(key, value);
        }
      } catch (error) {
        console.error('Failed to set auth item:', error);
        authMemoryStorage.set(key, value);
      }
    },

    async removeAuthItem(key: string): Promise<void> {
      try {
        if (isAndroidPlatform()) {
          // 安卓端使用SQLite存储
          if (!('removeAuthItem' in adapter) || typeof adapter.removeAuthItem !== 'function') {
            throw new Error('SQLiteAdapter does not implement removeAuthItem');
          }
          await adapter.removeAuthItem(key);
        } else if (Capacitor.isNativePlatform()) {
          // 其他原生平台使用Preferences
          await Preferences.remove({ key });
        } else {
          // Web端使用localStorage
          localStorage.removeItem(key);
        }
      } catch (error) {
        console.error('Failed to remove auth item:', error);
        authMemoryStorage.delete(key);
      }
    },

    async hasLoggedIn(): Promise<boolean> {
      // 统一使用SQLite存储登录状态，确保与API设置同步
      try {
        const apiSettings = await adapter.getApiSettings();
        // 如果存在API设置且包含supabase配置，认为已登录
        if (apiSettings && apiSettings.supabase) {
          return true;
        }
        // 回退到Preferences检查（兼容旧数据）
        const value = await this.getAuthItem(STORAGE_KEY_HAS_LOGGED_IN);
        return value === 'true';
      } catch (error) {
        console.error('[hasLoggedIn] 检查登录状态失败:', error);
        // 出错时回退到Preferences
        const value = await this.getAuthItem(STORAGE_KEY_HAS_LOGGED_IN);
        return value === 'true';
      }
    },

    async setLoggedIn(value: boolean): Promise<void> {
      // 登录状态现在由API设置的存在性决定，不再单独存储
      // 但为了兼容性，仍然更新Preferences
      if (value) {
        await this.setAuthItem(STORAGE_KEY_HAS_LOGGED_IN, 'true');
      } else {
        await this.removeAuthItem(STORAGE_KEY_HAS_LOGGED_IN);
      }
    }
  };
};

class SyncQueueManager {
  private static instance: SyncQueueManager;
  private syncQueue: OperationLog[] = [];
  private listeners: ((status: SyncStatus) => void)[] = [];
  private isProcessing: boolean = false;

  private constructor() {}

  public static getInstance(): SyncQueueManager {
    if (!SyncQueueManager.instance) {
      SyncQueueManager.instance = new SyncQueueManager();
    }
    return SyncQueueManager.instance;
  }

  public addOperation(operation: OperationLog): void {
    this.syncQueue.push(operation);

    if (operation.type !== 'clear' && operation.type !== 'sync') {
      this.notifyListeners({
        type: operation.type,
        status: 'pending',
        message: this.getOperationMessage(operation),
        timestamp: Date.now()
      });
    }

    this.processSyncQueue();
  }

  private async processSyncQueue(): Promise<void> {
    if (this.isProcessing || this.syncQueue.length === 0) return;

    this.isProcessing = true;

    // 使用setTimeout让UI线程有时间更新
    setTimeout(async () => {
      try {
        const operation = this.syncQueue.shift();
        if (!operation) {
          this.isProcessing = false;
          return;
        }

        const logs = await this.getLogs();

        try {
          await this.syncOperation(operation, logs);

          if (operation.type !== 'clear' && operation.type !== 'sync') {
            this.notifyListeners({
              type: operation.type,
              status: 'success',
              message: this.getOperationMessage(operation),
              timestamp: Date.now()
            });
          }
        } catch (error) {
          if (operation.type !== 'clear' && operation.type !== 'sync') {
            this.notifyListeners({
              type: operation.type,
              status: 'error',
              message: `同步失败: ${error instanceof Error ? error.message : '未知错误'}`,
              timestamp: Date.now()
            });
          }
        }
      } finally {
        this.isProcessing = false;

        if (this.syncQueue.length > 0) {
          setTimeout(() => this.processSyncQueue(), 0);
        }
      }
    }, 0);
  }

  private async syncOperation(operation: OperationLog, logs: SmokeLog[]): Promise<void> {
    if (operation.data.user_id === 'local') {
      console.log('Local mode, skipping sync to Supabase');
      return;
    }

    switch (operation.type) {
      case 'create':
        await apiService.saveLog(operation.data);
        break;
      case 'update':
        await apiService.updateLog(operation.data, logs);
        break;
      case 'delete':
        await apiService.deleteLog(operation.data.id, operation.data.user_id, logs);
        break;
    }
  }

  public onSyncStatusChange(callback: (status: SyncStatus) => void): () => void {
    this.listeners.push(callback);
    return () => {
      this.listeners = this.listeners.filter(cb => cb !== callback);
    };
  }

  private notifyListeners(status: SyncStatus): void {
    this.listeners.forEach(callback => {
      try {
        callback(status);
      } catch (error) {
        console.error('Sync status listener error:', error);
      }
    });
  }

  private getOperationMessage(operation: OperationLog): string {
    switch (operation.type) {
      case 'create':
        return '创建记录';
      case 'update':
        return '更新记录';
      case 'delete':
        return '删除记录';
      case 'clear':
        return '清除数据';
      case 'sync':
        return '同步数据';
      default:
        return '未知操作';
    }
  }

  public getQueueLength(): number {
    return this.syncQueue.length;
  }

  public clearQueue(): void {
    this.syncQueue = [];
  }

  public async getLogs(): Promise<SmokeLog[]> {
    const adapter = getStorageAdapter();
    return await adapter.getLogs();
  }
}

export const getSyncQueueManager = (): SyncQueueManager => {
  return SyncQueueManager.getInstance();
};

export const hasLoggedInBefore = async (): Promise<boolean> => {
  const authStorage = getAuthStorageAdapter();
  return await authStorage.hasLoggedIn();
};

export const setLoggedInFlag = async (value: boolean): Promise<void> => {
  const authStorage = getAuthStorageAdapter();
  await authStorage.setLoggedIn(value);
};

export interface SupabaseRuntimeConfig {
  apiUrl: string;
  anonKey: string;
}

const RUNTIME_CONFIG_KEY = 'popsmoke_supabase_runtime_config';

export const getSupabaseRuntimeConfig = async (): Promise<SupabaseRuntimeConfig | null> => {
  const authStorage = getAuthStorageAdapter();
  
  try {
    // 安卓端：优先从 Preferences 读取
    if (isAndroidPlatform()) {
      const value = await authStorage.getAuthItem(RUNTIME_CONFIG_KEY);
      if (value) {
        const parsed = JSON.parse(value);
        if (parsed?.apiUrl && parsed?.anonKey) {
          return {
            apiUrl: parsed.apiUrl,
            anonKey: parsed.anonKey,
          };
        }
      }
      
      // Preferences 中没有，从 SQLite 读取
      const adapter = getStorageAdapter();
      if (adapter instanceof SQLiteAdapter) {
        const sqliteValue = await adapter.getRuntimeConfig(RUNTIME_CONFIG_KEY);
        if (sqliteValue) {
          const parsed = JSON.parse(sqliteValue);
          if (parsed?.apiUrl && parsed?.anonKey) {
            // 同步回 Preferences
            await authStorage.setAuthItem(RUNTIME_CONFIG_KEY, sqliteValue);
            return {
              apiUrl: parsed.apiUrl,
              anonKey: parsed.anonKey,
            };
          }
        }
      }
      
      return null;
    }
    
    // 非安卓端：直接从 Preferences/localStorage 读取
    const value = await authStorage.getAuthItem(RUNTIME_CONFIG_KEY);
    if (!value) return null;

    const parsed = JSON.parse(value);
    if (parsed?.apiUrl && parsed?.anonKey) {
      return {
        apiUrl: parsed.apiUrl,
        anonKey: parsed.anonKey,
      };
    }
  } catch (error) {
    console.error('Failed to read Supabase runtime config:', error);
  }

  return null;
};

export const setSupabaseRuntimeConfig = async (config: SupabaseRuntimeConfig): Promise<void> => {
  const authStorage = getAuthStorageAdapter();
  const configJson = JSON.stringify(config);
  
  // 保存到 Preferences
  await authStorage.setAuthItem(RUNTIME_CONFIG_KEY, configJson);
  
  // 安卓端：同时保存到 SQLite
  if (isAndroidPlatform()) {
    try {
      const adapter = getStorageAdapter();
      if (adapter instanceof SQLiteAdapter) {
        await adapter.saveRuntimeConfig(RUNTIME_CONFIG_KEY, configJson);
      }
    } catch (error) {
      console.error('Failed to save runtime config to SQLite:', error);
    }
  }
};

export const clearSupabaseRuntimeConfig = async (): Promise<void> => {
  const authStorage = getAuthStorageAdapter();
  
  // 清除 Preferences
  await authStorage.removeAuthItem(RUNTIME_CONFIG_KEY);
  
  // 安卓端：同时清除 SQLite
  if (isAndroidPlatform()) {
    try {
      const adapter = getStorageAdapter();
      if (adapter instanceof SQLiteAdapter) {
        await adapter.deleteRuntimeConfig(RUNTIME_CONFIG_KEY);
      }
    } catch (error) {
      console.error('Failed to delete runtime config from SQLite:', error);
    }
  }
};

export const createSupabaseAuthStorage = () => {
  const authStorage = getAuthStorageAdapter();
  
  return {
    getItem: async (key: string) => {
      return await authStorage.getAuthItem(key);
    },
    setItem: async (key: string, value: string) => {
      await authStorage.setAuthItem(key, value);
    },
    removeItem: async (key: string) => {
      await authStorage.removeAuthItem(key);
    },
  };
};

interface AvatarCacheData {
  data?: string;
  url?: string;
  timestamp: number;
}

interface AvatarCache {
  [userId: string]: AvatarCacheData;
}

const AVATAR_CACHE_EXPIRY = 7 * 24 * 60 * 60 * 1000;

const parseAvatarCache = (apiSettings: EncryptedApiSettings | null): AvatarCache => {
  if (!apiSettings || !apiSettings.avatarCache) {
    return {};
  }
  try {
    return typeof apiSettings.avatarCache === 'string'
      ? JSON.parse(apiSettings.avatarCache)
      : apiSettings.avatarCache;
  } catch {
    return {};
  }
};

const saveAvatarCache = async (
  apiSettings: EncryptedApiSettings | null,
  avatarCache: AvatarCache
): Promise<void> => {
  const adapter = getStorageAdapter();
  const newSettings: EncryptedApiSettings = {
    ...apiSettings,
    avatarCache: JSON.stringify(avatarCache)
  };
  await adapter.saveApiSettings(newSettings);
};

const getAvatarFallbackItem = async (key: string): Promise<string | null> => {
  try {
    if (Capacitor.isNativePlatform()) {
      const { value } = await Preferences.get({ key });
      return value;
    } else {
      return localStorage.getItem(key);
    }
  } catch (error) {
    return null;
  }
};

const setAvatarFallbackItem = async (key: string, value: string): Promise<void> => {
  try {
    if (Capacitor.isNativePlatform()) {
      await Preferences.set({ key, value });
    } else {
      localStorage.setItem(key, value);
    }
  } catch (error) {
    console.warn('Failed to set fallback item:', error);
  }
};

const removeAvatarFallbackItem = async (key: string): Promise<void> => {
  try {
    if (Capacitor.isNativePlatform()) {
      await Preferences.remove({ key });
    } else {
      localStorage.removeItem(key);
    }
  } catch (error) {
    console.warn('Failed to remove fallback item:', error);
  }
};

const getAvatarCacheKey = (userId: string): string => `avatar_cache_${userId}`;

export const avatarCacheService = {
  async cacheAvatar(userId: string, avatarUrl: string): Promise<void> {
    try {
      const response = await fetch(avatarUrl);
      const blob = await response.blob();
      const reader = new FileReader();
      reader.readAsDataURL(blob);

      return new Promise((resolve, reject) => {
        reader.onload = async () => {
          const base64data = reader.result as string;
          const cacheData: AvatarCacheData = { data: base64data, timestamp: Date.now() };

          try {
            const adapter = getStorageAdapter();
            const apiSettings = await adapter.getApiSettings();
            const avatarCache = parseAvatarCache(apiSettings);
            avatarCache[userId] = cacheData;
            await saveAvatarCache(apiSettings, avatarCache);
            resolve();
          } catch (error) {
            const urlCacheData: AvatarCacheData = { url: avatarUrl, timestamp: Date.now() };
            try {
              const adapter = getStorageAdapter();
              const apiSettings = await adapter.getApiSettings();
              const avatarCache = parseAvatarCache(apiSettings);
              avatarCache[userId] = urlCacheData;
              await saveAvatarCache(apiSettings, avatarCache);
            } catch (storageError) {
              await setAvatarFallbackItem(getAvatarCacheKey(userId), JSON.stringify(urlCacheData));
            }
            resolve();
          }
        };
        reader.onerror = reject;
      });
    } catch (error) {
      const urlCacheData: AvatarCacheData = { url: avatarUrl, timestamp: Date.now() };
      try {
        const adapter = getStorageAdapter();
        const apiSettings = await adapter.getApiSettings();
        const avatarCache = parseAvatarCache(apiSettings);
        avatarCache[userId] = urlCacheData;
        await saveAvatarCache(apiSettings, avatarCache);
      } catch (storageError) {
        await setAvatarFallbackItem(getAvatarCacheKey(userId), JSON.stringify(urlCacheData));
      }
    }
  },

  async getCachedAvatar(userId: string): Promise<string | null> {
    try {
      const adapter = getStorageAdapter();
      const apiSettings = await adapter.getApiSettings();
      const avatarCache = parseAvatarCache(apiSettings);
      const cacheData = avatarCache[userId];

      if (cacheData) {
        if (Date.now() - cacheData.timestamp > AVATAR_CACHE_EXPIRY) {
          delete avatarCache[userId];
          await saveAvatarCache(apiSettings, avatarCache);
          return null;
        }
        return cacheData.data || cacheData.url || null;
      }

      const cached = await getAvatarFallbackItem(getAvatarCacheKey(userId));
      if (!cached) return null;

      const fallbackData: AvatarCacheData = JSON.parse(cached);
      if (Date.now() - fallbackData.timestamp > AVATAR_CACHE_EXPIRY) {
        await removeAvatarFallbackItem(getAvatarCacheKey(userId));
        return null;
      }

      try {
        const adapter = getStorageAdapter();
        const apiSettings = await adapter.getApiSettings();
        const avatarCache = parseAvatarCache(apiSettings);
        avatarCache[userId] = fallbackData;
        await saveAvatarCache(apiSettings, avatarCache);
        await removeAvatarFallbackItem(getAvatarCacheKey(userId));
      } catch {}

      return fallbackData.data || fallbackData.url || null;
    } catch (error) {
      try {
        const cached = await getAvatarFallbackItem(getAvatarCacheKey(userId));
        if (!cached) return null;
        const cacheData: AvatarCacheData = JSON.parse(cached);
        if (Date.now() - cacheData.timestamp > AVATAR_CACHE_EXPIRY) {
          await removeAvatarFallbackItem(getAvatarCacheKey(userId));
          return null;
        }
        return cacheData.data || cacheData.url || null;
      } catch {}
    }
    return null;
  },

  async clearCache(userId: string): Promise<void> {
    try {
      const adapter = getStorageAdapter();
      const apiSettings = await adapter.getApiSettings();
      const avatarCache = parseAvatarCache(apiSettings);
      delete avatarCache[userId];
      await saveAvatarCache(apiSettings, avatarCache);
      await removeAvatarFallbackItem(getAvatarCacheKey(userId));
    } catch {
      await removeAvatarFallbackItem(getAvatarCacheKey(userId));
    }
  },

  async preloadAvatar(userId: string, avatarUrl: string): Promise<string> {
    const cached = await this.getCachedAvatar(userId);
    if (cached) return cached;
    await this.cacheAvatar(userId, avatarUrl);
    const newCached = await this.getCachedAvatar(userId);
    return newCached || avatarUrl;
  }
};
