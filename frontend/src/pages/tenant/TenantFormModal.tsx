import { useEffect, useState } from 'react';
import { Modal, Form, Input, DatePicker, message } from 'antd';
import { tenantMgmtApi } from '../../services/tenantApi';
import type { TenantCreateResultVO } from '../../services/tenantApi';
import dayjs from 'dayjs';

interface Props {
  open: boolean;
  tenantId: number | null;
  onSuccess: () => void;
  onCreated: (result: TenantCreateResultVO) => void;
  onCancel: () => void;
}

export default function TenantFormModal({ open, tenantId, onSuccess, onCreated, onCancel }: Props) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = tenantId !== null;

  useEffect(() => {
    if (open && isEdit) {
      tenantMgmtApi.detail(tenantId!).then((res) => {
        const d = res.data;
        form.setFieldsValue({
          code: d.code,
          name: d.name,
          contact: d.contact,
          phone: d.phone,
          email: d.email,
          expireTime: d.expireTime ? dayjs(d.expireTime) : null,
          remark: d.remark,
        });
      });
    } else if (open) {
      form.resetFields();
    }
  }, [open, tenantId]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const expireTime = values.expireTime
        ? values.expireTime.format('YYYY-MM-DDTHH:mm:ss')
        : null;

      if (isEdit) {
        const { code, ...updateData } = values;
        await tenantMgmtApi.update(tenantId!, { ...updateData, expireTime });
        message.success('编辑成功');
        onSuccess();
      } else {
        const res = await tenantMgmtApi.create({ ...values, expireTime });
        onCreated(res.data);
      }
    } catch (err: any) {
      if (err?.errorFields) return;
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑租户' : '新增租户'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnHidden
      width={560}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="code"
          label="租户编码"
          rules={[
            { required: true, message: '请输入租户编码' },
            { pattern: /^[a-zA-Z0-9]{6,20}$/, message: '6-20字符，仅字母数字' },
          ]}
        >
          <Input placeholder="请输入租户编码" disabled={isEdit} />
        </Form.Item>

        <Form.Item
          name="name"
          label="租户名称"
          rules={[
            { required: true, message: '请输入租户名称' },
            { min: 2, max: 100, message: '2-100字符' },
          ]}
        >
          <Input placeholder="请输入租户名称" />
        </Form.Item>

        <Form.Item name="contact" label="联系人" rules={[{ max: 50, message: '最长50字符' }]}>
          <Input placeholder="请输入联系人" />
        </Form.Item>

        <Form.Item
          name="phone"
          label="联系电话"
          rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}
        >
          <Input placeholder="请输入联系电话" />
        </Form.Item>

        <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
          <Input placeholder="请输入邮箱" />
        </Form.Item>

        <Form.Item name="expireTime" label="到期时间">
          <DatePicker showTime style={{ width: '100%' }} placeholder="不选则永不过期" />
        </Form.Item>

        <Form.Item name="remark" label="备注" rules={[{ max: 500, message: '最长500字符' }]}>
          <Input.TextArea rows={3} placeholder="请输入备注" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
