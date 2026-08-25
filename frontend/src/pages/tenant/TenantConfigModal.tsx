import { useEffect, useState } from 'react';
import { Modal, Form, InputNumber, message, Spin } from 'antd';
import { useTranslation } from 'react-i18next';
import { tenantMgmtApi } from '../../services/tenantApi';

interface Props {
  open: boolean;
  tenantId: number | null;
  onSuccess: () => void;
  onCancel: () => void;
}

/**
 * 租户扩展配置弹窗。
 *
 * 字段清单以后端 `TenantConfigDTO` 为准（那边是白名单，只写声明过的键）。
 * 原来这里还有「视频 / 报警 / 报表」开关、「最大设备数」、「地图服务商」——
 * 车辆定位平台的业务概念，已由 `V13__cleanup_business_leftovers.sql` 清掉。
 *
 * 派生项目要加自己的租户级配置：后端 DTO 加字段 + 这里加一个 Form.Item 即可，不用改表。
 */
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
          form.setFieldsValue({ maxUsers: config.maxUsers });
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
          <Form.Item
            name="maxUsers"
            label={t('config.maxUsers')}
            rules={[{ type: 'number', min: 1, message: t('config.minOne') }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder={t('config.maxUsers.placeholder')} />
          </Form.Item>
        </Form>
      </Spin>
    </Modal>
  );
}
