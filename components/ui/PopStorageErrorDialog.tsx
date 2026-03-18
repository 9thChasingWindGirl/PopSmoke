import React from 'react';
import { ViewState } from '../../types';

interface PopStorageErrorDialogProps {
  isOpen: boolean;
  errorMessage: string;
  onDismiss: () => void;
  onGoToSettings: () => void;
  themeColor: string;
  t: Record<string, string>;
}

export const PopStorageErrorDialog: React.FC<PopStorageErrorDialogProps> = ({
  isOpen,
  errorMessage,
  onDismiss,
  onGoToSettings,
  themeColor,
  t
}) => {
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
        
        <div className="flex space-x-3">
          <button
            onClick={onGoToSettings}
            className="flex-1 bg-black text-white font-bold py-3 border-4 border-black hover:bg-white hover:text-black transition-colors"
          >
            {t.goToSettings || 'Go to Settings'}
          </button>
          <button
            onClick={onDismiss}
            className="flex-1 bg-gray-200 text-black font-bold py-3 border-4 border-black hover:bg-gray-300 transition-colors"
          >
            {t.dismiss || 'Dismiss'}
          </button>
        </div>
      </div>
    </div>
  );
};
