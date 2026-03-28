import React, { memo } from 'react';
import { createPortal } from 'react-dom';
import { POP_DESIGN_SYSTEM } from '../../styles/designSystem';

type NotificationType = 'success' | 'error' | 'info' | 'warning';

interface PopConfirmProps {
  type?: NotificationType;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void;
  onCancel: () => void;
  confirmThemeColor?: string;
}

export const PopConfirm: React.FC<PopConfirmProps> = memo(({
  type = 'warning',
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  onConfirm,
  onCancel,
  confirmThemeColor = '#FFD700'
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
      <div className="bg-white border-4 border-black p-6 max-w-sm w-full mx-4 transform transition-all duration-300">
        <h3 className={`font-display text-2xl mb-4 border-b-2 border-black pb-2 ${getTitleColor()}`}>
          {title}
        </h3>
        <p className="mb-6 text-lg">{message}</p>
        <div className="flex space-x-3">
          <button
            onClick={onConfirm}
            className="relative px-3 py-2 md:px-4 md:py-3 lg:px-6 lg:py-3 font-display text-sm md:text-base lg:text-xl uppercase tracking-wider border-4 border-black shadow-pop transition-all transform hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px] text-black flex-1"
            style={{ backgroundColor: confirmThemeColor }}
          >
            {confirmText}
          </button>
          <button
            onClick={onCancel}
            className="relative px-3 py-2 md:px-4 md:py-3 lg:px-6 lg:py-3 font-display text-sm md:text-base lg:text-xl uppercase tracking-wider border-4 border-black shadow-pop transition-all transform hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px] text-black flex-1"
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

export type { PopConfirmProps, NotificationType };
