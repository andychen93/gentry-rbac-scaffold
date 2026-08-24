import React, { useEffect } from 'react';
import { Modal, Form, Input, Select, Switch, DatePicker, Row, Col, message } from 'antd';
import DictSelect from '../common/DictSelect';
import { useTranslation } from 'react-i18next';

export type FormFieldType =
  | 'input'
  | 'textarea'
  | 'select'
  | 'dict'
  | 'switch'
  | 'dateRange'
  | 'date'
  | 'number'
  | 'render';

export interface FormField {
  name: string;
  label: string;
  type?: FormFieldType;
  rules?: any[];
  options?: { label: string; value: any }[];
  dictType?: string;
  required?: boolean;
  /** type=render 逃逸口，用于 icon picker / 树选择等复杂态 */
  render?: (form: any) => React.ReactNode;
  /** Col span（24 栅格），默认 24 单列；12 双列；8 三列 */
  span?: number;
  /** Col offset（24 栅格），可选 */
  offset?: number;
}

export interface CrudFormModalProps {
  open: boolean;
  /** null=新增，否则编辑 */
  recordId: string | number | null;
  fields: FormField[];
  title: string;
  /** 编辑回填 */
  onLoad?: (id: string | number) => Promise<Record<string, any>>;
  onSubmit: (values: Record<string, any>, isEdit: boolean) => Promise<void>;
  onSuccess?: () => void;
  onCancel: () => void;
  width?: number;
}

const CrudFormModal: React.FC<CrudFormModalProps> = ({
  open,
  recordId,
  fields,
  title,
  onLoad,
  onSubmit,
  onSuccess,
  onCancel,
  width = 600,
}) => {
  const { t } = useTranslation('common');
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);
  const isEdit = recordId !== null && recordId !== undefined;

  useEffect(() => {
    if (!open) return;
    if (isEdit && onLoad) {
      setLoading(true);
      onLoad(recordId!)
        .then((v) => {
          const vals = { ...v };
          // switch 字段：库内 0/1 → 表单 boolean
          fields
            .filter((f) => f.type === 'switch')
            .forEach((f) => {
              vals[f.name] = vals[f.name] === 1;
            });
          form.setFieldsValue(vals);
        })
        .finally(() => setLoading(false));
    } else {
      form.resetFields();
    }
  }, [open, recordId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleOk = async () => {
    let values: Record<string, any>;
    try {
      values = await form.validateFields();
    } catch {
      // 校验失败：antd Form 已在 UI 显示错误，这里仅吞掉以避免 unhandled rejection
      return;
    }
    // switch 字段：boolean → 0/1（undefined → 0）
    fields
      .filter((f) => f.type === 'switch')
      .forEach((f) => {
        values[f.name] = values[f.name] ? 1 : 0;
      });
    setLoading(true);
    try {
      await onSubmit(values, isEdit);
      message.success(isEdit ? t('msg.updateSuccess') : t('msg.createSuccess'));
      onSuccess?.();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? t('crud.editTitle', { name: title }) : t('crud.createTitle', { name: title })}
      open={open}
      onOk={handleOk}
      confirmLoading={loading}
      onCancel={onCancel}
      destroyOnHidden
      width={width}
    >
      <Form form={form} layout="vertical">
        <Row gutter={16}>
          {fields.map((f) => (
            <Col key={f.name} span={f.span ?? 24} offset={f.offset}>
              <Form.Item
                name={f.name}
                label={f.label}
                rules={f.rules}
                valuePropName={f.type === 'switch' ? 'checked' : undefined}
              >
                {f.type === 'render' ? (
                  f.render?.(form) ?? null
                ) : f.type === 'select' ? (
                  <Select options={f.options} style={{ width: '100%' }} />
                ) : f.type === 'dict' ? (
                  <DictSelect dictType={f.dictType!} />
                ) : f.type === 'switch' ? (
                  <Switch />
                ) : f.type === 'date' ? (
                  <DatePicker style={{ width: '100%' }} />
                ) : f.type === 'textarea' ? (
                  <Input.TextArea rows={2} maxLength={500} />
                ) : f.type === 'number' ? (
                  <Input type="number" style={{ width: '100%' }} />
                ) : (
                  <Input maxLength={100} style={{ width: '100%' }} />
                )}
              </Form.Item>
            </Col>
          ))}
        </Row>
      </Form>
    </Modal>
  );
};

export default CrudFormModal;
export { CrudFormModal };
