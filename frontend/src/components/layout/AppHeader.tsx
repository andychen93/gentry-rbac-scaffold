import React, { useCallback, useMemo } from 'react';
import { Layout, Button, Tooltip, Breadcrumb } from 'antd';
import { AppstoreOutlined, SettingOutlined } from '@ant-design/icons';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useLayoutStore } from '../../stores/layoutStore';
import { useUserStore } from '../../stores/userStore';
import SidebarToggle from './SidebarToggle';
import LocaleSwitcher from './LocaleSwitcher';
import ThemeToggle from './ThemeToggle';
import NotificationBell from './NotificationBell';
import UserMenu from './UserMenu';

/** 路径 → 面包屑名称映射（新增业务页面时在此登记） */
const BREADCRUMB_MAP: Record<string, string> = {
  '/home': '工作台',
  '/profile': '个人中心',
  '/system/tenants': '租户管理',
  '/system/users': '用户管理',
  '/system/roles': '角色管理',
  '/system/menu': '菜单管理',
  '/system/dept': '部门管理',
  '/system/dict': '字典管理',
  '/system/config': '参数配置',
  '/monitor/operlog': '操作日志',
  '/monitor/loginlog': '登录日志',
  '/monitor/online': '在线用户',
  '/monitor-center/redis': 'Redis 监控',
};

/** 业务系统模式的落地页（脚手架默认工作台） */
const APP_HOME_PATH = '/home';
/** 系统管理模式的落地页 */
const ADMIN_HOME_PATH = '/system/users';

const AppHeader: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const isSystemMode = ['/system/', '/monitor/', '/monitor-center/'].some(
    (p) => location.pathname.startsWith(p)
  );

  const sidebarPinned = useLayoutStore((s) => s.sidebarPinned);
  const theme = useLayoutStore((s) => s.theme);
  const toggleSidebarPinned = useLayoutStore((s) => s.toggleSidebarPinned);
  const setTheme = useLayoutStore((s) => s.setTheme);

  const userInfo = useUserStore((s) => s.userInfo);
  const logout = useUserStore((s) => s.logout);

  const handleLogout = useCallback(async () => {
    await logout();
    navigate('/login');
  }, [logout, navigate]);

  const toggleMode = useCallback(() => {
    navigate(isSystemMode ? APP_HOME_PATH : ADMIN_HOME_PATH);
  }, [isSystemMode, navigate]);

  const breadcrumbItems = useMemo(() => {
    const path = location.pathname;
    const items: { title: React.ReactNode }[] = [];

    // 第一级：当前模式名称，可点击跳回模式首页
    if (isSystemMode) {
      items.push({ title: <Link to={ADMIN_HOME_PATH}>系统管理</Link> });
    } else {
      items.push({ title: <Link to={APP_HOME_PATH}>业务系统</Link> });
    }

    // 第二级：当前页面名称
    const pageName = BREADCRUMB_MAP[path];
    if (pageName) {
      items.push({ title: pageName });
    }

    return items;
  }, [location.pathname, isSystemMode]);

  return (
    <Layout.Header className={['ps-header', sidebarPinned ? 'is-pinned' : 'is-mini'].filter(Boolean).join(' ')}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <SidebarToggle pinned={sidebarPinned} onToggle={toggleSidebarPinned} />
        <Breadcrumb items={breadcrumbItems} style={{ marginLeft: 8 }} />
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Tooltip title={isSystemMode ? '业务系统' : '系统管理'}>
          <Button
            type="text"
            icon={isSystemMode ? <AppstoreOutlined style={{ fontSize: 18 }} /> : <SettingOutlined style={{ fontSize: 18 }} />}
            onClick={toggleMode}
          />
        </Tooltip>
        <NotificationBell />
        <LocaleSwitcher />
        <ThemeToggle value={theme} onChange={setTheme} />
        <UserMenu
          username={userInfo?.nickname || userInfo?.username}
          avatar={userInfo?.avatar}
          onLogout={handleLogout}
        />
      </div>
    </Layout.Header>
  );
};

export default React.memo(AppHeader);
