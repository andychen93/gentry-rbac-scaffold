import { Card, Col, Descriptions, Row, Tag, Tooltip, theme } from 'antd';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation('monitor');

  return (
    <Card title={t('info.title')} size="small">
      <Descriptions column={{ xs: 1, sm: 2, md: 3 }} size="small">
        <Descriptions.Item label={t('info.version')}>
          <Tag color="geekblue">{info.redisVersion || '-'}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label={t('info.mode')}>
          {/* redisMode 是 Redis 自己返回的英文枚举（standalone/sentinel/cluster），
              属于外部系统的取值，不翻译 */}
          <Tag color={MODE_COLOR[info.redisMode] || 'default'}>
            {info.redisMode || '-'}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label={t('info.uptime')}>
          {formatUptime(info.uptimeInSeconds, t)}
        </Descriptions.Item>
        <Descriptions.Item label={t('info.clients')}>
          {info.connectedClients ?? '-'}
        </Descriptions.Item>
        <Descriptions.Item label="AOF">
          <Tag color={info.aofEnabled === '1' ? 'green' : 'red'}>
            {info.aofEnabled === '1' ? t('info.aofEnabled') : t('info.aofDisabled')}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label={t('info.rdbLastSave')}>
          {formatTimestamp(info.rdbLastSaveTime)}
        </Descriptions.Item>
        <Descriptions.Item label={t('info.pubsub')}>
          {t('info.pubsubValue', {
            channels: info.pubsubChannels ?? 0,
            patterns: info.pubsubPatterns ?? 0,
          })}
        </Descriptions.Item>
        <Descriptions.Item label={t('info.totalConnections')}>
          {info.totalConnectionsReceived?.toLocaleString() ?? '-'}
        </Descriptions.Item>
        <Descriptions.Item label={t('info.totalCommands')}>
          {info.totalCommandsProcessed?.toLocaleString() ?? '-'}
        </Descriptions.Item>
      </Descriptions>

      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col span={6}>
          <Metric label={t('info.dbSize')} value={dbSize} />
        </Col>
        <Col span={6}>
          <Metric label={t('info.expiresKeys')} value={info.expiresKeys ?? '-'} />
        </Col>
        <Col span={6}>
          <Metric label={t('info.usedMemory')} value={formatBytes(info.usedMemory)} />
        </Col>
        <Col span={6}>
          <Metric
            label={t('info.maxMemory')}
            value={info.maxMemory ? formatBytes(info.maxMemory) : t('info.unlimited')}
          />
        </Col>
      </Row>

      <Row gutter={16} style={{ marginTop: 8 }}>
        <Col span={6}>
          <Tooltip title={t('info.qpsHint')}>
            <Metric
              label="QPS"
              value={info.instantaneousOpsPerSec?.toLocaleString() ?? '-'}
              accent="success"
            />
          </Tooltip>
        </Col>
        <Col span={6}>
          <Tooltip title={t('info.hitRateHint')}>
            <Metric
              label={t('info.hitRate')}
              value={info.hitRate != null ? `${info.hitRate.toFixed(2)}%` : '-'}
              accent={getHitRateAccent(info.hitRate)}
            />
          </Tooltip>
        </Col>
        <Col span={6}>
          <Metric
            label={t('info.hits')}
            value={info.keyspaceHits?.toLocaleString() ?? '-'}
          />
        </Col>
        <Col span={6}>
          <Metric
            label={t('info.misses')}
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
 * token 在 @gentry/kit 的 theme/argonTheme.ts 里指向 argonColors，所以改配色仍然只动一处。
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
