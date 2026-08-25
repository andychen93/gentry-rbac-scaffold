import { ReloadOutlined, SearchOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
  Col,
  Input,
  message,
  Modal,
  Row,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import {
  redisMonitorApi,
  type RedisKeyDefineVO,
  type RedisKeyVO,
  type RedisMonitorVO,
} from '../../services/monitorApi';
import { useUserStore } from '../../stores/userStore';
import { RowActions } from '../../components/pro';
import { formatTtl } from '../../utils/format';
import CommandStatsChart from './components/CommandStatsChart';
import KeyDefineTable from './components/KeyDefineTable';
import KeyValueModal from './components/KeyValueModal';
import MemoryGaugeChart from './components/MemoryGaugeChart';
import RedisInfoCard from './components/RedisInfoCard';
import SlowLogTable from './components/SlowLogTable';

const DEFAULT_PATTERN = 'Authorization:*';
const AUTO_REFRESH_INTERVAL_MS = 30_000;

export default function RedisMonitorPage() {
  const { t } = useTranslation(['monitor', 'common', 'nav']);
  const hasPermission = useUserStore((s) => s.hasPermission);
  const canList = hasPermission('monitor:redis:key:list');
  const canQuery = hasPermission('monitor:redis:key:query');
  const canDelete = hasPermission('monitor:redis:key:delete');

  // 监控信息
  const [monitor, setMonitor] = useState<RedisMonitorVO | null>(null);
  const [defines, setDefines] = useState<RedisKeyDefineVO[]>([]);
  const [loadingInfo, setLoadingInfo] = useState(false);

  // 自动刷新（监控信息 Tab）
  const [autoRefresh, setAutoRefresh] = useState(false);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Key 管理
  const [activeTab, setActiveTab] = useState<'monitor' | 'keys' | 'slowlog'>('monitor');
  const [pattern, setPattern] = useState(DEFAULT_PATTERN);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [keyList, setKeyList] = useState<RedisKeyVO[]>([]);
  const [keyTotal, setKeyTotal] = useState(0);
  const [loadingKeys, setLoadingKeys] = useState(false);

  // 弹窗
  const [modalOpen, setModalOpen] = useState(false);
  const [modalKey, setModalKey] = useState('');

  const fetchMonitor = useCallback(async () => {
    setLoadingInfo(true);
    try {
      const [info, kd] = await Promise.all([
        redisMonitorApi.getInfo(),
        redisMonitorApi.getKeyDefines(),
      ]);
      setMonitor(info.data);
      setDefines(kd.data);
    } finally {
      setLoadingInfo(false);
    }
  }, []);

  const fetchKeys = useCallback(
    async (nextPattern: string, nextPage: number, nextSize: number) => {
      if (!canList) return;
      if (!nextPattern || !nextPattern.trim()) {
        message.warning(t('key.patternRequired'));
        return;
      }
      setLoadingKeys(true);
      try {
        const res = await redisMonitorApi.scanKeys({
          pattern: nextPattern.trim(),
          pageNum: nextPage,
          pageSize: nextSize,
        });
        setKeyList(res.data.list);
        setKeyTotal(res.data.total);
      } finally {
        setLoadingKeys(false);
      }
    },
    [canList]
  );

  useEffect(() => {
    fetchMonitor();
  }, [fetchMonitor]);

  // 自动刷新：仅在监控信息 Tab 且开关开启时生效
  useEffect(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    if (autoRefresh && activeTab === 'monitor') {
      timerRef.current = setInterval(() => {
        fetchMonitor();
      }, AUTO_REFRESH_INTERVAL_MS);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [autoRefresh, activeTab, fetchMonitor]);

  const handleSearch = () => {
    setPageNum(1);
    fetchKeys(pattern, 1, pageSize);
  };

  const handleQuickSearch = (template: string) => {
    setPattern(template);
    setActiveTab('keys');
    setPageNum(1);
    fetchKeys(template, 1, pageSize);
  };

  const handleView = (record: RedisKeyVO) => {
    setModalKey(record.key);
    setModalOpen(true);
  };

  const handleDelete = (record: RedisKeyVO) => {
    Modal.confirm({
      title: t('key.deleteTitle'),
      content: (
        <span style={{ wordBreak: 'break-all' }}>
          <Trans
            i18nKey="key.deleteConfirm"
            ns="monitor"
            values={{ key: record.key }}
            components={{ b: <b /> }}
          />
        </span>
      ),
      okType: 'danger',
      onOk: async () => {
        try {
          await redisMonitorApi.deleteKey(record.key);
          message.success(t('common:msg.deleteSuccess'));
          fetchKeys(pattern, pageNum, pageSize);
        } catch {
          // 错误已由 request 拦截器统一提示
        }
      },
    });
  };

  const keyColumns: ColumnsType<RedisKeyVO> = [
    {
      title: 'Key',
      dataIndex: 'key',
      ellipsis: true,
      render: (v: string) => <span style={{ fontFamily: 'monospace' }}>{v}</span>,
    },
    {
      title: t('common:type'),
      dataIndex: 'type',
      width: 100,
      render: (v: string) => <Tag color="blue">{v}</Tag>,
    },
    {
      title: 'TTL',
      dataIndex: 'ttl',
      width: 140,
      render: (v: number) => formatTtl(v, t),
    },
    {
      title: t('table.action'),
      key: 'action',
      width: 90,
      render: (_: unknown, record: RedisKeyVO) => (
        <RowActions items={[
          ...(canQuery ? [{
            key: 'view', label: t('common:view'), icon: <EyeOutlined />,
            onClick: () => handleView(record),
          }] : []),
          // 只传 danger（红色样式）不传 confirmText：handleDelete 内部已有 Modal.confirm，
          // 再包一层 Popconfirm 会让用户确认两次
          ...(canDelete ? [{
            key: 'del', label: t('common:delete'), icon: <DeleteOutlined />, danger: true,
            onClick: () => handleDelete(record),
          }] : []),
        ]} />
      ),
    },
  ];

  const monitorPanel = (
    <>
      <Row gutter={16}>
        <Col xs={24} xl={14}>
          {monitor?.info && (
            <RedisInfoCard info={monitor.info} dbSize={monitor.dbSize ?? 0} />
          )}
        </Col>
        <Col xs={24} xl={10}>
          {monitor?.info && (
            <MemoryGaugeChart
              usedMemory={monitor.info.usedMemory ?? 0}
              maxMemory={monitor.info.maxMemory ?? 0}
              percent={monitor.info.usedMemoryPercent ?? 0}
            />
          )}
        </Col>
      </Row>
      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col xs={24} xl={14}>
          <KeyDefineTable defines={defines} onQuickSearch={handleQuickSearch} />
        </Col>
        <Col xs={24} xl={10}>
          <CommandStatsChart stats={monitor?.commandStats ?? []} />
        </Col>
      </Row>
    </>
  );

  const keysPanel = (
    <>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space>
          <Input
            placeholder={t('key.patternPlaceholder')}
            value={pattern}
            onChange={(e) => setPattern(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 420 }}
            allowClear
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
            disabled={!canList}
          >
            {t('common:search')}
          </Button>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {t('key.scanHint')}
          </Typography.Text>
        </Space>
      </Card>
      <Card size="small">
        <Table<RedisKeyVO>
          rowKey="key"
          size="small"
          columns={keyColumns}
          dataSource={keyList}
          loading={loadingKeys}
          pagination={{
            current: pageNum,
            pageSize,
            total: keyTotal,
            showSizeChanger: true,
            showTotal: (count) => t('common:total', { count }),
            onChange: (p, s) => {
              setPageNum(p);
              setPageSize(s);
              fetchKeys(pattern, p, s);
            },
          }}
        />
      </Card>
    </>
  );

  return (
    <>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 12,
        }}
      >
        {/* 页面标题复用菜单的派生 key，不另建词条 —— 它就是菜单名 */}
        <h2 style={{ margin: 0 }}>{t('menu.monitor.redis.info', { ns: 'nav' })}</h2>
        <Space>
          <Tooltip title={t('autoRefresh.hint')}>
            <Typography.Text type="secondary">{t('autoRefresh')}</Typography.Text>
          </Tooltip>
          <Switch
            checked={autoRefresh}
            onChange={setAutoRefresh}
            checkedChildren={t('common:switch.on')}
            unCheckedChildren={t('common:switch.off')}
          />
          <Button icon={<ReloadOutlined />} loading={loadingInfo} onClick={fetchMonitor}>
            {t('common:refresh')}
          </Button>
        </Space>
      </div>

      <Tabs
        activeKey={activeTab}
        onChange={(k) => setActiveTab(k as 'monitor' | 'keys' | 'slowlog')}
        items={[
          { key: 'monitor', label: t('tab.monitor'), children: monitorPanel },
          { key: 'keys', label: t('tab.keys'), children: keysPanel, disabled: !canList },
          { key: 'slowlog', label: t('tab.slowlog'), children: <SlowLogTable /> },
        ]}
      />

      <KeyValueModal
        open={modalOpen}
        keyName={modalKey}
        onClose={() => setModalOpen(false)}
      />
    </>
  );
}
