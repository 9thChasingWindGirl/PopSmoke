import React, { memo } from 'react';
import LoadingSpinner from '../common/LoadingSpinner';

type MaxWidth = 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl' | 'full';

interface PageContainerProps {
  children: React.ReactNode;
  maxWidth?: MaxWidth;
  className?: string;
  style?: React.CSSProperties;
  as?: 'div' | 'main' | 'section';
  id?: string;
  isLoading?: boolean;
  loadingText?: string;
  error?: Error | string | null;
  errorTitle?: string;
  onRetry?: () => void;
}

const MAX_WIDTH_MAP: Record<MaxWidth, string> = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-xl',
  '2xl': 'max-w-2xl',
  '3xl': 'max-w-3xl',
  '4xl': 'max-w-4xl',
  full: 'max-w-full',
};

export const PageContainer: React.FC<PageContainerProps> = memo(({
  children,
  maxWidth = 'xl',
  className = '',
  style,
  as: Tag = 'div',
  id,
  isLoading = false,
  loadingText,
  error,
  errorTitle,
  onRetry,
}) => {
  const widthClass = MAX_WIDTH_MAP[maxWidth] || MAX_WIDTH_MAP.xl;

  const errorMessage = error 
    ? (typeof error === 'string' ? error : error.message)
    : null;

  return (
    <Tag
      id={id}
      className={`
        w-full ${widthClass} mx-auto
        pt-[80px] md:pt-[80px]
        pb-[calc(80px+env(safe-area-inset-bottom))]
        md:pb-0
        min-h-screen
        ${className}
      `.trim().replace(/\s+/g, ' ')}
      style={style}
    >
      {errorMessage ? (
        <div className="flex flex-col items-center justify-center min-h-[400px] p-8">
          <div className="bg-red-50 border-4 border-red-500 rounded-lg p-6 max-w-md w-full text-center">
            <div className="text-5xl mb-4">⚠️</div>
            <h3 className="font-display text-xl font-bold text-red-800 mb-2">
              {errorTitle || 'Error'}
            </h3>
            <p className="font-body text-red-600 mb-4 text-sm">{errorMessage}</p>
            {onRetry && (
              <button
                onClick={onRetry}
                className="px-6 py-2 bg-red-500 hover:bg-red-600 text-white font-display font-bold uppercase tracking-wide border-2 border-black shadow-pop transition-all"
              >
                Retry
              </button>
            )}
          </div>
        </div>
      ) : isLoading ? (
        <div className="flex flex-col items-center justify-center min-h-[400px]">
          <LoadingSpinner size="lg" />
          {loadingText && (
            <p className="mt-4 font-body text-gray-500 font-medium animate-pulse">
              {loadingText}
            </p>
          )}
        </div>
      ) : (
        children
      )}
    </Tag>
  );
});

PageContainer.displayName = 'PageContainer';
