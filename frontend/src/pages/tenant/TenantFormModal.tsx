import { useEffect, useState } from 'react';
import { Modal, Form, Input, DatePicker, message } from 'antd';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation(['tenant', 'common']);
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
        message.success(t('common:msg.updateSuccess'));
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
      title={isEdit ? t('form.title.edit') : t('form.title.create')}
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
          label={t('form.code')}
          rules={[
            { required: true, message: t('form.code.placeholder') },
            { pattern: /^[a-zA-Z0-9]{6,20}$/, message: t('form.code.hint') },
          ]}
        >
          <Input placeholder={t('form.code.placeholder')} disabled={isEdit} />
        </Form.Item>

        <Form.Item
          name="name"
          label={t('form.name')}
          rules={[
            { required: true, message: t('form.name.placeholder') },
            { min: 2, max: 100, message: t('form.name.hint') },
          ]}
        >
          <Input placeholder={t('form.name.placeholder')} />
        </Form.Item>

        <Form.Item name="contact" label={t('table.contact')} rules={[{ max: 50, message: t('common:valid.max50') }]}>
          <Input placeholder={t('common:placeholder.contact')} />
        </Form.Item>

        <Form.Item
          name="phone"
          label={t('common:contactPhone')}
          rules={[{ pattern: /^1[3-9]\d{9}$/, message: t('common:valid.phone') }]}
        >
          <Input placeholder={t('common:placeholder.phone')} />
        </Form.Item>

        <Form.Item name="email" label={t('common:email')} rules={[{ type: 'email', message: t('common:valid.email') }]}>
          <Input placeholder={t('common:placeholder.email')} />
        </Form.Item>

        <Form.Item name="expireTime" label={t('form.expireTime')}>
          <DatePicker showTime style={{ width: '100%' }} placeholder={t('form.expireTime.placeholder')} />
        </Form.Item>

        <Form.Item name="remark" label={t('common:remark')} rules={[{ max: 500, message: t('common:valid.max500') }]}>
          <Input.TextArea rows={3} placeholder={t('common:placeholder.remark')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
