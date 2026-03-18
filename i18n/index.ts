import { i18n_EN, type I18nEN, type I18nTranslations } from './i18n_EN';
import { i18n_ZH, type I18nZH } from './i18n_ZH';
import { i18n_JP, type I18nJP } from './i18n_JP';
import { i18n_KO, type I18nKO } from './i18n_KO';
import type { Language } from '../types';

const translations: Record<Language, I18nTranslations> = {
  en: i18n_EN,
  zh: i18n_ZH,
  ja: i18n_JP,
  ko: i18n_KO
};

export const TRANSLATIONS: Record<Language, I18nTranslations> = translations;

export function getTranslations(language: Language): I18nTranslations {
  return translations[language] || translations.en;
}

export function t(language: Language, key: string): string {
  const translations = getTranslations(language);
  return (translations as Record<string, string>)[key] || key;
}

export { i18n_EN, i18n_ZH, i18n_JP, i18n_KO };
export type { I18nEN, I18nZH, I18nJP, I18nKO, I18nTranslations };
