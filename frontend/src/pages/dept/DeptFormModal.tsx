import { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, Select, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { deptApi } from '../../services/deptApi';
import DeptTreeSelect from '../../components/common/DeptTreeSelect';
import { DICT_TYPES, dictOptions } from '../../locales/dictEnum';

interface Props {
  open: boolean;
  deptId: number | null;
  defaultParentId: number;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function DeptFormModal({ open, deptId, defaultParentId, onSuccess, onCancel }: Props) {
  const { t } = useTranslation(['dept', 'common', 'dict']);
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
        message.success(t('common:msg.updateSuccess'));
      } else {
        await deptApi.create(payload);
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
      width={560}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="parentId" label={t('form.parent')}>
          <DeptTreeSelect
            showRoot
            excludeId={isEdit ? deptId : null}
          />
        </Form.Item>

        <Form.Item
          name="name"
          label={t('form.name')}
          rules={[
            { required: true, message: t('form.name.placeholder') },
            { min: 2, max: 50, message: t('common:valid.len2to50') },
          ]}
        >
          <Input placeholder={t('form.name.placeholder')} />
        </Form.Item>

        <Form.Item
          name="phone"
          label={t('common:contactPhone')}
          rules={[{ pattern: /^1[3-9]\d{9}$/, message: t('common:valid.phone') }]}
        >
          <Input placeholder={t('common:placeholder.phone')} />
        </Form.Item>

        <Form.Item
          name="email"
          label={t('common:email')}
          rules={[{ type: 'email', message: t('common:valid.email') }]}
        >
          <Input placeholder={t('common:placeholder.email')} />
        </Form.Item>

        <Form.Item
          name="sort"
          label={t('form.sort')}
          rules={[{ required: true, message: t('form.sort.required') }]}
        >
          <InputNumber min={0} max={999} style={{ width: '100%' }} placeholder={t('form.sort.placeholder')} />
        </Form.Item>

        <Form.Item name="status" label={t('common:status')}>
          <Select
            placeholder={t('form.status.placeholder')}
            options={dictOptions(t, DICT_TYPES.normalDisable, { numeric: true })}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
