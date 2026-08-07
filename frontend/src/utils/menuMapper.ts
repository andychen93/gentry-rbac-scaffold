import type { MenuNavItem } from '../types/menu';
import type { MenuItem } from '../types/menu';

/** 路由配置 */
export interface RouteConfig {
  path: string;
  component: string;
}

/**
 * 后端 component 值 → 前端页面模块懒加载映射
 * 新增页面时在此处添加映射
 */
const COMPONENT_MAP: Record<string, () => Promise<{ default: React.ComponentType }>> = {
  'pages/home/HomePage': () => import('../pages/home/HomePage'),
  'pages/tenant/TenantPage': () => import('../pages/tenant/TenantPage'),
  'pages/user/UserPage': () => import('../pages/user/UserPage'),
  'pages/role/RolePage': () => import('../pages/role/RolePage'),
  'pages/menu/MenuPage': () => import('../pages/menu/MenuPage'),
  'pages/dept/DeptPage': () => import('../pages/dept/DeptPage'),
  'pages/dict/DictPage': () => import('../pages/dict/DictPage'),
  'pages/log/OperLogPage': () => import('../pages/log/OperLogPage'),
  'pages/log/LoginLogPage': () => import('../pages/log/LoginLogPage'),
  'pages/log/OnlineUserPage': () => import('../pages/log/OnlineUserPage'),
  'pages/monitor/RedisMonitorPage': () => import('../pages/monitor/RedisMonitorPage'),
  // ↓↓↓ 新业务页面在此登记：'pages/{module}/{Xxx}Page': () => import('../pages/{module}/{Xxx}Page'),
  // 兼容旧 component 路径
  'system/user/index': () => import('../pages/user/UserPage'),
  'system/role/index': () => import('../pages/role/RolePage'),
  'system/menu/index': () => import('../pages/menu/MenuPage'),
  'system/dept/index': () => import('../pages/dept/DeptPage'),
  'system/dict/index': () => import('../pages/dict/DictPage'),
  'system/tenant/index': () => import('../pages/tenant/TenantPage'),
  'monitor/log/operation/index': () => import('../pages/log/OperLogPage'),
  'monitor/log/login/index': () => import('../pages/log/LoginLogPage'),
  'monitor/online/index': () => import('../pages/log/OnlineUserPage'),
};

/**
 * 获取组件懒加载函数
 */
export function getComponentLoader(componentPath: string | null) {
  if (!componentPath) return null;
  return COMPONENT_MAP[componentPath] ?? null;
}

/** 后端图标名 → 前端 Ant Design 图标名（支持短名和全名） */
const ICON_NAME_MAP: Record<string, string> = {
  // 短名映射（兼容旧数据）
  setting: 'SettingOutlined',
  user: 'UserOutlined',
  team: 'TeamOutlined',
  menu: 'AppstoreOutlined',
  cluster: 'ClusterOutlined',
  book: 'BookOutlined',
  monitor: 'DashboardOutlined',
  form: 'ProfileOutlined',
  login: 'LoginOutlined',
  online: 'WifiOutlined',
  apartment: 'ApartmentOutlined',
  safety: 'SafetyOutlined',
  tool: 'ToolOutlined',
  file: 'FileOutlined',
  laptop: 'LaptopOutlined',
  car: 'CarOutlined',
  home: 'HomeOutlined',
  // 全名直通（新数据已经是 Ant Design 图标名）
  SettingOutlined: 'SettingOutlined',
  MonitorOutlined: 'MonitorOutlined',
  UserOutlined: 'UserOutlined',
  TeamOutlined: 'TeamOutlined',
  CarOutlined: 'CarOutlined',
  DashboardOutlined: 'DashboardOutlined',
  HomeOutlined: 'HomeOutlined',
};

/**
 * 将后端菜单树 MenuNavItem[] 转换为侧边栏 MenuItem[]
 */
export function toSidebarItems(menus: MenuNavItem[]): MenuItem[] {
  return menus.map((menu) => {
    const item: MenuItem = {
      id: String(menu.id),
      label: menu.name,
      icon: menu.icon ? ICON_NAME_MAP[menu.icon] ?? menu.icon : undefined,
      path: menu.path ?? undefined,
      order: menu.sort,
      visible: menu.visible === 1,
    };
    if (menu.children?.length) {
      item.children = toSidebarItems(menu.children);
    }
    return item;
  });
}

/**
 * 从菜单树中提取所有 type=2 的路由配置（叶子菜单页面）
 */
export function toRouteConfigs(menus: MenuNavItem[]): RouteConfig[] {
  const routes: RouteConfig[] = [];

  const walk = (items: MenuNavItem[]) => {
    for (const item of items) {
      if (item.type === 2 && item.path && item.component) {
        routes.push({
          path: item.path.startsWith('/') ? item.path.slice(1) : item.path,
          component: item.component,
        });
      }
      if (item.children?.length) {
        walk(item.children);
      }
    }
  };

  walk(menus);
  return routes;
}
