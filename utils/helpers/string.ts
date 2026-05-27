/**
 * 字符串和验证工具函数
 */

export const isEmpty = (value: unknown): boolean => {
  if (value === null || value === undefined) return true;
  if (typeof value === 'string') return value.trim() === '';
  if (Array.isArray(value)) return value.length === 0;
  if (typeof value === 'object') return Object.keys(value).length === 0;
  return false;
};

export const isNotEmpty = (value: unknown): boolean => !isEmpty(value);

export const truncate = (str: string, maxLength: number, suffix: string = '...'): string => {
  if (str.length <= maxLength) return str;
  return str.substring(0, maxLength - suffix.length) + suffix;
};

export const capitalize = (str: string): string => 
  str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();

export const camelCase = (str: string): string =>
  str
    .toLowerCase()
    .replace(/[^a-zA-Z0-9]+(.)/g, (_, char) => char.toUpperCase())
    .replace(/^./, char => char.toLowerCase());

export const snakeCase = (str: string): string =>
  str
    .replace(/([A-Z])/g, '_$1')
    .toLowerCase()
    .replace(/^_/, '');

export const generateId = (prefix: string = ''): string =>
  `${prefix}${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

export const validateEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

export const validatePassword = (password: string): { valid: boolean; message?: string } => {
  if (!password || password.length === 0) {
    return { valid: false, message: '密码不能为空' };
  }
  
  if (password.length < 6) {
    return { valid: false, message: '密码长度至少6个字符' };
  }

  return { valid: true };
};

export const validateUrl = (url: string): boolean => {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
};

export const maskEmail = (email: string): string => {
  const [username, domain] = email.split('@');
  if (!domain) return email;
  
  const maskedUsername = username.length > 2 
    ? `${username[0]}${'*'.repeat(username.length - 2)}${username[username.length - 1]}`
    : username;
    
  return `${maskedUsername}@${domain}`;
};

export const maskString = (str: string, visibleChars: number = 4): string => {
  if (str.length <= visibleChars) return str;
  return `${str.substring(0, visibleChars)}${'*'.repeat(str.length - visibleChars)}`;
};

export const formatNumber = (num: number, locale: string = 'zh-CN'): string =>
  new Intl.NumberFormat(locale).format(num);

export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes';
  
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
};

export const pluralize = (count: number, singular: string, plural?: string): string =>
  count === 1 ? singular : (plural || `${singular}s`);
