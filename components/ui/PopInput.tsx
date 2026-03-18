import React from 'react';
import { POP_COMPONENT_STYLES } from '../../styles/componentStyles';

interface PopInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  variant?: 'default' | 'error' | 'success';
}

export const PopInput: React.FC<PopInputProps> = ({ 
  variant = 'default',
  className = '',
  ...props 
}) => {
  const variantClass = POP_COMPONENT_STYLES.input.variants[variant];
  
  return (
    <input
      className={`${POP_COMPONENT_STYLES.input.base} ${variantClass} ${className}`}
      {...props}
    />
  );
};
