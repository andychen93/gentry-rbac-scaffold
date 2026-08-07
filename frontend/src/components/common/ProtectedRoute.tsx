import React from 'react';
import { Navigate } from 'react-router-dom';
import { useUserStore } from '../../stores/userStore';
import AccessDenied from './AccessDenied';

interface ProtectedRouteProps {
  children: React.ReactNode;
  /** 可选：需要的权限标识，如果指定则检查用户是否拥有该权限 */
  permission?: string;
}

/**
 * ProtectedRoute 组件 - 路由保护
 *
 * 检查用户是否已登录（token 存在），未登录则重定向到 /login。
 * 可选检查用户是否拥有特定权限，无权限则显示 403 页面。
 */
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, permission }) => {
  const isLoggedIn = useUserStore((s) => s.isLoggedIn);
  const hasPermission = useUserStore((s) => s.hasPermission);

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  if (permission && !hasPermission(permission)) {
    return <AccessDenied />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
