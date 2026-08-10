import { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Radio, Switch, Transfer, message } from 'antd';
import { userApi } from '../../services/userApi';
import { roleApi } from '../../services/roleApi';
import DeptTreeSelect from '../../components/common/DeptTreeSelect';

interface Props {
  open: boolean;
  userId: number | null;
  onSuccess: () => void;
  onCancel: () => void;
}

/** Transfer 包装组件，适配 Form 的 value/onChange 协议 */
function RoleTransfer({ value, onChange, dataSource }: {
  value?: number[];
  onChange?: (val: number[]) => void;
  dataSource: { key: string; title: string }[];
}) {
  const targetKeys = (value || []).map(String);
  return (
    <Transfer
      dataSource={dataSource}
      targetKeys={targetKeys}
      onChange={(keys) => onChange?.(keys.map(Number))}
      render={(item) => item.title}
      titles={['可选角色', '已选角色']}
      listStyle={{ width: 210, height: 200 }}
      showSearch
      filterOption={(input, item) => (item.title ?? '').toLowerCase().includes(input.toLowerCase())}
    />
  );
}

export default function UserFormModal({ open, userId, onSuccess, onCancel }: Props) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [roleOptions, setRoleOptions] = useState<{ key: string; title: string }[]>([]);
  const isEdit = userId !== null;

  // 加载角色列表
  useEffect(() => {
    if (open) {
      roleApi.options()
        .then((res) => {
          setRoleOptions((res.data || []).map((r) => ({ key: String(r.id), title: r.roleName })));
        })
        .catch(() => setRoleOptions([]));
    }
  }, [open]);

  useEffect(() => {
    if (open && isEdit) {
      userApi.detail(userId!).then((res) => {
        const d = res.data;
        form.setFieldsValue({
          username: d.username, nickname: d.nickname, deptId: d.deptId || undefined,
          phone: d.phone, email: d.email, gender: d.gender ?? 0, postName: d.postName,
          status: d.status === 1, roleIds: d.roleIds, remark: d.remark,
        });
      });
    } else if (open) {
      form.resetFields();
      form.setFieldsValue({ gender: 0, status: true });
    }
  }, [open, userId]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const payload = { ...values, status: values.status ? 1 : 0 };
      if (isEdit) {
        const { username, password, ...updateData } = payload;
        await userApi.update(userId!, updateData);
        message.success('编辑成功');
      } else {
        await userApi.create(payload);
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
    <Modal title={isEdit ? '编辑用户' : '新增用户'} open={open} onOk={handleOk} onCancel={onCancel}
      confirmLoading={loading} destroyOnHidden width={560}>
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="username" label="用户名"
          rules={[{ required: true, message: '请输入用户名' }, { pattern: /^[a-zA-Z][a-zA-Z0-9_]{3,19}$/, message: '4-20字符，字母开头' }]}>
          <Input placeholder="请输入用户名" disabled={isEdit} />
        </Form.Item>
        <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }, { min: 2, max: 20, message: '2-20字符' }]}>
          <Input placeholder="请输入昵称" />
        </Form.Item>
        {!isEdit && (
          <Form.Item name="password" label="密码"
            rules={[{ required: true, message: '请输入密码' }, { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,20}$/, message: '8-20位，含大小写字母和数字' }]}>
            <Input.Password placeholder="请输入密码" />
          </Form.Item>
        )}
        <Form.Item name="deptId" label="所属部门">
          <DeptTreeSelect placeholder="请选择所属部门" showRoot={false} />
        </Form.Item>
        <Form.Item name="roleIds" label="角色">
          <RoleTransfer dataSource={roleOptions} />
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
