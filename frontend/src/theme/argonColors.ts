/** Argon 色板 —— 取自 argon-dashboard-pro-react scss/custom/_variables.scss */
export const argonColors = {
  primary: '#5e72e4',
  info: '#11cdef',
  success: '#2dce89',
  warning: '#fb6340',
  danger: '#f5365c',
  default: '#172b4d',
  bodyBg: '#f8f9fe',
  gray100: '#f6f9fc',
  gray200: '#e9ecef',
  gray500: '#adb5bd',
  gray600: '#8898aa',
  gray700: '#525f7f',
  gray800: '#32325d',
} as const;

/** 渐变公式 linear-gradient(87deg, C, adjust-hue(C,+25deg)) —— _background-variant.scss:16 */
export const argonGradients = {
  primary: 'linear-gradient(87deg, #5e72e4 0%, #7b5ce4 100%)',
  info: 'linear-gradient(87deg, #11cdef 0%, #118def 100%)',
  success: 'linear-gradient(87deg, #2dce89 0%, #2dc7ce 100%)',
  warning: 'linear-gradient(87deg, #fb6340 0%, #fb9840 100%)',
  danger: 'linear-gradient(87deg, #f5365c 0%, #f56336 100%)',
  default: 'linear-gradient(87deg, #172b4d 0%, #24174d 100%)',
} as const;

export type ArgonVariant = keyof typeof argonGradients;

export const argonShadow = '0 0 2rem 0 rgba(136, 152, 170, 0.15)';
export const argonShadowLg = '0 0 3rem 0 rgba(136, 152, 170, 0.25)';
export const argonRadius = 6;
export const argonRadiusXl = 8;
export const argonSidebarWidth = { mini: 62, open: 250 } as const;
