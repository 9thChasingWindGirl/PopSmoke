import React from 'react';
import { createPortal } from 'react-dom';
import { AppSettings, Language } from '../../types';
import { TRANSLATIONS } from '../../i18n';

interface PopLoadingProps {
  settings: AppSettings;
  language?: Language;
  message?: string;
  status?: 'initializing' | 'loading' | 'connecting' | 'syncing' | 'authenticating' | 'resetting' | 'restoring';
  isInitialize?: boolean;
}

export const PopLoading: React.FC<PopLoadingProps> = ({
  settings,
  language = settings?.language || 'en',
  message,
  status = 'loading',
  isInitialize = false
}) => {
  const t = TRANSLATIONS[language as Language];
  
  const getDefaultMessage = () => {
    if (message) return message;
    
    const statusMessages: Record<string, string> = {
      initializing: t.initializing || '初始化中...',
      loading: t.loading || '加载中...',
      connecting: t.connecting || '连接中...',
      syncing: t.syncing || '同步中...',
      authenticating: t.authenticating || '认证中...',
      resetting: t.resetting || '重置中...',
      restoring: t.restoring || '恢复中...'
    };
    
    return statusMessages[status] || t.loading || '加载中...';
  };
  
  const getLoaderStyle = () => {
    const statusColors: Record<string, string> = {
      initializing: 'border-purple-600',
      loading: 'border-black',
      connecting: 'border-blue-600',
      syncing: 'border-green-600',
      authenticating: 'border-red-600',
      resetting: 'border-pink-600',
      restoring: 'border-teal-600'
    };
    
    return statusColors[status] || 'border-black';
  };
  
  const content = (
    <div className="fixed inset-0 flex items-center justify-center bg-paper z-50">
      <div className="flex flex-col items-center space-y-4">
        <div className={`w-12 h-12 border-4 ${getLoaderStyle()} border-t-transparent rounded-full animate-spin`}></div>
        <div className="font-display text-xl">
          POP<span style={{ color: settings?.themeColor || '#FFD700' }}>SMOKE</span>
        </div>
        <div className="text-gray-600 text-sm">
          {getDefaultMessage()}
        </div>
      </div>
    </div>
  );
  
  return createPortal(content, document.body);
};
