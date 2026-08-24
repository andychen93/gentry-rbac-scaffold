import React, { useMemo } from 'react';
import { Menu, Skeleton, Alert, Button } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import type { MenuItem } from '../../types/menu';
import { useTranslation } from 'react-i18next';
import { useUserStore } from '../../stores/userStore';
import {
  UserOutlined,
  SettingOutlined,
  DashboardOutlined,
  TeamOutlined,
  SafetyOutlined,
  AppstoreOutlined,
  FileOutlined,
  FileTextOutlined,
  ToolOutlined,
  ApartmentOutlined,
  ClusterOutlined,
  BookOutlined,
  ProfileOutlined,
  LoginOutlined,
  WifiOutlined,
  MonitorOutlined,
  CarOutlined,
  EyeOutlined,
  NodeIndexOutlined,
  HomeOutlined,
} from '@ant-design/icons';

/** 图标名称 → React 节点映射 */
const iconMap: Record<string, React.ReactNode> = {
  UserOutlined: <UserOutlined />,
  SettingOutlined: <SettingOutlined />,
  DashboardOutlined: <DashboardOutlined />,
  TeamOutlined: <TeamOutlined />,
  SafetyOutlined: <SafetyOutlined />,
  AppstoreOutlined: <AppstoreOutlined />,
  FileOutlined: <FileOutlined />,
  FileTextOutlined: <FileTextOutlined />,
  ToolOutlined: <ToolOutlined />,
  ApartmentOutlined: <ApartmentOutlined />,
  ClusterOutlined: <ClusterOutlined />,
  BookOutlined: <BookOutlined />,
  ProfileOutlined: <ProfileOutlined />,
  LoginOutlined: <LoginOutlined />,
  WifiOutlined: <WifiOutlined />,
  MonitorOutlined: <MonitorOutlined />,
  CarOutlined: <CarOutlined />,
  EyeOutlined: <EyeOutlined />,
  NodeIndexOutlined: <NodeIndexOutlined />,
  HomeOutlined: <HomeOutlined />,
};

/** 根据图标名称获取图标节点 */
const getIcon = (iconName?: string): React.ReactNode | undefined => {
  if (!iconName) return undefined;
  return iconMap[iconName];
};

/** MenuList 组件 Props */
export interface MenuListProps {
  /** 菜单数据 */
  items: MenuItem[];
  /** 当前选中的菜单 key */
  selectedKey?: string;
  /** 当前展开的子菜单 keys */
  openKeys?: string[];
  /** 菜单项选中回调 */
  onSelect?: (key: string) => void;
  /** 子菜单展开/收起回调 */
  onOpenChange?: (keys: string[]) => void;
  /** 侧边栏是否折叠 */
  collapsed?: boolean;
  /** 是否加载中 */
  loading?: boolean;
  /** 错误信息 */
  error?: string | null;
  /** 重试回调 */
  onRetry?: () => void;
}

/**
 * 将 MenuItem[] 递归转换为 Ant Design Menu 的 items 格式
 * 同时根据用户权限过滤不可见的菜单项
 */
const convertToAntdItems = (
  menuItems: MenuItem[],
  hasPermission: (perm: string) => boolean,
): NonNullable<MenuProps['items']> => {
  return menuItems
    .filter((item) => {
      // 显式设置 visible=false 则隐藏
      if (item.visible === false) return false;
      // 如果有权限要求，检查用户是否拥有其中任一权限
      if (item.permissions && item.permissions.length > 0) {
        return item.permissions.some((p) => hasPermission(p));
      }
      return true;
    })
    .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
    .map((item) => {
      const icon = getIcon(item.icon);
      // 有子菜单 → SubMenu 类型
      if (item.children && item.children.length > 0) {
        const children = convertToAntdItems(item.children, hasPermission);
        // 过滤后无可见子项则不渲染该父级
        if (children.length === 0) return null;
        return {
          key: item.id,
          icon,
          label: item.label,
          children,
        };
      }
      // 叶子菜单项
      return {
        key: item.id,
        icon,
        label: item.label,
      };
    })
    .filter(Boolean) as NonNullable<MenuProps['items']>;
};

/**
 * MenuList 组件 - 侧边栏菜单列表
 *
 * 将 MenuItem[] 数据转换为 Ant Design Menu 的 items 格式渲染，
 * 支持权限过滤、嵌套菜单、加载/错误状态。
 */
const MenuList: React.FC<MenuListProps> = ({
  items,
  selectedKey,
  openKeys,
  onSelect,
  onOpenChange,
  collapsed = false,
  loading = false,
  error = null,
  onRetry,
}) => {
  const hasPermission = useUserStore((s) => s.hasPermission);

  const { t } = useTranslation('common');
  const antdItems = useMemo(
    () => convertToAntdItems(items, hasPermission),
    [items, hasPermission],
  );

  // 加载状态
  if (loading) {
    return (
      <div style={{ padding: 16 }}>
        <Skeleton active paragraph={{ rows: 6 }} title={false} />
      </div>
    );
  }

  // 错误状态
  if (error) {
    return (
      <div style={{ padding: 16 }}>
        <Alert
          type="error"
          message={t('common:menu.loadFailed')}
          description={error}
          showIcon
          style={{ marginBottom: 12 }}
        />
        {onRetry && (
          <Button
            icon={<ReloadOutlined />}
            onClick={onRetry}
            block
          >
            重试
          </Button>
        )}
      </div>
    );
  }

  return (
    <Menu
      mode="inline"
      inlineCollapsed={collapsed}
      selectedKeys={selectedKey ? [selectedKey] : []}
      openKeys={openKeys}
      onOpenChange={onOpenChange}
      onSelect={({ key }) => onSelect?.(key)}
      items={antdItems}
      style={{ borderRight: 0 }}
    />
  );
};

export default React.memo(MenuList);
