import { useEffect, useState } from 'react';
import { Modal, Descriptions, Tag, Spin } from 'antd';
import { useTranslation } from 'react-i18next';
import { tenantMgmtApi } from '../../services/tenantApi';
import type { TenantDetailVO } from '../../services/tenantApi';

interface Props {
  open: boolean;
  tenantId: number | null;
  onCancel: () => void;
}

export default function TenantDetailModal({ open, tenantId, onCancel }: Props) {
  const { t } = useTranslation(['tenant', 'common']);
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<TenantDetailVO | null>(null);

  useEffect(() => {
    if (open && tenantId) {
      setLoading(true);
      tenantMgmtApi
        .detail(tenantId)
        .then((res) => setDetail(res.data))
        .finally(() => setLoading(false));
    }
  }, [open, tenantId]);

  return (
    <Modal
      title={t('detail.title')}
      open={open}
      onCancel={onCancel}
      footer={null}
      destroyOnHidden
      width={700}
    >
      <Spin spinning={loading}>
        {detail && (
          <>
            <Descriptions bordered column={2} style={{ marginTop: 16 }}>
              <Descriptions.Item label={t('table.code')}>{detail.code}</Descriptions.Item>
              <Descriptions.Item label={t('table.name')}>{detail.name}</Descriptions.Item>
              <Descriptions.Item label={t('table.contact')}>{detail.contact || '-'}</Descriptions.Item>
              <Descriptions.Item label={t('common:contactPhone')}>{detail.phone || '-'}</Descriptions.Item>
              <Descriptions.Item label={t('detail.email')}>{detail.email || '-'}</Descriptions.Item>
              <Descriptions.Item label={t('common:address')}>{detail.address || '-'}</Descriptions.Item>
              <Descriptions.Item label={t('table.expireTime')}>
                {detail.expireTime
                  ? detail.expireTime.replace('T', ' ').substring(0, 10)
                  : t('common:neverExpire')}
              </Descriptions.Item>
              <Descriptions.Item label={t('common:status')}>
                {/* 「已禁用」与字典里的「停用」不是同一句，故留在 tenant namespace */}
                <Tag color={detail.status === 1 ? 'success' : 'error'}>
                  {detail.status === 1 ? t('status.normal') : t('status.disabled')}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label={t('detail.accountLimit')}>{detail.accountLimit}</Descriptions.Item>
              <Descriptions.Item label={t('detail.deviceLimit')}>{detail.deviceLimit}</Descriptions.Item>
              <Descriptions.Item label={t('common:remark')} span={2}>{detail.remark || '-'}</Descriptions.Item>
              <Descriptions.Item label={t('common:createTime')}>
                {detail.createTime?.replace('T', ' ')}
              </Descriptions.Item>
              <Descriptions.Item label={t('common:updateTime')}>
                {detail.updateTime?.replace('T', ' ')}
              </Descriptions.Item>
            </Descriptions>

            {detail.statistics && (
              <Descriptions bordered column={3} title={t('detail.statistics')} style={{ marginTop: 24 }}>
                <Descriptions.Item label={t('common:userCount')}>{detail.statistics.userCount}</Descriptions.Item>
                <Descriptions.Item label={t('detail.deptCount')}>{detail.statistics.deptCount}</Descriptions.Item>
                <Descriptions.Item label={t('detail.roleCount')}>{detail.statistics.roleCount}</Descriptions.Item>
              </Descriptions>
            )}
          </>
        )}
      </Spin>
    </Modal>
  );
}
