// @gentry/kit —— 平台共享层（组件/主题/hook/类型），源码包，由消费方编译
export type { ApiResult, PageResult, PageQuery } from './types/api';
export type { UsePagedListOptions } from './hooks/usePagedList';
export { usePagedList } from './hooks/usePagedList';
export type { ArgonVariant } from './theme/argonColors';
export { argonColors, argonGradients } from './theme/argonColors';
export { argonTheme } from './theme/argonTheme';
export { buildArgonLessVars } from './theme/argonLessVars';
