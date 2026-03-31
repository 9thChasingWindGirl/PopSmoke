import React, { useState, useEffect, useRef } from 'react';
import { PopCard } from '../ui/PopCard';
import { PopButton } from '../ui/PopButton';
import { PopColorPicker } from '../ui/PopColorPicker';
import { PopDropdown, PopDropdownItem } from '../ui/PopDropdown';
import { PopNotification } from '../ui/PopNotification';
import { PopConfirm } from '../ui/PopConfirm';
import { PopForm } from '../ui/PopForm';
import { PopExternalLinkWarning } from '../ui/PopExternalLinkWarning';
import { PopLoading } from '../ui/PopLoading';
import { THEME_PRESETS } from '../../constants';
import { TRANSLATIONS } from '../../i18n';
import { AppSettings, User, SmokeLog, OperationLog as OperationLogType } from '../../types';
import { POP_COMPONENT_STYLES } from '../../styles/componentStyles';
import { isLightColor } from '../../utils/colorUtils';

import { authService } from '../../services/authService';
import { getSupabaseClient } from '../../services/apiService';
import { avatarCacheService, isAndroidPlatform } from '../../services/storageAdapter';
import { getStorageType, getStorageAdapter } from '../../services/storageAdapter';

interface PopSettingsProps {
  settings: AppSettings;
  onSave: (newSettings: AppSettings) => void;
  user: User | null;
  onSignOut: () => void;
  onNavigateToAuth?: () => void;
  onNavigateToDashboard?: () => void;
  onRefreshLogs?: (logs: SmokeLog[]) => void;
  onAddOperationLog?: (log: OperationLogType) => void;
  onOpenSystemLog?: () => void;
}

export const PopSettings: React.FC<PopSettingsProps> = ({ settings, onSave, user, onSignOut, onNavigateToAuth, onNavigateToDashboard, onRefreshLogs, onAddOperationLog, onOpenSystemLog }) => {
  const t = TRANSLATIONS[settings.language];
  
  const textColor = isLightColor(settings.themeColor) ? '#000000' : '#FFFFFF';
  
  // 语言和主题选择状态
  const [showLanguageMenu, setShowLanguageMenu] = useState(false);
  const [showThemeMenu, setShowThemeMenu] = useState(false);
  const languageMenuRef = useRef<HTMLDivElement>(null);
  const themeMenuRef = useRef<HTMLDivElement>(null);
  
  // 页面状态
  const [activePage, setActivePage] = useState<'main' | 'account'>('main');
  
  // 账户管理状态
  const [avatarUrl, setAvatarUrl] = useState<string>(settings.avatarUrl || '');
  const [displayAvatarUrl, setDisplayAvatarUrl] = useState<string>(settings.avatarUrl || '');
  const [isUploading, setIsUploading] = useState(false);
  const [isResetting, setIsResetting] = useState(false);

  // 加载缓存的头像
  useEffect(() => {
    if (user && avatarUrl) {
      const loadCachedAvatar = async () => {
        try {
          const cachedAvatar = await avatarCacheService.getCachedAvatar(user.id);
          if (cachedAvatar) {
            setDisplayAvatarUrl(cachedAvatar);
          } else {
            // 没有缓存，预加载头像
            const preloadedAvatar = await avatarCacheService.preloadAvatar(user.id, avatarUrl);
            setDisplayAvatarUrl(preloadedAvatar);
          }
        } catch (error) {
          console.error('Failed to load avatar:', error);
          // 加载失败时使用原始URL
          setDisplayAvatarUrl(avatarUrl);
        }
      };
      
      loadCachedAvatar();
    }
  }, [user, avatarUrl]);
  const [email, setEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [accountStatus, setAccountStatus] = useState<{ success?: boolean; message?: string } | null>(null);
  const [showSignOutDialog, setShowSignOutDialog] = useState(false);
  const [showPasswordResetDialog, setShowPasswordResetDialog] = useState(false);
  const [showClearDataDialog, setShowClearDataDialog] = useState(false);
  const [storageInfo, setStorageInfo] = useState<{ used: number; total: number; percentage: number } | null>(null);
  const [externalLinkUrl, setExternalLinkUrl] = useState<string | null>(null);
  
  const triggerVibration = () => {
    if (window.navigator.vibrate) {
      window.navigator.vibrate([100, 50, 100]);
    }
  };
  
  const analysisRef = useRef<HTMLDivElement>(null);
  
  // 点击外部关闭下拉菜单
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (languageMenuRef.current && !languageMenuRef.current.contains(event.target as Node)) {
        setShowLanguageMenu(false);
      }
      if (themeMenuRef.current && !themeMenuRef.current.contains(event.target as Node)) {
        setShowThemeMenu(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // 处理设置变化
  const handleChange = (key: keyof AppSettings, value: any) => {
    onSave({ ...settings, [key]: value });
  };
  

  
  // 处理头像上传
  const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    
    setIsUploading(true);
    
    try {
      // 生成唯一文件名
      const fileName = `${user?.id}-${Date.now()}-${file.name}`;
      
      // 上传文件到Supabase Storage
      const client = await getSupabaseClient();
      const { data, error } = await client.storage
        .from('avatars')
        .upload(fileName, file, {
          cacheControl: '3600',
          upsert: false
        });
      
      if (error) {
        // 处理存储桶不存在的错误
        if (error.message.includes('Bucket not found')) {
          throw new Error(t.bucketNotExist);
        }
        // 处理权限错误
        if (error.message.includes('permission')) {
          throw new Error(t.permissionDenied);
        }
        throw error;
      }
      
      // 获取文件URL
      const { data: urlData } = client.storage
        .from('avatars')
        .getPublicUrl(data.path);
      
      const newAvatarUrl = urlData.publicUrl;
      setAvatarUrl(newAvatarUrl);
      
      // 清除旧缓存并缓存新头像
      if (user) {
        avatarCacheService.clearCache(user.id);
        await avatarCacheService.cacheAvatar(user.id, newAvatarUrl);
        const cachedAvatar = await avatarCacheService.getCachedAvatar(user.id);
        if (cachedAvatar) {
          setDisplayAvatarUrl(cachedAvatar);
        }
      }
      
      // 保存头像URL到用户设置
      const updatedSettings = { ...settings, avatarUrl: newAvatarUrl };
      onSave(updatedSettings);
      
      setAccountStatus({
        success: true,
        message: t.avatarUploadSuccess
      });
    } catch (error) {
      setAccountStatus({
        success: false,
        message: error instanceof Error ? error.message : t.avatarUploadFailed
      });
    } finally {
      setIsUploading(false);
    }
  };
  
  // 处理邮箱绑定
  const handleEmailUpdate = async () => {
    if (!email.trim()) {
      setAccountStatus({
        success: false,
        message: t.enterEmail
      });
      return;
    }
    
    try {
      const authClient = await getSupabaseClient();
      const { error } = await authClient.auth.updateUser({
        email
      });
      
      if (error) {
        throw error;
      }
      
      setAccountStatus({
        success: true,
        message: t.emailBindSuccess
      });
    } catch (error) {
      setAccountStatus({
        success: false,
        message: error instanceof Error ? error.message : t.emailBindFailed
      });
    }
  };
  
  // 处理密码重置
  const handlePasswordReset = async (pwd?: string, confirmPwd?: string) => {
    const newPwd = pwd || newPassword;
    const confirm = confirmPwd || confirmPassword;
    
    // 验证输入
    if (!newPwd.trim()) {
      setAccountStatus({
        success: false,
        message: t.newPassword ? t.enterNewPassword : 'Please enter new password'
      });
      return;
    }
    
    if (newPwd.length < 6) {
      setAccountStatus({
        success: false,
        message: t.passwordMinLength || 'Password must be at least 6 characters'
      });
      return;
    }
    
    if (newPwd !== confirm) {
      setAccountStatus({
        success: false,
        message: t.passwordMismatch || 'Passwords do not match'
      });
      return;
    }
    
    setIsResetting(true);
    try {
      // 直接更新密码（Supabase会自动验证当前会话）
      const pwdClient = await getSupabaseClient();
      const { error: updateError } = await pwdClient.auth.updateUser({
        password: newPwd
      });
      
      if (updateError) {
        // 处理特定错误
        if (updateError.message.includes('New password should be different')) {
          throw new Error(t.passwordSameAsCurrent);
        }
        throw updateError;
      }
      
      setAccountStatus({
        success: true,
        message: t.passwordResetSuccess || 'Password reset successful'
      });
      setShowPasswordResetDialog(false);
      setNewPassword('');
      setConfirmPassword('');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : (t.passwordResetFailed || 'Password reset failed');
      setAccountStatus({
        success: false,
        message: errorMessage
      });
    } finally {
      setIsResetting(false);
    }
  };
  
  // 处理登出
  const handleSignOut = async () => {
    try {
      // 先关闭对话框
      setShowSignOutDialog(false);
      // 执行登出
      await authService.signOut();
      onSignOut();
      // 导航到记录页
      onNavigateToDashboard?.();
    } catch (error) {
      setAccountStatus({
        success: false,
        message: error instanceof Error ? error.message : t.signOutFailed
      });
    }
  };

  // 获取存储信息
  const getStorageInfo = async () => {
    try {
      const adapter = getStorageAdapter();
      const storageType = getStorageType();
      const logs = await adapter.getLogs();
      const logsSize = JSON.stringify(logs).length;
      const totalSize = logsSize;
      
      let maxSize: number;
      switch (storageType) {
        case 'IndexedDB':
          maxSize = 50 * 1024 * 1024; // 50MB for IndexedDB
          break;
        case 'SQLite':
          maxSize = 100 * 1024 * 1024; // 100MB for SQLite
          break;
        case 'localStorage':
        default:
          maxSize = 5 * 1024 * 1024; // 5MB for localStorage
          break;
      }
      
      const percentage = Math.min((totalSize / maxSize) * 100, 100);
      
      setStorageInfo({
        used: Math.round((totalSize / (1024 * 1024)) * 100) / 100, // 转换为MB并保留两位小数
        total: Math.round((maxSize / (1024 * 1024)) * 100) / 100, // 转换为MB并保留两位小数
        percentage: Math.round(percentage)
      });
    } catch (error) {
      console.error('Failed to get storage info:', error);
    }
  };

  // 组件加载时获取存储信息
  useEffect(() => {
    getStorageInfo();
  }, []);

  // 处理数据清理
  const handleClearData = async () => {
    setIsResetting(true);
    try {
      const adapter = getStorageAdapter();
      await adapter.clearLogsOnly();
      setShowClearDataDialog(false);
      setAccountStatus({
        success: true,
        message: t.dataCleared || 'Data cleared successfully'
      });
      // 刷新存储信息
      await getStorageInfo();
      // 通知父组件刷新日志状态
      if (onRefreshLogs) {
        onRefreshLogs([]);
      }
      // 添加操作日志
      if (onAddOperationLog) {
        const clearLog: OperationLogType = {
          id: `clear_${Date.now()}`,
          type: 'clear',
          data: {
            id: '',
            user_id: user?.id || 'local',
            record_date: new Date().toISOString().split('T')[0],
            record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
            timestamp: Date.now()
          } as SmokeLog,
          syncStatus: 'synced',
          timestamp: Date.now(),
          message: t.clearAll
        };
        onAddOperationLog(clearLog);
      }
    } catch (error) {
      setAccountStatus({
        success: false,
        message: error instanceof Error ? error.message : 'Failed to clear data'
      });
    } finally {
      setIsResetting(false);
    }
  };

  if (isResetting) {
    return <PopLoading settings={settings} status="resetting" isInitialize={false} />;
  }

  return (
    <div className="w-full max-w-md mx-auto space-y-6 pt-8 pb-[calc(80px+env(safe-area-inset-bottom))] md:pb-8">
        {/* 页面导航 */}
        {activePage === 'main' ? (
            <PopCard title={t.settings} className="mb-8">
                <div className="space-y-4 mt-2">
                    {/* 账户管理入口 / 登录按钮 */}
                    {user ? (
                        <PopButton
                            onClick={() => setActivePage('account')}
                            themeColor={settings.themeColor}
                            className="w-full"
                        >
                            {t.accountManagement}
                        </PopButton>
                    ) : (
                        <div className="space-y-2">
                            <p className="text-sm text-gray-600 text-center mb-2">{t.localModeHint || '当前使用本地模式，登录后可同步数据到云端'}</p>
                            <PopButton
                                onClick={() => {
                                  onNavigateToAuth?.();
                                }}
                                themeColor={settings.themeColor}
                                className="w-full"
                            >
                                {t.signIn}
                            </PopButton>
                        </div>
                    )}
                    
                    {/* 阈值设置 */}
                    <div className="mt-6">
                        <h3 className="font-bold mb-4">{t.thresholdSettings}</h3>
                        <div className="space-y-8">
                            <div>
                                <div className="flex justify-between items-end mb-2">
                                    <label className="block font-display text-xl">{t.dailyLimit}</label>
                                    <span className="font-display text-2xl font-bold bg-black text-white px-2 transform -rotate-2">{settings.dailyLimit}</span>
                                </div>
                                <input 
                                    type="range" 
                                    min="1"
                                    max="50"
                                    value={settings.dailyLimit}
                                    onChange={(e) => {
                                        const val = parseInt(e.target.value);
                                        // Ensure warning limit isn't higher than daily limit
                                        if (val < settings.warningLimit) {
                                            onSave({ ...settings, dailyLimit: val, warningLimit: val });
                                        } else {
                                            handleChange('dailyLimit', val);
                                        }
                                    }}
                                    onTouchStart={(e) => e.preventDefault()}
                                    onTouchMove={(e) => e.preventDefault()}
                                    className="w-full h-4 bg-gray-200 rounded-full appearance-none border-2 border-black"
                                />
                            </div>

                            <div>
                                <div className="flex justify-between items-end mb-2">
                                     <label className="block font-display text-xl">{t.warningLimit}</label>
                                     <span className="font-display text-2xl font-bold bg-yellow-400 text-black px-2 border-2 border-black transform rotate-1">{settings.warningLimit}</span>
                                </div>
                                <input 
                                    type="range" 
                                    min="1"
                                    max={settings.dailyLimit}
                                    value={settings.warningLimit}
                                    onChange={(e) => handleChange('warningLimit', parseInt(e.target.value))}
                                    onTouchStart={(e) => e.preventDefault()}
                                    onTouchMove={(e) => e.preventDefault()}
                                    className="w-full h-4 bg-gray-200 rounded-full appearance-none border-2 border-black"
                                />
                            </div>
                        </div>
                    </div>
                    
                    {/* 语言和主题选择 */}
                    <div className="mt-6 space-y-4">
                        <h3 className="font-bold mb-4">{t.language} &amp; {t.themeColor}</h3>
                        
                        {/* 语言选择 */}
                        <PopDropdown
                          ref={languageMenuRef}
                          isOpen={showLanguageMenu}
                          onToggle={() => setShowLanguageMenu(!showLanguageMenu)}
                          trigger={
                            <div className={POP_COMPONENT_STYLES.settings.dropdown.trigger}>
                              <span>{t.language}: {settings.language.toUpperCase()}</span>
                              <span>▼</span>
                            </div>
                          }
                        >
                          <div className="py-1">
                            {(['en', 'zh', 'ja', 'ko'] as const).map((lang) => (
                              <PopDropdownItem
                                key={lang}
                                isActive={settings.language === lang}
                                onClick={() => {
                                  handleChange('language', lang);
                                  setShowLanguageMenu(false);
                                }}
                              >
                                {lang === 'en' && 'English'}
                                {lang === 'zh' && '中文'}
                                {lang === 'ja' && '日本語'}
                                {lang === 'ko' && '한국어'}
                              </PopDropdownItem>
                            ))}
                          </div>
                        </PopDropdown>
                        
                        {/* 主题颜色选择 */}
                        <PopDropdown
                          ref={themeMenuRef}
                          isOpen={showThemeMenu}
                          onToggle={() => setShowThemeMenu(!showThemeMenu)}
                          trigger={
                            <div className={POP_COMPONENT_STYLES.settings.dropdown.trigger}>
                              <span className="flex items-center">
                                {t.theme}: 
                                <div 
                                  className={POP_COMPONENT_STYLES.settings.dropdown.colorPreview} 
                                  style={{ backgroundColor: settings.themeColor }}
                                />
                              </span>
                              <span>▼</span>
                            </div>
                          }
                        >
                          <div className="py-2">
                            {/* 自定义颜色选择 - 置于上方 */}
                            <div className="px-3 pb-3 border-b-2 border-gray-200 mb-3">
                              <label className="block text-sm font-medium text-gray-700 mb-2">{t.customColor}</label>
                              <PopColorPicker
                                value={settings.themeColor}
                                onChange={(color) => handleChange('themeColor', color)}
                                themeColor={settings.themeColor}
                                onClose={() => setShowThemeMenu(false)}
                              />
                            </div>
                            
                            {/* 预设颜色 - 双列布局 */}
                            <div className="px-3">
                              <label className="block text-xs font-medium text-gray-500 mb-2">{t.presetColors || 'Preset Colors'}</label>
                              <div className="grid grid-cols-2 gap-2">
                                {THEME_PRESETS.map(color => (
                                  <button
                                    key={color}
                                    onClick={() => {
                                      handleChange('themeColor', color);
                                      setShowThemeMenu(false);
                                    }}
                                    className={`flex items-center px-2 py-2 hover:bg-gray-100 w-full text-left border-2 transition-colors ${
                                      settings.themeColor === color ? 'border-black bg-gray-50' : 'border-transparent'
                                    }`}
                                  >
                                    <div className="w-6 h-6 border-2 border-black mr-2 flex-shrink-0" style={{ backgroundColor: color }} />
                                    <span className="text-sm font-mono truncate">{color}</span>
                                  </button>
                                ))}
                              </div>
                            </div>
                          </div>
                        </PopDropdown>
                    </div>
                    
                    {/* 数据管理 */}
                    <div className="mt-6">
                        <h3 className="font-bold mb-4">{t.dataManagement || 'Data Management'}</h3>
                        
                        {/* 存储方式 */}
                        <div className="bg-gray-100 border-2 border-black p-3 rounded mb-3">
                            <p className="font-bold mb-1">{t.storageMethod}</p>
                            <p className="font-display text-lg">{getStorageType()}</p>
                        </div>
                        
                        {/* 存储使用情况 */}
                        {storageInfo && (
                            <div className="bg-gray-100 border-2 border-black p-3 rounded mb-3">
                                <div className="flex justify-between items-center mb-2">
                                    <span className="font-bold">{t.storageUsage || 'Storage Usage'}</span>
                                    <span className={`font-display ${storageInfo.percentage > 80 ? 'text-red-600' : 'text-green-600'}`}>
                                        {storageInfo.percentage}%
                                    </span>
                                </div>
                                <div className="w-full h-4 bg-gray-300 border-2 border-black">
                                    <div 
                                        className={`h-full ${storageInfo.percentage > 80 ? 'bg-red-500' : 'bg-green-500'}`}
                                        style={{ width: `${storageInfo.percentage}%` }}
                                    />
                                </div>
                                <p className="text-xs mt-1 text-gray-600">
                                    {storageInfo.used} {t.mb} / {storageInfo.total} {t.mb}
                                </p>
                            </div>
                        )}
                        
                        {/* 清理数据按钮 */}
                        <PopButton
                            onClick={() => {
                              triggerVibration();
                              setShowClearDataDialog(true);
                            }}
                            themeColor="#ff6b6b"
                            className="w-full mb-3"
                        >
                            {t.clearOldData || 'Clear Old Data'}
                        </PopButton>
                        
                        {/* 系统日志按钮 - 仅安卓端显示 */}
                        {isAndroidPlatform() && (
                            <PopButton
                                onClick={() => {
                                    triggerVibration();
                                    onOpenSystemLog?.();
                                }}
                                themeColor="#636e72"
                                className="w-full"
                            >
                                {t.systemLog || 'System Logs'}
                            </PopButton>
                        )}
                    </div>
                </div>
            </PopCard>
        ) : (
            <PopCard title={t.accountManagement} className="mb-8">
                <div className="space-y-6 mt-2">
                    {/* 返回按钮 */}
                    <PopButton
                        onClick={() => setActivePage('main')}
                        themeColor="#e0e0e0"
                        className="w-full mb-4"
                    >
                        {t.back}
                    </PopButton>
                    
                    {/* 用户头像 */}
                    <div className="flex flex-col items-center">
                        <div className="relative mb-4">
                            <div className="w-24 h-24 rounded-full border-4 border-black overflow-hidden">
                                {displayAvatarUrl ? (
                                    <img src={displayAvatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                                ) : (
                                    <div className="w-full h-full bg-gray-200 flex items-center justify-center">
                                        <span className="text-gray-500">{user?.email?.charAt(0).toUpperCase() || 'U'}</span>
                                    </div>
                                )}
                            </div>
                            <input
                                type="file"
                                accept="image/*"
                                onChange={handleAvatarUpload}
                                className="absolute bottom-0 right-0 w-8 h-8 bg-black text-white rounded-full flex items-center justify-center cursor-pointer opacity-0"
                                style={{ zIndex: 1 }}
                            />
                            <div className="absolute bottom-0 right-0 w-8 h-8 bg-black text-white rounded-full flex items-center justify-center cursor-pointer">
                                <span>+</span>
                            </div>
                        </div>
                        <p className="text-sm text-gray-500">{isUploading ? t.uploading : t.uploadAvatar}</p>
                    </div>
                    
                    {/* 邮箱绑定 */}
                    <div>
                        <label className="block font-bold mb-2">{t.email}</label>
                        <input
                            type="email"
                            value={email || user?.email || ''}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full border-4 border-black p-2 font-display"
                            placeholder={t.emailPlaceholder}
                        />
                        <PopButton
                            onClick={handleEmailUpdate}
                            themeColor={settings.themeColor}
                            className="mt-2 w-full"
                        >
                            {t.updateEmail}
                        </PopButton>
                    </div>
                    
                    {/* 密码重置 */}
                    <div>
                        <PopButton
                            onClick={() => setShowPasswordResetDialog(true)}
                            themeColor={settings.themeColor}
                            className="w-full"
                        >
                            {t.resetPassword}
                        </PopButton>
                    </div>
                    
                    {/* 登出 */}
                    <div>
                        <PopButton
                            onClick={() => setShowSignOutDialog(true)}
                            themeColor="#ff4444"
                            className="w-full"
                        >
                            {t.signOut}
                        </PopButton>
                    </div>
                    
                    {/* 账户状态消息 */}
                    {accountStatus && (
                        <PopNotification
                          title={accountStatus.success ? t.notificationSuccess : t.notificationError}
                          message={accountStatus.message || ''}
                          type={accountStatus.success ? 'success' : 'error'}
                          onClose={() => setAccountStatus(null)}
                          duration={3000}
                        />
                    )}
                </div>
            </PopCard>
        )}
        

        
        {/* 密码重置对话框 */}
        {showPasswordResetDialog && (
            <PopForm
                type="info"
                title={t.resetPassword}
                message={settings.language === 'zh' ? t.passwordRequirements : 
                         settings.language === 'ja' ? 'パスワード要件：6文字以上' :
                         settings.language === 'ko' ? '비밀번호 요구사항: 6자 이상' :
                         'Password requirements: at least 6 characters'}
                fields={[
                    { name: 'newPassword', label: t.newPassword, type: 'password', placeholder: t.passwordPlaceholder },
                    { name: 'confirmPassword', label: t.confirmPassword, type: 'password', placeholder: t.passwordPlaceholder }
                ]}
                confirmText={t.confirm}
                cancelText={t.cancel}
                confirmThemeColor={settings.themeColor}
                onConfirm={(values) => {
                    handlePasswordReset(values.newPassword, values.confirmPassword);
                }}
                onCancel={() => {
                    setShowPasswordResetDialog(false);
                    setNewPassword('');
                    setConfirmPassword('');
                }}
            />
        )}
        
        {/* 登出确认对话框 */}
        {showSignOutDialog && (
            <PopConfirm
                type="warning"
                title={t.signOut}
                message={t.signOutConfirm}
                confirmText={t.confirm}
                cancelText={t.cancel}
                confirmThemeColor="#ff4444"
                onConfirm={handleSignOut}
                onCancel={() => setShowSignOutDialog(false)}
            />
        )}
        
        {/* 数据清理确认对话框 */}
        {showClearDataDialog && (
            <PopConfirm
                type="error"
                title={t.clearData || 'Clear Data'}
                message={t.clearDataConfirm || 'This will clear all smoking logs. This action cannot be undone. Are you sure?'}
                confirmText={t.confirm || 'Confirm'}
                cancelText={t.cancel || 'Cancel'}
                confirmThemeColor="#ff4444"
                onConfirm={handleClearData}
                onCancel={() => setShowClearDataDialog(false)}
            />
        )}

        {activePage === 'main' && (
            <PopCard title={t.about} className="mb-8">
                 <div className="flex flex-col space-y-3 mt-2">
                     <button 
                       onClick={() => setExternalLinkUrl('https://blog.whysoserious.dpdns.org/')}
                       className="block w-full text-center border-2 border-black p-3 font-display uppercase hover:bg-black hover:text-white transition-colors shadow-pop-hover"
                     >
                         {t.blog}
                     </button>
                     <button 
                       onClick={() => setExternalLinkUrl('https://github.com/9thChasingWindGirl')}
                       className="block w-full text-center border-2 border-black p-3 font-display uppercase hover:bg-black hover:text-white transition-colors shadow-pop-hover"
                     >
                         {t.github}
                     </button>
                     <p className="text-center text-xs font-bold mt-2 font-display text-gray-500 tracking-widest">ver.0.3.30.beta</p>
                     <p className="text-center text-[10px] font-bold mt-1 font-body text-gray-400">
                         {t.fontAttribution}
                     </p>
                 </div>
            </PopCard>
        )}
        
        {/* 外部链接警示对话框 */}
        {externalLinkUrl && (
            <PopExternalLinkWarning
                url={externalLinkUrl}
                title={t.externalLinkWarningTitle}
                message={t.externalLinkWarningMessage}
                confirmText={t.externalLinkContinue}
                cancelText={t.externalLinkCancel}
                onConfirm={() => {
                    window.open(externalLinkUrl, '_blank', 'noopener,noreferrer');
                    setExternalLinkUrl(null);
                }}
                onCancel={() => setExternalLinkUrl(null)}
            />
        )}
    </div>
  );
};