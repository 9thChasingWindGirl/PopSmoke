import React, { useState, useRef, useEffect } from 'react';
import { POP_COMPONENT_STYLES } from '../../styles/componentStyles';
import type { I18nTranslations } from '../../i18n';

interface PopDatePickerProps {
  value?: string;
  onChange?: (date: string) => void;
  placeholder?: string;
  className?: string;
  translations?: I18nTranslations;
}

export const PopDatePicker: React.FC<PopDatePickerProps> = ({ 
  value = '',
  onChange,
  placeholder = 'Select date',
  className = '',
  translations
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const containerRef = useRef<HTMLDivElement>(null);

  // Close when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const parseDate = (dateStr: string): Date | null => {
    if (!dateStr) return null;
    const [year, month, day] = dateStr.split('-').map(Number);
    return new Date(year, month - 1, day);
  };

  const formatDate = (date: Date): string => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const selectedDate = parseDate(value);

  const getDaysInMonth = (year: number, month: number): number => {
    return new Date(year, month + 1, 0).getDate();
  };

  const getFirstDayOfMonth = (year: number, month: number): number => {
    return new Date(year, month, 1).getDay();
  };

  const handlePrevMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1));
  };

  const handleNextMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1));
  };

  const handleDateSelect = (day: number) => {
    const newDate = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), day);
    onChange?.(formatDate(newDate));
    setIsOpen(false);
  };

  const handleClear = () => {
    onChange?.('');
    setIsOpen(false);
  };

  const handleToday = () => {
    const today = new Date();
    onChange?.(formatDate(today));
    setCurrentMonth(today);
    setIsOpen(false);
  };

  const year = currentMonth.getFullYear();
  const month = currentMonth.getMonth();
  const daysInMonth = getDaysInMonth(year, month);
  const firstDay = getFirstDayOfMonth(year, month);

  // Use translations or fallback to English
  const t = translations || {} as I18nTranslations;
  const monthNames = [
    t.january || 'Jan',
    t.february || 'Feb',
    t.march || 'Mar',
    t.april || 'Apr',
    t.may || 'May',
    t.june || 'Jun',
    t.july || 'Jul',
    t.august || 'Aug',
    t.september || 'Sep',
    t.october || 'Oct',
    t.november || 'Nov',
    t.december || 'Dec'
  ];
  const weekDays = [
    t.sunday || 'Su',
    t.monday || 'Mo',
    t.tuesday || 'Tu',
    t.wednesday || 'We',
    t.thursday || 'Th',
    t.friday || 'Fr',
    t.saturday || 'Sa'
  ];

  const displayValue = value || placeholder;

  return (
    <div ref={containerRef} className="relative w-full">
      {/* Trigger button */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={`${POP_COMPONENT_STYLES.input.base} bg-white text-left flex items-center justify-between ${className}`}
      >
        <span className={value ? 'text-black' : 'text-gray-400'}>
          {displayValue}
        </span>
        <svg 
          className="w-5 h-5 text-gray-500" 
          fill="none" 
          stroke="currentColor" 
          viewBox="0 0 24 24"
        >
          <path 
            strokeLinecap="round" 
            strokeLinejoin="round" 
            strokeWidth={2} 
            d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" 
          />
        </svg>
      </button>

      {/* Calendar popup */}
      {isOpen && (
        <div className="absolute z-50 mt-1 bg-white border-4 border-black shadow-pop-lg p-4 min-w-[280px]">
          {/* Header */}
          <div className="flex items-center justify-between mb-4">
            <button
              type="button"
              onClick={handlePrevMonth}
              className="p-1 hover:bg-gray-100 border-2 border-black"
            >
              ←
            </button>
            <span className="font-display text-lg font-bold">
              {monthNames[month]} {year}
            </span>
            <button
              type="button"
              onClick={handleNextMonth}
              className="p-1 hover:bg-gray-100 border-2 border-black"
            >
              →
            </button>
          </div>

          {/* Week days */}
          <div className="grid grid-cols-7 gap-1 mb-2">
            {weekDays.map(day => (
              <div key={day} className="text-center text-xs font-bold text-gray-500 py-1">
                {day}
              </div>
            ))}
          </div>

          {/* Days */}
          <div className="grid grid-cols-7 gap-1">
            {/* Empty cells for days before the first day of month */}
            {Array.from({ length: firstDay }).map((_, i) => (
              <div key={`empty-${i}`} className="h-8" />
            ))}
            
            {/* Days of the month */}
            {Array.from({ length: daysInMonth }).map((_, i) => {
              const day = i + 1;
              const dateStr = formatDate(new Date(year, month, day));
              const isSelected = value === dateStr;
              const isToday = formatDate(new Date()) === dateStr;

              return (
                <button
                  key={day}
                  type="button"
                  onClick={() => handleDateSelect(day)}
                  className={`
                    h-8 w-8 flex items-center justify-center text-sm font-display
                    border-2 transition-all
                    ${isSelected 
                      ? 'bg-black text-white border-black' 
                      : isToday
                        ? 'bg-yellow-300 border-black'
                        : 'bg-white border-transparent hover:border-black'
                    }
                  `}
                >
                  {day}
                </button>
              );
            })}
          </div>

          {/* Footer */}
          <div className="flex justify-between mt-4 pt-2 border-t-2 border-gray-200">
            <button
              type="button"
              onClick={handleClear}
              className="text-sm text-gray-500 hover:text-black font-medium"
            >
              {t.clear || 'Clear'}
            </button>
            <button
              type="button"
              onClick={handleToday}
              className="text-sm text-blue-600 hover:text-blue-800 font-medium"
            >
              {t.today || 'Today'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
