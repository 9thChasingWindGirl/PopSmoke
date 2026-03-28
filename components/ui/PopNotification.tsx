import React, { useEffect, memo } from 'react';
import { createPortal } from 'react-dom';
import { POP_DESIGN_SYSTEM } from '../../styles/designSystem';

type NotificationType = 'success' | 'error' | 'info' | 'warning';

interface PopNotificationProps {
  type?: NotificationType;
  title: string;
  message: string;
  onClose?: () => void;
  duration?: number;
}

export const PopNotification: React.FC<PopNotificationProps> = memo(({ 
  type = 'info',
  title,
  message, 
  onClose, 
  duration = 3000 
}) => {
  useEffect(() => {
    if (duration > 0 && onClose) {
      const timer = setTimeout(() => {
        onClose();
      }, duration);
      return () => clearTimeout(timer);
    }
    return undefined;
  }, [duration, onClose]);

  const getTitleAndColor = () => {
    const colors = {
      success: 'text-green-600',
      error: 'text-red-600',
      info: 'text-blue-600',
      warning: 'text-yellow-600'
    };
    return {
      title: title,
      titleColor: colors[type] || colors.info
    };
  };

  const { title: displayTitle, titleColor } = getTitleAndColor();

  const content = (
    <div 
      className="fixed bg-black bg-opacity-50 flex items-center justify-center"
      style={{ 
        top: 0, 
        left: 0, 
        width: '100vw', 
        height: '100vh', 
        zIndex: POP_DESIGN_SYSTEM.zIndex.notification 
      }}
    >
      <div className="bg-white border-4 border-black p-6 max-w-sm w-full mx-4 shadow-pop transform transition-all duration-300">
        <div className="flex justify-between items-start">
          <div className="flex-1">
            <h3 className={`font-display text-2xl mb-4 border-b-2 border-black pb-2 ${titleColor}`}>
              {displayTitle}
            </h3>
            <p className="text-lg text-gray-700">
              {message}
            </p>
          </div>
        </div>
        <div className="flex justify-end mt-4">
          <button
            onClick={onClose}
            className="relative px-3 py-2 md:px-4 md:py-3 font-display text-sm md:text-base uppercase tracking-wider border-4 border-black shadow-pop transition-all transform hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px] text-black"
            style={{ backgroundColor: '#e0e0e0' }}
          >
            OK
          </button>
        </div>
      </div>
    </div>
  );

  return createPortal(content, document.body);
});
