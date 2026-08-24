import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useLayoutStore } from '../../stores/layoutStore';
import { useUserStore } from '../../stores/userStore';
import { useTranslation } from 'react-i18next';
import { toSidebarItems } from '../../utils/menuMapper';
import { makeNavLabel } from '../../locales/navLabel';
import type { MenuItem, MenuNavItem } from '../../types/menu';
import AppHeader from './AppHeader';
import AppFooter from './AppFooter';
import GentrySidenav from './GentrySidenav';
import PageShell from './PageShell';

const BREAKPOINT_MOBILE = 768;
const BREAKPOINT_TABLET = 1024;

function debounce<T extends (...args: any[]) => void>(fn: T, delay: number): T & { cancel: () => void } {
  let timer: ReturnType<typeof setTimeout> | null = null;
  const debounced = (...args: any[]) => {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
  debounced.cancel = () => {
    if (timer) clearTimeout(timer);
  };
  return debounced as T & { cancel: () => void };
}

/** 系统管理路径前缀（菜单过滤用） */
const SYSTEM_MENU_PREFIXES = ['/system', '/monitor', '/monitor-center'];

/** 系统管理路径前缀（路由判断用，带尾部 / 避免误匹配） */
const SYSTEM_ROUTE_PREFIXES = ['/system/', '/monitor/', '/monitor-center/'];

/** 判断菜单项是否属于系统布局 */
const isSystemMenu = (menu: MenuNavItem): boolean =>
  SYSTEM_MENU_PREFIXES.some((p) => (menu.path ?? '').startsWith(p));

/**
 * 根据 MenuItem 树构建 id → path 映射表
 */
const buildPathMap = (items: MenuItem[]): Record<string, string> => {
  const map: Record<string, string> = {};
  const walk = (list: MenuItem[]) => {
    for (const item of list) {
      if (item.path) {
        map[item.id] = item.path;
      }
      if (item.children?.length) {
        walk(item.children);
      }
    }
  };
  walk(items);
  return map;
};

/**
 * 根据当前 pathname 在菜单树中查找匹配的菜单 id
 */
const findSelectedKey = (
  items: MenuItem[],
  pathname: string,
): string | undefined => {
  for (const item of items) {
    if (item.path && pathname === item.path) {
      return item.id;
    }
    if (item.children?.length) {
      const found = findSelectedKey(item.children, pathname);
      if (found) return found;
    }
  }
  return undefined;
};

/**
 * 根据选中的菜单 id 查找需要展开的父级 id 列表
 */
const findOpenKeys = (
  items: MenuItem[],
  selectedKey: string,
): string[] => {
  const search = (list: MenuItem[], parents: string[]): string[] | null => {
    for (const item of list) {
      if (item.id === selectedKey) {
        return parents;
      }
      if (item.children?.length) {
        const result = search(item.children, [...parents, item.id]);
        if (result) return result;
      }
    }
    return null;
  };
  return search(items, []) ?? [];
};

interface AppLayoutProps {
  children: React.ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation('nav');

  // layoutStore 状态
  const sidebarPinned = useLayoutStore((s) => s.sidebarPinned);
  const setSidebarPinned = useLayoutStore((s) => s.setSidebarPinned);
  const initializeLayout = useLayoutStore((s) => s.initializeLayout);

  // userStore 状态
  const token = useUserStore((s) => s.token);
  const userInfo = useUserStore((s) => s.userInfo);
  const fetchUserInfo = useUserStore((s) => s.fetchUserInfo);
  const navMenus = useUserStore((s) => s.menus);
  const isLoggedIn = useUserStore((s) => s.isLoggedIn);

  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    initializeLayout();

    if (token && !userInfo) {
      fetchUserInfo().catch((err) => {
        console.error('Failed to fetch user info:', err);
      });
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth;
      if (width < BREAKPOINT_MOBILE) {
        setSidebarPinned(false);
      } else if (width >= BREAKPOINT_TABLET) {
        setSidebarPinned(true);
      }
    };

    const debouncedResize = debounce(handleResize, 200);
    handleResize();

    window.addEventListener('resize', debouncedResize);
    return () => {
      window.removeEventListener('resize', debouncedResize);
      debouncedResize.cancel();
    };
  }, [setSidebarPinned]);

  // 根据当前路由路径判断模式
  const systemMode = SYSTEM_ROUTE_PREFIXES.some((p) => location.pathname.startsWith(p))
    ? 'admin' : 'app';

  // 将后端 MenuNavItem[] 按模式分流为侧边栏 MenuItem[]。
  // 依赖里必须有 t：label 是渲染时才算的，语言一变就要重建 items。
  // 少了这个依赖，切语言侧边栏不会更新，而且中文环境下永远发现不了。
  const menuItems = useMemo(() => {
    if (!isLoggedIn) return [];
    const labelOf = makeNavLabel(t);
    if (systemMode === 'admin') {
      return toSidebarItems(navMenus.filter(isSystemMenu), labelOf);
    }
    return toSidebarItems(navMenus.filter((m) => !isSystemMenu(m)), labelOf);
  }, [navMenus, isLoggedIn, systemMode, t]);

  // 展开的子菜单 keys
  const [openKeys, setOpenKeys] = useState<string[]>([]);

  // id → path 映射表
  const pathMap = useMemo(() => buildPathMap(menuItems), [menuItems]);

  // 根据当前路径计算选中的菜单 key
  const selectedKey = useMemo(
    () => findSelectedKey(menuItems, location.pathname),
    [menuItems, location.pathname],
  );

  // 路径变化时自动展开对应的父级菜单
  useEffect(() => {
    if (selectedKey && menuItems.length > 0) {
      const keys = findOpenKeys(menuItems, selectedKey);
      setOpenKeys((prev) => {
        const merged = new Set([...prev, ...keys]);
        return Array.from(merged);
      });
    }
  }, [selectedKey, menuItems]);

  // 菜单项点击 → 导航
  const handleSelect = useCallback(
    (key: string) => {
      const path = pathMap[key];
      if (path) {
        navigate(path);
      }
    },
    [pathMap, navigate],
  );

  // 子菜单展开/收起
  const handleOpenChange = useCallback((keys: string[]) => {
    setOpenKeys(keys);
  }, []);

  return (
    <div className="ps-app">
      <AppHeader />
      <GentrySidenav
        items={menuItems}
        selectedKey={selectedKey}
        openKeys={openKeys}
        onSelect={handleSelect}
        onOpenChange={handleOpenChange}
        pinned={sidebarPinned}
      />
      <PageShell>{children}</PageShell>
      <AppFooter />
    </div>
  );
};

export default React.memo(AppLayout);
