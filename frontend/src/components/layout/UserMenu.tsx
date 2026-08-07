import React from 'react';
import { Dropdown, Avatar, Space } from 'antd';
import { UserOutlined, LogoutOutlined } from '@ant-design/icons';

interface UserMenuProps {
  username?: string;
  avatar?: string;
  onLogout: () => void;
}

/**
 * UserMenu 组件 - 用户下拉菜单
 * 显示用户头像和用户名，下拉菜单包含退出登录
 */
const UserMenu: React.FC<UserMenuProps> = ({ username = '用户', avatar, onLogout }) => {
  const items = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: onLogout,
    },
  ];

  return (
    <Dropdown menu={{ items }} placement="bottomRight">
      <Space style={{ cursor: 'pointer' }}>
        <Avatar size="small" src={avatar} icon={!avatar ? <UserOutlined /> : undefined} />
        <span>{username}</span>
      </Space>
    </Dropdown>
  );
};

export default React.memo(UserMenu);
