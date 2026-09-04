// @gentry/kit —— 平台共享层（组件/主题/hook/类型），源码包，由消费方编译
export type { ApiResult, PageResult, PageQuery } from './types/api';
export type { UsePagedListOptions } from './hooks/usePagedList';
export { usePagedList } from './hooks/usePagedList';
export type { ArgonVariant } from './theme/argonColors';
export { argonColors, argonGradients } from './theme/argonColors';
export { argonTheme } from './theme/argonTheme';
export { buildArgonLessVars } from './theme/argonLessVars';

// Pro 组件（原应用侧 components/pro 整体迁入）
export { ProTable } from './components/pro/ProTable';
export type { ProTableProps } from './components/pro/ProTable';
export { default as QueryForm } from './components/pro/QueryForm';
export type { QueryField } from './components/pro/QueryForm';
export { default as StatusSwitch } from './components/pro/StatusSwitch';
export { default as RowActions } from './components/pro/RowActions';
export type { RowActionItem } from './components/pro/RowActions';
export { default as CrudFormModal } from './components/pro/CrudFormModal';
export type { FormField, FormFieldType } from './components/pro/CrudFormModal';
export { PageSelect } from './components/pro/PageSelect';
export type { PageSelectProps } from './components/pro/PageSelect';
export { default as SweetAlert, type SweetAlertType } from './components/pro/SweetAlert';
export type { SweetAlertProps } from './components/pro/SweetAlert';

// 权限注入点：应用在根组件包 <PermissionProvider can={...}>，不包则默认放行
export { PermissionProvider, usePermission } from './components/pro/permission';
