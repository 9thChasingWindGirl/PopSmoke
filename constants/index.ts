import type { AppSettings, Language } from '../types';

export const STORAGE_KEY_LOGS = 'popsmoke_logs_local';
export const STORAGE_KEY_SETTINGS = 'popsmoke_settings';
export const STORAGE_KEY_HAS_LOGGED_IN = 'popsmoke_has_logged_in';
export const STORAGE_KEY_USER_ID = 'popsmoke_user_id';

export const DEFAULT_SETTINGS: AppSettings = {
  user_id: 'local',
  dailyLimit: 20,
  warningLimit: 15,
  themeColor: '#FFD700',
  language: 'en' as Language
};

export const THEME_PRESETS = [
  '#FFD700',
  '#FF6B6B',
  '#4ECDC4',
  '#C7F464',
  '#A78BFA',
  '#FF9F43'
];

export const LANGUAGE_OPTIONS: { value: Language; label: string }[] = [
  { value: 'en', label: 'English' },
  { value: 'zh', label: '中文' },
  { value: 'ja', label: '日本語' },
  { value: 'ko', label: '한국어' }
];

export const MAX_LOGS_PER_PAGE = 1000;
export const MAX_LOGS_TO_DISPLAY = 500;
export const SYNC_BATCH_SIZE = 500;
export const CLEANUP_DAYS_THRESHOLD = 90;
export const STORAGE_WARNING_THRESHOLD = 0.8;

export const DATE_FORMAT = 'YYYY-MM-DD';
export const TIME_FORMAT = 'HH:mm:ss';
export const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

export const APP_NAME = 'PopSmoke tracker';
export const APP_VERSION = '0.3.30';

export const TIMEOUT_MS = 10000;
export const RETRY_COUNT = 3;
export const RETRY_DELAY_MS = 1000;
