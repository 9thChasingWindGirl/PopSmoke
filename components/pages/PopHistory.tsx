import React, { useState, useMemo } from 'react';
import { PopCard } from '../ui/PopCard';
import { PopButton } from '../ui/PopButton';
import { PopDatePicker } from '../ui/PopDatePicker';
import { SmokeLog, AppSettings, HistoryFilters, HistorySort, HistoryPagination } from '../../types';
import { HISTORY_PAGE_SIZE } from '../../constants';
import { TRANSLATIONS } from '../../i18n';
import { getCountBadgeStyle } from '../../styles';

interface PopHistoryProps {
  logs: SmokeLog[];
  settings: AppSettings;
  userId?: string;
  onLoadMore?: (page: number) => Promise<SmokeLog[]>;
  hasMore?: boolean;
  loading?: boolean;
}

interface DateSummary {
  date: string;
  count: number;
  logs: SmokeLog[];
}

interface MonthSummary {
  month: string;
  monthName: string;
  count: number;
  days: DateSummary[];
}

interface GroupedLogs {
  [date: string]: SmokeLog[];
}

export const PopHistory: React.FC<PopHistoryProps> = ({ logs, settings, onLoadMore, hasMore = false, loading = false }) => {
  const t = TRANSLATIONS[settings.language];
  
  const [filters, setFilters] = useState<HistoryFilters>({});
  const [sort, setSort] = useState<HistorySort>({ field: 'date', direction: 'desc' });
  const [pagination, setPagination] = useState<HistoryPagination>({
    currentPage: 1,
    pageSize: HISTORY_PAGE_SIZE,
    totalCount: 0
  });
  const [expandedMonth, setExpandedMonth] = useState<string | null>(null);
  const [expandedDay, setExpandedDay] = useState<string | null>(null);
  
  const handleLoadMore = async () => {
    if (onLoadMore && !loading) {
      const nextPage = pagination.currentPage + 1;
      const moreLogs = await onLoadMore(nextPage);
      if (moreLogs.length > 0) {
        setPagination(prev => ({
          ...prev,
          currentPage: nextPage,
          totalCount: prev.totalCount + moreLogs.length
        }));
      }
    }
  };
  
  const formatDate = (dateStr: string): string => {
    const date = new Date(dateStr);
    return date.toLocaleDateString(settings.language, {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'long'
    });
  };
  
  const formatTime = (timestamp: number): string => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString(settings.language, {
      hour: '2-digit',
      minute: '2-digit',
      hour12: settings.language === 'en'
    });
  };
  
  const formatMonth = (monthStr: string): string => {
    const date = new Date(monthStr + '-01');
    return date.toLocaleDateString(settings.language, {
      year: 'numeric',
      month: 'long'
    });
  };
  
  const toggleMonth = (month: string) => {
    if (expandedMonth === month) {
      setExpandedMonth(null);
      setExpandedDay(null);
    } else {
      setExpandedMonth(month);
      setExpandedDay(null);
    }
  };
  
  const toggleDay = (date: string) => {
    if (expandedDay === date) {
      setExpandedDay(null);
    } else {
      setExpandedDay(date);
    }
  };
  
  const groupedByMonth = useMemo(() => {
    const groupedByDate: GroupedLogs = {};
    
    logs.forEach(log => {
      const date = new Date(log.timestamp);
      const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
      
      if (!groupedByDate[dateStr]) {
        groupedByDate[dateStr] = [];
      }
      groupedByDate[dateStr].push(log);
    });
    
    let dateSummaries: DateSummary[] = Object.entries(groupedByDate).map(([date, dateLogs]) => ({
      date,
      count: dateLogs.length,
      logs: dateLogs
    }));
    
    if (filters.startDate) {
      dateSummaries = dateSummaries.filter(item => item.date >= filters.startDate!);
    }
    if (filters.endDate) {
      dateSummaries = dateSummaries.filter(item => item.date <= filters.endDate!);
    }
    
    if (filters.minCount !== undefined) {
      dateSummaries = dateSummaries.filter(item => item.count >= filters.minCount!);
    }
    if (filters.maxCount !== undefined) {
      dateSummaries = dateSummaries.filter(item => item.count <= filters.maxCount!);
    }
    
    dateSummaries.sort((a, b) => {
      return sort.direction === 'asc' 
        ? a.date.localeCompare(b.date)
        : b.date.localeCompare(a.date);
    });
    
    const groupedByMonth: Record<string, DateSummary[]> = {};
    
    dateSummaries.forEach(day => {
      const month = day.date.slice(0, 7);
      if (!groupedByMonth[month]) {
        groupedByMonth[month] = [];
      }
      groupedByMonth[month].push(day);
    });
    
    const monthSummaries: MonthSummary[] = Object.entries(groupedByMonth).map(([month, days]) => {
      const totalCount = days.reduce((sum, day) => sum + day.count, 0);
      
      return {
        month,
        monthName: formatMonth(month),
        count: totalCount,
        days
      };
    });
    
    monthSummaries.sort((a, b) => {
      if (sort.field === 'count') {
        return sort.direction === 'asc' 
          ? a.count - b.count
          : b.count - a.count;
      } else {
        return sort.direction === 'asc' 
          ? a.month.localeCompare(b.month)
          : b.month.localeCompare(a.month);
      }
    });
    
    setPagination(prev => ({
      ...prev,
      totalCount: monthSummaries.length
    }));
    
    return monthSummaries;
  }, [logs, filters, sort, settings.language]);
  
  const paginatedMonths = useMemo(() => {
    const startIndex = (pagination.currentPage - 1) * pagination.pageSize;
    const endIndex = startIndex + pagination.pageSize;
    return groupedByMonth.slice(startIndex, endIndex);
  }, [groupedByMonth, pagination]);
  
  const handleFilterChange = (key: keyof HistoryFilters, value: any) => {
    setFilters(prev => ({
      ...prev,
      [key]: value
    }));
    setPagination(prev => ({
      ...prev,
      currentPage: 1
    }));
  };
  
  const handleSortChange = (field: HistorySort['field']) => {
    setSort(prev => ({
      field,
      direction: prev.field === field && prev.direction === 'asc' ? 'desc' : 'asc'
    }));
  };
  
  const handlePageChange = (page: number) => {
    setPagination(prev => ({
      ...prev,
      currentPage: page
    }));
  };
  
  const resetFilters = () => {
    setFilters({});
    setPagination(prev => ({
      ...prev,
      currentPage: 1
    }));
  };
  
  return (
    <div className="w-full max-w-3xl mx-auto space-y-6 pt-8 pb-8">
      <div className="text-center">
        <h1 className="font-display text-4xl md:text-5xl mb-2">
          {t.history}
        </h1>
        <p className="text-gray-800 font-medium font-body font-bold bg-white border-2 border-black px-3 py-1 shadow-pop transform rotate-1 inline-block">{t.totalCigarettes}: {logs.length}</p>
      </div>
      
      <PopCard title={t.filtersAndSort} className="mb-6">
        <div className="space-y-4 mt-2">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2 md:gap-3">
            <div>
              <label className="block font-bold mb-1 text-sm md:text-base">{t.startDate}</label>
              <PopDatePicker
                value={filters.startDate || ''}
                onChange={(date) => handleFilterChange('startDate', date || undefined)}
                placeholder={t.selectStartDate || 'Select start date'}
                className="text-sm md:text-base"
                translations={t}
              />
            </div>
            <div>
              <label className="block font-bold mb-1 text-sm md:text-base">{t.endDate}</label>
              <PopDatePicker
                value={filters.endDate || ''}
                onChange={(date) => handleFilterChange('endDate', date || undefined)}
                placeholder={t.selectEndDate || 'Select end date'}
                className="text-sm md:text-base"
                translations={t}
              />
            </div>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2 md:gap-3">
            <div>
              <label className="block font-bold mb-1 text-sm md:text-base">{t.minCount}</label>
              <input
                type="number"
                min="0"
                value={filters.minCount || ''}
                onChange={(e) => handleFilterChange('minCount', e.target.value ? parseInt(e.target.value) : undefined)}
                className="w-full border-4 border-black p-1 md:p-2 font-display text-sm md:text-base"
                placeholder="0"
              />
            </div>
            <div>
              <label className="block font-bold mb-1 text-sm md:text-base">{t.maxCount}</label>
              <input
                type="number"
                min="1"
                value={filters.maxCount || ''}
                onChange={(e) => handleFilterChange('maxCount', e.target.value ? parseInt(e.target.value) : undefined)}
                className="w-full border-4 border-black p-1 md:p-2 font-display text-sm md:text-base"
                placeholder="99"
              />
            </div>
          </div>
          
          <div className="flex flex-col space-y-2 md:flex-row md:space-x-2 lg:space-x-3">
            <PopButton
              onClick={() => handleSortChange('date')}
              themeColor={sort.field === 'date' ? settings.themeColor : '#e0e0e0'}
              className="flex-1"
            >
              {t.sortByDate} {sort.field === 'date' && (sort.direction === 'asc' ? '↑' : '↓')}
            </PopButton>
            <PopButton
              onClick={() => handleSortChange('count')}
              themeColor={sort.field === 'count' ? settings.themeColor : '#e0e0e0'}
              className="flex-1"
            >
              {t.sortByCount} {sort.field === 'count' && (sort.direction === 'asc' ? '↑' : '↓')}
            </PopButton>
            <PopButton
              onClick={resetFilters}
              themeColor="#FF4d4d"
              className="flex-1"
            >
              {t.resetFilters}
            </PopButton>
          </div>
        </div>
      </PopCard>
      
      {paginatedMonths.length > 0 ? (
        <div className="space-y-6">
          {paginatedMonths.map((monthSummary) => (
            <div key={monthSummary.month} className="space-y-3">
              <PopCard className="overflow-hidden">
                <div 
                  className="p-4 flex justify-between items-center bg-gray-50 border-b-2 border-black cursor-pointer shadow-pop transition-all transform hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px]"
                  onClick={() => toggleMonth(monthSummary.month)}
                >
                  <div className="flex items-center space-x-3">
                    <span className="text-xl">
                      {expandedMonth === monthSummary.month ? '▼' : '▶'}
                    </span>
                    <h2 className="font-display text-xl cursor-pointer" onClick={(e) => { e.stopPropagation(); toggleMonth(monthSummary.month); }}>
                      {monthSummary.monthName}
                    </h2>
                  </div>
                  <div className="flex items-center space-x-3">
                    <span 
                      className="font-bold text-lg bg-black text-white px-3 py-1 transform -rotate-2 cursor-pointer border-2 border-black"
                      style={getCountBadgeStyle(settings.themeColor)}
                      onClick={(e) => { e.stopPropagation(); toggleMonth(monthSummary.month); }}
                    >
                      {monthSummary.count}
                    </span>
                  </div>
                </div>
                
                {expandedMonth === monthSummary.month && (
                  <div className="space-y-3 p-4 bg-white">
                    {monthSummary.days.map((daySummary) => (
                      <div key={daySummary.date} className="space-y-2">
                        <div 
                          className="p-3 flex justify-between items-center bg-gray-100 border-l-4 border-black cursor-pointer shadow-pop transition-all transform hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px]"
                          onClick={() => toggleDay(daySummary.date)}
                        >
                          <div className="flex items-center space-x-3">
                            <span className="text-lg">
                              {expandedDay === daySummary.date ? '▼' : '▶'}
                            </span>
                            <h3 className="font-display text-lg cursor-pointer" onClick={(e) => { e.stopPropagation(); toggleDay(daySummary.date); }}>
                              {formatDate(daySummary.date)}
                            </h3>
                          </div>
                          <div className="flex items-center space-x-3">
                            <span 
                              className="font-bold text-md bg-yellow-400 text-black px-2 py-1 border-2 border-black transform rotate-1 cursor-pointer"
                              onClick={(e) => { e.stopPropagation(); toggleDay(daySummary.date); }}
                            >
                              {daySummary.count}
                            </span>
                          </div>
                        </div>
                        
                        {expandedDay === daySummary.date && (
                          <div className="pl-6 space-y-2">
                            {daySummary.logs
                              .sort((a, b) => b.timestamp - a.timestamp)
                              .map((log) => (
                                <div
                                  key={log.id}
                                  className="flex justify-between items-center p-3 bg-gray-50 hover:bg-gray-100 transition-colors border-l-2 border-black"
                                >
                                  <div>
                                    <p className="font-bold">
                                      {formatTime(log.timestamp)}
                                    </p>
                                  </div>
                                  <span className="text-xs text-gray-500">
                                    ID: {log.id.slice(0, 8)}...
                                  </span>
                                </div>
                              ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </PopCard>
            </div>
          ))}
        </div>
      ) : (
        <PopCard className="text-center p-8">
          <p className="text-gray-800 font-medium text-lg">
            {filters.startDate || filters.endDate || filters.minCount !== undefined || filters.maxCount !== undefined
              ? t.noMatchingRecords
              : t.noRecords}
          </p>
        </PopCard>
      )}
      
      {groupedByMonth.length > pagination.pageSize && (
        <div className="flex justify-center items-center space-x-3 mt-6">
          {pagination.currentPage > 1 && (
            <PopButton
              onClick={() => handlePageChange(Math.max(1, pagination.currentPage - 1))}
              themeColor={settings.themeColor}
            >
              {t.prev}
            </PopButton>
          )}
          
          <div className="flex space-x-1">
            <span className="font-bold">
              {pagination.currentPage} / {Math.ceil(pagination.totalCount / pagination.pageSize)}
            </span>
          </div>
          
          {pagination.currentPage < Math.ceil(pagination.totalCount / pagination.pageSize) && (
            <PopButton
              onClick={() => handlePageChange(Math.min(
                Math.ceil(pagination.totalCount / pagination.pageSize),
                pagination.currentPage + 1
              ))}
              themeColor={settings.themeColor}
            >
              {t.next}
            </PopButton>
          )}
        </div>
      )}
      
      {hasMore && onLoadMore && (
        <div className="flex justify-center items-center mt-6">
          <PopButton
            onClick={handleLoadMore}
            disabled={loading}
            themeColor={settings.themeColor}
            className="min-w-[200px]"
          >
            {loading ? `${t.loading}...` : `${t.loadMore} (${pagination.currentPage * HISTORY_PAGE_SIZE}+)`}
          </PopButton>
        </div>
      )}
      
      <PopCard className="mt-6">
        <div className="p-4">
          <h3 className="font-display text-lg mb-2 border-b-2 border-black pb-1">{t.stats}</h3>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <p className="text-sm text-gray-800 font-medium">{t.totalRecords}</p>
              <p className="font-bold text-xl">{logs.length}</p>
            </div>
            <div>
              <p className="text-sm text-gray-800 font-medium">{t.filteredRecords}</p>
              <p className="font-bold text-xl">{groupedByMonth.reduce((sum, month) => sum + month.count, 0)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-800 font-medium">{t.dateRange}</p>
              <p className="font-bold text-xl">
                {(() => {
                  if (filters.startDate || filters.endDate) {
                    return `${filters.startDate || t.startDate} 至 ${filters.endDate || t.endDate}`;
                  } else if (groupedByMonth.length > 0) {
                    const allDays = groupedByMonth.flatMap(month => month.days);
                    if (allDays.length > 0) {
                      const firstDate = allDays[allDays.length - 1].date;
                      const lastDate = allDays[0].date;
                      return `${firstDate} 至 ${lastDate}`;
                    }
                  }
                  return '-';
                })()}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-800 font-medium">{t.averagePerDay}</p>
              <p className="font-bold text-xl">
                {(() => {
                  const allDays = groupedByMonth.flatMap(month => month.days);
                  const totalDays = allDays.length;
                  const totalCount = allDays.reduce((sum, day) => sum + day.count, 0);
                  return totalDays > 0 ? Math.round(totalCount / totalDays * 10) / 10 : 0;
                })()}
              </p>
            </div>
          </div>
        </div>
      </PopCard>
    </div>
  );
};
