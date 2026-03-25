import React, { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { POP_COMPONENT_STYLES, getModalTitleColor, getNotificationStyle } from '../../styles/componentStyles';
import { AppSettings, Language } from '../../types';
import { TRANSLATIONS } from '../../i18n';

type NotificationType = 'success' | 'error' | 'info' | 'warning';

interface PopNotificationProps {
  type?: NotificationType;
  title: string;
  message: string;
  onClose?: () => void;
  duration?: number;
}

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

interface PopPromptProps {
  type?: NotificationType;
  title: string;
  message: string;
  placeholder?: string;
  defaultValue?: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: (value: string) => void;
  onCancel: () => void;
  confirmThemeColor?: string;
  isPassword?: boolean;
}

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

interface PopFormField {
  name: string;
  label: string;
  type?: 'text' | 'password';
  placeholder?: string;
  defaultValue?: string;
}

interface PopFormProps {
  type?: NotificationType;
  title: string;
  message?: string;
  fields: PopFormField[];
  confirmText?: string;
  cancelText?: string;
  onConfirm: (values: Record<string, string>) => void;
  onCancel: () => void;
  confirmThemeColor?: string;
  themeColor?: string;
}

export const PopNotification: React.FC<PopNotificationProps> = ({ 
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
        zIndex: 9999 
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
};

export const PopForm: React.FC<PopFormProps> = ({
  type = 'info',
  title,
  message,
  fields,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  onConfirm,
  onCancel,
  confirmThemeColor = '#FFD700'
}) => {
  const [values, setValues] = useState<Record<string, string>>(() => {
    const initial: Record<string, string> = {};
    fields.forEach(field => {
      initial[field.name] = field.defaultValue || '';
    });
    return initial;
  });

  const getTitleColor = () => {
    switch (type) {
      case 'success': return 'text-green-600';
      case 'error': return 'text-red-600';
      case 'warning': return 'text-yellow-600';
      default: return 'text-blue-600';
    }
  };

  const handleConfirm = () => {
    onConfirm(values);
  };

  const handleKeyDown = (e: React.KeyboardEvent, fieldIndex: number) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (fieldIndex < fields.length - 1) {
        const nextInput = document.getElementById(`popform-field-${fields[fieldIndex + 1].name}`);
        nextInput?.focus();
      } else {
        handleConfirm();
      }
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
        zIndex: 9999 
      }}
    >
      <div className="bg-white border-4 border-black p-6 max-w-sm w-full mx-4">
        <h3 className={`font-display text-2xl mb-4 border-b-2 border-black pb-2 ${getTitleColor()}`}>
          {title}
        </h3>
        <div className="space-y-4">
          {message && <p className="text-lg">{message}</p>}
          {fields.map((field, index) => (
            <div key={field.name}>
              <label className="block font-bold mb-1">{field.label}</label>
              <input
                id={`popform-field-${field.name}`}
                type={field.type || 'text'}
                value={values[field.name] || ''}
                onChange={(e) => setValues(prev => ({ ...prev, [field.name]: e.target.value }))}
                placeholder={field.placeholder || ''}
                className="w-full border-4 border-black p-2 font-display"
                onKeyDown={(e) => handleKeyDown(e, index)}
              />
            </div>
          ))}
        </div>
        <div className="flex space-x-3 mt-4">
          <button
            onClick={handleConfirm}
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
};

export const PopConfirm: React.FC<PopConfirmProps> = ({
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
        zIndex: 9999 
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
};

export const PopPrompt: React.FC<PopPromptProps> = ({
  type = 'info',
  title,
  message,
  placeholder = '',
  defaultValue = '',
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  onConfirm,
  onCancel,
  confirmThemeColor = '#FFD700',
  isPassword = false
}) => {
  const [value, setValue] = useState(defaultValue);

  const getTitleColor = () => {
    switch (type) {
      case 'success': return 'text-green-600';
      case 'error': return 'text-red-600';
      case 'warning': return 'text-yellow-600';
      default: return 'text-blue-600';
    }
  };

  const handleConfirm = () => {
    onConfirm(value);
  };

  const content = (
    <div 
      className="fixed bg-black bg-opacity-50 flex items-center justify-center"
      style={{ 
        top: 0, 
        left: 0, 
        width: '100vw', 
        height: '100vh', 
        zIndex: 9999 
      }}
    >
      <div className="bg-white border-4 border-black p-6 max-w-sm w-full mx-4">
        <h3 className={`font-display text-2xl mb-4 border-b-2 border-black pb-2 ${getTitleColor()}`}>
          {title}
        </h3>
        <div className="space-y-4">
          {message && <p className="text-lg">{message}</p>}
          <div>
            <input
              type={isPassword ? 'password' : 'text'}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder={placeholder}
              className="w-full border-4 border-black p-2 font-display"
              onKeyDown={(e) => e.key === 'Enter' && handleConfirm()}
            />
          </div>
        </div>
        <div className="flex space-x-3 mt-4">
          <button
            onClick={handleConfirm}
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
};

export const PopSelect: React.FC<PopSelectProps> = ({
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
        zIndex: 9999 
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
};

