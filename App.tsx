import React, { useEffect, useRef, useCallback } from 'react';
import { ViewState, SmokeLog } from './types';
import { PopDashboard } from './components/pages/PopDashboard';
import { PopAnalysis } from './components/pages/PopAnalysis';
import { PopHistory } from './components/pages/PopHistory';
import { PopSettings } from './components/pages/PopSettings';
import { PopAPI } from './components/pages/PopAPI';
import { PopAuthModal } from './components/ui/PopAuthModal';
import { PopCloudDataDialog } from './components/ui/PopCloudDataDialog';
import { PopLoading } from './components/ui/PopLoading';
import PopNav from './components/ui/PopNav';
import { PopSystemLog } from './components/ui/PopSystemLog';
import { ErrorBoundary } from './components/ErrorBoundary';
import { TRANSLATIONS } from './i18n';

// 新架构导入
import { useAuthStore } from './stores/authStore';
import { useAppStore } from './stores/appStore';
import { useDataStore } from './stores/dataStore';
import { useInitialization } from './hooks/useInitialization';
import { useDataSync } from './hooks/useDataSync';
import { useCloudData } from './hooks/useCloudData';

export default function App() {
  // 使用新的 Stores
  const {
    view,
    settings,
    setView,
    updateSettings
  } = useAppStore();

  const {
    user,
    error: authError,
    isLoading: isAuthenticating,
    showAuthModal,
    authMode,
    email,
    password,
    login,
    signup,
    logout,
    setShowAuthModal,
    setAuthMode,
    setEmail,
    setPassword,
    operationLogs,
    addOperationLog,
    clearOperationLogs
  } = useAuthStore();

  const {
    logs,
    isLoading: isDataLoading,
    loadLogs,
    addLog,
    updateLog,
    removeLog
  } = useDataStore();

  // 使用自定义 Hooks
  const {
    isInitialized,
    isLoading: isInitLoading,
    initializeApp
  } = useInitialization();

  useDataSync(settings);

  const cloudData = useCloudData();
  const { showCloudDataDialog } = cloudData;

  // UI 状态（保留在组件中）
  const [showSystemLog, setShowSystemLog] = React.useState(false);
  
  // Refs
  const lastRecordTimeRef = useRef<number>(0);

  // 应用初始化
  useEffect(() => {
    if (!isInitialized) {
      initializeApp({
        onSettingsLoaded: updateSettings,
        onLogsLoaded: loadLogs,
        onAuthStateChange: () => {}
      });
    }
  }, [isInitialized, initializeApp, updateSettings, loadLogs]);

  // 处理记录添加（使用 useCallback 优化）
  const handleAddLog = useCallback(async () => {
    const now = Date.now();

    if (now - lastRecordTimeRef.current < 3000) {
      return;
    }

    lastRecordTimeRef.current = now;

    const newLog: SmokeLog = {
      id: `local_${now}_${Math.random().toString(36).substring(2, 11)}`,
      user_id: settings.user_id || 'anonymous',
      record_date: new Date().toISOString().split('T')[0],
      record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
      record_index: 0,
      timestamp: now,
      notes: ''
    };

    await addLog(newLog);
    addOperationLog({
      id: `op_${now}`,
      type: 'create',
      action: 'add_smoke_record',
      data: newLog,
      timestamp: now,
      message: `Added smoke record (${newLog.record_date}, #${logs.length + 1})`
    });
  }, [settings.user_id, addLog, addOperationLog, logs.length]);

  // UI 回调函数（使用 useCallback 优化）
  const handleOpenAuthModal = useCallback(() => setShowAuthModal(true), []);
  const handleShowSystemLog = useCallback(() => setShowSystemLog(true), []);
  
  // 包装 updateLog 以匹配组件期望的签名
  const handleUpdateLog = useCallback((log: SmokeLog) => {
    updateLog(log.id, log);
    addOperationLog({
      id: `op_${Date.now()}`,
      type: 'update',
      action: 'update_smoke_record',
      data: log,
      timestamp: Date.now(),
      message: `Updated record (${log.record_date}, #${log.record_index + 1})`
    });
  }, [updateLog, addOperationLog]);

  // 包装 removeLog 以记录删除操作
  const handleDeleteLog = useCallback((id: string) => {
    const targetLog = logs.find(l => l.id === id);
    removeLog(id);
    addOperationLog({
      id: `op_${Date.now()}`,
      type: 'delete',
      action: 'delete_smoke_record',
      timestamp: Date.now(),
      message: targetLog
        ? `Deleted record (${targetLog.record_date}, #${targetLog.record_index + 1})`
        : `Deleted record (id: ${id})`
    });
  }, [removeLog, addOperationLog, logs]);

  // 渲染加载状态
  if (!isInitialized || isInitLoading || isDataLoading) {
    return <PopLoading settings={settings} />;
  }

  // 根据视图渲染页面
  const renderContent = () => {
    switch (view) {
      case ViewState.DASHBOARD:
        return (
          <PopDashboard
            logs={logs}
            settings={settings}
            onRecord={handleAddLog}
            onUpdate={handleUpdateLog}
            onDelete={handleDeleteLog}
          />
        );
      
      case ViewState.ANALYSIS:
        return (
          <PopAnalysis
            logs={logs}
            settings={settings}
            user={user}
            onNavigateToSettings={() => setView(ViewState.SETTINGS)}
            onRefreshLogs={loadLogs}
            operationLogs={operationLogs}
            onAddOperationLog={addOperationLog}
            onClearOperationLogs={clearOperationLogs}
          />
        );
      
      case ViewState.HISTORY:
        return (
          <PopHistory
            logs={logs}
            settings={settings}
          />
        );
      
      case ViewState.SETTINGS:
        return (
          <PopSettings
            settings={settings}
            onSave={(newSettings) => updateSettings(newSettings)}
            user={user}
            onSignOut={logout}
            onNavigateToAuth={handleOpenAuthModal}
            onOpenSystemLog={handleShowSystemLog}
          />
        );
      
      case ViewState.API:
        return (
          <PopAPI
            settings={settings}
            onSave={updateSettings}
            refreshLogs={loadLogs}
            user={user}
          />
        );
      
      default:
        return null;
    }
  };

  return (
    <ErrorBoundary>
      <div className="app-container">
        {/* 导航栏 */}
        <PopNav
          currentView={view}
          onViewChange={setView}
          settings={settings}
          onUpdateSettings={updateSettings}
        />

        {/* 主内容区 */}
        {renderContent()}

        {/* 模态框和对话框 */}
        {showAuthModal && (
          <PopAuthModal
            isOpen={showAuthModal}
            mode={authMode}
            email={email}
            password={password}
            error={authError?.message || null}
            loading={isAuthenticating}
            showErrorNotification={!!authError}
            onClose={() => setShowAuthModal(false)}
            onSwitchMode={setAuthMode}
            onEmailChange={setEmail}
            onPasswordChange={setPassword}
            onSubmit={() => authMode === 'signin' ? login(email, password) : signup(email, password)}
            onSkipLogin={() => setShowAuthModal(false)}
            onCloseError={() => setShowAuthModal(false)}
            themeColor={settings.themeColor}
            t={{
              signin: TRANSLATIONS[settings.language].signIn,
              signup: TRANSLATIONS[settings.language].signUp,
              email: TRANSLATIONS[settings.language].email,
              password: TRANSLATIONS[settings.language].password
            }}
          />
        )}

        {showCloudDataDialog && (
          <PopCloudDataDialog
            isOpen={showCloudDataDialog}
            onClose={() => cloudData.setShowCloudDataDialog(false)}
          />
        )}

        {showSystemLog && (
          <PopSystemLog
            isOpen={showSystemLog}
            onClose={() => setShowSystemLog(false)}
          />
        )}
      </div>
    </ErrorBoundary>
  );
}
