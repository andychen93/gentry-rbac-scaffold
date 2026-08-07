import { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, Select, message } from 'antd';
import { deptApi } from '../../services/deptApi';
import DeptTreeSelect from '../../components/common/DeptTreeSelect';

interface Props {
  open: boolean;
  deptId: number | null;
  defaultParentId: number;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function DeptFormModal({ open, deptId, defaultParentId, onSuccess, onCancel }: Props) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = deptId !== null;

  useEffect(() => {
    if (open && isEdit) {
      deptApi.detail(deptId!).then((res) => {
        const d = res.data;
        form.setFieldsValue({
          parentId: d.parentId,
          name: d.name,
          leaderId: d.leaderId,
          phone: d.phone,
          email: d.email,
          sort: d.sort,
          status: d.status,
        });
      });
    } else if (open) {
      form.resetFields();
      form.setFieldsValue({ parentId: defaultParentId, sort: 0, status: 1 });
    }
  }, [open, deptId, defaultParentId, form, isEdit]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const payload = {
        parentId: values.parentId ?? 0,
        name: values.name,
        leaderId: values.leaderId || undefined,
        phone: values.phone || undefined,
        email: values.email || undefined,
        sort: values.sort,
        status: values.status ?? 1,
      };

      if (isEdit) {
        await deptApi.update(deptId!, payload);
        message.success('编辑成功');
      } else {
        await deptApi.create(payload);
        message.success('新增成功');
      }
      onSuccess();
    } catch (err: any) {
      if (err?.errorFields) return;
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑部门' : '新增部门'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnHidden
      width={560}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="parentId" label="上级部门">
          <DeptTreeSelect
            showRoot
            excludeId={isEdit ? deptId : null}
          />
        </Form.Item>

        <Form.Item
          name="name"
          label="部门名称"
          rules={[
            { required: true, message: '请输入部门名称' },
            { min: 2, max: 50, message: '2-50字符' },
          ]}
        >
          <Input placeholder="请输入部门名称" />
        </Form.Item>

        <Form.Item
          name="phone"
          label="联系电话"
          rules={[{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式' }]}
        >
          <Input placeholder="请输入联系电话" />
        </Form.Item>

        <Form.Item
          name="email"
          label="邮箱"
          rules={[{ type: 'email', message: '请输入正确的邮箱格式' }]}
        >
          <Input placeholder="请输入邮箱" />
        </Form.Item>

        <Form.Item
          name="sort"
          label="显示排序"
          rules={[{ required: true, message: '请输入排序值' }]}
        >
          <InputNumber min={0} max={999} style={{ width: '100%' }} placeholder="排序值" />
        </Form.Item>

        <Form.Item name="status" label="状态">
          <Select placeholder="请选择状态">
            <Select.Option value={1}>正常</Select.Option>
            <Select.Option value={0}>停用</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
}
