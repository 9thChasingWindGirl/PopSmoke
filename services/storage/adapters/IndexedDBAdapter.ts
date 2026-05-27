import { SmokeLog, AppSettings, EncryptedApiSettings, DataStorageAdapter } from '../../../types';
import { DEFAULT_SETTINGS } from '../../../constants';

export class IndexedDBAdapter implements DataStorageAdapter {
  private dbName = 'popsmoke_db';
  private dbVersion = 1;
  private db: IDBDatabase | null = null;
  private dbLogged = false;
  private openingPromise: Promise<IDBDatabase> | null = null;

  private async openDB(): Promise<IDBDatabase> {
    if (this.db) {
      if (!this.dbLogged) {
        console.log('[IndexedDBAdapter.openDB] 使用现有数据库连接');
        this.dbLogged = true;
      }
      return this.db;
    }

    if (this.openingPromise) {
      return this.openingPromise;
    }

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
        resolve(this.db);
      };

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBRequest<IDBDatabase>).result;

        if (!db.objectStoreNames.contains('logs')) {
          const logsStore = db.createObjectStore('logs', { keyPath: 'id' });
          logsStore.createIndex('timestamp', 'timestamp');
          logsStore.createIndex('record_date', 'record_date');
        }

        if (!db.objectStoreNames.contains('settings')) {
          db.createObjectStore('settings', { keyPath: 'key' });
        }

        if (!db.objectStoreNames.contains('api_settings')) {
          db.createObjectStore('api_settings', { keyPath: 'key' });
        }
      };
    });

    return this.openingPromise;
  }

  private async getStore(storeName: string, mode: IDBTransactionMode = 'readonly'): Promise<IDBObjectStore> {
    const db = await this.openDB();
    const transaction = db.transaction(storeName, mode);
    return transaction.objectStore(storeName);
  }

  async getLogs(): Promise<SmokeLog[]> {
    try {
      const store = await this.getStore('logs', 'readonly');
      return new Promise((resolve, reject) => {
        const request = store.getAll();
        request.onsuccess = () => {
          const logs = (request.result as SmokeLog[]).sort(
            (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
          );
          resolve(logs);
        };
        request.onerror = () => reject(request.error);
      });
    } catch (error) {
      console.error('[IndexedDBAdapter.getLogs] 获取日志失败:', error);
      return [];
    }
  }

  async saveLogs(logs: SmokeLog[]): Promise<void> {
    try {
      const db = await this.openDB();
      const transaction = db.transaction(['logs'], 'readwrite');
      const store = transaction.objectStore('logs');
      
      store.clear();
      
      logs.forEach(log => {
        store.put(log);
      });

      return new Promise((resolve, reject) => {
        transaction.oncomplete = () => resolve();
        transaction.onerror = () => reject(transaction.error);
      });
    } catch (error) {
      console.error('[IndexedDBAdapter.saveLogs] 保存日志失败:', error);
      throw error;
    }
  }

  async getSettings(): Promise<AppSettings> {
    try {
      const store = await this.getStore('settings', 'readonly');
      return new Promise((resolve, reject) => {
        const request = store.get('app_settings');
        request.onsuccess = () => {
          resolve(request.result?.value || DEFAULT_SETTINGS);
        };
        request.onerror = () => {
          console.warn('[IndexedDBAdapter.getSettings] 获取设置失败，使用默认值');
          resolve(DEFAULT_SETTINGS);
        };
      });
    } catch (error) {
      console.error('[IndexedDBAdapter.getSettings] 错误:', error);
      return DEFAULT_SETTINGS;
    }
  }

  async saveSettings(settings: AppSettings): Promise<void> {
    try {
      const store = await this.getStore('settings', 'readwrite');
      store.put({ key: 'app_settings', value: settings });
    } catch (error) {
      console.error('[IndexedDBAdapter.saveSettings] 保存设置失败:', error);
      throw error;
    }
  }

  async getApiSettings(): Promise<EncryptedApiSettings | null> {
    try {
      const store = await this.getStore('api_settings', 'readonly');
      return new Promise((resolve, reject) => {
        const request = store.get('api_config');
        request.onsuccess = () => {
          resolve(request.result?.value || null);
        };
        request.onerror = () => resolve(null);
      });
    } catch (error) {
      console.error('[IndexedDBAdapter.getApiSettings] 错误:', error);
      return null;
    }
  }

  async saveApiSettings(settings: EncryptedApiSettings): Promise<void> {
    try {
      const store = await this.getStore('api_settings', 'readwrite');
      store.put({ key: 'api_config', value: settings });
    } catch (error) {
      console.error('[IndexedDBAdapter.saveApiSettings] 保存API设置失败:', error);
      throw error;
    }
  }

  async deleteApiSettings(): Promise<void> {
    try {
      const store = await this.getStore('api_settings', 'readwrite');
      store.delete('api_config');
    } catch (error) {
      console.error('[IndexedDBAdapter.deleteApiSettings] 删除API设置失败:', error);
      throw error;
    }
  }

  async clearAll(): Promise<void> {
    try {
      const db = await this.openDB();
      const transaction = db.transaction(['logs', 'settings', 'api_settings'], 'readwrite');
      
      transaction.objectStore('logs').clear();
      transaction.objectStore('settings').clear();
      transaction.objectStore('api_settings').clear();

      return new Promise((resolve, reject) => {
        transaction.oncomplete = () => resolve();
        transaction.onerror = () => reject(transaction.error);
      });
    } catch (error) {
      console.error('[IndexedDBAdapter.clearAll] 清除所有数据失败:', error);
      throw error;
    }
  }

  async clearLogsOnly(): Promise<void> {
    try {
      const store = await this.getStore('logs', 'readwrite');
      store.clear();
    } catch (error) {
      console.error('[IndexedDBAdapter.clearLogsOnly] 清除日志失败:', error);
      throw error;
    }
  }

  async hasLoggedIn(): Promise<boolean> {
    try {
      const store = await this.getStore('settings', 'readonly');
      return new Promise((resolve, reject) => {
        const request = store.get('has_logged_in');
        request.onsuccess = () => {
          resolve(request.result?.value === true);
        };
        request.onerror = () => resolve(false);
      });
    } catch (error) {
      console.error('[IndexedDBAdapter.hasLoggedIn] 错误:', error);
      return false;
    }
  }

  async setLoggedIn(value: boolean): Promise<void> {
    try {
      const store = await this.getStore('settings', 'readwrite');
      store.put({ key: 'has_logged_in', value });
    } catch (error) {
      console.error('[IndexedDBAdapter.setLoggedIn] 设置登录状态失败:', error);
      throw error;
    }
  }

  async closeConnection(): Promise<void> {
    if (this.db) {
      this.db.close();
      this.db = null;
      this.dbLogged = false;
      console.log('[IndexedDBAdapter.closeConnection] 数据库连接已关闭');
    }
  }
}
