import React, { useCallback, useMemo } from 'react';
import { Layout, Button, Tooltip, Breadcrumb } from 'antd';
import { AppstoreOutlined, SettingOutlined } from '@ant-design/icons';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useLayoutStore } from '../../stores/layoutStore';
import { useUserStore } from '../../stores/userStore';
import { findMenuByPath, navKeyFromPath } from '../../locales/navKey';
import SidebarToggle from './SidebarToggle';
import LocaleSwitcher from './LocaleSwitcher';
import ThemeToggle from './ThemeToggle';
import NotificationBell from './NotificationBell';
import UserMenu from './UserMenu';

/*
 * 原来这里有一张 BREADCRUMB_MAP（13 条 path → 中文名），它是**菜单名的第二份拷贝** ——
 * 库里 sys_menu.name 已经有这些名字，后端还派生了 i18nKey 下发。保留它意味着 i18n 之后
 * 同一个名字要维护两处译文，必然漂移；而且新增页面得记着两个地方都要登记。
 *
 * 现在改为：优先从 userStore.menus 里按 path 找到节点、直接用它的 i18nKey（后端算的，权威）；
 * 找不到（非菜单的子页面）才按同一规则从 path 派生 key。
 */

/** 不在菜单树里、但需要面包屑的页面 */
const EXTRA_PAGE_KEYS: Record<string, string> = {
  '/profile': 'common:profile',
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
  const menus = useUserStore((s) => s.menus);
  const logout = useUserStore((s) => s.logout);
  const { t } = useTranslation(['common', 'nav']);

  const handleLogout = useCallback(async () => {
    await logout();
    navigate('/login');
  }, [logout, navigate]);

  const toggleMode = useCallback(() => {
    navigate(isSystemMode ? APP_HOME_PATH : ADMIN_HOME_PATH);
  }, [isSystemMode, navigate]);

  // 依赖里必须有 t：面包屑文案是渲染时算的，语言一变就要重建
  const breadcrumbItems = useMemo(() => {
    const path = location.pathname;
    const items: { title: React.ReactNode }[] = [];

    // 第一级：当前模式名称，可点击跳回模式首页
    if (isSystemMode) {
      items.push({ title: <Link to={ADMIN_HOME_PATH}>{t('common:mode.admin')}</Link> });
    } else {
      items.push({ title: <Link to={APP_HOME_PATH}>{t('common:mode.app')}</Link> });
    }

    // 第二级：当前页面名称。菜单树里有就用节点自己的 i18nKey（权威），
    // 没有（非菜单子页面）再按 path 派生
    const node = findMenuByPath(menus, path);
    if (node) {
      items.push({ title: node.i18nKey ? t(node.i18nKey, { ns: 'nav', defaultValue: node.name }) : node.name });
    } else if (EXTRA_PAGE_KEYS[path]) {
      items.push({ title: t(EXTRA_PAGE_KEYS[path]) });
    } else {
      const derived = navKeyFromPath(path);
      const label = derived ? t(derived, { ns: 'nav', defaultValue: '' }) : '';
      if (label) items.push({ title: label });
    }

    return items;
  }, [location.pathname, isSystemMode, menus, t]);

  return (
    <Layout.Header className={['ps-header', sidebarPinned ? 'is-pinned' : 'is-mini'].filter(Boolean).join(' ')}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <SidebarToggle pinned={sidebarPinned} onToggle={toggleSidebarPinned} />
        <Breadcrumb items={breadcrumbItems} style={{ marginLeft: 8 }} />
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Tooltip title={isSystemMode ? t('common:mode.app') : t('common:mode.admin')}>
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
