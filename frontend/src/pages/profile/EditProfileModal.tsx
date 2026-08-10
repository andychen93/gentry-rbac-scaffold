import { useEffect, useState } from 'react';
import { Modal, Form, Input, Radio, Select, message } from 'antd';
import { authApi } from '../../services/userApi';

interface Props {
  open: boolean;
  onSuccess: () => void;
  onCancel: () => void;
}

/**
 * 编辑个人资料弹窗。
 * 回显走 authApi.getProfile（仅需登录，不依赖 system:user:list 权限），
 * 提交走 authApi.updateProfile。
 */
export default function EditProfileModal({ open, onSuccess, onCancel }: Props) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open) {
      authApi.getProfile()
        .then((res) => {
          const d = res.data;
          form.setFieldsValue({
            nickname: d.nickname, phone: d.phone, email: d.email,
            gender: d.gender ?? 0, postName: d.postName,
          });
        })
        .catch(() => { /* 拦截器已弹 toast */ });
    }
  }, [open, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await authApi.updateProfile(values);
      message.success('资料修改成功');
      form.resetFields();
      onSuccess();
    } catch (err: any) {
      if (err?.errorFields) return;
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title="编辑资料" open={open} onOk={handleOk}
      onCancel={() => { form.resetFields(); onCancel(); }}
      confirmLoading={loading} destroyOnHidden>
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="nickname" label="昵称"
          rules={[{ required: true, message: '请输入昵称' }, { min: 2, max: 20, message: '2-20字符' }]}>
          <Input placeholder="请输入昵称" />
        </Form.Item>
        <Form.Item name="phone" label="手机号" rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}>
          <Input placeholder="请输入手机号" />
        </Form.Item>
        <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
          <Input placeholder="请输入邮箱" />
        </Form.Item>
        <Form.Item name="gender" label="性别">
          <Radio.Group>
            <Radio value={0}>未知</Radio><Radio value={1}>男</Radio><Radio value={2}>女</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item name="postName" label="职务">
          <Select placeholder="请选择职务" allowClear>
            <Select.Option value="首席执行官">首席执行官</Select.Option>
            <Select.Option value="总监">总监</Select.Option>
            <Select.Option value="经理">经理</Select.Option>
            <Select.Option value="主管">主管</Select.Option>
            <Select.Option value="高级工程师">高级工程师</Select.Option>
            <Select.Option value="工程师">工程师</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
}
