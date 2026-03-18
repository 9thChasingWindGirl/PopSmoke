export class AppError extends Error {
  public readonly code: string;
  public readonly category: ErrorCategory;
  public readonly timestamp: number;
  public readonly data?: Record<string, unknown>;

  constructor(
    message: string,
    code: string,
    category: ErrorCategory,
    data?: Record<string, unknown>
  ) {
    super(message);
    this.name = 'AppError';
    this.code = code;
    this.category = category;
    this.timestamp = Date.now();
    this.data = data;
  }

  static fromError(error: Error, category: ErrorCategory = 'unknown'): AppError {
    return new AppError(error.message, 'UNKNOWN_ERROR', category, {
      originalName: error.name,
      stack: error.stack
    });
  }
}

export type ErrorCategory = 'init' | 'auth' | 'storage' | 'api' | 'sync' | 'ui' | 'unknown';

export interface ErrorInfo {
  code: string;
  message: string;
  category: ErrorCategory;
  userMessage: string;
  recoverable: boolean;
  action?: string;
}

export const ERROR_CODES: Record<string, ErrorInfo> = {
  INIT_001: {
    code: 'INIT_001',
    message: 'Platform detection failed',
    category: 'init',
    userMessage: '无法检测运行平台',
    recoverable: false
  },
  INIT_002: {
    code: 'INIT_002',
    message: 'Storage initialization failed',
    category: 'init',
    userMessage: '存储初始化失败',
    recoverable: true,
    action: 'retry'
  },
  AUTH_001: {
    code: 'AUTH_001',
    message: 'Invalid credentials',
    category: 'auth',
    userMessage: '邮箱或密码错误',
    recoverable: true
  },
  AUTH_002: {
    code: 'AUTH_002',
    message: 'Session expired',
    category: 'auth',
    userMessage: '登录已过期，请重新登录',
    recoverable: true,
    action: 'relogin'
  },
  AUTH_003: {
    code: 'AUTH_003',
    message: 'Network error during authentication',
    category: 'auth',
    userMessage: '网络错误，请检查网络连接',
    recoverable: true,
    action: 'retry'
  },
  STORAGE_001: {
    code: 'STORAGE_001',
    message: 'Storage quota exceeded',
    category: 'storage',
    userMessage: '存储空间不足',
    recoverable: true,
    action: 'cleanup'
  },
  STORAGE_002: {
    code: 'STORAGE_002',
    message: 'Failed to read from storage',
    category: 'storage',
    userMessage: '读取数据失败',
    recoverable: true,
    action: 'retry'
  },
  STORAGE_003: {
    code: 'STORAGE_003',
    message: 'Failed to write to storage',
    category: 'storage',
    userMessage: '保存数据失败',
    recoverable: true,
    action: 'retry'
  },
  API_001: {
    code: 'API_001',
    message: 'API request failed',
    category: 'api',
    userMessage: 'API请求失败',
    recoverable: true,
    action: 'retry'
  },
  API_002: {
    code: 'API_002',
    message: 'API configuration missing',
    category: 'api',
    userMessage: 'API配置缺失',
    recoverable: true,
    action: 'configure'
  },
  API_003: {
    code: 'API_003',
    message: 'API rate limit exceeded',
    category: 'api',
    userMessage: 'API请求频率超限，请稍后再试',
    recoverable: true,
    action: 'wait'
  },
  SYNC_001: {
    code: 'SYNC_001',
    message: 'Sync failed',
    category: 'sync',
    userMessage: '同步失败',
    recoverable: true,
    action: 'retry'
  },
  SYNC_002: {
    code: 'SYNC_002',
    message: 'Data conflict during sync',
    category: 'sync',
    userMessage: '数据同步冲突',
    recoverable: true,
    action: 'resolve'
  },
  UI_001: {
    code: 'UI_001',
    message: 'Component render error',
    category: 'ui',
    userMessage: '界面渲染错误',
    recoverable: true,
    action: 'refresh'
  }
};

export function getErrorInfo(code: string): ErrorInfo {
  return ERROR_CODES[code] || {
    code: 'UNKNOWN',
    message: 'Unknown error',
    category: 'unknown',
    userMessage: '发生未知错误',
    recoverable: true
  };
}

export function createAppError(code: string, data?: Record<string, unknown>): AppError {
  const info = getErrorInfo(code);
  return new AppError(info.message, code, info.category, data);
}

export function handleError(error: unknown): AppError {
  if (error instanceof AppError) {
    return error;
  }

  if (error instanceof Error) {
    return AppError.fromError(error);
  }

  return new AppError(
    String(error),
    'UNKNOWN_ERROR',
    'unknown',
    { originalError: error }
  );
}

export function isRecoverable(error: unknown): boolean {
  if (error instanceof AppError) {
    const info = getErrorInfo(error.code);
    return info.recoverable;
  }
  return true;
}

export function getErrorAction(error: unknown): string | undefined {
  if (error instanceof AppError) {
    const info = getErrorInfo(error.code);
    return info.action;
  }
  return undefined;
}
