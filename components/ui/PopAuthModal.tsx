import React from 'react';
import { PopNotification } from './PopNotification';

interface PopAuthModalProps {
  isOpen: boolean;
  mode: 'signin' | 'signup';
  email: string;
  password: string;
  error: string | null;
  loading: boolean;
  showErrorNotification: boolean;
  onEmailChange: (email: string) => void;
  onPasswordChange: (password: string) => void;
  onSubmit: () => void;
  onClose: () => void;
  onSwitchMode: (mode: 'signin' | 'signup') => void;
  onSkipLogin: () => void;
  onCloseError: () => void;
  themeColor: string;
  t: Record<string, string>;
}

export const PopAuthModal: React.FC<PopAuthModalProps> = ({
  isOpen,
  mode,
  email,
  password,
  error,
  loading,
  showErrorNotification,
  onEmailChange,
  onPasswordChange,
  onSubmit,
  onClose,
  onSwitchMode,
  onSkipLogin,
  onCloseError,
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
        
        <h2 className="font-bold text-xl mb-4">
          {mode === 'signin' ? t.signIn : t.signUp}
        </h2>
        
        <div className="space-y-4">
          <div>
            <label className="block font-bold mb-1">{t.email}</label>
            <input
              type="email"
              value={email}
              onChange={(e) => onEmailChange(e.target.value)}
              className="w-full border-4 border-black p-2 font-display"
              placeholder="email@example.com"
            />
          </div>
          
          <div>
            <label className="block font-bold mb-1">{t.password}</label>
            <input
              type="password"
              value={password}
              onChange={(e) => onPasswordChange(e.target.value)}
              className="w-full border-4 border-black p-2 font-display"
              placeholder={t.passwordPlaceholder || '••••••••'}
            />
          </div>
          
          <button
            onClick={onSubmit}
            disabled={loading}
            className="w-full bg-black text-white font-bold py-3 border-4 border-black hover:bg-white hover:text-black transition-colors"
          >
            {loading ? t.loading || 'Loading...' : (mode === 'signin' ? t.signIn : t.signUp)}
          </button>
          
          <div className="text-center mt-4">
            <button
              onClick={() => onSwitchMode(mode === 'signin' ? 'signup' : 'signin')}
              className="text-blue-600 hover:underline"
            >
              {mode === 'signin' ? t.noAccount || 'No account?' : t.haveAccount || 'Already have an account?'}
            </button>
          </div>
          
          <div className="border-t-2 border-gray-200 mt-4 pt-4">
            <button
              onClick={onSkipLogin}
              className="w-full text-gray-600 hover:text-black font-medium py-2"
            >
              {t.skipLogin || 'Skip login, use local mode'}
            </button>
          </div>
        </div>
      </div>
      
      {showErrorNotification && error && (
        <PopNotification
          type={error.includes('注册成功') || error.includes('verify') ? 'success' : 'error'}
          title={error.includes('注册成功') || error.includes('verify') ? t.notificationSuccess || 'Success' : t.notificationError || 'Error'}
          message={error}
          onClose={onCloseError}
        />
      )}
    </div>
  );
};
