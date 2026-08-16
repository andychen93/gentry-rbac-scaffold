import { Card, Col, Descriptions, Row, Tag, Tooltip, theme } from 'antd';
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
              accent="success"
            />
          </Tooltip>
        </Col>
        <Col span={6}>
          <Tooltip title="命中率 = hits / (hits + misses)">
            <Metric
              label="命中率"
              value={info.hitRate != null ? `${info.hitRate.toFixed(2)}%` : '-'}
              accent={getHitRateAccent(info.hitRate)}
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

/**
 * 指标强调色档位。
 *
 * 只表达「语义」不表达「颜色」—— 具体色值由 Metric 从 antd token 取，
 * token 在 theme/argonTheme.ts 里指向 argonColors，所以改配色仍然只动一处。
 */
type Accent = 'primary' | 'success' | 'warning' | 'danger';

/** 命中率分档：≥90% 健康、≥70% 需关注、其余告警；无数据时不着色（走主色） */
function getHitRateAccent(hitRate: number | null | undefined): Accent {
  if (hitRate == null) return 'primary';
  if (hitRate >= 90) return 'success';
  if (hitRate >= 70) return 'warning';
  return 'danger';
}

function Metric({
  label,
  value,
  accent = 'primary',
}: {
  label: string;
  value: React.ReactNode;
  accent?: Accent;
}) {
  const { token } = theme.useToken();
  // 语义档位 → antd 语义 token（ConfigProvider 里已被 argonTheme 覆盖成 Argon 色板）
  const accentColor: Record<Accent, string> = {
    primary: token.colorPrimary,
    success: token.colorSuccess,
    warning: token.colorWarning,
    danger: token.colorError,
  };

  return (
    <div style={{ textAlign: 'center', padding: '4px 0' }}>
      <div style={{ fontSize: 22, fontWeight: 600, color: accentColor[accent] }}>{value}</div>
      <div style={{ color: token.colorTextSecondary, fontSize: 12 }}>{label}</div>
    </div>
  );
}
