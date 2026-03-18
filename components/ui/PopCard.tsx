import React from 'react';
import { POP_COMPONENT_STYLES } from '../../styles/componentStyles';

interface PopCardProps {
  children: React.ReactNode;
  title?: string;
  className?: string;
  style?: React.CSSProperties;
}

export const PopCard: React.FC<PopCardProps> = ({ children, title, className = '', style }) => {
  let baseClassName = POP_COMPONENT_STYLES.card.base;
  if (title) {
    baseClassName += ' pt-8';
  }

  return (
    <div className={`${baseClassName} ${className}`} style={style}>
      {title && (
        <div className={POP_COMPONENT_STYLES.card.title}>
          {title}
        </div>
      )}
      {children}
    </div>
  );
};