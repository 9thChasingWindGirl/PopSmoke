import React from 'react';
import { TRANSLATIONS } from '../../i18n';
import { Language } from '../../types';

interface PopStorageErrorDialogProps {
  isOpen: boolean;
  onClose: () => void;
  errorMessage?: string;
  themeColor?: string;
  language?: string;
}

export const PopStorageErrorDialog: React.FC<PopStorageErrorDialogProps> = ({
  isOpen,
  onClose,
  errorMessage = 'Storage is running low. Please clear some data.',
  themeColor = '#F59E0B',
  language = 'en'
}) => {
  const t = TRANSLATIONS[language as Language];

  if (!isOpen) return null;

  return (
    <div className="min-h-screen flex items-center justify-center bg-paper p-4">
      <div className="bg-white border-4 border-black p-8 max-w-md w-full shadow-pop">
        <h1 className="font-display text-3xl mb-6 text-center">
          POP<span style={{ color: themeColor }}>SMOKE</span>
        </h1>
        
        <h2 className="font-bold text-xl mb-4 text-red-600">
          {t.storageError || 'Storage Error'}
        </h2>
        
        <p className="mb-4 text-sm text-gray-600">
          {errorMessage}
        </p>
        
        <p className="mb-4 text-sm text-gray-600">
          {t.storageErrorHint || 'Please go to Settings and clear some old data to free up space.'}
        </p>
        
        <button
          onClick={onClose}
          className="w-full bg-black text-white font-bold py-3 border-4 border-black hover:bg-white hover:text-black transition-colors"
        >
          {t.dismiss || 'Dismiss'}
        </button>
      </div>
    </div>
  );
};
