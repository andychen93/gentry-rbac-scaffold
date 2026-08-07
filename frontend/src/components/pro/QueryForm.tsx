import React from 'react';
import { Card, Form, Input, Select, Button, Space, DatePicker, Row, Col } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import DictSelect from '../common/DictSelect';

export type QueryField = {
  name: string;
  label: string;
  type?: 'input' | 'select' | 'dict' | 'dateRange';
  options?: { label: string; value: string | number }[];
  dictType?: string;
  placeholder?: string;
};

export interface QueryFormProps {
  fields: QueryField[];
  onSearch: (values: Record<string, unknown>) => void;
}

const QueryForm: React.FC<QueryFormProps> = ({ fields, onSearch }) => {
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
                {f.type === 'select' ? (
                  <Select placeholder={f.placeholder ?? '全部'} allowClear options={f.options} style={{ width: '100%' }} />
                ) : f.type === 'dict' ? (
                  <DictSelect dictType={f.dictType!} placeholder={f.placeholder ?? '全部'} />
                ) : f.type === 'dateRange' ? (
                  <DatePicker.RangePicker style={{ width: '100%' }} />
                ) : (
                  <Input placeholder={f.placeholder ?? '请输入'} allowClear style={{ width: '100%' }} />
                )}
              </Form.Item>
            </Col>
          ))}
          <Col xs={24} sm={12} md={8}>
            <Space style={{ marginBottom: 0 }}>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch} aria-label="查询">查询</Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset} aria-label="重置">重置</Button>
            </Space>
          </Col>
        </Row>
      </Form>
    </Card>
  );
};

export default QueryForm;
export { QueryForm };
