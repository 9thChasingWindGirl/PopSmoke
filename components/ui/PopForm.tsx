import React, { useState, useEffect, memo } from 'react';
import { createPortal } from 'react-dom';
import { POP_DESIGN_SYSTEM } from '../../styles/designSystem';

type NotificationType = 'success' | 'error' | 'info' | 'warning';

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

export const PopForm: React.FC<PopFormProps> = memo(({
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

  // 监听fields变化，更新初始值
  useEffect(() => {
    const updatedValues: Record<string, string> = {};
    fields.forEach(field => {
      updatedValues[field.name] = field.defaultValue || '';
    });
    setValues(updatedValues);
  }, [fields]);

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
        zIndex: POP_DESIGN_SYSTEM.zIndex.modal 
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
});

export type { PopFormProps, PopFormField, NotificationType };
