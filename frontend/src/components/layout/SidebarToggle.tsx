import React from 'react';
import { Button, Tooltip } from 'antd';
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

interface SidebarToggleProps {
  pinned: boolean;
  onToggle: () => void;
}

/**
 * SidebarToggle 组件 - 侧边栏折叠/展开切换按钮
 */
const SidebarToggle: React.FC<SidebarToggleProps> = ({ pinned, onToggle }) => {
  // 原来是「箭头函数直接返回 JSX」，用不了 hook，故改成带函数体
  const { t } = useTranslation('common');
  const label = pinned ? t('sidebar.collapse') : t('sidebar.expand');
  return (
    <Tooltip title={label}>
      <Button
        type="text"
        icon={pinned ? <MenuFoldOutlined /> : <MenuUnfoldOutlined />}
        onClick={onToggle}
        aria-label={label}
      />
    </Tooltip>
  );
};

export default React.memo(SidebarToggle);
