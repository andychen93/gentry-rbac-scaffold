import { Routes, Route, Navigate } from 'react-router-dom';
import { useMemo, useEffect } from 'react';
import { Spin } from 'antd';
import LoginPage from './pages/login/LoginPage';
import PermissionPage from './pages/role/PermissionPage';
import { AppLayout } from './components/layout';
import ProtectedRoute from './components/common/ProtectedRoute';
import LazyPage from './components/common/LazyPage';
import StylePreviewPage from './pages/dev/StylePreviewPage';
import { useUserStore } from './stores/userStore';
import { toRouteConfigs, getComponentLoader } from './utils/menuMapper';

export default function App() {
  const menus = useUserStore((s) => s.menus);
  const isLoggedIn = useUserStore((s) => s.isLoggedIn);
  const token = useUserStore((s) => s.token);
  const userInfo = useUserStore((s) => s.userInfo);
  const fetchUserInfo = useUserStore((s) => s.fetchUserInfo);

  // 刷新恢复：有 token 但 userInfo/menus 丢失（内存 store 刷新后重置）→ 顶层重新拉取
  // 不等 AppLayout 的 fetchUserInfo（它在 ProtectedRoute 内，但 dynamicRoutes 在此处先算）
  useEffect(() => {
    if (token && !userInfo) {
      fetchUserInfo().catch(() => { /* token 过期 → store clearAuth → isLoggedIn=false → 正常跳 login */ });
    }
  }, [token, userInfo, fetchUserInfo]);

  const dynamicRoutes = useMemo(() => {
    if (!isLoggedIn) return [];
    return toRouteConfigs(menus);
  }, [menus, isLoggedIn]);

  const firstRoute = dynamicRoutes[0]?.path ?? '/login';

  // loading 门必须在所有 hooks 之后（Rules of Hooks：条件 return 不能在 hooks 之前）
  if (token && !userInfo) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', gap: 16 }}>
        <Spin size="large" />
        <span style={{ color: '#8898aa', fontSize: 14 }}>恢复登录状态...</span>
      </div>
    );
  }

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/dev/style" element={<StylePreviewPage />} />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <AppLayout>
              <Routes>
                {dynamicRoutes.map((route) => {
                  const loader = getComponentLoader(route.component);
                  if (!loader) return null;
                  return (
                    <Route
                      key={route.path}
                      path={route.path}
                      element={
                        <LazyPage
                          loader={loader}
                          componentKey={route.component}
                        />
                      }
                    />
                  );
                })}
                {/* 子页面（非菜单页面，从角色管理跳转） */}
                <Route
                  path="system/roles/:id/permissions"
                  element={<PermissionPage />}
                />
                <Route path="*" element={<Navigate to={`/${firstRoute}`} replace />} />
              </Routes>
            </AppLayout>
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}
