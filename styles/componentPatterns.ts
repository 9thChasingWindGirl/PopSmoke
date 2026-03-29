import { POP_DESIGN_SYSTEM } from './designSystem';

export const LAYOUT_PATTERNS = {
  container: {
    responsive: `
      w-full mx-auto px-3 xs:px-3 sm:px-4 md:px-6 lg:px-8
      max-w-[${POP_DESIGN_SYSTEM.layout.containerWidth.xs}]
      xs:max-w-[${POP_DESIGN_SYSTEM.layout.containerWidth.xs}]
      sm:max-w-[${POP_DESIGN_SYSTEM.layout.containerWidth.sm}]
      md:max-w-[${POP_DESIGN_SYSTEM.layout.containerWidth.md}]
      lg:max-w-[${POP_DESIGN_SYSTEM.layout.containerWidth.lg}]
    `,
  },

  card: {
    responsive: `
      bg-white border-4 border-black rounded-2xl shadow-pop
      p-3 xs:p-3 sm:p-4 md:p-6 lg:p-8
    `,
  },

  grid: {
    responsive: `
      grid grid-cols-1 gap-2 xs:gap-2 sm:gap-3 md:gap-4 lg:gap-6
      xs:grid-cols-1 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3
    `,
  },

  twoColumn: {
    responsive: `
      grid grid-cols-1 gap-2 xs:gap-2 sm:gap-3 md:gap-4 lg:gap-6
      xs:grid-cols-1 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-2
    `,
  },

  buttonGroup: {
    responsive: `
      flex flex-col gap-2 xs:gap-2 sm:gap-2 md:gap-3
      sm:flex-row
    `,
  },

  form: {
    responsive: `
      space-y-3 xs:space-y-3 sm:space-y-4 md:space-y-6
    `,
  },

  input: {
    responsive: `
      w-full border-4 border-black p-2 xs:p-2 sm:p-2 md:p-3 font-display
      text-sm sm:text-sm md:text-base
    `,
  },

  text: {
    h1: `
      font-display font-black
      text-[${POP_DESIGN_SYSTEM.responsiveFont.h1.xs}]
      sm:text-[${POP_DESIGN_SYSTEM.responsiveFont.h1.sm}]
      md:text-[${POP_DESIGN_SYSTEM.responsiveFont.h1.md}]
      lg:text-[${POP_DESIGN_SYSTEM.responsiveFont.h1.lg}]
    `,
    h2: `
      font-display font-bold
      text-[${POP_DESIGN_SYSTEM.responsiveFont.h2.xs}]
      sm:text-[${POP_DESIGN_SYSTEM.responsiveFont.h2.sm}]
      md:text-[${POP_DESIGN_SYSTEM.responsiveFont.h2.md}]
      lg:text-[${POP_DESIGN_SYSTEM.responsiveFont.h2.lg}]
    `,
    h3: `
      font-display font-bold
      text-[${POP_DESIGN_SYSTEM.responsiveFont.h3.xs}]
      sm:text-[${POP_DESIGN_SYSTEM.responsiveFont.h3.sm}]
      md:text-[${POP_DESIGN_SYSTEM.responsiveFont.h3.md}]
      lg:text-[${POP_DESIGN_SYSTEM.responsiveFont.h3.lg}]
    `,
    body: `
      font-body
      text-[${POP_DESIGN_SYSTEM.responsiveFont.body.xs}]
      sm:text-[${POP_DESIGN_SYSTEM.responsiveFont.body.sm}]
      md:text-[${POP_DESIGN_SYSTEM.responsiveFont.body.md}]
      lg:text-[${POP_DESIGN_SYSTEM.responsiveFont.body.lg}]
    `,
    small: `
      font-body
      text-[${POP_DESIGN_SYSTEM.responsiveFont.small.xs}]
      sm:text-[${POP_DESIGN_SYSTEM.responsiveFont.small.sm}]
      md:text-[${POP_DESIGN_SYSTEM.responsiveFont.small.md}]
      lg:text-[${POP_DESIGN_SYSTEM.responsiveFont.small.lg}]
    `,
  },

  icon: {
    responsive: `
      w-[${POP_DESIGN_SYSTEM.icon.xs}]
      h-[${POP_DESIGN_SYSTEM.icon.xs}]
      sm:w-[${POP_DESIGN_SYSTEM.icon.sm}]
      sm:h-[${POP_DESIGN_SYSTEM.icon.sm}]
      md:w-[${POP_DESIGN_SYSTEM.icon.md}]
      md:h-[${POP_DESIGN_SYSTEM.icon.md}]
      lg:w-[${POP_DESIGN_SYSTEM.icon.lg}]
      lg:h-[${POP_DESIGN_SYSTEM.icon.lg}]
    `,
  },

  spacing: {
    gap: {
      xs: 'gap-3',
      sm: 'gap-3',
      md: 'gap-4',
      lg: 'gap-6',
    },
    padding: {
      xs: 'p-4',
      sm: 'p-4',
      md: 'p-6',
      lg: 'p-8',
    },
    margin: {
      xs: 'm-2',
      sm: 'm-2',
      md: 'm-3',
      lg: 'm-4',
    },
  },
} as const;

export type LayoutPatterns = typeof LAYOUT_PATTERNS;
