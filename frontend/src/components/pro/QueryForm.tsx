import React from 'react';
import { Card, Form, Input, Select, Button, Space, DatePicker, Row, Col } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import DictSelect from '../common/DictSelect';
import { useTranslation } from 'react-i18next';

export type QueryField = {
  name: string;
  label: string;
  type?: 'input' | 'select' | 'dict' | 'dateRange' | 'node';
  options?: { label: string; value: string | number }[];
  dictType?: string;
  placeholder?: string;
  /**
   * type='node' 时渲染的自定义控件，用于「下拉 table」这类内置类型覆盖不到的筛选。
   *
   * 该元素会被 Form.Item 克隆并注入 value/onChange，所以它必须是**受控组件**：
   * 接受 value、变更时调 onChange，且值应是可直接进查询参数的标量
   * （别把整条记录塞进去，那会被当成查询参数序列化）。
   * 现成的：components/common/UserTableSelect、MenuTableSelect。
   */
  node?: React.ReactElement;
};

export interface QueryFormProps {
  fields: QueryField[];
  onSearch: (values: Record<string, unknown>) => void;
}

const QueryForm: React.FC<QueryFormProps> = ({ fields, onSearch }) => {
  const { t } = useTranslation('common');
  const [form] = Form.useForm();
  const handleSearch = () => {
    const v = form.getFieldsValue();
    onSearch(Object.fromEntries(Object.entries(v).filter(([, x]) => x !== undefined && x !== '' && x !== null)));
  };
  const handleReset = () => {
    form.resetFields();
    onSearch({});
  };

  return (
    <Card style={{ marginBottom: 16 }}>
      <Form form={form} component={false}>
        <Row gutter={[16, 16]}>
          {fields.map((f) => (
            <Col key={f.name} xs={24} sm={12} md={8} lg={6}>
              <Form.Item name={f.name} label={f.label} style={{ marginBottom: 0 }}>
                {f.type === 'node' ? (
                  f.node
                ) : f.type === 'select' ? (
                  <Select placeholder={f.placeholder ?? t('common:all')} allowClear options={f.options} style={{ width: '100%' }} />
                ) : f.type === 'dict' ? (
                  <DictSelect dictType={f.dictType!} placeholder={f.placeholder ?? t('common:all')} />
                ) : f.type === 'dateRange' ? (
                  <DatePicker.RangePicker style={{ width: '100%' }} />
                ) : (
                  <Input placeholder={f.placeholder ?? t('common:placeholder.input')} allowClear style={{ width: '100%' }} />
                )}
              </Form.Item>
            </Col>
          ))}
          {/*
            查询/重置固定靠右：flex="auto" 吃掉本行剩余宽度，textAlign 把按钮推到最右。
            字段刚好占满一行时该 Col 会换行，此时 auto = 整行宽，按钮仍在右端。
          */}
          <Col flex="auto" style={{ textAlign: 'right' }}>
            <Space style={{ marginBottom: 0 }}>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch} aria-label={t('common:query')}>{t('common:query')}</Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset} aria-label={t('common:reset')}>{t('common:reset')}</Button>
            </Space>
          </Col>
        </Row>
      </Form>
    </Card>
  );
};

export default QueryForm;
export { QueryForm };
