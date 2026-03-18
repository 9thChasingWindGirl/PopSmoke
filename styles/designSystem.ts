export const POP_DESIGN_SYSTEM = {
  colors: {
    primary: {
      black: '#000000',
      white: '#FFFFFF',
      paper: '#fdfbf7',
      background: '#f0f0f0',
    },
    theme: {
      gold: '#FFD700',
      coral: '#FF6B6B',
      teal: '#4ECDC4',
      lime: '#C7F464',
      purple: '#A78BFA',
      orange: '#FF9F43',
    },
    status: {
      success: '#22c55e',
      successLight: '#dcfce7',
      error: '#ef4444',
      errorLight: '#fee2e2',
      warning: '#eab308',
      warningLight: '#fef9c3',
      info: '#3b82f6',
      infoLight: '#dbeafe',
    },
    text: {
      primary: '#000000',
      secondary: '#374151',
      muted: '#6b7280',
      light: '#9ca3af',
    },
  },

  borders: {
    width: {
      thin: '2px',
      default: '4px',
      thick: '6px',
    },
    radius: {
      none: '0',
      default: '0',
      full: '9999px',
    },
    color: '#000000',
  },

  shadows: {
    pop: {
      default: '4px 4px 0px 0px rgba(0,0,0,1)',
      hover: '2px 2px 0px 0px rgba(0,0,0,1)',
      large: '8px 8px 0px 0px rgba(0,0,0,1)',
    },
  },

  typography: {
    fontFamily: {
      display: ['Bangers', '"Noto Sans SC"', '"Noto Sans JP"', '"Noto Sans KR"', 'cursive'],
      body: ['"HarmonyOS Sans"', '"HarmonyOS Sans SC"', '"Noto Sans SC"', '"Noto Sans JP"', '"Noto Sans KR"', 'sans-serif'],
    },
    fontSize: {
      xs: '0.75rem',
      sm: '0.875rem',
      base: '1rem',
      lg: '1.125rem',
      xl: '1.25rem',
      '2xl': '1.5rem',
      '3xl': '1.875rem',
      '4xl': '2.25rem',
    },
    fontWeight: {
      normal: 400,
      bold: 700,
      black: 900,
    },
    letterSpacing: {
      tight: '-0.025em',
      normal: '0',
      wide: '0.025em',
      wider: '0.05em',
      widest: '0.1em',
    },
  },

  spacing: {
    px: '1px',
    0: '0',
    1: '0.25rem',
    2: '0.5rem',
    3: '0.75rem',
    4: '1rem',
    5: '1.25rem',
    6: '1.5rem',
    8: '2rem',
    10: '2.5rem',
    12: '3rem',
    16: '4rem',
    20: '5rem',
    24: '6rem',
  },

  animation: {
    duration: {
      fast: '150ms',
      normal: '300ms',
      slow: '500ms',
    },
    easing: {
      default: 'ease-out',
      smooth: 'cubic-bezier(0.4, 0, 0.2, 1)',
      bounce: 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
    },
    keyframes: {
      slideIn: {
        '0%': { opacity: '0', transform: 'translateY(10px) scale(0.98)' },
        '100%': { opacity: '1', transform: 'translateY(0) scale(1)' },
      },
      popBlast: {
        '0%': { opacity: '0', transform: 'translate(-50%, -50%) scale(0.5) rotate(-15deg)' },
        '50%': { opacity: '1', transform: 'translate(-50%, -50%) scale(1.2) rotate(15deg)' },
        '100%': { opacity: '0', transform: 'translate(-50%, -50%) scale(1.5) rotate(0deg)' },
      },
      slideDown: {
        '0%': { opacity: '0', transform: 'translateY(-100%)' },
        '100%': { opacity: '1', transform: 'translateY(0)' },
      },
      slideUp: {
        '0%': { opacity: '1', transform: 'translateY(0)' },
        '100%': { opacity: '0', transform: 'translateY(-100%)' },
      },
    },
  },

  breakpoints: {
    sm: '640px',
    md: '768px',
    lg: '1024px',
    xl: '1280px',
    '2xl': '1536px',
  },

  zIndex: {
    base: 0,
    dropdown: 1000,
    sticky: 1020,
    fixed: 1030,
    modalBackdrop: 1040,
    modal: 1050,
    popover: 1060,
    tooltip: 1070,
    notification: 9999,
  },
} as const;

export type PopColors = typeof POP_DESIGN_SYSTEM.colors;
export type PopBorders = typeof POP_DESIGN_SYSTEM.borders;
export type PopShadows = typeof POP_DESIGN_SYSTEM.shadows;
export type PopTypography = typeof POP_DESIGN_SYSTEM.typography;
export type PopSpacing = typeof POP_DESIGN_SYSTEM.spacing;
export type PopAnimation = typeof POP_DESIGN_SYSTEM.animation;
