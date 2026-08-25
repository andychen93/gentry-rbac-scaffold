import { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Switch, InputNumber, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { dictApi, DictDataVO } from '../../services/dictApi';
import { DICT_TYPES, dictOptions } from '../../locales/dictEnum';

interface Props {
  open: boolean; mode: 'create' | 'edit'; dictType: string; record: DictDataVO | null;
  onSuccess: () => void; onCancel: () => void;
}

export default function DataFormModal({ open, mode, dictType, record, onSuccess, onCancel }: Props) {
  const { t, i18n } = useTranslation(['dictMgmt', 'common', 'dict']);
  const [form] = Form.useForm();
  /*
   * 该字典项的显示名是否已被语言包接管。
   *
   * 派生 key 让**译文优先于库里的 dict_label**：管理员把 sys_user_gender/1 从「男」
   * 改成「男性」会保存成功、界面却毫无变化，且无法自查。所以直接禁用输入，
   * 而不是保存后让人困惑 —— 允许保存一个不生效的值等于制造一个必然被当成 bug 的状态。
   * 判据放前端：语言包是唯一真源，且它就在前端，一句 i18n.exists() 够了。
   */
  const managedByLocale = !!record?.i18nKey && i18n.exists(record.i18nKey, { ns: 'dict' });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open && mode === 'edit' && record) {
      form.setFieldsValue({ dictLabel: record.dictLabel, dictValue: record.dictValue, cssClass: record.cssClass,
        listClass: record.listClass, isDefault: record.isDefault === 1, sort: record.sort, status: record.status, remark: record.remark });
    } else if (open) { form.resetFields(); form.setFieldsValue({ status: 1, sort: 0, isDefault: false }); }
  }, [open, mode, record]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const payload = { ...values, isDefault: values.isDefault ? 1 : 0 };
      if (mode === 'create') { await dictApi.createData(dictType, payload); message.success(t('common:msg.createSuccess')); }
      else { const { dictValue, ...updateData } = payload; await dictApi.updateData(record!.id, updateData); message.success(t('common:msg.updateSuccess')); }
      onSuccess();
    } catch (err: any) { if (err?.errorFields) return; } finally { setLoading(false); }
  };

  return (
    <Modal title={mode === 'create' ? t('data.form.title.create') : t('data.form.title.edit')} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} destroyOnHidden>
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="dictLabel"
          label={t('form.dictLabel')}
          rules={[{ required: true, message: t('common:placeholder.input') }]}
          extra={managedByLocale ? t('form.dictLabel.localeManaged') : undefined}
        >
          <Input placeholder={t('form.dictLabel.placeholder')} disabled={managedByLocale} />
        </Form.Item>
        <Form.Item name="dictValue" label={t('form.dictValue')} rules={[{ required: true, message: t('common:placeholder.input') }]}>
          <Input placeholder={t('form.dictValue.placeholder')} disabled={mode === 'edit'} />
        </Form.Item>
        <Form.Item name="cssClass" label={t('form.cssClass')}>
          <Select
            placeholder={t('common:placeholder.select')}
            allowClear
            options={[
              { value: 'primary', label: t('form.cssClass.primary') },
              { value: 'success', label: t('form.cssClass.success') },
              { value: 'warning', label: t('form.cssClass.warning') },
              { value: 'danger', label: t('form.cssClass.danger') },
              { value: 'default', label: t('form.cssClass.default') },
            ]}
          />
        </Form.Item>
        <Form.Item name="sort" label={t('common:sort')}><InputNumber min={0} max={999} style={{ width: '100%' }} /></Form.Item>
        <Form.Item name="isDefault" label={t('form.isDefault')} valuePropName="checked"><Switch /></Form.Item>
        <Form.Item name="status" label={t('common:status')}>
          <Select options={dictOptions(t, DICT_TYPES.normalDisable, { numeric: true })} />
        </Form.Item>
        <Form.Item name="remark" label={t('common:remark')} rules={[{ max: 500 }]}>
          <Input.TextArea rows={2} placeholder={t('common:placeholder.remark')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
