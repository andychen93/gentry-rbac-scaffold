import { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, Switch, Select, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { roleApi } from '../../services/roleApi';
import { DICT_TYPES, dictOptions } from '../../locales/dictEnum';

interface Props {
  open: boolean;
  roleId: number | null;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function RoleFormModal({ open, roleId, onSuccess, onCancel }: Props) {
  // 数据权限档位取 dict namespace（dict.sys_data_scope.*），原来这里有一份硬编码拷贝
  const { t } = useTranslation(['role', 'common', 'dict']);
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
        message.success(t('common:msg.updateSuccess'));
      } else {
        await roleApi.create({
          roleCode: values.roleCode,
          roleName: values.roleName,
          dataScope: values.dataScope,
          sort: values.sort,
          status: values.status ? 1 : 0,
          remark: values.remark,
        });
        message.success(t('common:msg.createSuccess'));
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
      title={isEdit ? t('form.title.edit') : t('form.title.create')}
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
          label={t('form.code')}
          rules={[
            { required: true, message: t('form.code.placeholder') },
            { pattern: /^[a-zA-Z][a-zA-Z0-9_]{1,49}$/, message: t('form.code.hint') },
          ]}
        >
          <Input placeholder={t('form.code.placeholder')} disabled={isEdit} />
        </Form.Item>

        <Form.Item
          name="roleName"
          label={t('form.name')}
          rules={[
            { required: true, message: t('form.name.placeholder') },
            { min: 2, max: 50, message: t('common:valid.len2to50') },
          ]}
        >
          <Input placeholder={t('form.name.placeholder')} />
        </Form.Item>

        <Form.Item name="dataScope" label={t('form.dataScope')}>
          <Select options={dictOptions(t, DICT_TYPES.dataScope, { numeric: true })} />
        </Form.Item>

        <Form.Item
          name="sort"
          label={t('common:sort')}
          rules={[{ required: true, message: t('form.sort.required') }]}
        >
          <InputNumber min={0} max={999} style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item name="status" label={t('common:status')} valuePropName="checked">
          <Switch checkedChildren={t('common:enable')} unCheckedChildren={t('common:disable')} />
        </Form.Item>

        <Form.Item name="remark" label={t('common:remark')} rules={[{ max: 500, message: t('common:valid.max500') }]}>
          <Input.TextArea rows={3} placeholder={t('common:placeholder.remark')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
