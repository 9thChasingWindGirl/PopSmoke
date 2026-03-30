import React, { useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { PopCard } from '../ui/PopCard';
import { PopTimePicker } from '../ui/PopTimePicker';
import { PopConfirm } from '../ui/PopConfirm';
import { SmokeLog, AppSettings, Language } from '../../types';
import { TRANSLATIONS } from '../../i18n';
import { 
  POP_COMPONENT_STYLES,
  getGaugeStyle,
  getCountBadgeStyle
} from '../../styles';
import { isLightColor } from '../../utils/colorUtils';

interface PopDashboardProps {
  logs: SmokeLog[];
  settings: AppSettings;
  onRecord: () => void;
  onDelete: (id: string) => void;
  onUpdate: (log: SmokeLog) => void;
}

// 半圆环仪表盘组件
const SemiCircleGauge: React.FC<{
  value: number;
  max: number;
  color: string;
  language: Language;
}> = ({ value, max, color, language }) => {
  const t = TRANSLATIONS[language];
  const percentage = Math.min((value / max) * 100, 100);
  
  const renderGauge = (isMobile: boolean) => {
    const gaugeSize = isMobile ? 150 : 280;
    const strokeWidth = isMobile ? 15 : 25;
    const radius = (gaugeSize - strokeWidth) / 2;
    const circumference = Math.PI * radius;
    const strokeDashoffset = circumference - (percentage / 100) * circumference;
    const containerHeight = gaugeSize / 2 + (isMobile ? 30 : 50);
    const valueFontSize = isMobile ? '2.5rem' : '4rem';
    const labelFontSize = isMobile ? '0.75rem' : '1rem';
    
    return (
      <div className="relative" style={{ width: gaugeSize, height: containerHeight }}>
        <svg width={gaugeSize} height={containerHeight} className="overflow-visible">
          {/* 背景半圆 */}
          <path
            d={`M ${strokeWidth/2} ${gaugeSize/2} A ${radius} ${radius} 0 0 1 ${gaugeSize - strokeWidth/2} ${gaugeSize/2}`}
            fill="none"
            stroke="#E5E7EB"
            strokeWidth={strokeWidth}
            strokeLinecap="round"
          />
          {/* 进度半圆 */}
          <path
            d={`M ${strokeWidth/2} ${gaugeSize/2} A ${radius} ${radius} 0 0 1 ${gaugeSize - strokeWidth/2} ${gaugeSize/2}`}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            className="transition-all duration-1000 ease-out"
            style={{ filter: `drop-shadow(0 4px 8px ${color}50)` }}
          />
        </svg>
        {/* 中心数值 */}
        <div className="absolute bottom-0 left-0 right-0 text-center">
          <div className="font-display font-black" style={{ color, fontSize: valueFontSize }}>
            {value}
          </div>
          <div className="font-body font-bold text-gray-500 uppercase tracking-wider" style={{ fontSize: labelFontSize }}>
            {t.smokes}
          </div>
        </div>
      </div>
    );
  };
  
  return (
    <>
      <div className="hidden md:block">
        {renderGauge(false)}
      </div>
      <div className="md:hidden">
        {renderGauge(true)}
      </div>
    </>
  );
};

// 统计卡片组件
const StatCard: React.FC<{
  label: string;
  value: string;
  color: string;
  bgColor: string;
}> = ({ label, value, color, bgColor }) => (
  <div 
    className="border-4 border-black p-3 flex flex-col justify-between min-h-[80px]"
    style={{ backgroundColor: bgColor }}
  >
    <span className="font-body text-xs font-bold uppercase tracking-wider" style={{ color }}>
      {label}
    </span>
    <span className="font-display text-xl md:text-2xl font-black" style={{ color }}>
      {value}
    </span>
  </div>
);

// 记录项组件
const LogItem: React.FC<{
  log: SmokeLog;
  onEdit: () => void;
  onDelete: () => void;
  themeColor: string;
  language: Language;
}> = ({ log, onEdit, onDelete, themeColor, language }) => {
  const t = TRANSLATIONS[language];
  
  const getIcon = () => {
    const hour = new Date(log.timestamp).getHours();
    const minute = new Date(log.timestamp).getMinutes();
    
    if (hour >= 23 && minute >= 30 || hour < 6) return '🌙';
    if (hour >= 6 && hour < 8) return '☀';
    if (hour >= 8 && hour < 12) return '🌤';
    if (hour >= 12 && hour < 14) return '🌞';
    if (hour >= 14 && hour < 18) return '🌅';
    return '🌆';
  };
  
  const getLabel = () => {
    const hour = new Date(log.timestamp).getHours();
    const minute = new Date(log.timestamp).getMinutes();
    
    if (hour >= 23 && minute >= 30 || hour < 6) return t.lateNight;
    if (hour >= 6 && hour < 8) return t.earlyMorning;
    if (hour >= 8 && hour < 12) return t.morning;
    if (hour >= 12 && hour < 14) return t.noon;
    if (hour >= 14 && hour < 18) return t.afternoon;
    return t.evening;
  };

  return (
    <div className="bg-white border-2 border-black p-3 flex items-center justify-between shadow-[2px_2px_0px_0px_rgba(0,0,0,0.1)]">
      <div className="flex items-center gap-3">
        <div 
          className="w-10 h-10 border-2 border-black flex items-center justify-center text-lg"
          style={{ backgroundColor: `${themeColor}30` }}
        >
          {getIcon()}
        </div>
        <div>
          <p className="font-display text-sm font-bold uppercase">{getLabel()}</p>
          <p className="font-body text-xs text-gray-500">
            {new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </p>
        </div>
      </div>
      <div className="flex items-center gap-2">
        <span className="font-display text-base font-bold text-gray-600">#{log.record_index}</span>
        <div className="flex gap-1">
          <button 
            onClick={onEdit}
            className="w-7 h-7 bg-yellow-200 border-2 border-black flex items-center justify-center hover:bg-yellow-300 active:scale-95 transition-all"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
            </svg>
          </button>
          <button 
            onClick={onDelete}
            className="w-7 h-7 bg-red-200 border-2 border-black flex items-center justify-center hover:bg-red-300 active:scale-95 transition-all"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
};

export const PopDashboard: React.FC<PopDashboardProps> = ({ logs, settings, onRecord, onDelete, onUpdate }) => {
  const t = TRANSLATIONS[settings.language];
  const [editingId, setEditingId] = useState<string | null>(null);
  const [isFlipped, setIsFlipped] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const confirmDelete = (id: string) => {
    onDelete(id);
    setDeletingId(null);
  };

  const todayLogs = useMemo(() => {
    const now = new Date();
    const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    return logs
      .filter(l => l.timestamp >= startOfDay)
      .sort((a, b) => b.timestamp - a.timestamp);
  }, [logs]);

  const todayCount = todayLogs.length;
  
  const gaugeColor = todayCount >= settings.dailyLimit 
    ? '#EF4444' 
    : todayCount >= settings.warningLimit 
      ? '#F59E0B' 
      : settings.themeColor || '#F97316';

  const buttonTextColor = isLightColor(settings.themeColor || '#F97316') ? '#000000' : '#FFFFFF';
  const tagTextColor = isLightColor(settings.themeColor || '#F97316') ? '#000000' : '#FFFFFF';

  // 计算连续达标天数
  const consecutiveDaysWithinLimit = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    let count = 0;
    let currentDate = new Date(today);
    
    for (let i = 0; i < 365; i++) {
      const dayStart = currentDate.getTime();
      const dayEnd = dayStart + 24 * 60 * 60 * 1000;
      
      const dayLogs = logs.filter(l => l.timestamp >= dayStart && l.timestamp < dayEnd);
      const dayCount = dayLogs.length;
      
      // 只有当某天有记录且记录数 <= dailyLimit 时，才算达标
      if (dayCount > 0 && dayCount <= settings.dailyLimit) {
        count++;
      } else {
        break;
      }
      
      currentDate.setDate(currentDate.getDate() - 1);
    }
    
    return count;
  }, [logs, settings.dailyLimit]);

  // 计算统计数据
  const costToday = (todayCount * 0.7).toFixed(2); // 假设每支$0.7
  const lifeMinutesLost = todayCount * 11; // 假设每支减少11分钟寿命
  const healthIndex = todayCount >= settings.dailyLimit ? 'CRITICAL' : todayCount >= settings.warningLimit ? 'WARNING' : 'NORMAL';
  const healthColor = todayCount >= settings.dailyLimit ? '#EF4444' : todayCount >= settings.warningLimit ? '#F59E0B' : '#22C55E';

  const lastSmokeTime = useMemo(() => {
    if (todayLogs.length === 0) return null;
    return new Date(todayLogs[0].timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }, [todayLogs]);

  const saveEditing = (newTimestamp: number) => {
    if (editingId) {
      const originalLog = logs.find(l => l.id === editingId);
      if (originalLog) {
        const newDate = new Date(newTimestamp);
        const recordDate = `${newDate.getFullYear()}-${String(newDate.getMonth() + 1).padStart(2, '0')}-${String(newDate.getDate()).padStart(2, '0')}`;
        const recordTime = `${String(newDate.getHours()).padStart(2, '0')}:${String(newDate.getMinutes()).padStart(2, '0')}`;
        
        const updatedLog = {
          ...originalLog,
          timestamp: newTimestamp,
          record_date: recordDate,
          record_time: recordTime
        };
        onUpdate(updatedLog);
      }
      setEditingId(null);
    }
  };

  // 渲染主界面
  return (
    <>
      {/* 编辑时间弹窗 */}
      {editingId && createPortal(
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
        >
          <div className="w-full max-w-md mx-4">
            <PopCard className="p-6">
              <PopTimePicker 
                initialTimestamp={logs.find(l => l.id === editingId)?.timestamp || Date.now()}
                onSave={saveEditing}
                onCancel={() => setEditingId(null)}
                themeColor={settings.themeColor}
                language={settings.language}
              />
            </PopCard>
          </div>
        </div>,
        document.body
      )}

      {/* 删除确认弹窗 */}
      {deletingId && (
        <PopConfirm
          type="warning"
          title={t.delete || 'Delete'}
          message={t.confirmDelete || 'Are you sure you want to delete this record?'}
          confirmText={t.delete || 'Delete'}
          cancelText={t.cancel || 'Cancel'}
          onConfirm={() => confirmDelete(deletingId)}
          onCancel={() => setDeletingId(null)}
          confirmThemeColor="#EF4444"
        />
      )}
      
      <div className="w-full max-w-4xl mx-auto flex flex-col p-2 md:p-4 min-h-[500px] sm:min-h-[600px] md:min-h-[700px] h-full pb-[calc(80px+env(safe-area-inset-bottom))] md:pb-0">
        {/* 翻转卡片容器 */}
        <div className="relative flex justify-center items-center flex-1" style={{ perspective: '1000px' }}>
          <div 
            className="relative w-full max-w-lg md:max-w-xl transition-transform duration-700 flex items-center justify-center h-full"
            style={{ 
              transformStyle: 'preserve-3d',
              transform: isFlipped ? 'rotateY(180deg)' : 'rotateY(0deg)'
            }}
          >
            {/* 正面：仪表盘 */}
              <div 
                className="w-full h-full"
                style={{ backfaceVisibility: 'hidden' }}
              >
                <PopCard className="p-2 sm:p-3 md:p-5 flex flex-col items-center h-full">
                  <div className="w-full flex justify-between items-center mb-3 flex-shrink-0">
                    <h2 className="font-display text-xl md:text-2xl font-bold uppercase tracking-wider text-gray-600">
                      {TRANSLATIONS[settings.language].currentGaugeLevel}
                    </h2>
                    <button 
                      onClick={() => setIsFlipped(true)}
                      className="w-12 h-12 md:w-14 md:h-14 bg-white border-2 border-black flex items-center justify-center hover:bg-gray-50 active:scale-95 transition-all"
                      title="View Recent Logs"
                    >
                      <svg className="w-6 h-6 md:w-7 md:h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                      </svg>
                    </button>
                  </div>
                  
                  <div className="flex-1 flex items-center justify-center min-h-0">
                    <SemiCircleGauge 
                      value={todayCount} 
                      max={settings.dailyLimit} 
                      color={gaugeColor}
                      language={settings.language}
                    />
                  </div>
                  
                  {/* 连续达标天数标签 */}
                  <div 
                    className="mt-4 px-6 py-3 border-2 border-black font-display text-base font-bold uppercase transform -rotate-2 flex-shrink-0"
                    style={{ backgroundColor: settings.themeColor || '#F97316', color: tagTextColor }}
                  >
                    {t.consecutiveDaysWithinLimit || 'Consecutive Days Within Limit'}: {consecutiveDaysWithinLimit} {t.days || 'days'}
                  </div>
                  
                  {/* 底部信息 */}
                  <div className="w-full flex justify-between mt-4 pt-4 border-t-2 border-gray-200 flex-shrink-0">
                    <div className="flex-1 text-center">
                      <p className="font-body text-sm font-bold text-gray-400 uppercase">{t.lastRecord || 'LAST RECORD TIME'}</p>
                      <p className="font-display text-xl font-black">
                        {lastSmokeTime || '--:--'}
                      </p>
                    </div>
                    <div className="flex-1 text-center">
                      <p className="font-body text-sm font-bold text-gray-400 uppercase">{t.dailyLimit || 'DAILY LIMIT'}</p>
                      <p className="font-display text-xl font-black" style={{ color: gaugeColor }}>
                        {settings.dailyLimit}
                      </p>
                    </div>
                  </div>

                  {/* 记录按钮 */}
                  <div className="w-full max-w-[280px] mx-auto mt-4 flex-shrink-0">
                    <button
                      onClick={onRecord}
                      className="w-full group relative"
                    >
                      {/* 阴影层 */}
                      <div 
                        className="absolute inset-0 border-4 border-black transform translate-x-2 translate-y-2 transition-transform group-hover:translate-x-0 group-hover:translate-y-0 group-active:translate-x-1 group-active:translate-y-1"
                        style={{ backgroundColor: '#000' }}
                      />
                      {/* 按钮主体 */}
                      <div 
                        className="relative border-4 border-black p-5 flex items-center justify-between transition-transform group-hover:translate-x-0 group-hover:translate-y-0 group-active:translate-x-1 group-active:translate-y-1"
                        style={{ backgroundColor: settings.themeColor || '#F97316' }}
                      >
                        <div className="flex flex-col">
                          <span className="font-display text-3xl md:text-4xl font-black italic tracking-tight" style={{ color: buttonTextColor }}>
                            BOOM!
                          </span>
                          <span className="font-display text-xl md:text-2xl font-black uppercase tracking-wider" style={{ color: buttonTextColor }}>
                            {t.smokeButton || 'LOG SMOKE'}
                          </span>
                        </div>
                        <div className="w-14 h-14 md:w-16 md:h-16 bg-white border-4 border-black rounded-full flex items-center justify-center">
                          <svg className="w-7 h-7 md:w-9 md:h-9" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M12 4v16m8-8H4" />
                          </svg>
                        </div>
                      </div>
                    </button>
                  </div>
                </PopCard>
              </div>

            {/* 背面：最近记录 */}
            <div 
              className="w-full h-full absolute top-0 left-0"
              style={{ 
                backfaceVisibility: 'hidden',
                transform: 'rotateY(180deg)'
              }}
            >
              <PopCard className="p-2 sm:p-3 md:p-5 flex flex-col h-full" style={{ backgroundColor: '#EEF2FF' }}>
                <div className="flex items-center justify-between mb-4 flex-shrink-0">
                  <div>
                    <h2 className="font-display text-2xl font-black uppercase">{t.todayLogs || "TODAY'S LOGS"}</h2>
                  </div>
                  <button 
                    onClick={() => setIsFlipped(false)}
                    className="w-12 h-12 bg-white border-2 border-black flex items-center justify-center hover:bg-gray-50 active:scale-95 transition-all"
                    title="Back to Dashboard"
                  >
                    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
                
                <div className="flex-1 space-y-3 overflow-y-auto scrollbar-hide min-h-0">
                  {todayLogs.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-full py-16 text-gray-400">
                      <p className="font-display text-xl">{t.noRecords || 'No records yet'}</p>
                    </div>
                  ) : (
                    todayLogs.slice(0, 8).map(log => (
                      <LogItem
                        key={log.id}
                        log={log}
                        onEdit={() => setEditingId(log.id)}
                        onDelete={() => setDeletingId(log.id)}
                        themeColor={settings.themeColor}
                        language={settings.language}
                      />
                    ))
                  )}
                </div>
              </PopCard>
            </div>
          </div>
        </div>

        {/* 警告提示 */}
        <div className="flex justify-center mt-2 md:mt-4 mb-4">
          {todayCount >= settings.warningLimit && todayCount < settings.dailyLimit && (
            <div className="w-full max-w-md bg-yellow-300 border-4 border-black p-3 text-center font-bold font-body animate-pulse">
              ⚠️ {t.warningMsg || 'Warning: Approaching daily limit!'}
            </div>
          )}
          
          {todayCount >= settings.dailyLimit && (
            <div className="w-full max-w-md bg-red-500 text-white border-4 border-black p-3 text-center font-bold animate-bounce font-body">
              🚫 {t.limitReachedMsg || 'Daily limit reached!'}
            </div>
          )}
        </div>
      </div>
    </>
  );
};

export default PopDashboard;
