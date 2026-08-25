import { Routes, Route, Navigate } from 'react-router-dom';
import { useMemo, useEffect } from 'react';
import { Spin, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import LoginPage from './pages/login/LoginPage';
import PermissionPage from './pages/role/PermissionPage';
import ProfilePage from './pages/profile/ProfilePage';
import { AppLayout } from './components/layout';
import ProtectedRoute from './components/common/ProtectedRoute';
import LazyPage from './components/common/LazyPage';
import StylePreviewPage from './pages/dev/StylePreviewPage';
import { useUserStore } from './stores/userStore';
import { toRouteConfigs, getComponentLoader } from './utils/menuMapper';

export default function App() {
  const { t } = useTranslation();
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

  /*
   * loading 门必须在所有 hooks 之后（Rules of Hooks：条件 return 不能在 hooks 之前）。
   *
   * `data-testid="session-restore"` 是给 E2E 用的：原先靠断言「恢复登录状态...」
   * 这句中文来等门消失，文案一进语言包，英文环境下那个断言就永远为真、等于没等。
   */
  if (token && !userInfo) {
    return (
      <div data-testid="session-restore" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', gap: 16 }}>
        <Spin size="large" />
        <Typography.Text type="secondary" style={{ fontSize: 14 }}>{t('restoringSession')}</Typography.Text>
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
                {/* 子页面（非菜单页面） */}
                <Route
                  path="system/roles/:id/permissions"
                  element={<PermissionPage />}
                />
                <Route path="profile" element={<ProfilePage />} />
                <Route path="*" element={<Navigate to={`/${firstRoute}`} replace />} />
              </Routes>
            </AppLayout>
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}
