import { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, Switch, Select, message } from 'antd';
import { roleApi } from '../../services/roleApi';

interface Props {
  open: boolean;
  roleId: number | null;
  onSuccess: () => void;
  onCancel: () => void;
}

const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '本部门及子部门数据' },
  { value: 3, label: '本部门数据' },
  { value: 4, label: '仅本人数据' },
  { value: 5, label: '自定义' },
];

export default function RoleFormModal({ open, roleId, onSuccess, onCancel }: Props) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = roleId !== null;

  useEffect(() => {
    if (open && isEdit) {
      roleApi.detail(roleId!).then((res) => {
        const d = res.data;
        form.setFieldsValue({
          roleCode: d.roleCode,
          roleName: d.roleName,
          dataScope: d.dataScope,
          sort: d.sort,
          status: d.status === 1,
          remark: d.remark,
        });
      });
    } else if (open) {
      form.resetFields();
      form.setFieldsValue({ dataScope: 1, sort: 0, status: true });
    }
  }, [open, roleId]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit) {
        await roleApi.update(roleId!, {
          roleName: values.roleName,
          dataScope: values.dataScope,
          sort: values.sort,
          status: values.status ? 1 : 0,
          remark: values.remark,
        });
        message.success('编辑成功');
      } else {
        await roleApi.create({
          roleCode: values.roleCode,
          roleName: values.roleName,
          dataScope: values.dataScope,
          sort: values.sort,
          status: values.status ? 1 : 0,
          remark: values.remark,
        });
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
      title={isEdit ? '编辑角色' : '新增角色'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnHidden
      width={520}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="roleCode"
          label="角色编码"
          rules={[
            { required: true, message: '请输入角色编码' },
            { pattern: /^[a-zA-Z][a-zA-Z0-9_]{1,49}$/, message: '字母开头，2-50字符，仅字母数字下划线' },
          ]}
        >
          <Input placeholder="请输入角色编码" disabled={isEdit} />
        </Form.Item>

        <Form.Item
          name="roleName"
          label="角色名称"
          rules={[
            { required: true, message: '请输入角色名称' },
            { min: 2, max: 50, message: '2-50字符' },
          ]}
        >
          <Input placeholder="请输入角色名称" />
        </Form.Item>

        <Form.Item name="dataScope" label="数据权限">
          <Select options={DATA_SCOPE_OPTIONS} />
        </Form.Item>

        <Form.Item
          name="sort"
          label="排序"
          rules={[{ required: true, message: '请输入排序' }]}
        >
          <InputNumber min={0} max={999} style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item name="status" label="状态" valuePropName="checked">
          <Switch checkedChildren="启用" unCheckedChildren="禁用" />
        </Form.Item>

        <Form.Item name="remark" label="备注" rules={[{ max: 500, message: '最长500字符' }]}>
          <Input.TextArea rows={3} placeholder="请输入备注" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
