import { useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { authApi } from '../../services/userApi';

interface Props {
  open: boolean;
  onSuccess: () => void;
  onCancel: () => void;
}

/**
 * 修改密码弹窗（当前用户自助）。
 * 改密成功后后端会踢下线，前端延迟跳登录页。
 */
export default function ChangePasswordModal({ open, onSuccess, onCancel }: Props) {
  const { t } = useTranslation(['profile', 'common', 'user']);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await authApi.updatePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword });
      message.success(t('pwd.success'));
      form.resetFields();
      onSuccess();
      // 后端已踢下线（黑名单 + kickout），清 token 跳登录
      setTimeout(() => {
        localStorage.removeItem('gentry_token');
        window.location.href = '/login';
      }, 1200);
    } catch (err: any) {
      if (err?.errorFields) return; // antd Form 校验失败，吞掉
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={t('pwd.title')} open={open} onOk={handleOk}
      onCancel={() => { form.resetFields(); onCancel(); }}
      confirmLoading={loading} destroyOnHidden>
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="oldPassword" label={t('pwd.old')} rules={[{ required: true, message: t('pwd.old.placeholder') }]}>
          <Input.Password placeholder={t('pwd.old.placeholder')} />
        </Form.Item>
        <Form.Item name="newPassword" label={t('pwd.new')}
          rules={[
            { required: true, message: t('common:placeholder.newPassword') },
            { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,20}$/, message: t('common:valid.password') },
          ]}>
          <Input.Password placeholder={t('common:placeholder.newPassword')} />
        </Form.Item>
        <Form.Item name="confirmPassword" label={t('pwd.confirm')} dependencies={['newPassword']}
          rules={[
            { required: true, message: t('user:pwd.confirmRequired') },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                return Promise.reject(new Error(t('user:pwd.mismatch')));
              },
            }),
          ]}>
          <Input.Password placeholder={t('user:pwd.confirmPlaceholder')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
