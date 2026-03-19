import React, { useMemo, useState } from 'react';
import { PopButton } from '../ui/PopButton';
import { PopCard } from '../ui/PopCard';
import { PopTimePicker } from '../ui/PopTimePicker';
import { SmokeLog, AppSettings } from '../../types';
import { TRANSLATIONS } from '../../i18n';
import { 
  POP_COMPONENT_STYLES,
  getFlipCardHeight,
  getFlipCardTransform,
  getBackfaceHidden,
  getGaugeStyle,
  getCountBadgeStyle
} from '../../styles';

interface PopDashboardProps {
  logs: SmokeLog[];
  settings: AppSettings;
  onRecord: () => void;
  onDelete: (id: string) => void;
  onUpdate: (log: SmokeLog) => void;
}

export const PopDashboard: React.FC<PopDashboardProps> = ({ logs, settings, onRecord, onDelete, onUpdate }) => {
  const t = TRANSLATIONS[settings.language];
  const [isFlipped, setIsFlipped] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const todayLogs = useMemo(() => {
    const now = new Date();
    const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    return logs
      .filter(l => l.timestamp >= startOfDay)
      .sort((a, b) => b.timestamp - a.timestamp);
  }, [logs]);

  const todayCount = todayLogs.length;
  const percentage = Math.min((todayCount / settings.dailyLimit) * 100, 100);
  
  const gaugeColor = todayCount >= settings.dailyLimit 
    ? '#FF4d4d' 
    : todayCount >= settings.warningLimit 
      ? '#FFA500' 
      : '#4CAF50';

  const lastSmokeTime = useMemo(() => {
    if (todayLogs.length === 0) return t.never;
    return new Date(todayLogs[0].timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }, [todayLogs, t]);

  const startEditing = (log: SmokeLog) => {
    setEditingId(log.id);
  };

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

  const cancelEditing = () => {
      setEditingId(null);
  };

  return (
    <div className={POP_COMPONENT_STYLES.dashboard.mainContainer}>
      
      <div className={POP_COMPONENT_STYLES.dashboard.titleHeader}>
        <h1 className="font-display text-3xl md:text-5xl lg:text-6xl text-center leading-none uppercase">
          {t.appTitle}<br/><span style={{ color: settings.themeColor }}>{t.appSubtitle}</span>
        </h1>
      </div>

      <div className={POP_COMPONENT_STYLES.dashboard.flipContainer} style={getFlipCardHeight()}>
        <div className="relative w-full h-full transition-all duration-700 preserve-3d" style={getFlipCardTransform(isFlipped)}>
          
          <div 
            className="w-full h-full absolute top-0 left-0"
            style={getBackfaceHidden()}
          >
            <PopCard className="flex flex-col items-center py-4 justify-evenly h-full overflow-hidden relative">
              
              <div className="flex flex-col items-center w-full gap-3 md:gap-4 shrink-0 max-w-sm">
                  <div className={POP_COMPONENT_STYLES.gauge.card}>
                      <h2 className={POP_COMPONENT_STYLES.gauge.header}>{t.currentGaugeLevel || 'Current Gauge Level'}</h2>
                      <div className={POP_COMPONENT_STYLES.gauge.container}>
                         <div className={POP_COMPONENT_STYLES.gauge.background}></div>
                         <div
                          className={POP_COMPONENT_STYLES.gauge.fill}
                          style={getGaugeStyle(gaugeColor, percentage)}
                         ></div>
                         <div className={POP_COMPONENT_STYLES.gauge.paper}></div>
                         <div className={POP_COMPONENT_STYLES.gauge.count}>
                           <span className={POP_COMPONENT_STYLES.gauge.countValue}>{todayCount}</span>
                           <span className={POP_COMPONENT_STYLES.gauge.countLabel}> / {settings.dailyLimit}</span>
                         </div>
                      </div>
                      <div className={POP_COMPONENT_STYLES.gauge.footer}>
                        <div className={POP_COMPONENT_STYLES.gauge.footerLabel}>
                          <span className={POP_COMPONENT_STYLES.gauge.footerLabelName}>Limit</span>
                          <span className={POP_COMPONENT_STYLES.gauge.footerLabelValue}>{settings.dailyLimit} {t.smokeUnit}</span>
                        </div>
                        <div className={POP_COMPONENT_STYLES.gauge.footerLabel}>
                          <span className={POP_COMPONENT_STYLES.gauge.footerLabelName}>Remains</span>
                          <span className={POP_COMPONENT_STYLES.gauge.footerLabelValueRemains} style={{ color: gaugeColor }}>{Math.round((1 - todayCount / settings.dailyLimit) * 100)}%</span>
                        </div>
                      </div>
                  </div>

                  <div className="text-center">
                      <p className="font-body text-xs md:text-sm lg:text-base font-bold text-gray-400 uppercase tracking-widest">{t.lastRecord}</p>
                      <p className="font-display text-3xl md:text-4xl lg:text-5xl font-black italic transform -translate-x-1">{lastSmokeTime}</p>
                  </div>
              </div>

              <div className="flex flex-col items-center w-full">
                  <div className="relative pb-4">
                    <div className="absolute inset-0 bg-black/10 blur-2xl rounded-2xl scale-110"></div>
                    <div className={POP_COMPONENT_STYLES.dashboard.smokeButton.container}>
                      <button
                        onClick={onRecord}
                        className="w-full h-full"
                      >
                        <div className={POP_COMPONENT_STYLES.dashboard.smokeButton.shadow}></div>
                        <div
                          className={POP_COMPONENT_STYLES.dashboard.smokeButton.button}
                          style={{ backgroundColor: settings.themeColor }}
                        >
                          <div className="flex flex-col items-center -mt-1">
                            <span className="text-2xl md:text-3xl lg:text-4xl font-display font-black text-black uppercase italic tracking-tighter leading-none drop-shadow-[1px_1px_0px_rgba(255,255,255,0.5)] transform -translate-x-1">{t.smokeButton}</span>
                          </div>
                          <div className="absolute top-6 left-10 w-6 h-3 bg-white/40 rounded-full rotate-[-45deg]"></div>
                          <div className="absolute bottom-4 left-0 right-0 flex justify-center">
                            <div 
                              onClick={(e) => {
                                e.stopPropagation();
                                setIsFlipped(true);
                              }}
                              className={POP_COMPONENT_STYLES.dashboard.smokeButton.moreLink}
                            >
                              {t.more} &rarr;
                            </div>
                          </div>
                        </div>
                      </button>
                    </div>
                  </div>
              </div>

            </PopCard>
          </div>

          <div 
            className="w-full h-full absolute top-0 left-0"
            style={{ 
              ...getBackfaceHidden(),
              transform: 'rotateY(180deg)'
            }}
          >
             <PopCard className="flex flex-col pt-4 pb-16 relative h-full overflow-hidden">
                {editingId && (
                    <PopTimePicker 
                        initialTimestamp={logs.find(l => l.id === editingId)?.timestamp || Date.now()}
                        onSave={saveEditing}
                        onCancel={cancelEditing}
                        themeColor={settings.themeColor}
                        language={settings.language}
                    />
                )}

                <div className="flex justify-center border-b-4 border-black pb-2 mb-2 shrink-0">
                   <h2 className="font-display text-xl uppercase">{t.todayLogs}</h2>
                </div>
                
                <div className="space-y-2 pr-1 overflow-y-auto flex-1">
                  {todayLogs.length === 0 && (
                    <div className="text-center text-gray-400 py-10 italic text-sm">{t.noRecords}</div>
                  )}
                  {todayLogs.map(log => (
                    <div key={log.id} className="bg-white border-2 border-black p-2 shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] flex items-center justify-between">
                        <span className="font-display text-lg">
                            {new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                        <div className="space-x-1 flex">
                             <button 
                                onClick={() => startEditing(log)}
                                className="text-[10px] font-bold bg-yellow-200 border border-black px-2 py-1 hover:bg-yellow-300 transition-colors uppercase"
                              >
                                {t.edit}
                              </button>
                             <button 
                                onClick={() => onDelete(log.id)}
                                className="text-[10px] font-bold bg-red-200 border border-black px-2 py-1 hover:bg-red-300 transition-colors cursor-pointer active:scale-95 uppercase"
                              >
                                {t.delete}
                              </button>
                        </div>
                    </div>
                  ))}
                </div>

                <div className="absolute bottom-4 left-0 right-0 flex justify-center">
                   <button 
                      onClick={() => setIsFlipped(false)} 
                      className="text-sm font-bold underline hover:text-gray-600 decoration-2 underline-offset-4 font-display tracking-wide uppercase p-1"
                   >
                      &larr; {t.back}
                   </button>
                </div>
             </PopCard>
          </div>

        </div>
      </div>

      <div className={POP_COMPONENT_STYLES.dashboard.quickStatus}>
        {todayCount >= settings.warningLimit && todayCount < settings.dailyLimit && (
            <div className="bg-yellow-300 border-4 border-black p-1 w-full text-center font-bold font-body animate-pulse text-sm">
                {t.warningMsg}
            </div>
        )}
        
        {todayCount >= settings.dailyLimit && (
            <div className="bg-red-500 text-white border-4 border-black p-1 w-full text-center font-bold animate-bounce font-body text-sm">
                {t.limitReachedMsg}
            </div>
        )}
      </div>

    </div>
  );
};
