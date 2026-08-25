import { Descriptions, Modal, Skeleton, Tag, theme } from 'antd';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { redisMonitorApi, type RedisKeyVO } from '../../../services/monitorApi';
import { formatTtl } from '../../../utils/format';

interface Props {
  open: boolean;
  keyName: string;
  onClose: () => void;
}

export default function KeyValueModal({ open, keyName, onClose }: Props) {
  const { token } = theme.useToken();
  const { t } = useTranslation(['monitor', 'common']);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<RedisKeyVO | null>(null);

  useEffect(() => {
    if (!open || !keyName) return;
    setLoading(true);
    redisMonitorApi
      .getKeyValue(keyName)
      .then((res) => setData(res.data))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [open, keyName]);

  return (
    <Modal
      title={t('key.detailTitle')}
      open={open}
      onCancel={onClose}
      onOk={onClose}
      width={680}
      cancelButtonProps={{ style: { display: 'none' } }}
    >
      {loading || !data ? (
        <Skeleton active paragraph={{ rows: 4 }} />
      ) : (
        <>
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="Key">
              <span style={{ wordBreak: 'break-all' }}>{data.key}</span>
            </Descriptions.Item>
            <Descriptions.Item label={t('common:type')}>
              <Tag color="blue">{data.type}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="TTL">{formatTtl(data.ttl, t)}</Descriptions.Item>
          </Descriptions>
          <div style={{ marginTop: 12, fontWeight: 500 }}>{t('key.value')}</div>
          <pre
            style={{
              marginTop: 8,
              padding: 12,
              background: token.colorFillTertiary,
              borderRadius: 4,
              maxHeight: 320,
              overflow: 'auto',
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
            }}
          >
            {data.value ?? t('key.emptyValue')}
          </pre>
        </>
      )}
    </Modal>
  );
}
