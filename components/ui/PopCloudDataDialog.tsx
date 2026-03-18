import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { PopButton } from './PopButton';
import { POP_DESIGN_SYSTEM } from '../../styles/designSystem';

type DialogMode = 'download' | 'login';

interface PopCloudDataDialogProps {
  visible: boolean;
  mode?: DialogMode;
  recordCount?: number;
  onDownload?: (password?: string) => void;
  onLogin?: () => void;
  onSkip: () => void;
  onClose: () => void;
  themeColor?: string;
  title: string;
  message: string;
  downloadText?: string;
  loginText?: string;
  skipText: string;
  requirePassword?: boolean;
  passwordPlaceholder?: string;
}

export const PopCloudDataDialog: React.FC<PopCloudDataDialogProps> = ({
  visible,
  mode = 'download',
  recordCount = 0,
  onDownload,
  onLogin,
  onSkip,
  onClose,
  themeColor = POP_DESIGN_SYSTEM.colors.theme.gold,
  title,
  message,
  downloadText,
  loginText,
  skipText,
  requirePassword = false,
  passwordPlaceholder = 'Enter password'
}) => {
  const [isClosing, setIsClosing] = useState(false);
  const [isVisible, setIsVisible] = useState(false);
  const [password, setPassword] = useState('');

  useEffect(() => {
    if (visible) {
      setIsVisible(true);
      setIsClosing(false);
      setPassword('');
    }
  }, [visible]);

  const handleClose = () => {
    setIsClosing(true);
    setTimeout(() => {
      setIsVisible(false);
      onClose();
    }, 300);
  };

  const handleDownload = () => {
    setIsClosing(true);
    setTimeout(() => {
      setIsVisible(false);
      onDownload?.(password);
    }, 300);
  };

  const handleLogin = () => {
    setIsClosing(true);
    setTimeout(() => {
      setIsVisible(false);
      onLogin?.();
    }, 300);
  };

  const handleSkip = () => {
    setIsClosing(true);
    setTimeout(() => {
      setIsVisible(false);
      onSkip();
    }, 300);
  };

  if (!isVisible) return null;

  const dialogContent = (
    <div 
      className="fixed inset-0 z-[1050] flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}
      onClick={handleClose}
    >
      <div
        className={`bg-white border-4 border-black p-6 max-w-lg w-full shadow-pop-lg transition-all duration-300 ${
          isClosing ? 'opacity-0 scale-95' : 'opacity-100 scale-100'
        }`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between mb-4">
          <h2 className="font-display text-xl font-bold">
            {title}
          </h2>
          <button
            onClick={handleClose}
            className="text-gray-500 hover:text-black font-bold text-xl leading-none"
          >
            ×
          </button>
        </div>
        
        <p className="mb-4 text-gray-700 font-body">
          {message.replace('{count}', String(recordCount))}
        </p>
        
        {requirePassword && (
          <div className="mb-4">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={passwordPlaceholder}
              className="w-full border-4 border-black p-2 font-display"
            />
          </div>
        )}
        
        <div className="flex gap-3">
          {mode === 'download' && (
            <>
              <PopButton
                themeColor={themeColor}
                onClick={handleDownload}
                className="flex-1"
              >
                {downloadText || 'Download'}
              </PopButton>
              <PopButton
                variant="secondary"
                onClick={handleSkip}
                className="flex-1"
              >
                {skipText}
              </PopButton>
            </>
          )}
          {mode === 'login' && (
            <>
              <PopButton
                themeColor={themeColor}
                onClick={handleLogin}
                className="flex-1"
              >
                {loginText || 'Login'}
              </PopButton>
              <PopButton
                variant="secondary"
                onClick={handleSkip}
                className="flex-1"
              >
                {skipText}
              </PopButton>
            </>
          )}
        </div>
      </div>
    </div>
  );

  return createPortal(dialogContent, document.body);
};
