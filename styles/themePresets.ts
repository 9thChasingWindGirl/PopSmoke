import type { Language } from '../types';

export interface ThemePreset {
  id: string;
  name: string;
  nameLocalized: {
    en: string;
    zh: string;
    ja: string;
    ko: string;
  };
  color: string;
  description: string;
}

export const THEME_PRESETS: ThemePreset[] = [
  {
    id: 'gold',
    name: 'Gold',
    nameLocalized: {
      en: 'Gold',
      zh: '金色',
      ja: 'ゴールド',
      ko: '골드',
    },
    color: '#FFD700',
    description: 'Classic pop art gold',
  },
  {
    id: 'coral',
    name: 'Coral',
    nameLocalized: {
      en: 'Coral',
      zh: '珊瑚红',
      ja: 'コーラル',
      ko: '코랄',
    },
    color: '#FF6B6B',
    description: 'Vibrant coral red',
  },
  {
    id: 'teal',
    name: 'Teal',
    nameLocalized: {
      en: 'Teal',
      zh: '青绿',
      ja: 'ティール',
      ko: '틸',
    },
    color: '#4ECDC4',
    description: 'Fresh teal green',
  },
  {
    id: 'lime',
    name: 'Lime',
    nameLocalized: {
      en: 'Lime',
      zh: '浅绿',
      ja: 'ライム',
      ko: '라임',
    },
    color: '#C7F464',
    description: 'Bright lime green',
  },
  {
    id: 'purple',
    name: 'Purple',
    nameLocalized: {
      en: 'Purple',
      zh: '紫色',
      ja: 'パープル',
      ko: '퍼플',
    },
    color: '#A78BFA',
    description: 'Soft purple',
  },
  {
    id: 'orange',
    name: 'Orange',
    nameLocalized: {
      en: 'Orange',
      zh: '橙色',
      ja: 'オレンジ',
      ko: '오렌지',
    },
    color: '#FF9F43',
    description: 'Warm orange',
  },
];

export const getThemePresetName = (preset: ThemePreset, language: Language): string => {
  return preset.nameLocalized[language] || preset.name;
};

export const getThemePresetById = (id: string): ThemePreset | undefined => {
  return THEME_PRESETS.find(preset => preset.id === id);
};

export const getThemePresetByColor = (color: string): ThemePreset | undefined => {
  return THEME_PRESETS.find(preset => preset.color.toUpperCase() === color.toUpperCase());
};
