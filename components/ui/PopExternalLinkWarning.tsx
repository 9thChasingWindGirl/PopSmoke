import React from 'react';
import { createPortal } from 'react-dom';
import { POP_COMPONENT_STYLES, getModalTitleColor } from '../../styles/componentStyles';

interface PopExternalLinkWarningProps {
  url: string;
  onConfirm: () => void;
  onCancel: () => void;
  confirmText?: string;
  cancelText?: string;
  title?: string;
  message?: string;
}

export const PopExternalLinkWarning: React.FC<PopExternalLinkWarningProps> = ({
  url,
  onConfirm,
  onCancel,
  confirmText = 'Continue',
  cancelText = 'Cancel',
  title = 'External Link',
  message = 'You are about to visit an external website:'
}) => {
  const content = (
    <div 
      className={POP_COMPONENT_STYLES.modal.overlay}
      style={{ 
        top: 0, 
        left: 0, 
        width: '100vw', 
        height: '100vh', 
        zIndex: 9999 
      }}
    >
      <div className={`${POP_COMPONENT_STYLES.modal.content} relative`}>
        <div className="absolute -top-4 -right-4 bg-black border-4 border-white px-3 py-1 transform rotate-6">
          <span className="font-display text-lg tracking-wide text-white">POP<span style={{ color: '#8EDDD3' }}>SMOKE</span></span>
        </div>
        <h3 className={`font-display text-2xl mb-4 border-b-2 border-black pb-2 ${getModalTitleColor('warning')}`}>
          ⚠️ {title}
        </h3>
        <p className="mb-4 text-lg">{message}</p>
        <div className="mb-6 p-4 bg-gray-100 border-2 border-black break-all font-mono text-sm">
          {url}
        </div>
        <div className="flex space-x-3">
          <button
            onClick={onConfirm}
            className="relative px-3 py-2 md:px-4 md:py-3 lg:px-6 lg:py-3 font-display text-sm md:text-base lg:text-xl uppercase tracking-wider border-4 border-black shadow-pop transition-all transform hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px] text-black flex-1"
            style={{ backgroundColor: '#FFD700' }}
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
};
