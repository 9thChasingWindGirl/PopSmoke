import React from 'react';

interface LoadingSpinnerProps {
  size?: 'small' | 'medium' | 'large';
  color?: string;
  message?: string;
  fullScreen?: boolean;
}

const sizeMap = {
  small: { width: 24, height: 24 },
  medium: { width: 40, height: 40 },
  large: { width: 56, height: 56 }
};

const LoadingSpinner: React.FC<LoadingSpinnerProps> = React.memo(({
  size = 'medium',
  color = '#FFD700',
  message,
  fullScreen = false
}) => {
  const dimensions = sizeMap[size];

  const spinnerStyle: React.CSSProperties = {
    border: `${size === 'small' ? 3 : 4}px solid rgba(255, 215, 0, 0.1)`,
    borderTopColor: color,
    borderRadius: '50%',
    width: dimensions.width,
    height: dimensions.height,
    animation: 'spin 0.8s linear infinite'
  };

  const containerStyle: React.CSSProperties = fullScreen ? {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    gap: '12px'
  } : {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    justifyContent: 'center',
    padding: '40px',
    gap: '12px'
  };

  return (
    <div style={containerStyle}>
      <div style={spinnerStyle} />
      {message && (
        <p style={{ 
          color: '#718096', 
          fontSize: '14px', 
          textAlign: 'center',
          maxWidth: '200px'
        }}>
          {message}
        </p>
      )}
      <style>{`
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
});

LoadingSpinner.displayName = 'LoadingSpinner';

export default LoadingSpinner;
