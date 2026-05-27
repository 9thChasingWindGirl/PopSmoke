import React, { useState, useEffect, useCallback } from 'react';
import { PopButton } from './PopButton';
import { PopConfirm } from './PopConfirm';
import { OperationLog } from '../../types';
import { getSyncQueueManager } from '../../services/storageAdapter';

interface PopSyncQueueProps {
  themeColor: string;
  language?: string;
}

const TRANSLATIONS: Record<string, { [key: string]: string }> = {
  en: {
    title: 'SYNC QUEUE',
    noRecords: 'No operations in queue',
    syncAll: 'SYNC ALL',
    syncOne: 'SYNC',
    remove: 'REMOVE',
    clearAll: 'CLEAR ALL',
    create: 'CREATE',
    update: 'UPDATE',
    delete: 'DELETE',
    pending: 'PENDING',
    failed: 'FAILED',
    processing: 'PROCESSING...',
    entries: 'entries',
    confirmSyncAll: 'Are you sure you want to sync all operations?',
    confirmRemove: 'Are you sure you want to remove this operation from the queue?',
    confirmClearAll: 'Are you sure you want to clear the entire queue?'
  },
  zh: {
    title: '同步队列',
    noRecords: '队列为空',
    syncAll: '全部同步',
    syncOne: '同步',
    remove: '移除',
    clearAll: '清空队列',
    create: '新增',
    update: '更新',
    delete: '删除',
    pending: '待同步',
    failed: '失败',
    processing: '处理中...',
    entries: '条记录',
    confirmSyncAll: '确定要同步所有操作吗？',
    confirmRemove: '确定要从队列中移除这条操作吗？',
    confirmClearAll: '确定要清空整个队列吗？'
  },
  ja: {
    title: '同期キュー',
    noRecords: 'キューに操作なし',
    syncAll: '全て同期',
    syncOne: '同期',
    remove: '削除',
    clearAll: 'キュークリア',
    create: '新規',
    update: '更新',
    delete: '削除',
    pending: '保留中',
    failed: '失敗',
    processing: '処理中...',
    entries: 'エントリー',
    confirmSyncAll: '全ての操作を同期しますか？',
    confirmRemove: 'この操作をキューから削除しますか？',
    confirmClearAll: 'キューをクリアしますか？'
  },
  ko: {
    title: '동기화 큐',
    noRecords: '큐에 작업 없음',
    syncAll: '전체 동기화',
    syncOne: '동기화',
    remove: '제거',
    clearAll: '큐 비우기',
    create: '생성',
    update: '수정',
    delete: '삭제',
    pending: '대기중',
    failed: '실패',
    processing: '처리중...',
    entries: '항목',
    confirmSyncAll: '모든 작업을 동기화하시겠습니까?',
    confirmRemove: '이 작업을 큐에서 제거하시겠습니까?',
    confirmClearAll: '큐를 비우시겠습니까?'
  }
};

export const PopSyncQueue: React.FC<PopSyncQueueProps> = ({ 
  themeColor,
  language = 'en'
}) => {
  const [queue, setQueue] = useState<OperationLog[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [showSyncAllConfirm, setShowSyncAllConfirm] = useState(false);
  const [showRemoveConfirm, setShowRemoveConfirm] = useState<string | null>(null);
  const [showClearAllConfirm, setShowClearAllConfirm] = useState(false);
  
  const t = TRANSLATIONS[language] || TRANSLATIONS.en;
  const queueManager = getSyncQueueManager();

  // Load queue and listen for changes
  useEffect(() => {
    setQueue(queueManager.getQueue());
    
    const unsubscribe = queueManager.onQueueChange((newQueue) => {
      setQueue(newQueue);
      setIsProcessing(queueManager.getIsProcessing());
    });

    return unsubscribe;
  }, []);

  const handleSyncAll = useCallback(async () => {
    setShowSyncAllConfirm(false);
    setIsProcessing(true);
    try {
      await queueManager.processSyncQueue();
    } finally {
      setIsProcessing(false);
    }
  }, [queueManager]);

  const handleSyncOne = useCallback(async (operationId: string) => {
    try {
      await queueManager.processSingleOperation(operationId);
    } catch (error) {
      console.error('Failed to sync operation:', error);
    }
  }, [queueManager]);

  const handleRemove = useCallback(async (operationId: string) => {
    setShowRemoveConfirm(null);
    try {
      await queueManager.removeOperation(operationId);
    } catch (error) {
      console.error('Failed to remove operation:', error);
    }
  }, [queueManager]);

  const handleClearAll = useCallback(async () => {
    setShowClearAllConfirm(false);
    try {
      await queueManager.clearQueue();
    } catch (error) {
      console.error('Failed to clear queue:', error);
    }
  }, [queueManager]);

  const getOperationIcon = (type: OperationLog['type']) => {
    switch (type) {
      case 'create':
        return { icon: '[+]', color: 'text-green-500' };
      case 'update':
        return { icon: '[~]', color: 'text-blue-500' };
      case 'delete':
        return { icon: '[-]', color: 'text-red-500' };
      case 'clear':
        return { icon: '[🗑]', color: 'text-orange-500' };
      case 'sync':
        return { icon: '[⬇]', color: 'text-purple-500' };
      case 'system':
        return { icon: '[⚙]', color: 'text-gray-500' };
    }
  };

  const getSyncStatus = (status?: 'pending' | 'synced' | 'failed') => {
    switch (status) {
      case 'pending':
        return { label: t.pending, color: 'text-yellow-600 bg-yellow-100' };
      case 'failed':
        return { label: t.failed, color: 'text-red-600 bg-red-100' };
      default:
        return { label: t.pending, color: 'text-gray-600 bg-gray-100' };
    }
  };

  const formatTimestamp = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleString([], { 
      month: '2-digit', 
      day: '2-digit', 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  if (queue.length === 0) {
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
            themeColor={themeColor}
            onClick={() => setShowSyncAllConfirm(true)}
            disabled={isProcessing}
            className="text-xs px-2 py-0.5 h-6 flex items-center justify-center"
          >
            {isProcessing ? t.processing : t.syncAll}
          </PopButton>
          <PopButton
            themeColor="#e0e0e0"
            onClick={() => setShowClearAllConfirm(true)}
            disabled={isProcessing}
            className="text-xs px-2 py-0.5 h-6 flex items-center justify-center text-black"
          >
            {t.clearAll}
          </PopButton>
        </div>
      </div>
      
      {/* Terminal Body - Scrollable */}
      <div className="bg-white p-4 font-mono text-sm h-64 overflow-y-auto border-b-4 border-black">
        {queue.map((log) => {
          const opStyle = getOperationIcon(log.type);
          const syncStatus = getSyncStatus(log.syncStatus);
          const date = log.data?.record_date || log.data?.date || new Date(log.data?.timestamp || Date.now()).toLocaleDateString();
          const time = log.data?.record_time || new Date(log.data?.timestamp || Date.now()).toTimeString().split(' ')[0].substring(0, 5);
          
          return (
            <div
              key={log.id}
              className="flex items-center gap-2 py-2 border-b border-gray-200 last:border-b-0"
            >
              <span className="text-gray-500 shrink-0 w-20">{formatTimestamp(log.timestamp)}</span>
              <span className={`${opStyle.color} shrink-0`}>{opStyle.icon}</span>
              <span className="text-gray-800 flex-1 min-w-0 truncate">
                {log.message ? log.message : `${t[log.type] || log.type.toUpperCase()} ${date} ${time}`}
              </span>
              <span className={`shrink-0 px-2 py-0.5 rounded text-xs font-medium ${syncStatus.color}`}>
                {syncStatus.label}
              </span>
              <div className="flex gap-1 shrink-0">
                <PopButton
                  themeColor={themeColor}
                  onClick={() => handleSyncOne(log.id)}
                  disabled={isProcessing}
                  className="text-xs px-2 py-0.5 h-6 flex items-center justify-center"
                >
                  {t.syncOne}
                </PopButton>
                <PopButton
                  themeColor="#e0e0e0"
                  onClick={() => setShowRemoveConfirm(log.id)}
                  disabled={isProcessing}
                  className="text-xs px-2 py-0.5 h-6 flex items-center justify-center text-black"
                >
                  {t.remove}
                </PopButton>
              </div>
            </div>
          );
        })}
      </div>
      
      {/* Terminal Status Bar */}
      <div className="bg-gray-100 border-t-4 border-black px-4 py-1 flex justify-between items-center text-xs font-mono">
        <span className="text-gray-600">{queue.length} {t.entries}</span>
        <span className={isProcessing ? 'text-blue-600' : 'text-green-600'}>
          {isProcessing ? t.processing : 'READY'}
        </span>
      </div>

      {/* Confirmation Dialogs */}
      {showSyncAllConfirm && (
        <PopConfirm
          title={t.confirmSyncAll}
          message={t.confirmSyncAll}
          confirmThemeColor={themeColor}
          onConfirm={handleSyncAll}
          onCancel={() => setShowSyncAllConfirm(false)}
        />
      )}
      
      {showRemoveConfirm && (
        <PopConfirm
          title={t.confirmRemove}
          message={t.confirmRemove}
          confirmThemeColor={themeColor}
          onConfirm={() => handleRemove(showRemoveConfirm)}
          onCancel={() => setShowRemoveConfirm(null)}
        />
      )}
      
      {showClearAllConfirm && (
        <PopConfirm
          title={t.confirmClearAll}
          message={t.confirmClearAll}
          confirmThemeColor={themeColor}
          onConfirm={handleClearAll}
          onCancel={() => setShowClearAllConfirm(false)}
        />
      )}
    </div>
  );
};
