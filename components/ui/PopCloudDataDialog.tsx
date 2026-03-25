import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { PopButton } from './PopButton';
import { POP_DESIGN_SYSTEM, POP_COMPONENT_STYLES } from '../../styles';
import { SyncDiffResult } from '../../services/apiService';
import { TRANSLATIONS } from '../../i18n';

type DialogMode = 'download' | 'login' | 'sync-diff';

interface PopCloudDataDialogProps {
  visible: boolean;
  mode?: DialogMode;
  recordCount?: number;
  syncDiff?: SyncDiffResult | null;
  onDownload?: (password?: string) => void;
  onLogin?: () => void;
  onSyncConfirm?: () => void;
  onSyncOptionChange?: (option: 'upload' | 'download', value: boolean) => void;
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
  language?: string;
}

export const PopCloudDataDialog: React.FC<PopCloudDataDialogProps> = ({
  visible,
  mode = 'download',
  recordCount = 0,
  syncDiff,
  onDownload,
  onLogin,
  onSyncConfirm,
  onSyncOptionChange,
  onSkip,
  onClose,
  themeColor = POP_DESIGN_SYSTEM.colors.theme.gold,
  title,
  message,
  downloadText,
  loginText,
  skipText,
  requirePassword = false,
  passwordPlaceholder = 'Enter password',
  language = 'en'
}) => {
  const [isClosing, setIsClosing] = useState(false);
  const [isVisible, setIsVisible] = useState(false);
  const [password, setPassword] = useState('');
  const [syncOptions, setSyncOptions] = useState({ upload: true, download: true });
  
  const t = TRANSLATIONS[language as keyof typeof TRANSLATIONS];

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

  const handleSyncOptionChange = (option: 'upload' | 'download', value: boolean) => {
    const newOptions = { ...syncOptions, [option]: value };
    setSyncOptions(newOptions);
    onSyncOptionChange?.(option, value);
  };

  const handleSyncConfirm = () => {
    setIsClosing(true);
    setTimeout(() => {
      setIsVisible(false);
      onSyncConfirm?.();
    }, 300);
  };

  if (!isVisible) return null;

  const dialogContent = (
    <div 
      className={POP_COMPONENT_STYLES.cloudDataDialog.overlay}
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}
      // 点击外部不关闭对话框
      // onClick={handleClose}
    >
      <div
        className={`${POP_COMPONENT_STYLES.cloudDataDialog.content} ${
          isClosing ? 'opacity-0 scale-95' : 'opacity-100 scale-100'
        }`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between mb-4">
          <h2 className={POP_COMPONENT_STYLES.cloudDataDialog.title}>
            {title}
          </h2>
          <button
            onClick={handleClose}
            className={POP_COMPONENT_STYLES.cloudDataDialog.closeButton}
          >
            ×
          </button>
        </div>
        
        <p className={POP_COMPONENT_STYLES.cloudDataDialog.message}>
          {message.replace('{count}', String(recordCount))}
        </p>
        
        {requirePassword && (
          <div>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={passwordPlaceholder}
              className={POP_COMPONENT_STYLES.cloudDataDialog.input}
            />
          </div>
        )}
        
        {mode === 'sync-diff' && syncDiff && (
          <div className="space-y-4 mb-6">
            <div className={POP_COMPONENT_STYLES.cloudDataDialog.diffSummary}>
              <p className="font-bold mb-2">{t.syncDiffSummary}</p>
              <p>{t.localRecords}: {syncDiff.diff.totalLocal}</p>
              <p>{t.cloudRecords}: {syncDiff.diff.totalCloud}</p>
              {syncDiff.diff.localOnly.length > 0 && (
                <p className="text-green-600">{t.localOnly}: {syncDiff.diff.localOnly.length}</p>
              )}
              {syncDiff.diff.cloudOnly.length > 0 && (
                <p className="text-blue-600">{t.cloudOnly}: {syncDiff.diff.cloudOnly.length}</p>
              )}
              {syncDiff.diff.conflicting.length > 0 && (
                <p className="text-red-600">{t.conflicts}: {syncDiff.diff.conflicting.length}</p>
              )}
            </div>
            
            {syncDiff.diff.cloudOnly.length > 0 && (
              <div>
                <h4 className="font-bold mb-2">{t.cloudOnlyRecords}</h4>
                <div className={POP_COMPONENT_STYLES.cloudDataDialog.diffList}>
                  {syncDiff.diff.cloudOnly.slice(0, 5).map((log, index) => (
                    <div key={index} className={POP_COMPONENT_STYLES.cloudDataDialog.diffItem}>
                      <p>{new Date(log.timestamp).toLocaleString()}</p>
                    </div>
                  ))}
                  {syncDiff.diff.cloudOnly.length > 5 && (
                    <p className={POP_COMPONENT_STYLES.cloudDataDialog.diffMore}>... and {syncDiff.diff.cloudOnly.length - 5} more</p>
                  )}
                </div>
              </div>
            )}
          </div>
        )}
        
        <div className={POP_COMPONENT_STYLES.cloudDataDialog.buttonGroup}>
          {mode === 'download' && (
            <>
              <PopButton
                themeColor={themeColor}
                onClick={handleDownload}
                className={POP_COMPONENT_STYLES.cloudDataDialog.button}
              >
                {downloadText || 'Download'}
              </PopButton>
              <PopButton
                variant="secondary"
                onClick={handleSkip}
                className={POP_COMPONENT_STYLES.cloudDataDialog.button}
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
                className={POP_COMPONENT_STYLES.cloudDataDialog.button}
              >
                {loginText || 'Login'}
              </PopButton>
              <PopButton
                variant="secondary"
                onClick={handleSkip}
                className={POP_COMPONENT_STYLES.cloudDataDialog.button}
              >
                {skipText}
              </PopButton>
            </>
          )}
          {mode === 'sync-diff' && syncDiff?.source === 'supabase' && (
            <>
              <PopButton
                themeColor={syncOptions.upload ? themeColor : '#e0e0e0'}
                onClick={() => handleSyncOptionChange('upload', !syncOptions.upload)}
                className={`${POP_COMPONENT_STYLES.cloudDataDialog.button} ${syncOptions.upload ? 'border-4' : 'border-2'}`}
              >
                {t.upload} ({syncDiff.diff.localOnly.length})
              </PopButton>
              <PopButton
                themeColor={syncOptions.download ? themeColor : '#e0e0e0'}
                onClick={() => handleSyncOptionChange('download', !syncOptions.download)}
                className={`${POP_COMPONENT_STYLES.cloudDataDialog.button} ${syncOptions.download ? 'border-4' : 'border-2'}`}
              >
                {t.download} ({syncDiff?.diff.cloudOnly.length || 0})
              </PopButton>
            </>
          )}
        </div>
        
        {mode === 'sync-diff' && (
          <div>
            <PopButton
              themeColor={themeColor}
              onClick={handleSyncConfirm}
              className={POP_COMPONENT_STYLES.cloudDataDialog.confirmButton}
            >
              {t.confirmSync}
            </PopButton>
          </div>
        )}
      </div>
    </div>
  );

  return createPortal(dialogContent, document.body);
};
