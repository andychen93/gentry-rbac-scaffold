import { useState } from 'react';
import { Button, Space, Tag, message, Drawer, Descriptions, Modal } from 'antd';
import { DownloadOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import { ProTable, RowActions } from '@gentry/kit';
import UserTableSelect from '../../components/common/UserTableSelect';
import { logApi } from '../../services/logApi';
import type { LoginLogListVO, LoginLogDetailVO } from '../../services/logApi';

export default function LoginLogPage() {
  const { t } = useTranslation(['log', 'common']);
  const qc = useQueryClient();
  const [exportFilters, setExportFilters] = useState<Record<string, unknown>>({});
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<LoginLogDetailVO | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const refresh = () => qc.invalidateQueries({ queryKey: ['login-logs'] });

  const handleViewDetail = async (id: number) => {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const res = await logApi.getLoginLogDetail(id);
      setDetail(res.data);
    } catch { /* handled */ } finally {
      setDetailLoading(false);
    }
  };

  const handleClean = () => {
    Modal.confirm({
      title: t('login.clean.title'),
      content: t('login.clean.content'),
      okType: 'danger',
      onOk: async () => {
        await logApi.cleanLoginLogs({ beforeDays: 30 });
        message.success(t('msg.cleaned'));
        refresh();
      },
    });
  };

  const handleExport = async () => {
    try {
      await logApi.exportLoginLogs(exportFilters);
      message.success(t('common:msg.exportSuccess'));
    } catch {
      message.error(t('msg.exportFailed'));
    }
  };

  const columns: ColumnsType<LoginLogListVO> = [
    { title: t('common:username'), dataIndex: 'username', key: 'username', width: 110 },
    {
      title: t('login.table.loginType'), dataIndex: 'loginType', key: 'loginType', width: 100,
      render: (v: string) => (
        <Tag color={v === 'password' ? 'blue' : 'green'}>
          {v === 'password' ? t('login.type.password') : v}
        </Tag>
      ),
    },
    { title: t('common:loginIp'), dataIndex: 'loginIp', key: 'loginIp', width: 130 },
    { title: t('common:location'), dataIndex: 'location', key: 'location', width: 140 },
    { title: t('common:browser'), dataIndex: 'browser', key: 'browser', width: 110 },
    { title: t('common:os'), dataIndex: 'os', key: 'os', width: 110 },
    {
      title: t('login.table.result'), dataIndex: 'status', key: 'status', width: 90, align: 'center',
      render: (s: number) => (
        <Tag color={s === 1 ? 'success' : 'error'}>{s === 1 ? t('result.success') : t('result.fail')}</Tag>
      ),
    },
    /*
     * message 列的**值**是后端写日志时落库的提示语（如「登录成功」），
     * 属于历史数据，不随界面语言变。后端已改成按请求 locale 落库当时的语言，
     * 换语言看旧记录仍是当时那句 —— 这是日志的正确语义（记录事实，不重写历史）。
     */
    { title: t('login.table.message'), dataIndex: 'message', key: 'message', width: 150 },
    { title: t('common:loginTime'), dataIndex: 'loginTime', key: 'loginTime', width: 170 },
    {
      title: t('table.action'), key: 'action', width: 70, fixed: 'right',
      render: (_: unknown, r: LoginLogListVO) => (
        <RowActions items={[
          { key: 'view', label: t('common:detail'), icon: <EyeOutlined />, onClick: () => handleViewDetail(r.id) },
        ]} />
      ),
    },
  ];

  return (
    <>
      <ProTable<LoginLogListVO>
        service={logApi.listLoginLogs}
        queryKey={['login-logs']}
        columns={columns}
        rowKey="id"
        scroll={{ x: 1200 }}
        querySchema={[
          { name: 'username', label: t('common:username'), type: 'node', node: <UserTableSelect /> },
          {
            name: 'status', label: t('common:status'), type: 'select',
            options: [
              { label: t('result.success'), value: 1 },
              { label: t('result.fail'), value: 0 },
            ],
          },
        ]}
        onFiltersChange={setExportFilters}
        toolbar={
          <Space>
            <Button icon={<DownloadOutlined />} onClick={handleExport}>{t('common:export')}</Button>
            <Button danger onClick={handleClean}>{t('login.action.clean')}</Button>
            <Button icon={<ReloadOutlined />} onClick={() => refresh()}>{t('common:refresh')}</Button>
          </Space>
        }
      />
      <Drawer
        title={t('login.detail.title')}
        open={detailOpen}
        onClose={() => { setDetailOpen(false); setDetail(null); }}
        width={560}
        loading={detailLoading}
      >
        {detail && (
          <>
            <Descriptions title={t('login.detail.section.login')} column={1} bordered size="small">
              <Descriptions.Item label={t('common:username')}>{detail.username}</Descriptions.Item>
              <Descriptions.Item label={t('login.table.loginType')}>{detail.loginType === 'password' ? t('login.type.password') : detail.loginType}</Descriptions.Item>
              <Descriptions.Item label={t('common:loginIp')}>{detail.loginIp}</Descriptions.Item>
              <Descriptions.Item label={t('common:location')}>{detail.location || '-'}</Descriptions.Item>
              <Descriptions.Item label={t('common:browser')}>{detail.browser}</Descriptions.Item>
              <Descriptions.Item label={t('common:os')}>{detail.os}</Descriptions.Item>
              <Descriptions.Item label={t('login.table.result')}><Tag color={detail.status === 1 ? 'success' : 'error'}>{detail.status === 1 ? t('result.success') : t('result.fail')}</Tag></Descriptions.Item>
              <Descriptions.Item label={t('login.table.message')}>{detail.message}</Descriptions.Item>
              <Descriptions.Item label={t('common:loginTime')}>{detail.loginTime}</Descriptions.Item>
            </Descriptions>
            <Descriptions title={t('login.detail.section.device')} column={1} bordered size="small" style={{ marginTop: 16 }}>
              <Descriptions.Item label={t('login.detail.deviceType')}>{detail.deviceType || '-'}</Descriptions.Item>
              <Descriptions.Item label="User-Agent"><code style={{ fontSize: 12, wordBreak: 'break-all' }}>{detail.userAgent || '-'}</code></Descriptions.Item>
            </Descriptions>
          </>
        )}
      </Drawer>
    </>
  );
}
