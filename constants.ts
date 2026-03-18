import { AppSettings } from './types';

export const DEFAULT_SETTINGS: AppSettings = {
  dailyLimit: 10,
  warningLimit: 7,
  themeColor: '#FFD700',
  language: 'en',
  user_id: '',
};

export const THEME_PRESETS = [
  '#FFD700',
  '#FF6B6B',
  '#4ECDC4',
  '#C7F464',
  '#A78BFA',
  '#FF9F43',
];

export const STORAGE_KEY_LOGS = 'popsmoke_logs';
export const STORAGE_KEY_SETTINGS = 'popsmoke_settings';
export const STORAGE_KEY_FEISHU_SETTINGS = 'popsmoke_feishu_settings';
export const STORAGE_KEY_HAS_LOGGED_IN = 'popsmoke_has_logged_in';

export const DEFAULT_FEISHU_API_URL = 'https://feishu.chasingwindgirl.workers.dev';

export const HISTORY_PAGE_SIZE = 6;
