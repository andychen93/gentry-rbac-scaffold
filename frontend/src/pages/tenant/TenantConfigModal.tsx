import { useEffect, useState } from 'react';
import { Modal, Form, InputNumber, Switch, Select, message, Spin } from 'antd';
import { useTranslation } from 'react-i18next';
import { tenantMgmtApi } from '../../services/tenantApi';

interface Props {
  open: boolean;
  tenantId: number | null;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function TenantConfigModal({ open, tenantId, onSuccess, onCancel }: Props) {
  const { t } = useTranslation(['tenant', 'common']);
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
      message.success(t('config.success'));
      onSuccess();
    } catch (err: any) {
      if (err?.errorFields) return;
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={t('config.title')}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnHidden
      width={520}
    >
      <Spin spinning={fetching}>
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          {/*
            * TODO [清理业务残留] 「视频 / 报警 / 报表」「最大设备数」「地图服务商」是车辆监控
            * 项目的遗留字段，与 RBAC 脚手架无关。清理要动 sys_tenant 的配置结构（Flyway 迁移
            * + 后端 DTO），属数据模型变更，不在 i18n 批次内做，先照原样翻。
            */}
          <Form.Item label={t('config.features')} style={{ marginBottom: 8 }}>
            <div style={{ display: 'flex', gap: 24 }}>
              <Form.Item name="videoEnabled" valuePropName="checked" noStyle>
                <Switch
                  checkedChildren={t('config.feature.video')}
                  unCheckedChildren={t('config.feature.video')}
                />
              </Form.Item>
              <Form.Item name="alarmEnabled" valuePropName="checked" noStyle>
                <Switch
                  checkedChildren={t('config.feature.alarm')}
                  unCheckedChildren={t('config.feature.alarm')}
                />
              </Form.Item>
              <Form.Item name="reportEnabled" valuePropName="checked" noStyle>
                <Switch
                  checkedChildren={t('config.feature.report')}
                  unCheckedChildren={t('config.feature.report')}
                />
              </Form.Item>
            </div>
          </Form.Item>

          <Form.Item
            name="maxDevices"
            label={t('config.maxDevices')}
            rules={[{ type: 'number', min: 1, message: t('config.minOne') }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder={t('config.maxDevices.placeholder')} />
          </Form.Item>

          <Form.Item
            name="maxUsers"
            label={t('config.maxUsers')}
            rules={[{ type: 'number', min: 1, message: t('config.minOne') }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder={t('config.maxUsers.placeholder')} />
          </Form.Item>

          <Form.Item
            name="dataRetentionDays"
            label={t('config.retentionDays')}
            rules={[{ type: 'number', min: 1, message: t('config.minOne') }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder={t('config.retentionDays.placeholder')} />
          </Form.Item>

          <Form.Item name="mapProvider" label={t('config.mapProvider')}>
            <Select
              placeholder={t('config.mapProvider.placeholder')}
              allowClear
              options={[
                { value: 'tianditu', label: t('config.mapProvider.tianditu') },
                { value: 'amap', label: t('config.mapProvider.amap') },
                { value: 'baidu', label: t('config.mapProvider.baidu') },
              ]}
            />
          </Form.Item>
        </Form>
      </Spin>
    </Modal>
  );
}
