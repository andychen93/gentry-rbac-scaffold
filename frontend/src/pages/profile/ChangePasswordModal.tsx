import { useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
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
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await authApi.updatePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword });
      message.success('密码修改成功，请重新登录');
      form.resetFields();
      onSuccess();
      // 后端已踢下线（黑名单 + kickout），清 token 跳登录
      setTimeout(() => {
        localStorage.removeItem('precision_token');
        window.location.href = '/login';
      }, 1200);
    } catch (err: any) {
      if (err?.errorFields) return; // antd Form 校验失败，吞掉
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title="修改密码" open={open} onOk={handleOk}
      onCancel={() => { form.resetFields(); onCancel(); }}
      confirmLoading={loading} destroyOnHidden>
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="oldPassword" label="原密码" rules={[{ required: true, message: '请输入原密码' }]}>
          <Input.Password placeholder="请输入原密码" />
        </Form.Item>
        <Form.Item name="newPassword" label="新密码"
          rules={[
            { required: true, message: '请输入新密码' },
            { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,20}$/, message: '8-20位，含大小写字母和数字' },
          ]}>
          <Input.Password placeholder="请输入新密码" />
        </Form.Item>
        <Form.Item name="confirmPassword" label="确认密码" dependencies={['newPassword']}
          rules={[
            { required: true, message: '请确认密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                return Promise.reject(new Error('两次密码不一致'));
              },
            }),
          ]}>
          <Input.Password placeholder="请再次输入密码" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
