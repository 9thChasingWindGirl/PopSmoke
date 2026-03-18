import React, { useRef, useEffect } from 'react';
import { TRANSLATIONS } from '../../i18n';
import { Language } from '../../types';
import { POP_DESIGN_SYSTEM } from '../../styles/designSystem';

interface TimePickerProps {
  initialTimestamp: number;
  onSave: (timestamp: number) => void;
  onCancel: () => void;
  themeColor: string;
  language: Language;
}

const generateArray = (start: number, end: number) => Array.from({ length: end - start + 1 }, (_, i) => start + i);

const ScrollColumn: React.FC<{
  items: (number | string)[];
  selectedValue: number | string;
  onSelect: (val: number | string) => void;
  label: string;
  widthClass?: string;
}> = ({ items, selectedValue, onSelect, label, widthClass = 'w-16' }) => {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (containerRef.current) {
      const selectedIndex = items.findIndex(item => String(item) === String(selectedValue));
      if (selectedIndex !== -1) {
        const itemHeight = 40;
        containerRef.current.scrollTop = selectedIndex * itemHeight;
      }
    }
  }, []);
  
  return (
    <div className={`flex flex-col items-center ${widthClass}`}>
      <div className="font-bold text-xs uppercase mb-1">{label}</div>
      <div 
        className="h-32 w-full overflow-y-scroll snap-y snap-mandatory border-2 border-black bg-white scrollbar-hide"
        ref={containerRef}
      >
        <div className="h-[40px] w-full"></div>
        {items.map((item) => (
          <div 
            key={String(item)}
            onClick={() => {
                onSelect(item);
                if (containerRef.current) {
                    const idx = items.findIndex(i => String(i) === String(item));
                    containerRef.current.scrollTo({ top: idx * 40, behavior: 'smooth' });
                }
            }}
            className={`
              h-[40px] flex items-center justify-center snap-center cursor-pointer font-display 
              transition-colors duration-200 text-center whitespace-nowrap px-1
              ${String(item) === String(selectedValue) ? 'bg-black text-white scale-100' : 'text-gray-400 hover:text-black'}
              ${typeof item === 'string' ? 'text-sm md:text-base' : 'text-xl'}
            `}
          >
            {typeof item === 'number' ? String(item).padStart(2, '0') : item}
          </div>
        ))}
        <div className="h-[40px] w-full"></div>
      </div>
    </div>
  );
};

export const PopTimePicker: React.FC<TimePickerProps> = ({ initialTimestamp, onSave, onCancel, themeColor, language }) => {
  const t = TRANSLATIONS[language];
  const dateObj = new Date(initialTimestamp);
  
  const [selectedHour, setSelectedHour] = React.useState(dateObj.getHours());
  const [selectedMinute, setSelectedMinute] = React.useState(dateObj.getMinutes());
  
  const hours = generateArray(0, 23);
  const minutes = generateArray(0, 59);

  const handleSave = () => {
      const targetDate = new Date(initialTimestamp);
      targetDate.setHours(selectedHour);
      targetDate.setMinutes(selectedMinute);
      targetDate.setSeconds(0);
      targetDate.setMilliseconds(0);
      onSave(targetDate.getTime());
  };

  return (
    <div className="flex flex-col w-full h-full bg-paper absolute inset-0 z-30 p-4">
        <h3 className="font-display text-xl text-center mb-4 border-b-4 border-black pb-2">{t.editTime}</h3>
        
        <div className="flex justify-center items-center relative flex-1 gap-2">
            {/* Selection Highlight Bar */}
            <div className="absolute top-1/2 left-4 right-4 h-[40px] -mt-[20px] bg-yellow-100 -z-10 border-y-2 border-black pointer-events-none opacity-50"></div>

            <ScrollColumn 
                label={t.hour}
                items={hours} 
                selectedValue={selectedHour} 
                onSelect={(val) => setSelectedHour(Number(val))} 
                widthClass="w-20"
            />
            <span className="font-display text-2xl pt-6">:</span>
            <ScrollColumn 
                label={t.minute}
                items={minutes} 
                selectedValue={selectedMinute} 
                onSelect={(val) => setSelectedMinute(Number(val))} 
                widthClass="w-20"
            />
        </div>

        <div className="grid grid-cols-2 gap-3 mt-4">
            <button 
                onClick={onCancel}
                className="font-bold border-2 border-black p-3 hover:bg-gray-200 text-sm uppercase"
            >
                {t.cancel}
            </button>
            <button 
                onClick={handleSave}
                style={{ backgroundColor: themeColor }}
                className="font-bold border-2 border-black p-3 text-black text-sm uppercase shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] active:translate-x-[1px] active:translate-y-[1px] active:shadow-none transition-all"
            >
                {t.confirm}
            </button>
        </div>
    </div>
  );
};