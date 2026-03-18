import React, { useState, useRef, useEffect } from 'react';
import { PopButton } from './PopButton';
import { OperationLog as OperationLogType } from '../../types';

interface PopOperationLogProps {
  logs: OperationLogType[];
  onClear?: () => void;
  themeColor: string;
  language?: string;
  apiFetchedCount?: number;
}

const TRANSLATIONS: Record<string, { [key: string]: string }> = {
  en: {
    title: 'OPERATION LOG',
    noRecords: 'No operation records',
    pause: 'PAUSE',
    resume: 'RESUME',
    clear: 'CLEAR',
    create: 'CREATE',
    update: 'UPDATE',
    delete: 'DELETE',
    clearAll: 'CLEAR ALL',
    sync: 'SYNC',
    synced: 'SYNCED',
    pending: 'PENDING',
    failed: 'FAILED',
    entries: 'entries',
    paused: 'PAUSED',
    live: 'LIVE'
  },
  zh: {
    title: '操作日志',
    noRecords: '暂无操作记录',
    pause: '暂停',
    resume: '继续',
    clear: '清空',
    create: '新增',
    update: '更新',
    delete: '删除',
    clearAll: '清除本地数据',
    sync: '同步',
    synced: '已同步',
    pending: '待同步',
    failed: '失败',
    entries: '条记录',
    paused: '已暂停',
    live: '实时'
  },
  ja: {
    title: '操作ログ',
    noRecords: '操作記録なし',
    pause: '一時停止',
    resume: '再開',
    clear: 'クリア',
    create: '新規',
    update: '更新',
    delete: '削除',
    clearAll: 'ローカルデータをクリア',
    sync: '同期',
    synced: '同期済',
    pending: '保留中',
    failed: '失敗',
    entries: 'エントリー',
    paused: '一時停止',
    live: 'ライブ'
  },
  ko: {
    title: '작업 로그',
    noRecords: '작업 기록 없음',
    pause: '일시정지',
    resume: '계속',
    clear: '지우기',
    create: '생성',
    update: '수정',
    delete: '삭제',
    clearAll: '로컬 데이터 지우기',
    sync: '동기화',
    synced: '동기화됨',
    pending: '대기중',
    failed: '실패',
    entries: '항목',
    paused: '일시정지',
    live: '라이브'
  }
};

export const PopOperationLog: React.FC<PopOperationLogProps> = ({ 
  logs, 
  onClear, 
  themeColor,
  language = 'en',
  apiFetchedCount
}) => {
  const [isPaused, setIsPaused] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);
  const t = TRANSLATIONS[language] || TRANSLATIONS.en;

  // Auto-scroll to bottom when new logs arrive (if not paused)
  useEffect(() => {
    if (!isPaused && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [logs, isPaused]);

  const getOperationPrefix = (type: 'create' | 'update' | 'delete' | 'clear' | 'sync') => {
    switch (type) {
      case 'create':
        return { prefix: '[+]', color: 'text-green-400' };
      case 'update':
        return { prefix: '[~]', color: 'text-blue-400' };
      case 'delete':
        return { prefix: '[-]', color: 'text-red-400' };
      case 'clear':
        return { prefix: '[🗑]', color: 'text-orange-400' };
      case 'sync':
        return { prefix: '[⬇]', color: 'text-purple-400' };
    }
  };

  const getSyncStatusPrefix = (status?: 'pending' | 'synced' | 'failed') => {
    switch (status) {
      case 'pending':
        return { prefix: '[⏳]', color: 'text-yellow-400', label: t.pending };
      case 'synced':
        return { prefix: '[✓]', color: 'text-green-400', label: t.synced };
      case 'failed':
        return { prefix: '[✗]', color: 'text-red-400', label: t.failed };
      default:
        return { prefix: '[?]', color: 'text-gray-400', label: '?' };
    }
  };

  const formatTimestamp = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  if (logs.length === 0) {
    return (
      <div className="p-0 overflow-hidden">
        {/* Terminal Header */}
        <div className="bg-black border-b-4 border-black px-4 py-2 flex justify-between items-center">
          <h3 className="font-display text-sm text-white uppercase tracking-wider">
            {t.title}
          </h3>
        </div>
        {/* Terminal Body */}
        <div className="bg-white p-4 font-mono text-sm border-b-4 border-black">
          <p className="text-gray-500 italic">{t.noRecords}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-0 overflow-hidden">
      {/* Terminal Header */}
      <div className="bg-black border-b-4 border-black px-4 py-2 flex justify-between items-center">
        <h3 className="font-display text-sm text-white uppercase tracking-wider">
          {t.title}
        </h3>
        <div className="flex gap-2">
          <PopButton
            themeColor="#e0e0e0"
            onClick={() => setIsPaused(!isPaused)}
            className="text-xs px-2 py-0.5 h-6 flex items-center justify-center text-black"
          >
            {isPaused ? t.resume : t.pause}
          </PopButton>
          {onClear && (
            <PopButton
              themeColor={themeColor}
              onClick={onClear}
              className="text-xs px-2 py-0.5 h-6 flex items-center justify-center"
            >
              {t.clear}
            </PopButton>
          )}
        </div>
      </div>
      
      {/* Terminal Body - Scrollable */}
      <div 
        ref={scrollRef}
        className="bg-white p-4 font-mono text-sm h-48 overflow-y-auto border-b-4 border-black"
      >
        {logs.map((log, index) => {
          const opStyle = getOperationPrefix(log.type);
          const syncStyle = getSyncStatusPrefix(log.syncStatus);
          const date = log.data.record_date || log.data.date || new Date(log.data.timestamp).toLocaleDateString();
          const time = log.data.record_time || new Date(log.data.timestamp).toTimeString().split(' ')[0].substring(0, 5);
          
          return (
            <div
              key={log.id || index}
              className="flex items-start gap-2 py-1 border-b border-gray-200 last:border-b-0"
            >
              <span className="text-gray-500 shrink-0">{formatTimestamp(log.timestamp)}</span>
              <span className={`${opStyle.color} shrink-0`}>{opStyle.prefix}</span>
              <span className="text-gray-800 flex-1 min-w-0 truncate">
                {log.message ? log.message : `${t[log.type] || log.type.toUpperCase()} ${date} ${time}`}
              </span>
              <span className={`${syncStyle.color} shrink-0`}>{syncStyle.prefix}</span>
            </div>
          );
        })}
      </div>
      
      {/* Terminal Status Bar */}
      <div className="bg-gray-100 border-t-4 border-black px-4 py-1 flex justify-between items-center text-xs font-mono">
        <div className="flex gap-4">
          <span className="text-gray-600">{logs.length} {t.entries}</span>
          {apiFetchedCount !== undefined && (
            <span className="text-blue-600">
              API: {apiFetchedCount}
            </span>
          )}
        </div>
        <span className={isPaused ? 'text-yellow-600' : 'text-green-600'}>
          {isPaused ? t.paused : t.live}
        </span>
      </div>
    </div>
  );
};
