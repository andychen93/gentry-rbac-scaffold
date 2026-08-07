import { Card, Col, Descriptions, Row, Tag, Tooltip } from 'antd';
import type { RedisInfoVO } from '../../../services/monitorApi';
import { formatBytes, formatTimestamp, formatUptime } from '../../../utils/format';

interface Props {
  info: RedisInfoVO;
  dbSize: number;
}

const MODE_COLOR: Record<string, string> = {
  standalone: 'blue',
  sentinel: 'orange',
  cluster: 'green',
};

export default function RedisInfoCard({ info, dbSize }: Props) {
  return (
    <Card title="服务基本信息" size="small">
      <Descriptions column={{ xs: 1, sm: 2, md: 3 }} size="small">
        <Descriptions.Item label="版本">
          <Tag color="geekblue">{info.redisVersion || '-'}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="运行模式">
          <Tag color={MODE_COLOR[info.redisMode] || 'default'}>
            {info.redisMode || '-'}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="运行时长">
          {formatUptime(info.uptimeInSeconds)}
        </Descriptions.Item>
        <Descriptions.Item label="连接数">
          {info.connectedClients ?? '-'}
        </Descriptions.Item>
        <Descriptions.Item label="AOF">
          <Tag color={info.aofEnabled === '1' ? 'green' : 'red'}>
            {info.aofEnabled === '1' ? '启用' : '未启用'}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="RDB 最近保存">
          {formatTimestamp(info.rdbLastSaveTime)}
        </Descriptions.Item>
        <Descriptions.Item label="订阅频道">
          {info.pubsubChannels ?? 0} / 模式 {info.pubsubPatterns ?? 0}
        </Descriptions.Item>
        <Descriptions.Item label="总连接数">
          {info.totalConnectionsReceived?.toLocaleString() ?? '-'}
        </Descriptions.Item>
        <Descriptions.Item label="总命令数">
          {info.totalCommandsProcessed?.toLocaleString() ?? '-'}
        </Descriptions.Item>
      </Descriptions>

      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col span={6}>
          <Metric label="Key 总数" value={dbSize} />
        </Col>
        <Col span={6}>
          <Metric label="有过期 Key" value={info.expiresKeys ?? '-'} />
        </Col>
        <Col span={6}>
          <Metric label="已用内存" value={formatBytes(info.usedMemory)} />
        </Col>
        <Col span={6}>
          <Metric
            label="最大内存"
            value={info.maxMemory ? formatBytes(info.maxMemory) : '未限制'}
          />
        </Col>
      </Row>

      <Row gutter={16} style={{ marginTop: 8 }}>
        <Col span={6}>
          <Tooltip title="瞬时每秒处理命令数 instantaneous_ops_per_sec">
            <Metric
              label="QPS"
              value={info.instantaneousOpsPerSec?.toLocaleString() ?? '-'}
              accent="#52c41a"
            />
          </Tooltip>
        </Col>
        <Col span={6}>
          <Tooltip title="命中率 = hits / (hits + misses)">
            <Metric
              label="命中率"
              value={info.hitRate != null ? `${info.hitRate.toFixed(2)}%` : '-'}
              accent={getHitRateColor(info.hitRate)}
            />
          </Tooltip>
        </Col>
        <Col span={6}>
          <Metric
            label="命中次数"
            value={info.keyspaceHits?.toLocaleString() ?? '-'}
          />
        </Col>
        <Col span={6}>
          <Metric
            label="未命中次数"
            value={info.keyspaceMisses?.toLocaleString() ?? '-'}
          />
        </Col>
      </Row>
    </Card>
  );
}

function getHitRateColor(hitRate: number | null | undefined): string {
  if (hitRate == null) return '#1677ff';
  if (hitRate >= 90) return '#52c41a';
  if (hitRate >= 70) return '#faad14';
  return '#ff4d4f';
}

function Metric({
  label,
  value,
  accent = '#1677ff',
}: {
  label: string;
  value: React.ReactNode;
  accent?: string;
}) {
  return (
    <div style={{ textAlign: 'center', padding: '4px 0' }}>
      <div style={{ fontSize: 22, fontWeight: 600, color: accent }}>{value}</div>
      <div style={{ color: '#666', fontSize: 12 }}>{label}</div>
    </div>
  );
}
