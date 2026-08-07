import { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, message } from 'antd';
import { dictApi, DictTypeListVO } from '../../services/dictApi';

interface Props {
  open: boolean; mode: 'create' | 'edit'; record: DictTypeListVO | null;
  onSuccess: () => void; onCancel: () => void;
}

export default function TypeFormModal({ open, mode, record, onSuccess, onCancel }: Props) {
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
      if (mode === 'create') { await dictApi.createType(values); message.success('新增成功'); }
      else { await dictApi.updateType(record!.id, { dictName: values.dictName, status: values.status, remark: values.remark }); message.success('编辑成功'); }
      onSuccess();
    } catch (err: any) { if (err?.errorFields) return; } finally { setLoading(false); }
  };

  return (
    <Modal title={mode === 'create' ? '新增字典类型' : '编辑字典类型'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} destroyOnHidden>
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="dictName" label="字典名称" rules={[{ required: true, message: '请输入' }, { min: 2, max: 100 }]}>
          <Input placeholder="请输入字典名称" />
        </Form.Item>
        <Form.Item name="dictType" label="字典类型" rules={[{ required: true, message: '请输入' }, { pattern: /^[a-zA-Z][a-zA-Z0-9_]{1,99}$/, message: '字母开头，字母数字下划线' }]}>
          <Input placeholder="请输入字典类型编码" disabled={mode === 'edit'} />
        </Form.Item>
        <Form.Item name="status" label="状态">
          <Select><Select.Option value={1}>正常</Select.Option><Select.Option value={0}>停用</Select.Option></Select>
        </Form.Item>
        <Form.Item name="remark" label="备注" rules={[{ max: 500 }]}>
          <Input.TextArea rows={3} placeholder="请输入备注" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
