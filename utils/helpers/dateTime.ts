/**
 * 日期时间工具函数
 */

export const formatDate = (date: Date | number): string => {
  const d = date instanceof Date ? date : new Date(date);
  return d.toISOString().split('T')[0];
};

export const formatTime = (date: Date | number): string => {
  const d = date instanceof Date ? date : new Date(date);
  return d.toTimeString().split(' ')[0].substring(0, 5);
};

export const formatDateTime = (timestamp: number): { date: string; time: string } => ({
  date: formatDate(timestamp),
  time: formatTime(timestamp)
});

export const formatFullDateTime = (date: Date | number): string => {
  const d = date instanceof Date ? date : new Date(date);
  return `${formatDate(d)} ${formatTime(d)}`;
};

export const getTimeAgo = (timestamp: number): string => {
  const now = Date.now();
  const diff = now - timestamp;
  
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}天前`;
  if (hours > 0) return `${hours}小时前`;
  if (minutes > 0) return `${minutes}分钟前`;
  if (seconds > 0) return `${seconds}秒前`;
  
  return '刚刚';
};

export const isToday = (timestamp: number): boolean => {
  const date = new Date(timestamp);
  const today = new Date();
  
  return (
    date.getDate() === today.getDate() &&
    date.getMonth() === today.getMonth() &&
    date.getFullYear() === today.getFullYear()
  );
};

export const isYesterday = (timestamp: number): boolean => {
  const date = new Date(timestamp);
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  
  return (
    date.getDate() === yesterday.getDate() &&
    date.getMonth() === yesterday.getMonth() &&
    date.getFullYear() === yesterday.getFullYear()
  );
};

export const getStartOfDay = (date?: Date): number => {
  const d = date || new Date();
  d.setHours(0, 0, 0, 0);
  return d.getTime();
};

export const getEndOfDay = (date?: Date): number => {
  const d = date || new Date();
  d.setHours(23, 59, 59, 999);
  return d.getTime();
};

export const getStartOfWeek = (date?: Date): number => {
  const d = date || new Date();
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  d.setDate(diff);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
};

export const getStartOfMonth = (date?: Date): number => {
  const d = date || new Date();
  d.setDate(1);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
};
