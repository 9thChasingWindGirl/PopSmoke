import { SmokeLog } from '../../../types';
import { systemLogService } from '../../systemLogService';

export interface FeishuApiConfig {
  apiUrl: string;
  writeAccessKey?: string;
}

export interface FeishuResponse<T = any> {
  code: number;
  msg: string;
  data: T;
}

class FeishuClient {
  private config: FeishuApiConfig | null = null;

  public setConfig(config: FeishuApiConfig): void {
    this.config = config;
  }

  public getConfig(): FeishuApiConfig | null {
    return this.config;
  }

  public hasConfig(): boolean {
    return this.config !== null;
  }

  public async fetchRecords(
    userId: string,
    password?: string,
    options?: { startDate?: string; endDate?: string }
  ): Promise<{ success: boolean; logs?: SmokeLog[]; error?: string }> {
    try {
      if (!this.config?.apiUrl) {
        throw new Error('飞书 API URL 未配置');
      }

      let url = `${this.config.apiUrl}?user_id=${encodeURIComponent(userId)}`;
      
      if (options?.startDate) {
        url += `&start_date=${options.startDate}`;
      }
      if (options?.endDate) {
        url += `&end_date=${options.endDate}`;
      }

      const headers: Record<string, string> = {
        'Content-Type': 'application/json'
      };

      if (password) {
        headers['X-Password'] = password;
      }
      if (this.config.writeAccessKey) {
        headers['X-Access-Key'] = this.config.writeAccessKey;
      }

      const response = await fetch(url, {
        method: 'GET',
        headers
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result: FeishuResponse<SmokeLog[]> = await response.json();

      if (result.code !== 0 && result.code !== 200) {
        throw new Error(result.msg || '获取数据失败');
      }

      systemLogService.info('feishu', '飞书数据获取成功', { 
        count: result.data?.length || 0 
      });

      return {
        success: true,
        logs: result.data || []
      };
    } catch (error) {
      systemLogService.error('feishu', '获取飞书数据失败', error as Error);
      return {
        success: false,
        error: error instanceof Error ? error.message : '获取飞书数据失败'
      };
    }
  }

  public async submitRecord(
    log: SmokeLog,
    password?: string
  ): Promise<{ success: boolean; error?: string; id?: string }> {
    try {
      if (!this.config?.apiUrl) {
        throw new Error('飞书 API URL 未配置');
      }

      const headers: Record<string, string> = {
        'Content-Type': 'application/json'
      };

      if (password) {
        headers['X-Password'] = password;
      }
      if (this.config.writeAccessKey) {
        headers['X-Access-Key'] = this.config.writeAccessKey;
      }

      const response = await fetch(this.config.apiUrl, {
        method: 'POST',
        headers,
        body: JSON.stringify(log)
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result: FeishuResponse<{ id: string }> = await response.json();

      if (result.code !== 0 && result.code !== 200) {
        throw new Error(result.msg || '提交记录失败');
      }

      systemLogService.info('feishu', '记录提交成功', { id: result.data?.id });

      return {
        success: true,
        id: result.data?.id
      };
    } catch (error) {
      systemLogService.error('feishu', '提交记录失败', error as Error);
      return {
        success: false,
        error: error instanceof Error ? error.message : '提交记录失败'
      };
    }
  }

  public async checkDataExists(): Promise<{ exists: boolean; count?: number }> {
    try {
      if (!this.config?.apiUrl) {
        return { exists: false };
      }

      // 使用HEAD请求检查数据是否存在
      const response = await fetch(this.config.apiUrl, {
        method: 'HEAD',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      // 如果返回200且有数据，说明存在
      if (response.ok) {
        const countHeader = response.headers.get('X-Total-Count');
        return { 
          exists: true, 
          count: countHeader ? parseInt(countHeader, 10) : undefined 
        };
      }

      return { exists: false };
    } catch (error) {
      systemLogService.warn('feishu', '检查数据存在性失败', { message: (error as Error).message });
      return { exists: false };
    }
  }
}

export const feishuClient = new FeishuClient();

export const getFeishuApiSettings = async (): Promise<FeishuApiConfig | null> => {
  try {
    const { getStorageAdapter } = await import('../../storageAdapter');
    const adapter = getStorageAdapter();
    const savedSettings = await adapter.getApiSettings();

    if (!savedSettings?.feishu) {
      return null;
    }

    // 检查localStorage中的飞书配置
    const feishuInLocalStorage = localStorage.getItem('feishuApiSettings');
    if (feishuInLocalStorage) {
      try {
        const config = JSON.parse(feishuInLocalStorage);
        if (config.apiUrl && config.apiUrl.trim()) {
          feishuClient.setConfig(config);
          return config;
        }
      } catch {
        // 解析失败
      }
    }

    return null;
  } catch (error) {
    console.error('[FeishuClient] 获取配置失败:', error);
    return null;
  }
};
