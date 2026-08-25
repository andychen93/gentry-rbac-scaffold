import { ReloadOutlined } from '@ant-design/icons';
import { Button, Card, InputNumber, message, Modal, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import { redisMonitorApi, type RedisSlowLogVO } from '../../../services/monitorApi';
import { useUserStore } from '../../../stores/userStore';

export default function SlowLogTable() {
  const { t } = useTranslation(['monitor', 'common']);
  const hasPermission = useUserStore((s) => s.hasPermission);
  const canReset = hasPermission('monitor:redis:slowlog:reset');

  const [limit, setLimit] = useState(20);
  const [data, setData] = useState<RedisSlowLogVO[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchLogs = useCallback(async (n: number) => {
    setLoading(true);
    try {
      const res = await redisMonitorApi.getSlowLog(n);
      setData(res.data ?? []);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchLogs(limit);
    // 不随 limit 变化自动拉（由用户手动刷新）
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleReset = () => {
    Modal.confirm({
      title: t('slowlog.resetTitle'),
      content: t('slowlog.resetConfirm'),
      okType: 'danger',
      onOk: async () => {
        await redisMonitorApi.resetSlowLog();
        message.success(t('slowlog.resetDone'));
        fetchLogs(limit);
      },
    });
  };

  const columns: ColumnsType<RedisSlowLogVO> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    {
      title: t('slowlog.time'),
      dataIndex: 'timestamp',
      width: 180,
      render: (ts: number) => new Date(ts * 1000).toLocaleString(),
    },
    {
      title: t('slowlog.cost'),
      dataIndex: 'durationMicros',
      width: 120,
      sorter: (a, b) => (a.durationMicros ?? 0) - (b.durationMicros ?? 0),
      render: (us: number) => formatDuration(us),
    },
    {
      title: t('slowlog.command'),
      dataIndex: 'args',
      ellipsis: true,
      render: (args: string[]) => (
        <span style={{ fontFamily: 'monospace', fontSize: 12 }}>
          {args?.join(' ')}
        </span>
      ),
    },
    {
      title: t('slowlog.client'),
      dataIndex: 'clientAddress',
      width: 160,
      render: (addr: string, r) => (
        <Space size="small">
          <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{addr || '-'}</span>
          {r.clientName && <Tag>{r.clientName}</Tag>}
        </Space>
      ),
    },
  ];

  return (
    <>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space>
          <span>{t('slowlog.limit')}</span>
          <InputNumber
            value={limit}
            onChange={(v) => setLimit(Number(v) || 20)}
            min={1}
            max={200}
            style={{ width: 100 }}
          />
          <Button
            type="primary"
            icon={<ReloadOutlined />}
            loading={loading}
            onClick={() => fetchLogs(limit)}
          >
            {t('common:refresh')}
          </Button>
          {canReset && (
            <Button danger onClick={handleReset}>
              {t('slowlog.reset')}
            </Button>
          )}
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {/* 整句进语言包，<c> 之间的命令由译文提供（中英文语序不同） */}
            <Trans i18nKey="slowlog.hint" ns="monitor" components={{ c: <code /> }} />
          </Typography.Text>
        </Space>
      </Card>
      <Card size="small">
        <Table<RedisSlowLogVO>
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={data}
          loading={loading}
          pagination={{ showSizeChanger: false, pageSize: 20 }}
        />
      </Card>
    </>
  );
}

function formatDuration(us: number): string {
  if (us == null) return '-';
  if (us < 1000) return `${us} µs`;
  if (us < 1_000_000) return `${(us / 1000).toFixed(2)} ms`;
  return `${(us / 1_000_000).toFixed(2)} s`;
}
