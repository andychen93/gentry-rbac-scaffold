import React from 'react';
import { Dropdown, Avatar, Space } from 'antd';
import { UserOutlined, LogoutOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

interface UserMenuProps {
  username?: string;
  avatar?: string;
  onLogout: () => void;
}

/**
 * UserMenu 组件 - 用户下拉菜单
 * 显示用户头像和用户名，下拉菜单包含「个人中心」「退出登录」。
 */
const UserMenu: React.FC<UserMenuProps> = ({ username, avatar, onLogout }) => {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const items = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: t('profile'),
      onClick: () => navigate('/profile'),
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: t('logout'),
      onClick: onLogout,
    },
  ];

  return (
    <Dropdown menu={{ items }} placement="bottomRight">
      <Space style={{ cursor: 'pointer' }}>
        <Avatar size="small" src={avatar} icon={!avatar ? <UserOutlined /> : undefined} />
        <span>{username || t('user')}</span>
      </Space>
    </Dropdown>
  );
};

export default React.memo(UserMenu);
