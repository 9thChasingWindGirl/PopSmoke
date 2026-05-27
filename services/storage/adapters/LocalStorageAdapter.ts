import { SmokeLog, AppSettings, EncryptedApiSettings, DataStorageAdapter } from '../../../types';
import { DEFAULT_SETTINGS, STORAGE_KEY_LOGS, STORAGE_KEY_SETTINGS } from '../../../constants';
import { safeSetItem } from '../../../utils/storageUtils';

const STORAGE_KEY_API_SETTINGS = 'popsmoke_api_settings';

export class LocalStorageAdapter implements DataStorageAdapter {
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
  }

  async hasLoggedIn(): Promise<boolean> {
    return localStorage.getItem('popsmoke_has_logged_in') === 'true';
  }

  async setLoggedIn(value: boolean): Promise<void> {
    localStorage.setItem('popsmoke_has_logged_in', String(value));
  }
}
