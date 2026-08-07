import { useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { userApi } from '../../services/userApi';

interface Props {
  open: boolean;
  userId: number;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function PasswordResetModal({ open, userId, onSuccess, onCancel }: Props) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await userApi.resetPassword(userId, { newPassword: values.newPassword });
      message.success('密码重置成功');
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
      title="重置密码"
      open={open}
      onOk={handleOk}
      onCancel={() => { form.resetFields(); onCancel(); }}
      confirmLoading={loading}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="newPassword"
          label="新密码"
          rules={[
            { required: true, message: '请输入新密码' },
            { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,20}$/, message: '8-20位，含大小写字母和数字' },
          ]}
        >
          <Input.Password placeholder="请输入新密码" />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label="确认密码"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: '请确认密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                return Promise.reject(new Error('两次密码不一致'));
              },
            }),
          ]}
        >
          <Input.Password placeholder="请再次输入密码" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
