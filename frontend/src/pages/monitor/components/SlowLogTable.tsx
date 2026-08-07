import { ReloadOutlined } from '@ant-design/icons';
import { Button, Card, InputNumber, message, Modal, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useState } from 'react';
import { redisMonitorApi, type RedisSlowLogVO } from '../../../services/monitorApi';
import { useUserStore } from '../../../stores/userStore';

export default function SlowLogTable() {
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
      title: '清空慢查询日志',
      content: '此操作会清空 Redis 端所有慢日志，不可恢复。确认继续？',
      okType: 'danger',
      onOk: async () => {
        await redisMonitorApi.resetSlowLog();
        message.success('慢日志已清空');
        fetchLogs(limit);
      },
    });
  };

  const columns: ColumnsType<RedisSlowLogVO> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    {
      title: '时间',
      dataIndex: 'timestamp',
      width: 180,
      render: (ts: number) => new Date(ts * 1000).toLocaleString(),
    },
    {
      title: '耗时',
      dataIndex: 'durationMicros',
      width: 120,
      sorter: (a, b) => (a.durationMicros ?? 0) - (b.durationMicros ?? 0),
      render: (us: number) => formatDuration(us),
    },
    {
      title: '命令',
      dataIndex: 'args',
      ellipsis: true,
      render: (args: string[]) => (
        <span style={{ fontFamily: 'monospace', fontSize: 12 }}>
          {args?.join(' ')}
        </span>
      ),
    },
    {
      title: '客户端',
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
          <span>条数：</span>
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
            刷新
          </Button>
          {canReset && (
            <Button danger onClick={handleReset}>
              清空慢日志
            </Button>
          )}
          <span style={{ color: '#999', fontSize: 12 }}>
            慢日志阈值通过 <code>config set slowlog-log-slower-than &lt;us&gt;</code> 设置，默认 10000 微秒
          </span>
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
