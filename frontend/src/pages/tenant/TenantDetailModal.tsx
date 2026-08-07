import { useEffect, useState } from 'react';
import { Modal, Descriptions, Tag, Spin } from 'antd';
import { tenantMgmtApi } from '../../services/tenantApi';
import type { TenantDetailVO } from '../../services/tenantApi';

interface Props {
  open: boolean;
  tenantId: number | null;
  onCancel: () => void;
}

export default function TenantDetailModal({ open, tenantId, onCancel }: Props) {
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
      title="租户详情"
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
              <Descriptions.Item label="租户编码">{detail.code}</Descriptions.Item>
              <Descriptions.Item label="租户名称">{detail.name}</Descriptions.Item>
              <Descriptions.Item label="联系人">{detail.contact || '-'}</Descriptions.Item>
              <Descriptions.Item label="联系电话">{detail.phone || '-'}</Descriptions.Item>
              <Descriptions.Item label="联系邮箱">{detail.email || '-'}</Descriptions.Item>
              <Descriptions.Item label="地址">{detail.address || '-'}</Descriptions.Item>
              <Descriptions.Item label="到期时间">
                {detail.expireTime ? detail.expireTime.replace('T', ' ').substring(0, 10) : '永不过期'}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={detail.status === 1 ? 'success' : 'error'}>
                  {detail.status === 1 ? '正常' : '已禁用'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="账号限额">{detail.accountLimit}</Descriptions.Item>
              <Descriptions.Item label="设备限额">{detail.deviceLimit}</Descriptions.Item>
              <Descriptions.Item label="备注" span={2}>{detail.remark || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {detail.createTime?.replace('T', ' ')}
              </Descriptions.Item>
              <Descriptions.Item label="更新时间">
                {detail.updateTime?.replace('T', ' ')}
              </Descriptions.Item>
            </Descriptions>

            {detail.statistics && (
              <Descriptions bordered column={3} title="统计数据" style={{ marginTop: 24 }}>
                <Descriptions.Item label="用户数">{detail.statistics.userCount}</Descriptions.Item>
                <Descriptions.Item label="部门数">{detail.statistics.deptCount}</Descriptions.Item>
                <Descriptions.Item label="角色数">{detail.statistics.roleCount}</Descriptions.Item>
              </Descriptions>
            )}
          </>
        )}
      </Spin>
    </Modal>
  );
}
