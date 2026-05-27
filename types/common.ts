/**
 * 通用类型定义
 * 用于消除 any 类型的使用
 */

// ============ 基础类型 ============

/** 任意对象类型（用于不确定结构的API响应） */
export interface UnknownObject {
  [key: string]: unknown;
}

/** 键值对类型 */
export type KeyValue<T = unknown> = Record<string, T>;

// ============ API 响应类型 ============

/** 通用 API 响应 */
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

/** 分页响应 */
export interface PaginatedResponse<T> extends ApiResponse<T[]> {
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

/** 同步差异结果 */
export interface SyncDiff {
  localOnly: unknown[];
  cloudOnly: unknown[];
  conflicting: unknown[];
  fieldsToUpdate: unknown[];
  totalLocal: number;
  totalCloud: number;
}

// ============ UI 状态类型 ============

/** 加载状态 */
export type LoadingState = 'idle' | 'loading' | 'success' | 'error';

/** 对话框状态 */
export interface DialogState<T = unknown> {
  isOpen: boolean;
  data?: T;
}

/** 表单字段状态 */
export interface FieldState<T> {
  value: T;
  error?: string;
  touched: boolean;
  isValid: boolean;
}

// ============ 事件类型 ============

/** 通用事件处理器 */
export type EventHandler<T = void> = (payload: T) => void;

/** 异步事件处理器 */
export type AsyncEventHandler<T = void> = (payload: T) => Promise<void>;

// ============ 配置类型 ============

/** API 配置 */
export interface ApiConfig {
  apiUrl: string;
  apiKey?: string;
  apiSecret?: string;
  [key: string]: unknown;
}

/** 存储配置 */
export interface StorageConfig {
  type: 'local' | 'indexeddb' | 'cloud';
  maxSize?: number;
  encryptionEnabled?: boolean;
}

// ============ 组件 Props 类型 ============

/** 基础组件 Props */
export interface BaseComponentProps {
  className?: string;
  style?: React.CSSProperties;
  children?: React.ReactNode;
}

/** 可选 Props 工具类型 */
export type OptionalProps<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;

// ============ 函数类型 ============

/** 无参数无返回值函数 */
export type VoidFunction = () => void;

/** 异步无返回值函数 */
export type AsyncVoidFunction = () => Promise<void>;

/** 返回布尔值的谓词函数 */
export type Predicate<T> = (value: T) => boolean;

/** 映射转换函数 */
export type Mapper<T, R> = (value: T, index: number) => R;

// ============ 工具类型 ============

/** 使所有属性变为可选 */
export type DeepPartial<T> = {
  [P in keyof T]?: T[P] extends object ? DeepPartial<T[P]> : T[P];
};

/** 使所有属性变为只读 */
export type DeepReadonly<T> = {
  readonly [P in keyof T]: T[P] extends object ? DeepReadonly<T[P]> : T[P];
};

/** 提取必需的属性 */
export type RequiredKeys<T, K extends keyof T> = Omit<T, K> & Required<Pick<T, K>>;

// ============ 字符串字面量工具 ============

/** 提取对象的所有键作为联合类型 */
export type KeysOf<T> = keyof T;

/** 提取对象所有值为字符串的键 */
export type StringKeys<T> = {
  [K in keyof T]: T[K] extends string ? K : never
}[keyof T];

// ============ Promise 工具类型 ============

/** 解包 Promise 的类型 */
export type UnwrapPromise<T extends Promise<unknown>> = T extends Promise<infer U> ? U : never;

/** 可能是 Promise 的类型 */
export type MaybePromise<T> = T | Promise<T>;
