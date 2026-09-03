import { useState } from 'react';
import { Card, Descriptions, Button, Space, Tag } from 'antd';
import { KeyOutlined, EditOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useUserStore } from '../../stores/userStore';
import { makeRoleLabel } from '../../locales/navLabel';
import ChangePasswordModal from './ChangePasswordModal';
import EditProfileModal from './EditProfileModal';

/**
 * 个人中心页（非菜单受控页面，由 UserMenu 下拉入口进入，App.tsx 静态路由 /profile）。
 */
export default function ProfilePage() {
  const { t } = useTranslation(['profile', 'common']);
  const userInfo = useUserStore((s) => s.userInfo);
  const fetchUserInfo = useUserStore((s) => s.fetchUserInfo);
  const [pwdOpen, setPwdOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const roleLabel = makeRoleLabel(t);

  return (
    <div style={{ padding: 24 }}>
      <Card title={t('title')} style={{ maxWidth: 760 }}>
        <Descriptions column={2} bordered size="middle">
          <Descriptions.Item label={t('common:username')}>{userInfo?.username || '-'}</Descriptions.Item>
          <Descriptions.Item label={t('common:nickname')}>{userInfo?.nickname || '-'}</Descriptions.Item>
          <Descriptions.Item label={t('common:dept')}>{userInfo?.deptName || '-'}</Descriptions.Item>
          <Descriptions.Item label={t('common:role')}>
            <Space wrap>
              {(userInfo?.roles || []).map((r) => (
                <Tag key={r.id} color="blue">{roleLabel(r)}</Tag>
              ))}
            </Space>
          </Descriptions.Item>
        </Descriptions>
        <Space style={{ marginTop: 24 }}>
          <Button type="primary" icon={<EditOutlined />} onClick={() => setProfileOpen(true)}>{t('action.edit')}</Button>
          <Button icon={<KeyOutlined />} onClick={() => setPwdOpen(true)}>{t('action.changePwd')}</Button>
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
