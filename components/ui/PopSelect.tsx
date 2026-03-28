import React, { memo } from 'react';
import { createPortal } from 'react-dom';
import { POP_DESIGN_SYSTEM } from '../../styles/designSystem';

type NotificationType = 'success' | 'error' | 'info' | 'warning';

interface PopSelectOption {
  label: string;
  value: string;
}

interface PopSelectProps {
  type?: NotificationType;
  title: string;
  options: PopSelectOption[];
  onSelect: (value: string) => void;
  onCancel: () => void;
  cancelText?: string;
  themeColor?: string;
}

export const PopSelect: React.FC<PopSelectProps> = memo(({
  type = 'info',
  title,
  options,
  onSelect,
  onCancel,
  cancelText = 'Cancel',
  themeColor = '#FFD700'
}) => {
  const getTitleColor = () => {
    switch (type) {
      case 'success': return 'text-green-600';
      case 'error': return 'text-red-600';
      case 'warning': return 'text-yellow-600';
      default: return 'text-blue-600';
    }
  };

  const content = (
    <div 
      className="fixed bg-black bg-opacity-50 flex items-center justify-center"
      style={{ 
        top: 0, 
        left: 0, 
        width: '100vw', 
        height: '100vh', 
        zIndex: POP_DESIGN_SYSTEM.zIndex.modal 
      }}
    >
      <div className="bg-white border-4 border-black p-6 max-w-sm w-full mx-4">
        <h3 className={`font-display text-2xl mb-4 border-b-2 border-black pb-2 ${getTitleColor()}`}>
          {title}
        </h3>
        <div className="space-y-3">
          {options.map((option) => (
            <button
              key={option.value}
              onClick={() => onSelect(option.value)}
              className="relative w-full px-3 py-2 md:px-4 md:py-3 lg:px-6 lg:py-3 font-display text-sm md:text-base lg:text-xl uppercase tracking-wider border-4 border-black shadow-pop transition-all transform hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px] text-black"
              style={{ backgroundColor: themeColor }}
            >
              {option.label}
            </button>
          ))}
        </div>
        <div className="mt-4">
          <button
            onClick={onCancel}
            className="relative w-full px-3 py-2 md:px-4 md:py-3 lg:px-6 lg:py-3 font-display text-sm md:text-base lg:text-xl uppercase tracking-wider border-4 border-black shadow-pop transition-all transform hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px] text-black"
            style={{ backgroundColor: '#e0e0e0' }}
          >
            {cancelText}
          </button>
        </div>
      </div>
    </div>
  );

  return createPortal(content, document.body);
});

export type { PopSelectProps, PopSelectOption, NotificationType };
