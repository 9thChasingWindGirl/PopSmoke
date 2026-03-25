import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { POP_DESIGN_SYSTEM, POP_COMPONENT_STYLES } from '../../styles';
import { TRANSLATIONS } from '../../i18n';
import { isAndroidPlatform } from '../../services/storageAdapter';

interface LogEntry {
  id: string;
  timestamp: number;
  level: 'debug' | 'info' | 'warn' | 'error';
  category: string;
  message: string;
  data?: any;
}

interface PopSystemLogProps {
  visible: boolean;
  onClose: () => void;
  themeColor?: string;
  language?: string;
}

export const PopSystemLog: React.FC<PopSystemLogProps> = ({
  visible,
  onClose,
  themeColor = POP_DESIGN_SYSTEM.colors.theme.gold,
  language = 'en'
}) => {
  const [isVisible, setIsVisible] = useState(false);
  const [isClosing, setIsClosing] = useState(false);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [filter, setFilter] = useState<'all' | 'debug' | 'info' | 'warn' | 'error'>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const logsEndRef = useRef<HTMLDivElement>(null);
  const t = TRANSLATIONS[language as keyof typeof TRANSLATIONS];

  // 拦截原始 console 方法
  useEffect(() => {
    if (!isAndroidPlatform()) return;

    const originalConsole = {
      log: console.log,
      info: console.info,
      warn: console.warn,
      error: console.error,
      debug: console.debug
    };

    const addLog = (level: LogEntry['level'], args: any[]) => {
      const message = args.map(arg => 
        typeof arg === 'object' ? JSON.stringify(arg) : String(arg)
      ).join(' ');

      const newLog: LogEntry = {
        id: `log_${Date.now()}_${Math.random()}`,
        timestamp: Date.now(),
        level,
        category: 'console',
        message: message.substring(0, 500) // 限制长度
      };

      setLogs(prev => {
        const updated = [newLog, ...prev].slice(0, 1000); // 最多保留1000条
        return updated;
      });
    };

    console.log = (...args) => {
      addLog('info', args);
      originalConsole.log.apply(console, args);
    };

    console.info = (...args) => {
      addLog('info', args);
      originalConsole.info.apply(console, args);
    };

    console.warn = (...args) => {
      addLog('warn', args);
      originalConsole.warn.apply(console, args);
    };

    console.error = (...args) => {
      addLog('error', args);
      originalConsole.error.apply(console, args);
    };

    console.debug = (...args) => {
      addLog('debug', args);
      originalConsole.debug.apply(console, args);
    };

    return () => {
      console.log = originalConsole.log;
      console.info = originalConsole.info;
      console.warn = originalConsole.warn;
      console.error = originalConsole.error;
      console.debug = originalConsole.debug;
    };
  }, []);

  useEffect(() => {
    if (visible) {
      setIsVisible(true);
      setIsClosing(false);
    }
  }, [visible]);

  const handleClose = () => {
    setIsClosing(true);
    setTimeout(() => {
      setIsVisible(false);
      onClose();
    }, 300);
  };

  const clearLogs = () => {
    setLogs([]);
  };

  const exportLogs = () => {
    const logText = filteredLogs.map(log => 
      `[${new Date(log.timestamp).toLocaleString()}] [${log.level.toUpperCase()}] ${log.message}`
    ).join('\n');
    
    const blob = new Blob([logText], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `system-logs-${new Date().toISOString().split('T')[0]}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const filteredLogs = logs.filter(log => {
    if (filter !== 'all' && log.level !== filter) return false;
    if (searchTerm && !log.message.toLowerCase().includes(searchTerm.toLowerCase())) return false;
    return true;
  });

  const getLevelColor = (level: LogEntry['level']) => {
    switch (level) {
      case 'error': return '#ff4757';
      case 'warn': return '#ffa502';
      case 'info': return '#2ed573';
      case 'debug': return '#74b9ff';
      default: return '#a4b0be';
    }
  };

  if (!isVisible || !isAndroidPlatform()) {
    return null;
  }

  const dialogContent = (
    <div 
      className={POP_COMPONENT_STYLES.cloudDataDialog.overlay}
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
    >
      <div
        className={`${POP_COMPONENT_STYLES.cloudDataDialog.content} max-w-4xl max-h-[90vh] flex flex-col ${
          isClosing ? 'opacity-0 scale-95' : 'opacity-100 scale-100'
        }`}
        onClick={(e) => e.stopPropagation()}
      >
        {/* 标题栏 */}
        <div className="flex items-center justify-between mb-4">
          <h2 className={POP_COMPONENT_STYLES.cloudDataDialog.title}>
            System Logs (Android)
          </h2>
          <button
            onClick={handleClose}
            className={POP_COMPONENT_STYLES.cloudDataDialog.closeButton}
          >
            ×
          </button>
        </div>

        {/* 工具栏 */}
        <div className="flex flex-wrap gap-2 mb-4">
          {/* 筛选器 */}
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value as any)}
            className="px-3 py-2 border-4 border-black font-display text-sm"
          >
            <option value="all">All Levels</option>
            <option value="error">Error</option>
            <option value="warn">Warning</option>
            <option value="info">Info</option>
            <option value="debug">Debug</option>
          </select>

          {/* 搜索框 */}
          <input
            type="text"
            placeholder="Search logs..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="flex-1 min-w-[200px] px-3 py-2 border-4 border-black font-display text-sm"
          />

          {/* 操作按钮 */}
          <button
            onClick={clearLogs}
            className="px-4 py-2 border-4 border-black font-display text-sm bg-red-400 hover:bg-red-500 transition-colors"
          >
            Clear
          </button>
          <button
            onClick={exportLogs}
            className="px-4 py-2 border-4 border-black font-display text-sm hover:bg-gray-100 transition-colors"
            style={{ backgroundColor: themeColor }}
          >
            Export
          </button>
        </div>

        {/* 日志统计 */}
        <div className="flex gap-4 mb-2 text-sm font-display">
          <span style={{ color: '#ff4757' }}>Errors: {logs.filter(l => l.level === 'error').length}</span>
          <span style={{ color: '#ffa502' }}>Warnings: {logs.filter(l => l.level === 'warn').length}</span>
          <span style={{ color: '#2ed573' }}>Info: {logs.filter(l => l.level === 'info').length}</span>
          <span>Total: {logs.length}</span>
          <span>Showing: {filteredLogs.length}</span>
        </div>

        {/* 日志列表 */}
        <div className="flex-1 overflow-y-auto border-4 border-black bg-gray-900 p-2 font-mono text-xs">
          {filteredLogs.length === 0 ? (
            <div className="text-gray-500 text-center py-8">No logs to display</div>
          ) : (
            filteredLogs.map((log) => (
              <div
                key={log.id}
                className="mb-1 p-2 hover:bg-gray-800 transition-colors"
              >
                <div className="flex items-start gap-2">
                  <span className="text-gray-500 shrink-0">
                    {new Date(log.timestamp).toLocaleTimeString()}
                  </span>
                  <span
                    className="shrink-0 font-bold px-1 rounded"
                    style={{ 
                      backgroundColor: getLevelColor(log.level),
                      color: '#000'
                    }}
                  >
                    {log.level.toUpperCase()}
                  </span>
                  <span className="text-white break-all whitespace-pre-wrap">
                    {log.message}
                  </span>
                </div>
              </div>
            ))
          )}
          <div ref={logsEndRef} />
        </div>
      </div>
    </div>
  );

  return createPortal(dialogContent, document.body);
};

export default PopSystemLog;
