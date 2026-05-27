import React, { useState } from 'react';
import { ViewState } from '../../types';
import { TRANSLATIONS } from '../../i18n';
import { Language } from '../../types';

interface ResetStatus {
  success?: boolean;
  message?: string;
}

interface PopPasswordResetDialogProps {
  isOpen: boolean;
  onClose: () => void;
  resetEmail?: string | null;
  themeColor?: string;
  language?: string;
}

export const PopPasswordResetDialog: React.FC<PopPasswordResetDialogProps> = ({
  isOpen,
  onClose,
  resetEmail = null,
  themeColor = '#F59E0B',
  language = 'en'
}) => {
  const t = TRANSLATIONS[language as Language];
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [resetStatus, setResetStatus] = useState<ResetStatus | null>(null);
  const [isResetting, setIsResetting] = useState(false);

  if (!isOpen) return null;

  const handleReset = async () => {
    if (newPassword.length < 6) {
      setResetStatus({ success: false, message: t.passwordTooShort || 'Password must be at least 6 characters' });
      return;
    }
    if (newPassword !== confirmPassword) {
      setResetStatus({ success: false, message: t.passwordMismatch || 'Passwords do not match' });
      return;
    }

    setIsResetting(true);
    try {
      // TODO: 实现密码重置逻辑
      setResetStatus({ success: true, message: t.resetSuccess || 'Password reset successfully' });
      setTimeout(() => onClose(), 1500);
    } catch (error) {
      setResetStatus({ success: false, message: t.resetFailed || 'Failed to reset password' });
    } finally {
      setIsResetting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-paper p-4">
      <div className="bg-white border-4 border-black p-8 max-w-md w-full shadow-pop">
        <h1 className="font-display text-3xl mb-6 text-center">
          POP<span style={{ color: themeColor }}>SMOKE</span>
        </h1>
        
        <h2 className="font-bold text-xl mb-4">
          {t.resetPassword || 'Reset Password'}
        </h2>

        {resetEmail && (
          <p className="mb-4 text-sm text-gray-600">
            {language === 'zh' ? `为账户 ${resetEmail} 设置新密码` : 
             language === 'ja' ? `アカウント ${resetEmail} の新しいパスワードを設定` :
             language === 'ko' ? `계정 ${resetEmail}의 새 비밀번호 설정` :
             `Set new password for ${resetEmail}`}
          </p>
        )}
        
        <div className="bg-yellow-50 border-2 border-yellow-400 p-3 rounded mb-4">
          <p className="text-sm text-yellow-800">
            {t.passwordRequirement || 'Password requirements: at least 6 characters'}
          </p>
        </div>
        
        {resetStatus && (
          <div className={`mb-4 p-3 border-2 rounded ${
            resetStatus.success ? 'bg-green-50 border-green-500 text-green-700' : 'bg-red-50 border-red-500 text-red-700'
          }`}>
            {resetStatus.message}
          </div>
        )}
        
        <div className="space-y-4">
          <div>
            <label className="block font-bold mb-1">{t.newPassword || 'New Password'}</label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full border-4 border-black p-2 font-display"
              placeholder={t.passwordPlaceholder || 'Enter password'}
            />
          </div>
          
          <div>
            <label className="block font-bold mb-1">{t.confirmPassword || 'Confirm Password'}</label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full border-4 border-black p-2 font-display"
              placeholder={t.confirmPasswordPlaceholder || 'Confirm password'}
            />
          </div>
          
          <button
            onClick={handleReset}
            disabled={isResetting}
            className="w-full bg-black text-white font-bold py-3 border-4 border-black hover:bg-white hover:text-black transition-colors disabled:opacity-50"
          >
            {isResetting ? (t.loading || 'Loading...') : (t.confirm || 'Confirm')}
          </button>
          
          <button
            onClick={onClose}
            className="w-full text-center text-sm text-gray-600 hover:underline mt-2"
          >
            {t.cancel || 'Cancel'}
          </button>
        </div>
      </div>
    </div>
  );
};
