import { useEffect, useState } from 'react';
import { Modal, Form, InputNumber, Switch, Select, message, Spin } from 'antd';
import { tenantMgmtApi } from '../../services/tenantApi';

interface Props {
  open: boolean;
  tenantId: number | null;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function TenantConfigModal({ open, tenantId, onSuccess, onCancel }: Props) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(false);

  useEffect(() => {
    if (open && tenantId) {
      setFetching(true);
      tenantMgmtApi
        .detail(tenantId)
        .then((res) => {
          const config = res.data.config || {};
          const features = (config.features as Record<string, boolean>) || {};
          form.setFieldsValue({
            maxDevices: config.maxDevices,
            maxUsers: config.maxUsers,
            dataRetentionDays: config.dataRetentionDays,
            videoEnabled: features.video ?? false,
            alarmEnabled: features.alarm ?? false,
            reportEnabled: features.report ?? false,
            mapProvider: config.mapProvider,
          });
        })
        .finally(() => setFetching(false));
    } else if (open) {
      form.resetFields();
    }
  }, [open, tenantId]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await tenantMgmtApi.updateConfig(tenantId!, values);
      message.success('配置更新成功');
      onSuccess();
    } catch (err: any) {
      if (err?.errorFields) return;
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="租户配置"
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnHidden
      width={520}
    >
      <Spin spinning={fetching}>
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="功能开关" style={{ marginBottom: 8 }}>
            <div style={{ display: 'flex', gap: 24 }}>
              <Form.Item name="videoEnabled" valuePropName="checked" noStyle>
                <Switch checkedChildren="视频" unCheckedChildren="视频" />
              </Form.Item>
              <Form.Item name="alarmEnabled" valuePropName="checked" noStyle>
                <Switch checkedChildren="报警" unCheckedChildren="报警" />
              </Form.Item>
              <Form.Item name="reportEnabled" valuePropName="checked" noStyle>
                <Switch checkedChildren="报表" unCheckedChildren="报表" />
              </Form.Item>
            </div>
          </Form.Item>

          <Form.Item name="maxDevices" label="最大设备数" rules={[{ type: 'number', min: 1, message: '最小为1' }]}>
            <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入最大设备数" />
          </Form.Item>

          <Form.Item name="maxUsers" label="最大用户数" rules={[{ type: 'number', min: 1, message: '最小为1' }]}>
            <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入最大用户数" />
          </Form.Item>

          <Form.Item
            name="dataRetentionDays"
            label="数据保留天数"
            rules={[{ type: 'number', min: 1, message: '最小为1' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入数据保留天数" />
          </Form.Item>

          <Form.Item name="mapProvider" label="地图服务商">
            <Select placeholder="请选择地图服务商" allowClear>
              <Select.Option value="tianditu">天地图</Select.Option>
              <Select.Option value="amap">高德地图</Select.Option>
              <Select.Option value="baidu">百度地图</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Spin>
    </Modal>
  );
}
