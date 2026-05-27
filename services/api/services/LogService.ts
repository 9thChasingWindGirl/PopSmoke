import { SmokeLog } from '../../../types';
import { getSupabaseClient } from '../clients/SupabaseClient';
import { systemLogService } from '../../systemLogService';
import { normalizeTime } from '../../../utils/logUtils';
import EventHandle from '../../../event/EventHandle';
import { EventType } from '../../../event/EventType';

export class LogService {
  
  public async saveLog(log: SmokeLog): Promise<{ success: boolean; error?: string }> {
    try {
      const client = await getSupabaseClient();
      
      const normalizedLog = {
        ...log,
        record_time: normalizeTime(log.record_time)
      };

      const { data, error } = await client
        .from('smoke_logs')
        .insert([normalizedLog])
        .select()
        .single();

      if (error) {
        throw error;
      }

      (systemLogService as any).info('log', '日志保存成功', { id: data.id });
      
      (EventHandle as any).publish({
        type: EventType.LOG_CREATE,
        category: 'log' as any,
        data: {
          success: true,
          logs: [data]
        },
        timestamp: Date.now()
      });

      return { success: true };
    } catch (error) {
      (systemLogService as any).error('log', '保存日志失败', error as Error);
      return {
        success: false,
        error: error instanceof Error ? error.message : '保存日志失败'
      };
    }
  }

  public async updateLog(
    log: SmokeLog, 
    currentLogs: SmokeLog[]
  ): Promise<{ success: boolean; error?: string; logs?: SmokeLog[] }> {
    try {
      const client = await getSupabaseClient();
      
      const normalizedLog = {
        ...log,
        record_time: normalizeTime(log.record_time),
        updated_at: new Date().toISOString()
      };

      const { data, error } = await client
        .from('smoke_logs')
        .update(normalizedLog)
        .eq('id', log.id)
        .select()
        .single();

      if (error) {
        throw error;
      }

      const updatedLogs = currentLogs.map(l => 
        l.id === log.id ? data : l
      );

      (systemLogService as any).info('log', '日志更新成功', { id: data.id });

      (EventHandle as any).publish({
        type: EventType.LOG_UPDATE,
        category: 'log' as any,
        data: {
          success: true,
          logs: updatedLogs
        },
        timestamp: Date.now()
      });

      return { success: true, logs: updatedLogs };
    } catch (error) {
      (systemLogService as any).error('log', '更新日志失败', error as Error);
      return {
        success: false,
        error: error instanceof Error ? error.message : '更新日志失败'
      };
    }
  }

  public async deleteLog(
    id: string, 
    userId: string, 
    currentLogs: SmokeLog[]
  ): Promise<{ success: boolean; error?: string; logs?: SmokeLog[] }> {
    try {
      const client = await getSupabaseClient();

      const { error } = await client
        .from('smoke_logs')
        .delete()
        .eq('id', id)
        .eq('user_id', userId);

      if (error) {
        throw error;
      }

      const updatedLogs = currentLogs.filter(l => l.id !== id);

      (systemLogService as any).info('log', '日志删除成功', { id });

      (EventHandle as any).publish({
        type: EventType.LOG_DELETE,
        category: 'log' as any,
        data: {
          success: true,
          logs: updatedLogs
        },
        timestamp: Date.now()
      });

      return { success: true, logs: updatedLogs };
    } catch (error) {
      (systemLogService as any).error('log', '删除日志失败', error as Error);
      return {
        success: false,
        error: error instanceof Error ? error.message : '删除日志失败'
      };
    }
  }

  public async getCloudLogs(
    userId: string, 
    page: number = 0, 
    pageSize: number = 20
  ): Promise<{
    success: boolean;
    logs?: SmokeLog[];
    error?: string;
    hasMore?: boolean;
  }> {
    try {
      const client = await getSupabaseClient();

      const from = page * pageSize;
      const to = from + pageSize - 1;

      const { data, error, count } = await client
        .from('smoke_logs')
        .select('*', { count: 'exact' })
        .eq('user_id', userId)
        .order('timestamp', { ascending: false })
        .range(from, to);

      if (error) {
        throw error;
      }

      (systemLogService as any).info('cloud', '获取云端日志成功', { 
        count: data?.length || 0,
        page,
        total: count || 0
      });

      return {
        success: true,
        logs: data || [],
        hasMore: (from + (data?.length || 0)) < (count || 0)
      };
    } catch (error) {
      (systemLogService as any).error('cloud', '获取云端日志失败', error as Error);
      return {
        success: false,
        error: error instanceof Error ? error.message : '获取云端日志失败'
      };
    }
  }

  public async getLogById(id: string): Promise<SmokeLog | null> {
    try {
      const client = await getSupabaseClient();

      const { data, error } = await client
        .from('smoke_logs')
        .select('*')
        .eq('id', id)
        .single();

      if (error) {
        throw error;
      }

      return data;
    } catch (error) {
      (systemLogService as any).error('log', '获取单条日志失败', error as Error);
      return null;
    }
  }

  public async searchLogs(
    query: string, 
    userId: string
  ): Promise<SmokeLog[]> {
    try {
      const client = await getSupabaseClient();

      const { data, error } = await client
        .from('smoke_logs')
        .select('*')
        .eq('user_id', userId)
        .or(`record_date.ilike.%${query}%,notes.ilike.%${query}%`)
        .order('timestamp', { ascending: false })
        .limit(50);

      if (error) {
        throw error;
      }

      return data || [];
    } catch (error) {
      (systemLogService as any).error('log', '搜索日志失败', error as Error);
      return [];
    }
  }
}

export const logService = new LogService();
