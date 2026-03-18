import { SmokeLog } from '../types';

export const getStorageKeys = (userId?: string) => {
  if (userId) {
    return {
      logs: `popsmoke_logs_${userId}`,
      settings: `popsmoke_settings_${userId}`
    };
  }
  return {
    logs: 'popsmoke_logs_local',
    settings: 'popsmoke_settings_local'
  };
};

export const normalizeTime = (time: string): string => {
  const parts = time.split(':');
  if (parts.length === 2) {
    const hour = parts[0].padStart(2, '0');
    const minute = parts[1].padStart(2, '0');
    return `${hour}:${minute}`;
  }
  return time;
};

export const getUniqueKey = (log: SmokeLog) => {
  const time = log.record_time || 
    new Date(log.timestamp).toTimeString().split(' ')[0].substring(0, 5);
  return `${log.record_date || String(log.date)}_${normalizeTime(time)}`;
};

export const completeLogFields = (log: SmokeLog): SmokeLog => {
  return {
    ...log,
    record_date: log.record_date || String(log.date),
    record_time: log.record_time || 
      new Date(log.timestamp).toTimeString().split(' ')[0].substring(0, 5),
    record_index: log.record_index || 1
  };
};
