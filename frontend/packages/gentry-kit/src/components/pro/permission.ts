import { createContext, createElement, useContext } from 'react';

/**
 * 权限判断注入点。kit 不持有登录态（userStore 是应用层的东西），
 * 应用在根组件包 <PermissionProvider can={useUserStore(s => s.hasPermission)}>。
 * 不包 Provider 时默认放行（保持「无权限体系也能用 Pro 组件」）。
 *
 * 注意不能直接导出 PermissionContext.Provider：原生 Provider 只认 value prop，
 * 传 can={...} 会被静默忽略（value=undefined 还会盖掉 Context 默认值），
 * 所以这里包一层做 can → value 的映射。用 createElement 而非 JSX，
 * 是为了让本文件保持 .ts（无 JSX 依赖，kit 里更好挪）。
 */
const PermissionContext = createContext<(perm: string) => boolean>(() => true);

export const PermissionProvider = ({
  can,
  children,
}: {
  can: (perm: string) => boolean;
  children: React.ReactNode;
}) => createElement(PermissionContext.Provider, { value: can }, children);

export const usePermission = () => useContext(PermissionContext);
