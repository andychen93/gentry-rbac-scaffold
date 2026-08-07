import { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Switch, InputNumber, message } from 'antd';
import { dictApi, DictDataVO } from '../../services/dictApi';

interface Props {
  open: boolean; mode: 'create' | 'edit'; dictType: string; record: DictDataVO | null;
  onSuccess: () => void; onCancel: () => void;
}

export default function DataFormModal({ open, mode, dictType, record, onSuccess, onCancel }: Props) {
  const [form] = Form.useForm();
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
      if (mode === 'create') { await dictApi.createData(dictType, payload); message.success('新增成功'); }
      else { const { dictValue, ...updateData } = payload; await dictApi.updateData(record!.id, updateData); message.success('编辑成功'); }
      onSuccess();
    } catch (err: any) { if (err?.errorFields) return; } finally { setLoading(false); }
  };

  return (
    <Modal title={mode === 'create' ? '新增字典数据' : '编辑字典数据'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} destroyOnHidden>
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="dictLabel" label="字典标签" rules={[{ required: true, message: '请输入' }]}>
          <Input placeholder="请输入字典标签" />
        </Form.Item>
        <Form.Item name="dictValue" label="字典键值" rules={[{ required: true, message: '请输入' }]}>
          <Input placeholder="请输入字典键值" disabled={mode === 'edit'} />
        </Form.Item>
        <Form.Item name="cssClass" label="样式属性">
          <Select placeholder="请选择" allowClear>
            <Select.Option value="primary">primary（蓝色）</Select.Option>
            <Select.Option value="success">success（绿色）</Select.Option>
            <Select.Option value="warning">warning（橙色）</Select.Option>
            <Select.Option value="danger">danger（红色）</Select.Option>
            <Select.Option value="default">default（灰色）</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item name="sort" label="排序"><InputNumber min={0} max={999} style={{ width: '100%' }} /></Form.Item>
        <Form.Item name="isDefault" label="是否默认" valuePropName="checked"><Switch /></Form.Item>
        <Form.Item name="status" label="状态">
          <Select><Select.Option value={1}>正常</Select.Option><Select.Option value={0}>停用</Select.Option></Select>
        </Form.Item>
        <Form.Item name="remark" label="备注" rules={[{ max: 500 }]}>
          <Input.TextArea rows={2} placeholder="请输入备注" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
