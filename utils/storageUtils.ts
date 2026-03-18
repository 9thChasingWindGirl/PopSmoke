const MAX_STORAGE_SIZE = 5 * 1024 * 1024;
const WARNING_THRESHOLD = 0.8;

export const getStorageUsage = (): { used: number; total: number; percent: number } => {
  let used = 0;
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key) {
      const value = localStorage.getItem(key);
      if (value) {
        used += (key.length + value.length) * 2;
      }
    }
  }
  return {
    used,
    total: MAX_STORAGE_SIZE,
    percent: used / MAX_STORAGE_SIZE
  };
};

export const safeSetItem = (key: string, value: string): { success: boolean; error?: string } => {
  try {
    const usage = getStorageUsage();
    const newItemSize = (key.length + value.length) * 2;
    
    if (usage.percent >= WARNING_THRESHOLD) {
      console.warn(`Storage usage is at ${Math.round(usage.percent * 100)}%. Consider clearing old data.`);
    }
    
    localStorage.setItem(key, value);
    return { success: true };
  } catch (error) {
    if (error instanceof DOMException && error.name === 'QuotaExceededError') {
      console.error('Storage quota exceeded!');
      return { 
        success: false, 
        error: 'Storage quota exceeded. Please clear some old data in Settings.'
      };
    }
    throw error;
  }
};

export const cleanupOldData = (userId?: string): number => {
  const prefix = userId ? `popsmoke_logs_${userId}` : 'popsmoke_logs_local';
  let removedCount = 0;
  
  const logsKey = userId ? `popsmoke_logs_${userId}` : 'popsmoke_logs_local';
  const logsData = localStorage.getItem(logsKey);
  
  if (logsData) {
    try {
      const logs = JSON.parse(logsData);
      
      const usage = getStorageUsage();
      let daysToKeep = 90;
      
      if (usage.percent > 0.9) {
        daysToKeep = 30;
        console.warn(`Storage usage is critical (${Math.round(usage.percent * 100)}%), keeping only ${daysToKeep} days of data`);
      } else if (usage.percent > 0.8) {
        daysToKeep = 60;
        console.warn(`Storage usage is high (${Math.round(usage.percent * 100)}%), keeping only ${daysToKeep} days of data`);
      }
      
      const cutoffDate = Date.now() - (daysToKeep * 24 * 60 * 60 * 1000);
      const filteredLogs = logs.filter((log: { timestamp: number }) => log.timestamp >= cutoffDate);
      
      if (filteredLogs.length < logs.length) {
        removedCount = logs.length - filteredLogs.length;
        const saveResult = safeSetItem(logsKey, JSON.stringify(filteredLogs));
        if (saveResult.success) {
          console.log(`Cleaned up ${removedCount} old logs (kept ${daysToKeep} days)`);
        } else {
          console.error('Failed to save cleaned logs:', saveResult.error);
        }
      }
    } catch (e) {
      console.error('Failed to cleanup old data:', e);
    }
  }
  
  return removedCount;
};

export const forceCleanup = (): void => {
  const allKeys: string[] = [];
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key) allKeys.push(key);
  }
  
  const logKeys = allKeys.filter(k => k.includes('_logs_') || k.includes('logs_local'));
  logKeys.forEach(key => {
    const data = localStorage.getItem(key);
    if (data) {
      try {
        const logs = JSON.parse(data);
        if (Array.isArray(logs) && logs.length > 1000) {
          const recentLogs = logs.slice(0, 500);
          localStorage.setItem(key, JSON.stringify(recentLogs));
          console.log(`Force cleaned ${key}: removed ${logs.length - 500} old entries`);
        }
      } catch (e) {
        console.error('Failed to force cleanup:', e);
      }
    }
  });
};

// Simple XOR encryption for API settings
export const simpleEncrypt = (text: string, password: string): string => {
  let result = '';
  for (let i = 0; i < text.length; i++) {
    const charCode = text.charCodeAt(i) ^ password.charCodeAt(i % password.length);
    result += String.fromCharCode(charCode);
  }
  return btoa(result);
};

export const simpleDecrypt = (encodedText: string, password: string): string => {
  const text = atob(encodedText);
  let result = '';
  for (let i = 0; i < text.length; i++) {
    const charCode = text.charCodeAt(i) ^ password.charCodeAt(i % password.length);
    result += String.fromCharCode(charCode);
  }
  return result;
};
