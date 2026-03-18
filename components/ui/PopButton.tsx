import React from 'react';
import { getButtonStyle } from '../../styles/componentStyles';

interface PopButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger';
  themeColor?: string;
}

export const PopButton: React.FC<PopButtonProps> = ({ 
  children, 
  variant = 'primary', 
  themeColor,
  className = '',
  ...props 
}) => {
  const buttonStyle = getButtonStyle(variant, themeColor);

  return (
    <button
      className={`${buttonStyle.className} ${className}`}
      style={buttonStyle.style}
      {...props}
    >
      {children}
    </button>
  );
};