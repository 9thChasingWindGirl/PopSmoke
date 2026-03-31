import React, { useState, useEffect } from 'react';
import { PopCard } from '../ui/PopCard';
import { PopButton } from '../ui/PopButton';
import { PopConfirm } from '../ui/PopConfirm';
import { PopPrompt } from '../ui/PopPrompt';
import { PopForm } from '../ui/PopForm';
import { PopSupabaseGuide } from '../ui/PopSupabaseGuide';
import { TRANSLATIONS } from '../../i18n';
import { AppSettings, ApiSettings, User, EncryptedApiSettings } from '../../types';
import { apiService, saveFeishuApiSettings, createSupabaseClient, getSupabaseClient, setSupabaseClient, persistSupabaseRuntimeConfig, clearPersistedSupabaseRuntimeConfig, decryptSupabaseConfig } from '../../services/apiService';
import { isWebPlatform, isAndroidPlatform, simpleEncrypt, simpleDecrypt, getStorageAdapter } from '../../services/storageAdapter';
import { POP_COMPONENT_STYLES, getApiManagementPadding, getApiManagementWrapper } from '../../styles';

interface PopAPIProps {
  settings: AppSettings;
  onSave: (newSettings: AppSettings) => void;
  refreshLogs: () => void;
  user: User | null;
}

export const PopAPI: React.FC<PopAPIProps> = ({ settings }) => {
  const t = TRANSLATIONS[settings.language];
  
  const isAndroid = isAndroidPlatform();
  
  const shouldShowSupabaseGuide = () => isAndroid || (isWebPlatform() && import.meta.env.DEV);
  
  const [apiSettings, setApiSettings] = useState<ApiSettings>({
    feishu: {
      apiUrl: ''
    },
    supabase: {
      apiUrl: '',
      anonKey: ''
    }
  });
  
  // 编辑模式下的API设置，使用独立的状态变量
  const [editApiSettings, setEditApiSettings] = useState<ApiSettings>({
    feishu: {
      apiUrl: ''
    },
    supabase: {
      apiUrl: '',
      anonKey: ''
    }
  });
  
  const [savedApiSettings, setSavedApiSettings] = useState<EncryptedApiSettings | null>(null);
  
  const [securityPassword, setSecurityPassword] = useState('');
  const [confirmSecurityPassword, setConfirmSecurityPassword] = useState('');
  const [showPasswordDialog, setShowPasswordDialog] = useState(false);
  const [showViewPasswordDialog, setShowViewPasswordDialog] = useState(false);
  const [viewPassword, setViewPassword] = useState('');
  const [showApiSettings, setShowApiSettings] = useState(false);
  const [decryptedApiSettings, setDecryptedApiSettings] = useState<ApiSettings | null>(null);
  
  const [isSaving, setIsSaving] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isRenaming, setIsRenaming] = useState(false);
  const [renamePassword, setRenamePassword] = useState('');
  
  // 自定义提示框状态
  const [showAlert, setShowAlert] = useState(false);
  const [alertMessage, setAlertMessage] = useState('');
  const [alertType, setAlertType] = useState<'warning' | 'error'>('warning');
  
  // 显示自定义提示框
  const showCustomAlert = (message: string, type: 'warning' | 'error' = 'warning') => {
    setAlertMessage(message);
    setAlertType(type);
    setShowAlert(true);
  };
  
  useEffect(() => {
    loadSavedApiSettings();
  }, []);
  
  const loadSavedApiSettings = async () => {
    const adapter = getStorageAdapter();
    const saved = await adapter.getApiSettings();
    setSavedApiSettings(saved);
  };
  
  const maskApiUrl = (url: string): string => {
    if (!url) return '';
    const parts = url.split('://');
    if (parts.length !== 2) return url;
    const protocol = parts[0];
    const domainAndPath = parts[1];
    const domainParts = domainAndPath.split('/');
    const domain = domainParts[0];
    const path = domainParts.slice(1).join('/');
    
    let maskedDomain = domain;
    if (domain.length > 6) {
      maskedDomain = domain.substring(0, 3) + '...' + domain.substring(domain.length - 3);
    }
    
    let maskedPath = path;
    if (path.length > 10) {
      maskedPath = path.substring(0, 5) + '...' + path.substring(path.length - 5);
    }
    
    return `${protocol}://${maskedDomain}/${maskedPath}`;
  };
  
  const handleApiSettingsChange = (service: 'feishu' | 'supabase', key: keyof any, value: any) => {
    const newSettings = {
      ...apiSettings,
      [service]: {
        ...apiSettings[service],
        [key]: value
      }
    };
    setApiSettings(newSettings);
  };
  
  const handleSaveApiSettings = () => {
    // 如果是在编辑模式下，直接进入密码验证流程
    if (isEditing) {
      setShowPasswordDialog(true);
      return;
    }
    // 如果已经存在保存的API设置，提示用户在已保存的API设置中验证安全密码进行修改
    if (savedApiSettings) {
      showCustomAlert(t.apiSettingsVerifyPassword);
      return;
    }
    setShowPasswordDialog(true);
  };
  
  const confirmSaveApiSettings = async (password?: string, confirmPwd?: string) => {
    const pwd = password || securityPassword;
    const confirm = confirmPwd || confirmSecurityPassword;
    
    if (pwd !== confirm) {
      showCustomAlert(t.passwordMismatch, 'error');
      return;
    }
    
    if (!pwd.trim()) {
      showCustomAlert(t.enterPassword, 'error');
      return;
    }
    
    setIsSaving(true);
    
    try {
      // 确定使用哪个API设置
      const currentSettings = isEditing ? editApiSettings : apiSettings;
      
      // Android 端：设置 Supabase 客户端
      if (isAndroidPlatform() && currentSettings.supabase.apiUrl && currentSettings.supabase.anonKey) {
        try {
          const client = createSupabaseClient(currentSettings.supabase.apiUrl, currentSettings.supabase.anonKey);
          setSupabaseClient(client, currentSettings.supabase);
          await persistSupabaseRuntimeConfig(currentSettings.supabase.apiUrl, currentSettings.supabase.anonKey);
          console.log('Supabase client initialized successfully');
        } catch (error) {
          console.error('Failed to initialize Supabase client:', error);
        }
      }
      
      const encryptedSettings: EncryptedApiSettings = {
        feishu: simpleEncrypt(JSON.stringify(currentSettings.feishu), pwd),
        supabase: simpleEncrypt(JSON.stringify(currentSettings.supabase), pwd),
        securityPassword: simpleEncrypt(pwd, pwd)
      };
      
      const adapter = getStorageAdapter();
      await adapter.saveApiSettings(encryptedSettings);
      setSavedApiSettings(encryptedSettings);
      
      // 同时更新localStorage中的飞书API设置，确保分析页的云同步功能可以使用
      if (currentSettings.feishu.apiUrl) {
        saveFeishuApiSettings(currentSettings.feishu);
      }
      
      setShowPasswordDialog(false);
      setSecurityPassword('');
      setConfirmSecurityPassword('');
      setIsEditing(false);
      // 清空输入框，确保输入框只用于输入，不显示保存的API地址
      setApiSettings({
        feishu: {
          apiUrl: ''
        },
        supabase: {
          apiUrl: currentSettings.supabase.apiUrl,
          anonKey: currentSettings.supabase.anonKey
        }
      });
      // 清空编辑模式的输入框
      setEditApiSettings({
        feishu: {
          apiUrl: ''
        },
        supabase: {
          apiUrl: currentSettings.supabase.apiUrl,
          anonKey: currentSettings.supabase.anonKey
        }
      });
    } catch (error) {
      console.error('Failed to save API settings:', error);
    } finally {
      setIsSaving(false);
    }
  };

  const handleViewClick = () => {
    if (!savedApiSettings) {
      showCustomAlert(t.apiConfigRequired, 'error');
      return;
    }
    setShowViewPasswordDialog(true);
  };

  const confirmViewPassword = async (password?: string) => {
    const pwd = password || viewPassword;
    if (!savedApiSettings) return;
    
    try {
      const decryptedSecurityPassword = simpleDecrypt(savedApiSettings.securityPassword || '', pwd);
      if (decryptedSecurityPassword !== pwd) {
        showCustomAlert(t.currentPasswordIncorrect || 'Incorrect password', 'error');
        return;
      }
      
      const decryptedFeishu = JSON.parse(simpleDecrypt(savedApiSettings.feishu || '', pwd));
      const decryptedSupabase = JSON.parse(simpleDecrypt(savedApiSettings.supabase || '', pwd));
      
      // Android 端：设置 Supabase 客户端
      if (isAndroidPlatform() && decryptedSupabase.apiUrl && decryptedSupabase.anonKey) {
        try {
          const client = createSupabaseClient(decryptedSupabase.apiUrl, decryptedSupabase.anonKey);
          setSupabaseClient(client, decryptedSupabase);
          await persistSupabaseRuntimeConfig(decryptedSupabase.apiUrl, decryptedSupabase.anonKey);
          console.log('Supabase client initialized successfully');
        } catch (error) {
          console.error('Failed to initialize Supabase client:', error);
        }
      }
      
      setDecryptedApiSettings({
        feishu: decryptedFeishu,
        supabase: decryptedSupabase
      });
      
      setShowViewPasswordDialog(false);
      setShowApiSettings(true);
    } catch (error) {
      console.error('Failed to decrypt API settings:', error);
      showCustomAlert(t.currentPasswordIncorrect || 'Incorrect password', 'error');
    }
  };

  const handleEditClick = () => {
    if (!savedApiSettings || !decryptedApiSettings) return;
    // 进入编辑模式，但保持输入框为空，避免显示已保存的API地址
    setIsEditing(true);
  };

  const handleDeleteClick = () => {
    setIsDeleting(true);
  };

  const confirmDeleteApiSettings = async (password?: string) => {
    const pwd = password || viewPassword;
    if (!savedApiSettings) return;
    
    try {
      const decryptedSecurityPassword = simpleDecrypt(savedApiSettings.securityPassword || '', pwd);
      if (decryptedSecurityPassword !== pwd) {
        showCustomAlert(t.currentPasswordIncorrect || 'Incorrect password', 'error');
        return;
      }
      
      const adapter = getStorageAdapter();
      await adapter.deleteApiSettings();
      await clearPersistedSupabaseRuntimeConfig();
      setSavedApiSettings(null);
      setShowApiSettings(false);
      setViewPassword('');
      setDecryptedApiSettings(null);
      setIsDeleting(false);
    } catch (error) {
      console.error('Failed to delete API settings:', error);
      showCustomAlert(t.currentPasswordIncorrect || 'Incorrect password', 'error');
    }
  };

  const handleRenameClick = () => {
    setIsRenaming(true);
  };

  const confirmRenameApiSettings = async (password?: string) => {
    const pwd = password || renamePassword;
    if (!savedApiSettings || !decryptedApiSettings) return;
    
    try {
      const decryptedSecurityPassword = simpleDecrypt(savedApiSettings.securityPassword || '', pwd);
      if (decryptedSecurityPassword !== pwd) {
        showCustomAlert(t.currentPasswordIncorrect || 'Incorrect password', 'error');
        return;
      }
      
      // 这里可以添加重命名逻辑，例如更新存储的 API 设置名称
      // 由于当前存储结构中没有名称字段，我们可以通过重新保存来实现
      const encryptedSettings: EncryptedApiSettings = {
        feishu: simpleEncrypt(JSON.stringify(decryptedApiSettings.feishu), pwd),
        supabase: simpleEncrypt(JSON.stringify(decryptedApiSettings.supabase), pwd),
        securityPassword: simpleEncrypt(pwd, pwd)
      };
      
      const adapter = getStorageAdapter();
      await adapter.saveApiSettings(encryptedSettings);
      setSavedApiSettings(encryptedSettings);
      setIsRenaming(false);
      setRenamePassword('');
    } catch (error) {
      console.error('Failed to rename API settings:', error);
      showCustomAlert(t.currentPasswordIncorrect || 'Incorrect password', 'error');
    }
  };

  return (
    <div className={POP_COMPONENT_STYLES.apiManagement.mainContainer} style={getApiManagementPadding()}>
      <div className={POP_COMPONENT_STYLES.apiManagement.contentWrapper} style={getApiManagementWrapper()}>
      <PopCard title={t.apiManagement}>
        <div className="space-y-6 mt-2">
          <div>
            <h3 className="font-bold mb-4">{t.feishuApi}</h3>
            <div className="space-y-4">
              <div>
                <input
                  type="text"
                  value={apiSettings.feishu.apiUrl}
                  onChange={(e) => handleApiSettingsChange('feishu', 'apiUrl', e.target.value)}
                  className="w-full border-4 border-black p-2 font-display"
                  placeholder={t.apiUrl}
                />
              </div>
            </div>
          </div>
          
          {shouldShowSupabaseGuide() && (
            <div>
              <div className="flex justify-between items-center mb-4">
                <h3 className="font-bold">Supabase API</h3>
                <PopSupabaseGuide themeColor={settings.themeColor} language={settings.language} />
              </div>
              <div className="space-y-4">
                <div>
                  <label className="block font-bold mb-2">{t.apiUrl}</label>
                  <input
                    type="text"
                    value={apiSettings.supabase.apiUrl}
                    onChange={(e) => handleApiSettingsChange('supabase', 'apiUrl', e.target.value)}
                    className="w-full border-4 border-black p-2 font-display"
                    placeholder={t.apiUrl}
                  />
                </div>

                <div>
                  <label className="block font-bold mb-2">{t.anonKey}</label>
                  <input
                    type="password"
                    value={apiSettings.supabase.anonKey}
                    onChange={(e) => handleApiSettingsChange('supabase', 'anonKey', e.target.value)}
                    className="w-full border-4 border-black p-2 font-display"
                    placeholder={t.anonKey}
                  />
                </div>
              </div>
            </div>
          )}
          
          <div>
            <PopButton
              onClick={handleSaveApiSettings}
              disabled={isSaving}
              themeColor={settings.themeColor}
              className="w-full"
            >
              {isSaving ? t.loading : t.saveApiSettings}
            </PopButton>
          </div>
        </div>
      </PopCard>
      
      {savedApiSettings && (
        <PopCard title={t.apiUrlDisplay} className="mb-8">
          <div className="space-y-4 mt-2">
            {!showApiSettings ? (
              <div className="space-y-4">
                <div>
                  <h3 className="font-bold mb-2">{t.feishuApi}</h3>
                  <div className="bg-gray-100 p-3 border-2 border-black rounded">
                    <p className="mb-2"><span className="font-bold">{t.apiUrl}:</span> {maskApiUrl(decryptedApiSettings?.feishu?.apiUrl || '')}</p>
                  </div>
                </div>
                
                {shouldShowSupabaseGuide() && (
                  <div>
                    <h3 className="font-bold mb-2">Supabase API</h3>
                    <div className="bg-gray-100 p-3 border-2 border-black rounded">
                      <p className="mb-2"><span className="font-bold">{t.apiUrl}:</span> {maskApiUrl(decryptedApiSettings?.supabase?.apiUrl || '')}</p>
                      <p><span className="font-bold">{t.anonKey}:</span> {'••••••••••••••••'}</p>
                    </div>
                  </div>
                )}
                <PopButton
                  onClick={handleViewClick}
                  themeColor={settings.themeColor}
                  className="w-full"
                >
                  {t.viewSavedApiSettings}
                </PopButton>
              </div>
            ) : (
              <div className="space-y-4">
                {isEditing ? (
                  <div>
                    <h3 className="font-bold mb-4">{t.feishuApi}</h3>
                    <div className="space-y-4">
                      <div>
                        <input
                          type="text"
                          value={editApiSettings.feishu.apiUrl}
                          onChange={(e) => setEditApiSettings(prev => ({
                            ...prev,
                            feishu: {
                              ...prev.feishu,
                              apiUrl: e.target.value
                            }
                          }))}
                          className="w-full border-4 border-black p-2 font-display"
                          placeholder={t.apiUrl}
                        />
                      </div>
                    </div>
                    
                    {shouldShowSupabaseGuide() && (
                      <div>
                        <h3 className="font-bold mb-4">Supabase API</h3>
                        <div className="space-y-4">
                          <div>
                            <label className="block font-bold mb-2">{t.apiUrl}</label>
                            <input
                              type="text"
                              value={editApiSettings.supabase.apiUrl}
                              onChange={(e) => setEditApiSettings(prev => ({
                                ...prev,
                                supabase: {
                                  ...prev.supabase,
                                  apiUrl: e.target.value
                                }
                              }))}
                              className="w-full border-4 border-black p-2 font-display"
                              placeholder={t.apiUrl}
                            />
                          </div>
                          
                          <div>
                            <label className="block font-bold mb-2">{t.anonKey}</label>
                            <input
                              type="password"
                              value={editApiSettings.supabase.anonKey}
                              onChange={(e) => setEditApiSettings(prev => ({
                                ...prev,
                                supabase: {
                                  ...prev.supabase,
                                  anonKey: e.target.value
                                }
                              }))}
                              className="w-full border-4 border-black p-2 font-display"
                              placeholder={t.anonKey}
                            />
                          </div>
                        </div>
                      </div>
                    )}
                    
                    <div className="flex flex-col gap-2 sm:flex-row sm:gap-3">
                      <PopButton
                        onClick={handleSaveApiSettings}
                        disabled={isSaving}
                        themeColor={settings.themeColor}
                        className="flex-1"
                      >
                        {isSaving ? t.loading : t.saveApiSettings}
                      </PopButton>
                      <PopButton
                        onClick={() => {
                          setIsEditing(false);
                          // 清空编辑模式的输入框
                          setEditApiSettings({
                            feishu: {
                              apiUrl: ''
                            },
                            supabase: {
                              apiUrl: '',
                              anonKey: ''
                            }
                          });
                        }}
                        themeColor="#e0e0e0"
                        className="flex-1"
                      >
                        {t.cancel}
                      </PopButton>
                    </div>
                  </div>
                ) : (
                  <>
                    <div>
                      <h3 className="font-bold mb-2">{t.feishuApi}</h3>
                      <div className="bg-gray-100 p-3 border-2 border-black rounded">
                        <p className="mb-2"><span className="font-bold">{t.apiUrl}:</span> {decryptedApiSettings?.feishu?.apiUrl || t.apiConfigRequired}</p>
                      </div>
                    </div>
                    
                    {shouldShowSupabaseGuide() && (
                      <div>
                        <h3 className="font-bold mb-2">Supabase API</h3>
                        <div className="bg-gray-100 p-3 border-2 border-black rounded">
                          <p className="mb-2"><span className="font-bold">{t.apiUrl}:</span> {decryptedApiSettings?.supabase?.apiUrl || t.apiConfigRequired}</p>
                          <p><span className="font-bold">{t.anonKey}:</span> {decryptedApiSettings?.supabase?.anonKey || t.apiConfigRequired}</p>
                        </div>
                      </div>
                    )}
                    <div className="flex flex-col gap-2 sm:flex-row sm:gap-3">
                      <PopButton
                        onClick={handleEditClick}
                        themeColor={settings.themeColor}
                        className="flex-1"
                      >
                        {t.edit}
                      </PopButton>
                      <PopButton
                        onClick={handleRenameClick}
                        themeColor={settings.themeColor}
                        className="flex-1"
                      >
                        {t.reset}
                      </PopButton>
                      <PopButton
                        onClick={handleDeleteClick}
                        themeColor="#ff4444"
                        className="flex-1"
                      >
                        {t.delete}
                      </PopButton>
                    </div>
                  </>
                )}
                <PopButton
                  onClick={() => {
                    setShowApiSettings(false);
                    setViewPassword('');
                    setDecryptedApiSettings(null);
                    setIsEditing(false);
                    // 清空编辑模式的输入框
                    setEditApiSettings({
                      feishu: {
                        apiUrl: ''
                      },
                      supabase: {
                        apiUrl: '',
                        anonKey: ''
                      }
                    });
                  }}
                  themeColor="#e0e0e0"
                  className="w-full"
                >
                  {t.hide}
                </PopButton>
              </div>
            )}
          </div>
        </PopCard>
      )}
      
      {showPasswordDialog && (
        <PopForm
          type="info"
          title={t.saveApiSettings}
          fields={[
            { name: 'password', label: t.securityPassword, type: 'password', placeholder: t.enterPassword },
            { name: 'confirmPassword', label: t.confirmPassword, type: 'password', placeholder: t.confirmPassword }
          ]}
          confirmText={t.confirm}
          cancelText={t.cancel}
          confirmThemeColor={settings.themeColor}
          onConfirm={(values) => {
            confirmSaveApiSettings(values.password, values.confirmPassword);
          }}
          onCancel={() => {
            setShowPasswordDialog(false);
            setSecurityPassword('');
            setConfirmSecurityPassword('');
          }}
        />
      )}

      {showViewPasswordDialog && (
        <PopPrompt
          type="info"
          title={t.securityPassword}
          message=""
          placeholder={t.enterPassword}
          confirmText={t.confirm}
          cancelText={t.cancel}
          confirmThemeColor={settings.themeColor}
          isPassword={true}
          onConfirm={(value) => {
            confirmViewPassword(value);
          }}
          onCancel={() => {
            setShowViewPasswordDialog(false);
            setViewPassword('');
          }}
        />
      )}

      {isDeleting && (
        <PopPrompt
          type="error"
          title={t.deleteApiSettings}
          message={t.confirmDeleteApiSettings}
          placeholder={t.enterPassword}
          confirmText={t.confirm}
          cancelText={t.cancel}
          confirmThemeColor="#ff4444"
          isPassword={true}
          onConfirm={(value) => {
            confirmDeleteApiSettings(value);
          }}
          onCancel={() => {
            setIsDeleting(false);
            setViewPassword('');
          }}
        />
      )}

      {isRenaming && (
        <PopPrompt
          type="info"
          title={t.resetApiSettings}
          message={t.enterNewPassword}
          placeholder={t.newPassword}
          confirmText={t.reset}
          cancelText={t.close}
          confirmThemeColor={settings.themeColor}
          isPassword={true}
          onConfirm={(value) => {
            confirmRenameApiSettings(value);
          }}
          onCancel={() => {
            setIsRenaming(false);
            setRenamePassword('');
          }}
        />
      )}
      
      {showAlert && (
        <PopConfirm
          type={alertType}
          title={alertType === 'error' ? 'Error!' : 'Warning!'}
          message={alertMessage}
          confirmText="OK"
          cancelText={t.cancel}
          confirmThemeColor={alertType === 'error' ? '#FF4d4d' : '#FFC107'}
          onConfirm={() => setShowAlert(false)}
          onCancel={() => setShowAlert(false)}
        />
      )}
      </div>
    </div>
  );
};
