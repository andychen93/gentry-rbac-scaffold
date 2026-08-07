import React from 'react';
import { Button, Tooltip } from 'antd';
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons';

interface SidebarToggleProps {
  pinned: boolean;
  onToggle: () => void;
}

/**
 * SidebarToggle 组件 - 侧边栏折叠/展开切换按钮
 */
const SidebarToggle: React.FC<SidebarToggleProps> = ({ pinned, onToggle }) => (
  <Tooltip title={pinned ? '收起侧边栏' : '展开侧边栏'}>
    <Button
      type="text"
      icon={pinned ? <MenuFoldOutlined /> : <MenuUnfoldOutlined />}
      onClick={onToggle}
      aria-label={pinned ? '收起侧边栏' : '展开侧边栏'}
    />
  </Tooltip>
);

export default React.memo(SidebarToggle);
