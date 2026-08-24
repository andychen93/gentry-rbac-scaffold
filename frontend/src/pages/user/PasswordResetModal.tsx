import { useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { userApi } from '../../services/userApi';
import { useTranslation } from 'react-i18next';

interface Props {
  open: boolean;
  userId: number;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function PasswordResetModal({ open, userId, onSuccess, onCancel }: Props) {
  const { t } = useTranslation(['user', 'common']);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await userApi.resetPassword(userId, { newPassword: values.newPassword });
      message.success(t('pwd.success'));
      form.resetFields();
      onSuccess();
    } catch (err: any) {
      if (err?.errorFields) return;
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={t('pwd.title')}
      open={open}
      onOk={handleOk}
      onCancel={() => { form.resetFields(); onCancel(); }}
      confirmLoading={loading}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="newPassword"
          label={t('pwd.new')}
          rules={[
            { required: true, message: t('common:placeholder.newPassword') },
            { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,20}$/, message: t('common:valid.password') },
          ]}
        >
          <Input.Password placeholder={t('common:placeholder.newPassword')} />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label={t('pwd.confirm')}
          dependencies={['newPassword']}
          rules={[
            { required: true, message: t('pwd.confirmRequired') },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                return Promise.reject(new Error(t('pwd.mismatch')));
              },
            }),
          ]}
        >
          <Input.Password placeholder={t('pwd.confirmPlaceholder')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
