import { useState } from 'react';
import { Card, Descriptions, Button, Space, Tag } from 'antd';
import { KeyOutlined, EditOutlined } from '@ant-design/icons';
import { useUserStore } from '../../stores/userStore';
import ChangePasswordModal from './ChangePasswordModal';
import EditProfileModal from './EditProfileModal';

/**
 * 个人中心页（非菜单受控页面，由 UserMenu 下拉入口进入，App.tsx 静态路由 /profile）。
 */
export default function ProfilePage() {
  const userInfo = useUserStore((s) => s.userInfo);
  const fetchUserInfo = useUserStore((s) => s.fetchUserInfo);
  const [pwdOpen, setPwdOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);

  return (
    <div style={{ padding: 24 }}>
      <Card title="个人中心" style={{ maxWidth: 760 }}>
        <Descriptions column={2} bordered size="middle">
          <Descriptions.Item label="用户名">{userInfo?.username || '-'}</Descriptions.Item>
          <Descriptions.Item label="昵称">{userInfo?.nickname || '-'}</Descriptions.Item>
          <Descriptions.Item label="部门">{userInfo?.deptName || '-'}</Descriptions.Item>
          <Descriptions.Item label="角色">
            <Space wrap>
              {(userInfo?.roles || []).map((r) => (
                <Tag key={r.id} color="blue">{r.roleName}</Tag>
              ))}
            </Space>
          </Descriptions.Item>
        </Descriptions>
        <Space style={{ marginTop: 24 }}>
          <Button type="primary" icon={<EditOutlined />} onClick={() => setProfileOpen(true)}>编辑资料</Button>
          <Button icon={<KeyOutlined />} onClick={() => setPwdOpen(true)}>修改密码</Button>
        </Space>
      </Card>

      <ChangePasswordModal open={pwdOpen} onSuccess={() => setPwdOpen(false)} onCancel={() => setPwdOpen(false)} />
      <EditProfileModal
        open={profileOpen}
        onSuccess={async () => { setProfileOpen(false); await fetchUserInfo(); }}
        onCancel={() => setProfileOpen(false)}
      />
    </div>
  );
}
