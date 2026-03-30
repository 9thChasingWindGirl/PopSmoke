import React, { forwardRef } from 'react';

interface PopDropdownProps {
  trigger: React.ReactNode;
  isOpen: boolean;
  onToggle: () => void;
  children: React.ReactNode;
  className?: string;
}

export const PopDropdown = forwardRef<HTMLDivElement, PopDropdownProps>(({
  trigger,
  isOpen,
  onToggle,
  children,
  className = '',
}, ref) => {
  return (
    <div ref={ref} className={`relative ${className}`}>
      <div onClick={onToggle} className="cursor-pointer">
        {trigger}
      </div>
      {isOpen && (
        <div className="absolute right-0 mt-1 bg-white border-4 border-black shadow-pop z-50 min-w-[80px]">
          {children}
        </div>
      )}
    </div>
  );
});

PopDropdown.displayName = 'PopDropdown';

interface PopDropdownTriggerProps {
  label: string;
  value?: React.ReactNode;
  themeColor?: string;
}

export const PopDropdownTrigger: React.FC<PopDropdownTriggerProps> = ({
  label,
  value,
  themeColor,
}) => {
  return (
    <div className="flex items-center justify-center w-8 h-8 border-2 border-black hover:bg-black hover:text-white transition-colors cursor-pointer">
      {value ? (
        <div 
          className="w-full h-full" 
          style={themeColor ? { backgroundColor: themeColor } : undefined}
        />
      ) : (
        <span className="text-xs font-bold">{label}</span>
      )}
    </div>
  );
};

interface PopDropdownItemProps {
  children: React.ReactNode;
  onClick: () => void;
  isActive?: boolean;
}

export const PopDropdownItem: React.FC<PopDropdownItemProps> = ({
  children,
  onClick,
  isActive = false,
}) => {
  return (
    <button
      onClick={onClick}
      className={`flex items-center px-4 py-2 hover:bg-gray-100 w-full text-left ${isActive ? 'bg-gray-200' : ''}`}
    >
      {children}
    </button>
  );
};
