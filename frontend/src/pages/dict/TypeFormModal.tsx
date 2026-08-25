import { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { dictApi, DictTypeListVO } from '../../services/dictApi';
import { DICT_TYPES, dictOptions } from '../../locales/dictEnum';

interface Props {
  open: boolean; mode: 'create' | 'edit'; record: DictTypeListVO | null;
  onSuccess: () => void; onCancel: () => void;
}

export default function TypeFormModal({ open, mode, record, onSuccess, onCancel }: Props) {
  const { t } = useTranslation(['dictMgmt', 'common', 'dict']);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open && mode === 'edit' && record) {
      form.setFieldsValue({ dictName: record.dictName, dictType: record.dictType, status: record.status, remark: record.remark });
    } else if (open) { form.resetFields(); form.setFieldsValue({ status: 1 }); }
  }, [open, mode, record]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      if (mode === 'create') { await dictApi.createType(values); message.success(t('common:msg.createSuccess')); }
      else { await dictApi.updateType(record!.id, { dictName: values.dictName, status: values.status, remark: values.remark }); message.success(t('common:msg.updateSuccess')); }
      onSuccess();
    } catch (err: any) { if (err?.errorFields) return; } finally { setLoading(false); }
  };

  return (
    <Modal title={mode === 'create' ? t('type.form.title.create') : t('type.form.title.edit')} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} destroyOnHidden>
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="dictName" label={t('table.dictName')} rules={[{ required: true, message: t('common:placeholder.input') }, { min: 2, max: 100 }]}>
          <Input placeholder={t('type.form.name.placeholder')} />
        </Form.Item>
        <Form.Item name="dictType" label={t('table.dictType')} rules={[{ required: true, message: t('common:placeholder.input') }, { pattern: /^[a-zA-Z][a-zA-Z0-9_]{1,99}$/, message: t('type.form.type.hint') }]}>
          <Input placeholder={t('type.form.type.placeholder')} disabled={mode === 'edit'} />
        </Form.Item>
        <Form.Item name="status" label={t('common:status')}>
          <Select options={dictOptions(t, DICT_TYPES.normalDisable, { numeric: true })} />
        </Form.Item>
        <Form.Item name="remark" label={t('common:remark')} rules={[{ max: 500 }]}>
          <Input.TextArea rows={3} placeholder={t('common:placeholder.remark')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
