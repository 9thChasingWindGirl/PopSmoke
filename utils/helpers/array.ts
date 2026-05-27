/**
 * 数组和集合操作工具函数
 */
import { SmokeLog } from '../../types';

/** 安全获取对象属性值 */
const getPropertyValue = <T>(obj: T, key: keyof T): T[keyof T] => obj[key];

export const prependToList = <T>(list: T[], item: T): T[] => [item, ...list];

export const appendToList = <T>(list: T[], items: T[]): T[] => [...list, ...items];

export const removeFromList = <T>(list: T[], predicate: (item: T) => boolean): T[] => 
  list.filter(item => !predicate(item));

export const updateInList = <T extends Record<string, unknown>>(
  list: T[], 
  id: string | number, 
  updates: Partial<T>,
  idKey: keyof T = 'id' as keyof T
): T[] =>
  list.map(item => 
    getPropertyValue(item, idKey) === id ? { ...item, ...updates } : item
  );

export const findInList = <T extends Record<string, unknown>>(
  list: T[],
  id: string | number,
  idKey: keyof T = 'id' as keyof T
): T | undefined =>
  list.find(item => getPropertyValue(item, idKey) === id);

export const groupByDate = (logs: SmokeLog[]): Record<string, SmokeLog[]> => 
  logs.reduce((groups, log) => {
    const date = new Date(log.timestamp).toISOString().split('T')[0];
    if (!groups[date]) {
      groups[date] = [];
    }
    groups[date].push(log);
    return groups;
  }, {} as Record<string, SmokeLog[]>);

export const sortByTimestamp = <T extends { timestamp: number }>(
  items: T[], 
  order: 'asc' | 'desc' = 'desc'
): T[] =>
  [...items].sort((a, b) => 
    order === 'desc' 
      ? b.timestamp - a.timestamp 
      : a.timestamp - b.timestamp
  );

export const uniqueBy = <T, K extends keyof T>(items: T[], key: K): T[] => {
  const seen = new Set<T[K]>();
  return items.filter(item => {
    const value = item[key];
    if (seen.has(value)) {
      return false;
    }
    seen.add(value);
    return true;
  });
};

export const chunk = <T>(array: T[], size: number): T[][] => {
  const chunks: T[][] = [];
  for (let i = 0; i < array.length; i += size) {
    chunks.push(array.slice(i, i + size));
  }
  return chunks;
};

export const paginate = <T>(
  items: T[], 
  page: number, 
  pageSize: number
): { items: T[]; total: number; totalPages: number; hasMore: boolean } => {
  const total = items.length;
  const totalPages = Math.ceil(total / pageSize);
  const start = (page - 1) * pageSize;
  const end = start + pageSize;
  
  return {
    items: items.slice(start, end),
    total,
    totalPages,
    hasMore: page < totalPages
  };
};
