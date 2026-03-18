import React from 'react';
import { ViewState } from '../../types';

interface ResetStatus {
  success?: boolean;
  message?: string;
}

interface PopPasswordResetDialogProps {
  isOpen: boolean;
  resetEmail: string | null;
  newPassword: string;
  confirmPassword: string;
  resetStatus: ResetStatus | null;
  isResetting: boolean;
  onNewPasswordChange: (password: string) => void;
  onConfirmPasswordChange: (password: string) => void;
  onReset: () => void;
  onClose: () => void;
  themeColor: string;
  language: string;
  t: Record<string, string>;
}

export const PopPasswordResetDialog: React.FC<PopPasswordResetDialogProps> = ({
  isOpen,
  resetEmail,
  newPassword,
  confirmPassword,
  resetStatus,
  isResetting,
  onNewPasswordChange,
  onConfirmPasswordChange,
  onReset,
  onClose,
  themeColor,
  language,
  t
}) => {
  if (!isOpen) return null;

  return (
    <div className="min-h-screen flex items-center justify-center bg-paper p-4">
      <div className="bg-white border-4 border-black p-8 max-w-md w-full shadow-pop">
        <h1 className="font-display text-3xl mb-6 text-center">
          POP<span style={{ color: themeColor }}>SMOKE</span>
        </h1>
        
        <h2 className="font-bold text-xl mb-4">
          {t.resetPassword || '重置密码'}
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
            {language === 'zh' ? '密码要求：至少6位字符' : 
             language === 'ja' ? 'パスワード要件：6文字以上' :
             language === 'ko' ? '비밀번호 요구사항: 6자 이상' :
             'Password requirements: at least 6 characters'}
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
            <label className="block font-bold mb-1">{t.newPassword || '新密码'}</label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => onNewPasswordChange(e.target.value)}
              className="w-full border-4 border-black p-2 font-display"
              placeholder={t.passwordPlaceholder || '请输入密码'}
            />
          </div>
          
          <div>
            <label className="block font-bold mb-1">{t.confirmPassword || '确认密码'}</label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => onConfirmPasswordChange(e.target.value)}
              className="w-full border-4 border-black p-2 font-display"
              placeholder={t.passwordPlaceholder || '请再次输入密码'}
            />
          </div>
          
          <button
            onClick={onReset}
            disabled={isResetting}
            className="w-full bg-black text-white font-bold py-3 border-4 border-black hover:bg-white hover:text-black transition-colors disabled:opacity-50"
          >
            {isResetting ? t.loading || 'Loading...' : (t.confirm || '确认')}
          </button>
          
          <button
            onClick={onClose}
            className="w-full text-center text-sm text-gray-600 hover:underline mt-2"
          >
            {t.cancel || '取消'}
          </button>
        </div>
      </div>
    </div>
  );
};
