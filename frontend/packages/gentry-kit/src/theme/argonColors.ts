/**
 * Argon 色板 —— 取自 argon-dashboard-pro-react scss/custom/_variables.scss
 *
 * 这里是**全站配色的唯一源头**，两条下游都从这里取值：
 *   1. antd 组件 → 本包 theme/argonTheme.ts 把它灌进 ConfigProvider token
 *   2. styles/argon.less → vite.config.ts 用 argonLessVars.ts 把它编译成
 *      `@ps-*` Less 变量注入（见该文件注释）
 * 所以改配色只动本文件，不要在 .tsx / .less 里写死 hex。
 */
export const argonColors = {
  primary: '#5e72e4',
  info: '#11cdef',
  success: '#2dce89',
  warning: '#fb6340',
  danger: '#f5365c',
  default: '#172b4d',
  bodyBg: '#f8f9fe',
  white: '#fff',
  // Bootstrap 灰阶（Argon 沿用 $gray-100 ~ $gray-900）
  gray100: '#f6f9fc',
  gray200: '#e9ecef',
  gray300: '#dee2e6',
  gray400: '#ced4da',
  gray500: '#adb5bd',
  gray600: '#8898aa',
  gray700: '#525f7f',
  gray800: '#32325d',
  gray900: '#212529',
} as const;

/**
 * 渐变，公式 linear-gradient(87deg, C, adjust-hue(C,+25deg)) —— _background-variant.scss:16
 *
 * ⚠️ 第二个色标是**独立字面量**，不是从上面 argonColors 算出来的：
 * 实测这些值与 adjust-hue(+25deg) 的计算结果并不完全一致（如 primary
 * 算出 #825ee4，Argon 实际用 #7b5ce4），说明它们抄的是 Argon 编译产物而非公式。
 * 所以**改了上面的基础色，记得同步改这里对应的渐变**，别只改一个。
 */
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
