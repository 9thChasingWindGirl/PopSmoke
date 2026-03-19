import { POP_DESIGN_SYSTEM } from './designSystem';

export const POP_COMPONENT_STYLES = {
  button: {
    base: `
      relative px-3 py-2 md:px-4 md:py-3 lg:px-6 lg:py-3 
      font-display text-sm md:text-base lg:text-xl uppercase tracking-wider
      border-4 border-black shadow-pop transition-all transform
      hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px]
      disabled:opacity-50 disabled:cursor-not-allowed
      overflow-hidden text-ellipsis whitespace-nowrap
      min-w-[80px] max-w-full
      text-wrap balance
    `,
    variants: {
      primary: (themeColor: string) => ({
        backgroundColor: themeColor,
        textColor: 'text-black',
      }),
      secondary: {
        backgroundColor: '#FFFFFF',
        textColor: 'text-black',
      },
      danger: {
        backgroundColor: '#FF4d4d',
        textColor: 'text-white',
      },
    },
  },

  card: {
    base: `
      bg-paper border-4 border-black shadow-pop-lg p-6 relative
    `,
    title: `
      absolute -top-5 left-4 bg-black text-white px-4 py-1 
      font-display text-lg transform -rotate-1 border-2 border-white
    `,
  },

  input: {
    base: `
      w-full border-4 border-black p-2 font-display
      focus:outline-none focus:ring-2 focus:ring-offset-2
    `,
    variants: {
      default: 'bg-white',
      error: 'bg-red-50 border-red-500',
      success: 'bg-green-50 border-green-500',
    },
  },

  modal: {
    overlay: `
      fixed bg-black bg-opacity-50 flex items-center justify-center
    `,
    content: `
      bg-white border-4 border-black p-6 max-w-sm w-full mx-4 shadow-pop
      transform transition-all duration-300
    `,
    title: {
      success: 'text-green-600',
      error: 'text-red-600',
      warning: 'text-yellow-600',
      info: 'text-blue-600',
    },
  },

  notification: {
    container: `
      fixed top-4 right-4 z-50 max-w-sm
    `,
    variants: {
      success: 'bg-green-50 border-green-500 text-green-800',
      error: 'bg-red-50 border-red-500 text-red-800',
      warning: 'bg-yellow-50 border-yellow-500 text-yellow-800',
      info: 'bg-blue-50 border-blue-500 text-blue-800',
    },
  },

  scrollbar: {
    hidden: `
      scrollbar-hide
      [-ms-overflow-style:none]
      [scrollbar-width:none]
      [&::-webkit-scrollbar]:hidden
    `,
    pop: `
      scrollbar-pop
      [scrollbar-width:thin]
      [scrollbar-color:#000_#fff]
      [&::-webkit-scrollbar]:w-3
      [&::-webkit-scrollbar]:h-3
      [&::-webkit-scrollbar-track]:bg-white
      [&::-webkit-scrollbar-track]:border-l-2
      [&::-webkit-scrollbar-track]:border-black
      [&::-webkit-scrollbar-thumb]:bg-black
      [&::-webkit-scrollbar-thumb]:border-2
      [&::-webkit-scrollbar-thumb]:border-white
    `,
  },

  nav: {
    container: `
      fixed bottom-0 left-0 right-0 bg-white border-t-4 border-black
      flex justify-around items-center py-2 px-4 z-40
      transform-style-3d
    `,
    item: {
      base: `
        flex flex-col items-center justify-center p-2 rounded-lg
        transition-all duration-200
      `,
      active: (themeColor: string) => `
        bg-opacity-20 scale-110
      `,
      inactive: 'opacity-60 hover:opacity-100',
    },
    header: {
      container: `
        p-3 md:p-4 flex justify-between items-center bg-white border-4 border-black shrink-0 z-50
      `,
      title: `
        font-display text-xl md:text-2xl tracking-wide cursor-pointer
      `,
      mobileSelect: `
        border-2 border-black px-1 py-1 text-sm font-bold cursor-pointer
      `,
      mobileColorPicker: `
        w-8 h-8 border-2 border-black cursor-pointer
      `,
      desktopNav: {
        container: `
          hidden md:flex items-center space-x-6
        `,
        item: `
          font-bold uppercase border-b-2 border-transparent hover:border-black w-[100px] text-center
        `,
        active: `
          border-black
        `,
      },
    },
  },

  flipCard: {
    container: `
      relative w-full h-full transform-style-3d transition-transform duration-500
    `,
    front: `
      absolute inset-0 backface-hidden
    `,
    back: `
      absolute inset-0 backface-hidden rotate-y-180
    `,
  },

  gauge: {
    card: `
      w-full bg-white border-4 border-black shadow-pop p-4 md:p-5 lg:p-6 rounded-2xl
    `,
    header: `
      text-center text-xs md:text-sm lg:text-base font-bold uppercase tracking-widest text-gray-500 mb-3 md:mb-4
    `,
    container: `
      relative w-40 h-20 md:w-48 md:h-24 lg:w-56 lg:h-28 overflow-hidden shrink-0 mx-auto mb-3 md:mb-4
    `,
    background: `
      absolute top-0 left-0 w-full h-full rounded-tl-full rounded-tr-full border-4 border-black bg-gray-200 box-border rounded-bl-none rounded-br-none
    `,
    fill: `
      absolute top-0 left-0 w-full h-full rounded-tl-full rounded-tr-full border-4 border-black border-b-0 origin-bottom transition-all duration-1000 ease-out
    `,
    paper: `
      absolute top-1/2 left-0 w-full h-1/2 bg-paper border-t-4 border-black z-10
    `,
    count: `
      absolute bottom-[-4px] w-full text-center z-20
    `,
    countValue: `
      font-display text-4xl md:text-5xl lg:text-6xl
    `,
    countLabel: `
      font-body text-xs md:text-sm lg:text-base font-bold text-gray-500
    `,
    footer: `
      w-full flex justify-between items-start gap-3 md:gap-4 pt-2 md:pt-3 border-t-2 border-gray-200
    `,
    footerLabel: `
      flex flex-col
    `,
    footerLabelName: `
      text-xs md:text-sm font-bold text-gray-400 uppercase leading-none
    `,
    footerLabelValue: `
      font-black text-sm md:text-base lg:text-lg uppercase
    `,
    footerLabelValueRemains: `
      font-black text-sm md:text-base lg:text-lg uppercase text-primary
    `,
  },

  dashboard: {
    mainContainer: `
      flex flex-col items-center justify-between md:justify-center w-full max-w-[420px] md:max-w-[480px] lg:max-w-[560px] mx-auto pt-2 pb-2 md:py-0 gap-2 md:gap-4
    `,
    titleHeader: `
      bg-white border-4 border-black p-3 md:p-4 rotate-1 shadow-pop z-10 shrink-0
    `,
    flipContainer: `
      w-full relative perspective-1000
    `,
    quickStatus: `
      w-full shrink-0 h-[40px] flex items-center justify-center
    `,
    smokeButton: {
      container: `
        relative group transition-all active:translate-y-1 active:translate-x-1 active:shadow-none w-40 h-40 md:w-44 md:h-44 lg:w-48 lg:h-48
      `,
      shadow: `
        absolute inset-0 bg-black rounded-2xl translate-y-2 translate-x-2 group-hover:translate-y-1 group-hover:translate-x-1 transition-all duration-100
      `,
      button: `
        relative w-full h-full rounded-2xl border-4 border-black flex flex-col items-center justify-center gap-0 overflow-hidden shadow-[inset_0_4px_0_rgba(255,255,255,0.4),inset_0_-4px_0_rgba(0,0,0,0.1)] transition-all duration-100 group-hover:translate-y-1 group-hover:translate-x-1 group-hover:shadow-[inset_0_2px_0_rgba(255,255,255,0.4),inset_0_-2px_0_rgba(0,0,0,0.1)]
      `,
      moreLink: `
        text-sm font-bold underline hover:text-gray-600 hover:underline-offset-6 hover:scale-105 decoration-2 font-display tracking-wide uppercase p-2 cursor-pointer transition-all duration-200
      `,
    },
  },

  apiManagement: {
    mainContainer: `
      w-full flex items-start justify-center
    `,
    contentWrapper: `
      w-[600px] gap-[30px] flex flex-col
    `,
  },

  settings: {
    dropdown: {
      trigger: `
        flex items-center border-2 border-black px-3 py-2 hover:bg-black hover:text-white transition-colors w-full justify-between
      `,
      colorPreview: `
        w-6 h-6 border-2 border-black ml-2
      `,
    },
  },

  animations: {
    shake: `
      @keyframes shake {
        0%, 100% { transform: translateX(0); }
        10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
        20%, 40%, 60%, 80% { transform: translateX(5px); }
      }
      .shake-animation {
        animation: shake 0.5s ease-in-out;
      }
    `,
  },
} as const;

export const getButtonStyle = (
  variant: 'primary' | 'secondary' | 'danger' = 'primary',
  themeColor?: string
) => {
  const base = POP_COMPONENT_STYLES.button.base;
  const variantConfig = POP_COMPONENT_STYLES.button.variants[variant];
  
  if (variant === 'primary' && themeColor) {
    return {
      className: base,
      style: { backgroundColor: themeColor },
    };
  }
  
  return {
    className: `${base} ${variant === 'danger' ? 'text-white' : 'text-black'}`,
    style: { backgroundColor: typeof variantConfig === 'function' ? variantConfig(themeColor || POP_DESIGN_SYSTEM.colors.theme.gold).backgroundColor : variantConfig.backgroundColor },
  };
};

export const getCardStyle = (hasTitle = false) => {
  let className = POP_COMPONENT_STYLES.card.base;
  if (hasTitle) {
    className += ' pt-8';
  }
  return className;
};

export const getModalTitleColor = (type: 'success' | 'error' | 'warning' | 'info') => {
  return POP_COMPONENT_STYLES.modal.title[type];
};

export const getNotificationStyle = (type: 'success' | 'error' | 'warning' | 'info') => {
  return POP_COMPONENT_STYLES.notification.variants[type];
};

export const getFlipCardHeight = () => ({
  height: 'calc(100vh - 280px)',
  minHeight: '450px',
  maxHeight: '600px'
});

export const getFlipCardTransform = (isFlipped: boolean) => ({
  transform: isFlipped ? 'rotateY(180deg)' : 'rotateY(0deg)',
  transformStyle: 'preserve-3d' as const
});

export const getBackfaceHidden = () => ({
  backfaceVisibility: 'hidden' as const
});

export const getGaugeStyle = (gaugeColor: string, percentage: number) => ({
  backgroundColor: gaugeColor,
  transform: `rotate(${percentage * 1.8 - 180}deg)`
});

export const getCountBadgeStyle = (themeColor: string) => ({
  backgroundColor: themeColor,
  color: '#000'
});

export const getApiManagementPadding = () => ({
  paddingTop: '40px',
  paddingBottom: '40px'
});

export const getApiManagementWrapper = () => ({
  gap: '30px',
  display: 'flex' as const,
  flexDirection: 'column' as const
});
