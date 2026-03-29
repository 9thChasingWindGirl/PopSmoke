import React, { useState, useMemo, useRef } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { PopCard } from '../ui/PopCard';
import { PopButton } from '../ui/PopButton';
import { PopNotification } from '../ui/PopNotification';
import { PopConfirm } from '../ui/PopConfirm';
import { PopSelect } from '../ui/PopSelect';
import { PopPrompt } from '../ui/PopPrompt';
import { PopCloudDataDialog } from '../ui/PopCloudDataDialog';
import { PopLoading } from '../ui/PopLoading';
import { PopOperationLog } from '../ui/PopOperationLog';
import { SmokeLog, AppSettings, User, OperationLog as OperationLogType } from '../../types';
import { TRANSLATIONS } from '../../i18n';
import { apiService, getFeishuApiSettings, syncFromFeishu, SyncDiffResult } from '../../services/apiService';
import { getStorageKeys } from '../../utils/logUtils';
import { getStorageAdapter, isWebPlatform, isAndroidPlatform } from '../../services/storageAdapter';
import { POP_COMPONENT_STYLES } from '../../styles';

interface PopAnalysisProps {
  logs: SmokeLog[];
  settings: AppSettings;
  user: User | null;
  onNavigateToSettings: () => void;
  onRefreshLogs: (newLogs: SmokeLog[]) => void;
  operationLogs?: OperationLogType[];
  onClearOperationLogs?: () => void;
  onAddOperationLog?: (log: OperationLogType) => void;
  isSyncing?: boolean;
  onSyncWithCloud?: (userId: string) => Promise<any>;
}

type ViewMode = 'week' | 'month';
type SyncSource = 'feishu' | 'supabase' | null;

export const PopAnalysis: React.FC<PopAnalysisProps> = ({ logs, settings, user, onNavigateToSettings, onRefreshLogs, operationLogs = [], onClearOperationLogs, onAddOperationLog, isSyncing = false, onSyncWithCloud }) => {
  const t = TRANSLATIONS[settings.language];
  const [viewMode, setViewMode] = useState<ViewMode>('week');
  const analysisRef = useRef<HTMLDivElement>(null);
  const [syncStatus, setSyncStatus] = useState<{ success: boolean; message: string } | null>(null);
  const [exportStatus, setExportStatus] = useState<{ success: boolean; message: string } | null>(null);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [showSyncSourceDialog, setShowSyncSourceDialog] = useState(false);
  const [showPasswordDialog, setShowPasswordDialog] = useState(false);
  const [pendingSyncSource, setPendingSyncSource] = useState<SyncSource>(null);
  const [feishuPassword, setFeishuPassword] = useState('');
  const [apiFetchedCount, setApiFetchedCount] = useState<number | undefined>(undefined);
  const [syncDiff, setSyncDiff] = useState<SyncDiffResult | null>(null);
  const [showDiffDialog, setShowDiffDialog] = useState(false);
  const [showSyncConfirmDialog, setShowSyncConfirmDialog] = useState(false);
  const [syncOptions, setSyncOptions] = useState<{ upload: boolean; download: boolean }>({ upload: true, download: true });
  const [isCalculatingDiff, setIsCalculatingDiff] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  
  const [apiConfig, setApiConfig] = useState<{
    hasFeishuConfig: boolean;
    hasSupabaseConfig: boolean;
  }>({
    hasFeishuConfig: false,
    hasSupabaseConfig: false
  });

  React.useEffect(() => {
    checkApiConfig();
  }, []);

  const checkApiConfig = async () => {
    let hasFeishu = false;
    let hasSupabase = false;
    
    try {
      const adapter = getStorageAdapter();
      const savedApiSettings = await adapter.getApiSettings();
      
      console.log('[Analysis] API settings loaded:', savedApiSettings);
      
      if (savedApiSettings) {
        // 检查是否有加密的 Feishu 数据 (feishu 字段存储加密后的飞书配置)
        // 同时尝试解密检查内容是否有效
        if (savedApiSettings.feishu) {
          try {
            // 尝试用空密码解密来检查内容（仅检查是否为有效加密数据）
            // 实际使用时需要正确密码
            hasFeishu = true;
            console.log('[Analysis] Feishu config detected');
          } catch (e) {
            console.log('[Analysis] Feishu config invalid');
          }
        }
        // 检查是否有 Supabase 配置 (supabase 字段存储加密后的 Supabase 配置)
        // 需要检查内容是否实际包含有效的 URL
        if (savedApiSettings.supabase) {
          try {
            // 尝试解密检查 Supabase 配置是否有效
            // 由于无法在没有密码的情况下解密，我们假设如果有 supabase 字段就有配置
            hasSupabase = true;
            console.log('[Analysis] Supabase config detected');
          } catch (e) {
            console.log('[Analysis] Supabase config invalid');
          }
        }
      } else {
        console.log('[Analysis] No API settings found');
      }
    } catch (error) {
      console.error('[Analysis] Failed to check saved API settings:', error);
    }
    
    // Web端：检查环境变量中的 Supabase 配置（最可靠的方式）
    if (isWebPlatform()) {
      const envUrl = (import.meta.env.VITE_SUPABASE_URL || '').trim();
      const envKey = (import.meta.env.VITE_SUPABASE_ANON_KEY || '').trim();
      hasSupabase = envUrl.length > 0 && envKey.length > 0;
      console.log('[Analysis] Web platform Supabase from env:', hasSupabase, { url: envUrl ? 'set' : 'not set', key: envKey ? 'set' : 'not set' });
    }
    
    console.log('[Analysis] Final apiConfig:', { hasFeishu, hasSupabase });
    
    setApiConfig({
      hasFeishuConfig: hasFeishu,
      hasSupabaseConfig: hasSupabase
    });
  };

  const hasAnyApiConfig = apiConfig.hasFeishuConfig || apiConfig.hasSupabaseConfig;
  const needToSelectSource = apiConfig.hasFeishuConfig && apiConfig.hasSupabaseConfig;

  const triggerVibration = () => {
    if (window.navigator.vibrate) {
      window.navigator.vibrate([100, 50, 100]);
    }
    if (analysisRef.current) {
      analysisRef.current.classList.add('shake-animation');
      setTimeout(() => {
        analysisRef.current?.classList.remove('shake-animation');
      }, 500);
    }
  };
  
  const handleSyncClick = () => {
    if (!hasAnyApiConfig) {
      setSyncStatus({
        success: false,
        message: t.apiConfigRequired
      });
      return;
    }

    if (needToSelectSource) {
      setShowSyncSourceDialog(true);
    } else if (apiConfig.hasFeishuConfig) {
      handleSync('feishu');
    } else if (apiConfig.hasSupabaseConfig) {
      handleSync('supabase');
    }
  };

  const handleSync = async (source: SyncSource, password?: string) => {
    setShowSyncSourceDialog(false);
    setSyncStatus(null);
    setIsCalculatingDiff(true);
    setIsLoading(true);

    try {
      if (source === 'feishu') {
        const userId = user?.id || 'local';
        const diffResult = await apiService.getSyncDiff('feishu', userId, password, settings.language);
        
        setSyncDiff(diffResult);
        setShowDiffDialog(true);
      } else if (source === 'supabase') {
        if (!user) {
          setSyncStatus({
            success: false,
            message: t.loginFirst
          });
          setTimeout(() => {
            onNavigateToSettings();
          }, 1500);
          return;
        }

        // 检查是否需要密码
        const { isApiConfigEncrypted } = await import('../../services/apiService');
        const needsPassword = await isApiConfigEncrypted('supabase');
        if (needsPassword && !password) {
          setPendingSyncSource('supabase');
          setShowPasswordDialog(true);
          return;
        }

        // 如果提供了密码，先解密配置
        if (password) {
          const { decryptSupabaseConfig } = await import('../../services/apiService');
          const decrypted = await decryptSupabaseConfig(password);
          if (!decrypted) {
            setSyncStatus({
              success: false,
              message: t.passwordError
            });
            return;
          }
        }

        const diffResult = await apiService.getSyncDiff('supabase', user.id, undefined, settings.language);
        setSyncDiff(diffResult);
        setShowDiffDialog(true);
      }
    } catch (error) {
      console.error('Sync error:', error);
      const errorMessage = error instanceof Error ? error.message : t.syncFailed;
      
      if (errorMessage.includes('encrypted')) {
        setPendingSyncSource(source);
        setShowPasswordDialog(true);
      } else {
        setSyncStatus({
          success: false,
          message: errorMessage
        });
      }
    } finally {
      setIsCalculatingDiff(false);
      setIsLoading(false);
    }
  };

  const handleSyncConfirm = async () => {
    if (!syncDiff) return;
    
    setShowDiffDialog(false);
    setShowSyncConfirmDialog(false);
    setIsLoading(true);
    
    try {
      let result;
      if (syncDiff.source === 'feishu') {
        // 飞书只支持下载
        result = await apiService.executeSync('feishu', syncDiff.diff, { download: true }, settings.language);
      } else {
        // Supabase支持上传和下载
        result = await apiService.executeSync('supabase', syncDiff.diff, syncOptions, settings.language);
      }
      
      setSyncStatus({
        success: result.success,
        message: result.message
      });
      
      if (result.success) {
        // 刷新本地日志
        const adapter = getStorageAdapter();
        const updatedLogs = await adapter.getLogs();
        onRefreshLogs(updatedLogs);
        
        // 添加操作日志
        if (onAddOperationLog) {
          const syncLog: OperationLogType = {
            id: `sync_${Date.now()}`,
            type: 'sync',
            data: {
              id: '',
              user_id: user?.id || 'local',
              record_date: new Date().toISOString().split('T')[0],
              record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
              timestamp: Date.now()
            } as SmokeLog,
            syncStatus: 'synced',
            timestamp: Date.now(),
            message: (syncDiff.source === 'feishu' ? t.syncWithFeishuCompleted : t.syncWithSupabaseCompleted) + '：' + result.message
          };
          onAddOperationLog(syncLog);
        }
      }
    } catch (error) {
      console.error('Sync execution error:', error);
      setSyncStatus({
        success: false,
        message: error instanceof Error ? error.message : t.syncFailed
      });
    } finally {
      setIsLoading(false);
    }
  };

  const getSyncButtonText = () => {
    if (!hasAnyApiConfig) return t.cloudSync;
    if (needToSelectSource) return t.cloudSync;
    if (apiConfig.hasFeishuConfig) return t.syncFromFeishu;
    if (apiConfig.hasSupabaseConfig) return t.syncFromSupabaseOnly;
    return t.cloudSync;
  };

  const goToPrevMonth = () => {
    const newMonth = new Date(currentMonth);
    newMonth.setMonth(currentMonth.getMonth() - 1);
    setCurrentMonth(newMonth);
  };
  
  const goToNextMonth = () => {
    const newMonth = new Date(currentMonth);
    newMonth.setMonth(currentMonth.getMonth() + 1);
    setCurrentMonth(newMonth);
  };
  
  const goToCurrentMonth = () => {
    setCurrentMonth(new Date());
  };

  const getWeekDates = () => {
    const today = new Date();
    const dayOfWeek = today.getDay();
    const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    const monday = new Date(today);
    monday.setDate(today.getDate() + mondayOffset);
    monday.setHours(0, 0, 0, 0);
    
    const dates: Date[] = [];
    for (let i = 0; i < 7; i++) {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      dates.push(d);
    }
    return dates;
  };

  const getWeekdayName = (date: Date) => {
    const weekdays = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
    const dayIndex = date.getDay();
    const weekdayKey = weekdays[dayIndex === 0 ? 6 : dayIndex - 1];
    return t[weekdayKey as keyof typeof t] || date.toLocaleDateString(settings.language, { weekday: 'short' });
  };

  const chartData = useMemo(() => {
    const data: { date: string; count: number; fullDate: string; dayIndex: number }[] = [];
    
    if (viewMode === 'week') {
      const weekDates = getWeekDates();
      weekDates.forEach((d, index) => {
        const dateStr = getWeekdayName(d);
        const fullDateStr = d.toDateString();
        const startOfDay = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
        const endOfDay = startOfDay + 86400000;
        const count = logs.filter(l => l.timestamp >= startOfDay && l.timestamp < endOfDay).length;
        data.push({ date: dateStr, count, fullDate: fullDateStr, dayIndex: index });
      });
    } else {
      const today = new Date();
      const isCurrentMonth = currentMonth.getMonth() === today.getMonth() && currentMonth.getFullYear() === today.getFullYear();
      const startDate = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1);
      const lastDay = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 0).getDate();
      const daysToProcess = isCurrentMonth ? Math.min(lastDay, today.getDate()) : lastDay;

      for (let i = 0; i < daysToProcess; i++) {
        const d = new Date(startDate);
        d.setDate(startDate.getDate() + i);
        const dateStr = `${d.getDate()}`;
        const fullDateStr = d.toDateString();
        const startOfDay = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
        const endOfDay = startOfDay + 86400000;
        const count = logs.filter(l => l.timestamp >= startOfDay && l.timestamp < endOfDay).length;
        data.push({ date: dateStr, count, fullDate: fullDateStr, dayIndex: i });
      }
    }
    return data;
  }, [logs, settings.language, viewMode, currentMonth]);

  const weekStats = useMemo(() => {
    if (viewMode !== 'week') return null;
    
    const weekDates = getWeekDates();
    const mondayTime = weekDates[0].getTime();
    const sundayTime = weekDates[6].getTime() + 86400000 - 1;
    
    const weekLogs = logs.filter(l => l.timestamp >= mondayTime && l.timestamp <= sundayTime);
    const totalCount = weekLogs.length;
    
    const dailyCounts: { [key: string]: number } = {};
    weekDates.forEach((d, index) => {
      const startOfDay = d.getTime();
      const endOfDay = startOfDay + 86400000;
      const count = logs.filter(l => l.timestamp >= startOfDay && l.timestamp < endOfDay).length;
      dailyCounts[index] = count;
    });
    
    let maxDayIndex = 0;
    let maxCount = 0;
    Object.entries(dailyCounts).forEach(([index, count]) => {
      if (count > maxCount) {
        maxCount = count;
        maxDayIndex = parseInt(index);
      }
    });
    
    const maxDayName = maxCount > 0 ? getWeekdayName(weekDates[maxDayIndex]) : '-';
    
    return { totalCount, maxDayName, maxCount };
  }, [logs, viewMode, settings.language]);

  const monthStats = useMemo(() => {
    if (viewMode !== 'month') return null;
    
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth();
    const firstDay = new Date(year, month, 1).getTime();
    const lastDay = new Date(year, month + 1, 0, 23, 59, 59, 999).getTime();
    
    const monthLogs = logs.filter(l => l.timestamp >= firstDay && l.timestamp <= lastDay);
    const currentMonthCount = monthLogs.length;
    
    const prevMonth = new Date(year, month, 0);
    const prevFirstDay = new Date(prevMonth.getFullYear(), prevMonth.getMonth(), 1).getTime();
    const prevLastDay = new Date(prevMonth.getFullYear(), prevMonth.getMonth() + 1, 0, 23, 59, 59, 999).getTime();
    const prevMonthLogs = logs.filter(l => l.timestamp >= prevFirstDay && l.timestamp <= prevLastDay);
    const prevMonthCount = prevMonthLogs.length;
    
    const dailyCounts: { [key: number]: number } = {};
    const lastDate = new Date(year, month + 1, 0).getDate();
    const today = new Date();
    const isCurrentMonth = month === today.getMonth() && year === today.getFullYear();
    const daysInMonth = isCurrentMonth ? today.getDate() : lastDate;
    
    for (let day = 1; day <= daysInMonth; day++) {
      const d = new Date(year, month, day);
      const startOfDay = d.getTime();
      const endOfDay = startOfDay + 86400000;
      const count = logs.filter(l => l.timestamp >= startOfDay && l.timestamp < endOfDay).length;
      dailyCounts[day] = count;
    }
    
    let maxDay = 0;
    let maxCount = 0;
    Object.entries(dailyCounts).forEach(([day, count]) => {
      if (count > maxCount) {
        maxCount = count;
        maxDay = parseInt(day);
      }
    });
    
    return { 
      currentMonthCount, 
      prevMonthCount, 
      maxDay: maxCount > 0 ? maxDay : 0, 
      maxCount 
    };
  }, [logs, viewMode, currentMonth]);

  const handleExportCSV = () => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth();
    
    const logsByDate: Record<string, Date[]> = {};
    
    logs.forEach(log => {
      const date = new Date(log.timestamp);
      if (date.getFullYear() === year && date.getMonth() === month) {
        const dateStr = `${date.getFullYear()}/${String(month + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`;
        if (!logsByDate[dateStr]) {
          logsByDate[dateStr] = [];
        }
        logsByDate[dateStr].push(date);
      }
    });
    
    if (Object.keys(logsByDate).length === 0) {
      setExportStatus({
        success: false,
        message: t.noDataToExport || '当前月份没有数据可导出'
      });
      return;
    }
    
    const headers = ['Date', ...Array.from({ length: 20 }, (_, i) => i + 1)];
    let csvContent = `data:text/csv;charset=utf-8,${headers.join(',')}\n`;
    
    const sortedDates = Object.keys(logsByDate).sort();
    
    sortedDates.forEach(date => {
      const sortedTimes = logsByDate[date].sort((a, b) => a.getTime() - b.getTime());
      const timeStrs = sortedTimes.map(time => {
        return `${String(time.getHours()).padStart(2, '0')}:${String(time.getMinutes()).padStart(2, '0')}`;
      });
      const row = [date, ...timeStrs.slice(0, 20)];
      csvContent += `${row.join(',')}\n`;
    });

    const monthStr = `${year}${String(month + 1).padStart(2, '0')}`;
    const fileName = `smoking_logs_${monthStr}.csv`;
    
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", fileName);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    setExportStatus({
      success: true,
      message: t.exportSuccess
    });
    
    // 添加操作日志
    if (onAddOperationLog) {
      const exportLog: OperationLogType = {
        id: `export_${Date.now()}`,
        type: 'sync',
        data: {
          id: '',
          user_id: user?.id || 'local',
          record_date: new Date().toISOString().split('T')[0],
          record_time: new Date().toTimeString().split(' ')[0].substring(0, 5),
          timestamp: Date.now()
        } as SmokeLog,
        syncStatus: 'synced',
        timestamp: Date.now(),
        message: `${t.export} CSV: ${fileName}`,
        apiFetchedCount: Object.keys(logsByDate).length
      };
      onAddOperationLog(exportLog);
    }
  };

  if (isLoading) {
    return <PopLoading settings={settings} status="loading" isInitialize={false} />;
  }

  return (
    <div className="w-full max-w-2xl mx-auto space-y-6 pb-[calc(80px+env(safe-area-inset-bottom))] md:pb-0">
       <PopCard className="relative pt-20 mb-8">
         <div className="absolute top-0 left-0 w-full flex flex-col border-b-4 border-black bg-gray-100 z-10">
            <div className="flex justify-between items-center px-4 py-2">
              <span className="font-display text-xl uppercase">{viewMode === 'week' ? t.thisWeek : t.monthlyFreq}</span>
              <div className="flex space-x-2">
                  <button 
                    onClick={() => setViewMode('week')}
                    className={`text-xs font-bold px-2 py-1 border-2 border-black ${viewMode === 'week' ? 'bg-black text-white' : 'bg-white'}`}
                  >
                      {t.weekData}
                  </button>
                  <button 
                    onClick={() => setViewMode('month')}
                    className={`text-xs font-bold px-2 py-1 border-2 border-black ${viewMode === 'month' ? 'bg-black text-white' : 'bg-white'}`}
                  >
                      {t.monthData}
                  </button>
              </div>
            </div>
            
            {viewMode === 'month' && (
              <div className="flex justify-between items-center px-4 py-2 bg-gray-200 border-t-2 border-black">
                <button 
                  onClick={goToPrevMonth}
                  className="text-sm font-bold px-2 py-1 border-2 border-black bg-white hover:bg-gray-100 transition-colors"
                >
                  &larr; {t.prevMonth}
                </button>
                <div className="flex items-center space-x-2">
                  <button 
                    onClick={goToCurrentMonth}
                    className={`text-sm font-bold px-2 py-1 border-2 border-black ${currentMonth.getMonth() === new Date().getMonth() && currentMonth.getFullYear() === new Date().getFullYear() ? 'bg-black text-white' : 'bg-white hover:bg-gray-100'}`}
                  >
                    {t.currentMonth}
                  </button>
                  <span className="text-sm font-bold">
                    {currentMonth.toLocaleDateString(settings.language, { year: 'numeric', month: 'long' })}
                  </span>
                </div>
                <button 
                  onClick={goToNextMonth}
                  className="text-sm font-bold px-2 py-1 border-2 border-black bg-white hover:bg-gray-100 transition-colors"
                >
                  {t.nextMonth} &rarr;
                </button>
              </div>
            )}
         </div>

         <div className="min-h-[200px] sm:min-h-[300px] md:min-h-[350px] lg:min-h-[400px] w-full mt-12 sm:mt-10 md:mt-8" style={{ height: '300px', minHeight: '250px' }}>
            <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 5, right: 5, left: 0, bottom: 5 }}>
                    <XAxis 
                        dataKey="date" 
                        tick={{fontSize: 8, fontFamily: 'Public Sans', fontWeight: 'bold'}} 
                        interval={viewMode === 'month' ? 
                            // 简单的间隔设置，避免类型错误
                            (window.innerWidth < 640 ? 5 : 2)
                            : 0
                        }
                        minTickGap={15}
                        axisLine={{strokeWidth: 2}} 
                        tickLine={false} 
                    />
                    <YAxis 
                        allowDecimals={false} 
                        axisLine={{strokeWidth: 2}} 
                        tickLine={{strokeWidth: 2}} 
                        width={30} 
                        minTickGap={15}
                    />
                    <Tooltip 
                        cursor={{fill: '#f0f0f0'}}
                        content={({ active, payload, label }) => {
                          if (active && payload && payload.length) {
                            return (
                              <div className="bg-white border-4 border-black p-3 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] min-w-[120px]">
                                <p className="font-display text-xl mb-2 border-b-2 border-black pb-1">{label}</p>
                                <p className="font-display text-2xl font-bold text-black">
                                  <span className="text-sm font-body font-medium mr-2 uppercase text-gray-800">{t.count}:</span>
                                  {payload[0].value}
                                </p>
                              </div>
                            );
                          }
                          return null;
                        }}
                    />
                    <Bar dataKey="count" fill={settings.themeColor} radius={[2, 2, 0, 0]} stroke="black" strokeWidth={1} minPointSize={0}>
                      {chartData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.count > 0 ? (entry.count > settings.dailyLimit ? '#FF4d4d' : settings.themeColor) : 'transparent'} />
                      ))}
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
         </div>
       </PopCard>

       <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-start">
           <PopCard title={t.stats} className="mb-8 min-h-[200px]">
               <div className="space-y-3">
                 {viewMode === 'week' && weekStats && (
                   <>
                     <div className="text-center p-2 bg-gray-50 rounded border-2 border-black">
                       <p className="font-display text-3xl">{weekStats.totalCount}</p>
                       <p className="text-xs text-gray-500 uppercase">{t.weekTotal}</p>
                     </div>
                     <div className="text-center p-2 bg-gray-50 rounded border-2 border-black">
                       <p className="font-display text-2xl">{weekStats.maxDayName}</p>
                       <p className="text-xs text-gray-500 uppercase">{t.mostSmokedDay} ({weekStats.maxCount})</p>
                     </div>
                   </>
                 )}
                 {viewMode === 'month' && monthStats && (
                   <>
                     <div className="text-center p-2 bg-gray-50 rounded border-2 border-black">
                       <p className="font-display text-3xl">{monthStats.currentMonthCount}</p>
                       <p className="text-xs text-gray-500 uppercase">{t.monthTotal}</p>
                     </div>
                     <div className="text-center p-2 bg-gray-50 rounded border-2 border-black">
                       <p className="font-display text-2xl">{monthStats.prevMonthCount}</p>
                       <p className="text-xs text-gray-500 uppercase">{t.lastMonthTotal}</p>
                     </div>
                     <div className="text-center p-2 bg-gray-50 rounded border-2 border-black">
                       <p className="font-display text-2xl">{monthStats.maxDay > 0 ? `${monthStats.maxDay}${t.day}` : '-'}</p>
                       <p className="text-xs text-gray-500 uppercase">{t.mostSmokedDate} ({monthStats.maxCount})</p>
                     </div>
                   </>
                 )}
               </div>
           </PopCard>

           <PopCard title={t.data} className="mb-8 min-h-[180px] sm:min-h-[200px] md:min-h-[220px]">
               <div className="flex flex-col space-y-3 items-center justify-center h-full">
                <PopButton 
                  themeColor={settings.themeColor} 
                  onClick={handleSyncClick} 
                  className="w-full text-sm"
                  disabled={isSyncing || !hasAnyApiConfig}
                >
                    {isSyncing ? t.cloudSyncing : getSyncButtonText()}
                </PopButton>
                {syncStatus && (
                  <PopNotification
                    title={syncStatus.success ? t.notificationSuccess : t.notificationError}
                    message={syncStatus.message}
                    type={syncStatus.success ? 'success' : 'error'}
                    onClose={() => setSyncStatus(null)}
                    duration={3000}
                  />
                )}
                {exportStatus && (
                  <PopNotification
                    title={exportStatus.success ? t.notificationSuccess : t.notificationError}
                    message={exportStatus.message}
                    type={exportStatus.success ? 'success' : 'error'}
                    onClose={() => setExportStatus(null)}
                    duration={3000}
                  />
                )}
                <PopButton themeColor="#e0e0e0" onClick={handleExportCSV} className="w-full text-sm">
                    {t.exportCSV} ({currentMonth.toLocaleDateString(settings.language, { year: 'numeric', month: 'short' })})
                </PopButton>
                <p className="text-xs text-gray-500 mt-2 text-center">{t.csvNote}</p>
               </div>
           </PopCard>
       </div>
       
       {/* Operation Log */}
       <PopCard title={t.operationLog} className="mb-8">
         <PopOperationLog 
           logs={operationLogs} 
           onClear={onClearOperationLogs}
           themeColor={settings.themeColor}
           language={settings.language}
           apiFetchedCount={apiFetchedCount}
         />
       </PopCard>
    
      {showSyncSourceDialog && (
        <PopSelect
          type="info"
          title={t.selectSyncSource}
          options={[
            { label: t.feishu, value: 'feishu' },
            { label: t.supabase, value: 'supabase' }
          ]}
          onSelect={(value) => handleSync(value as SyncSource)}
          onCancel={() => setShowSyncSourceDialog(false)}
          cancelText={t.cancel}
          themeColor={settings.themeColor}
        />
      )}
      
      {showPasswordDialog && pendingSyncSource && (
        <PopPrompt
          type="info"
          title={t.enterPassword || 'Enter Password'}
          message={(t as any).apiPasswordRequired || `Please enter your API password to decrypt ${pendingSyncSource} settings`}
          placeholder={t.enterPassword || 'Enter password'}
          confirmText={t.confirm || 'Confirm'}
          cancelText={t.cancel || 'Cancel'}
          confirmThemeColor={settings.themeColor}
          isPassword={true}
          onConfirm={(value) => {
            setShowPasswordDialog(false);
            handleSync(pendingSyncSource, value);
            setPendingSyncSource(null);
          }}
          onCancel={() => {
            setShowPasswordDialog(false);
            setPendingSyncSource(null);
          }}
        />
      )}

      <PopCloudDataDialog
        visible={showDiffDialog}
        mode="sync-diff"
        syncDiff={syncDiff}
        title={syncDiff?.source === 'feishu' ? t.syncFromFeishu : t.syncFromSupabaseOnly}
        message=""
        skipText={t.cancel}
        onSkip={() => {
          setShowDiffDialog(false);
          setSyncDiff(null);
        }}
        onClose={() => {
          setShowDiffDialog(false);
          setSyncDiff(null);
        }}
        onSyncConfirm={handleSyncConfirm}
        onSyncOptionChange={(option: 'upload' | 'download', value: boolean) => {
          if (option === 'upload') {
            setSyncOptions({ ...syncOptions, upload: value });
          } else if (option === 'download') {
            setSyncOptions({ ...syncOptions, download: value });
          }
        }}
        themeColor={settings.themeColor}
        language={settings.language}
      />

    </div>
  );
};
