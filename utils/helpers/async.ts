/**
 * 错误处理和异步操作工具函数
 */

export interface AsyncResult<T> {
  success: boolean;
  data?: T;
  error?: Error;
}

export const handleAsyncError = async <T>(
  promise: Promise<T>,
  errorHandler?: (error: Error) => void
): Promise<AsyncResult<T>> => {
  try {
    const data = await promise;
    return { success: true, data };
  } catch (error) {
    const err = error instanceof Error ? error : new Error(String(error));
    if (errorHandler) {
      errorHandler(err);
    }
    return { success: false, error: err };
  }
};

export const retryAsync = async <T>(
  fn: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> => {
  let lastError: Error | null = null;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error));
      
      if (attempt < maxRetries) {
        console.warn(`[retry] 第${attempt}次尝试失败，${delay}ms后重试...`, lastError.message);
        await new Promise(resolve => setTimeout(resolve, delay * attempt));
      }
    }
  }

  throw lastError;
};

export const withTimeout = async <T>(
  promise: Promise<T>,
  timeoutMs: number,
  errorMessage: string = '操作超时'
): Promise<T> => {
  const timeoutPromise = new Promise<never>((_, reject) => {
    setTimeout(() => reject(new Error(errorMessage)), timeoutMs);
  });

  return Promise.race([promise, timeoutPromise]);
};

export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  return (...args: Parameters<T>) => {
    if (timeoutId) {
      clearTimeout(timeoutId);
    }

    timeoutId = setTimeout(() => {
      func(...args);
      timeoutId = null;
    }, wait);
  };
};

export const throttle = <T extends (...args: any[]) => any>(
  func: T,
  limit: number
): ((...args: Parameters<T>) => void) => {
  let inThrottle = false;

  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      func(...args);
      inThrottle = true;
      setTimeout(() => {
        inThrottle = false;
      }, limit);
    }
  };
};

export const createLoadingState = <T>(
  isLoading: boolean,
  data?: T,
  error?: Error
): { isLoading: boolean; data?: T; error?: Error } => ({
  isLoading,
  data,
  error
});
