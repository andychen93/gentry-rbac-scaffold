import { useState } from 'react';
import { Button, Space, Tag, message, Drawer, Descriptions, Modal } from 'antd';
import { DownloadOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import type { ColumnsType } from 'antd/es/table';
import { ProTable, RowActions } from '../../components/pro';
import UserTableSelect from '../../components/common/UserTableSelect';
import { logApi } from '../../services/logApi';
import type { LoginLogListVO, LoginLogDetailVO } from '../../services/logApi';

export default function LoginLogPage() {
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
      title: '清理登录日志',
      content: '此操作将清理30天前的登录日志且无法恢复，确定继续？',
      okType: 'danger',
      onOk: async () => {
        await logApi.cleanLoginLogs({ beforeDays: 30 });
        message.success('清理成功');
        refresh();
      },
    });
  };

  const handleExport = async () => {
    try {
      await logApi.exportLoginLogs(exportFilters);
      message.success('导出成功');
    } catch {
      message.error('导出失败，请稍后重试');
    }
  };

  const columns: ColumnsType<LoginLogListVO> = [
    { title: '用户名', dataIndex: 'username', key: 'username', width: 110 },
    {
      title: '登录方式', dataIndex: 'loginType', key: 'loginType', width: 100,
      render: (v: string) => <Tag color={v === 'password' ? 'blue' : 'green'}>{v === 'password' ? '密码' : v}</Tag>,
    },
    { title: '登录IP', dataIndex: 'loginIp', key: 'loginIp', width: 130 },
    { title: '登录地点', dataIndex: 'location', key: 'location', width: 140 },
    { title: '浏览器', dataIndex: 'browser', key: 'browser', width: 110 },
    { title: '操作系统', dataIndex: 'os', key: 'os', width: 110 },
    {
      title: '登录状态', dataIndex: 'status', key: 'status', width: 90, align: 'center',
      render: (s: number) => <Tag color={s === 1 ? 'success' : 'error'}>{s === 1 ? '成功' : '失败'}</Tag>,
    },
    { title: '提示消息', dataIndex: 'message', key: 'message', width: 150 },
    { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', width: 170 },
    {
      title: '操作', key: 'action', width: 70, fixed: 'right',
      render: (_: unknown, r: LoginLogListVO) => (
        <RowActions items={[
          { key: 'view', label: '详情', icon: <EyeOutlined />, onClick: () => handleViewDetail(r.id) },
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
          { name: 'username', label: '用户名', type: 'node', node: <UserTableSelect /> },
          {
            name: 'status', label: '状态', type: 'select',
            options: [{ label: '成功', value: 1 }, { label: '失败', value: 0 }],
          },
        ]}
        onFiltersChange={setExportFilters}
        toolbar={
          <Space>
            <Button icon={<DownloadOutlined />} onClick={handleExport}>导出</Button>
            <Button danger onClick={handleClean}>清理日志</Button>
            <Button icon={<ReloadOutlined />} onClick={() => refresh()}>刷新</Button>
          </Space>
        }
      />
      <Drawer
        title="登录日志详情"
        open={detailOpen}
        onClose={() => { setDetailOpen(false); setDetail(null); }}
        width={560}
        loading={detailLoading}
      >
        {detail && (
          <>
            <Descriptions title="登录信息" column={1} bordered size="small">
              <Descriptions.Item label="用户名">{detail.username}</Descriptions.Item>
              <Descriptions.Item label="登录方式">{detail.loginType === 'password' ? '密码' : detail.loginType}</Descriptions.Item>
              <Descriptions.Item label="登录IP">{detail.loginIp}</Descriptions.Item>
              <Descriptions.Item label="登录地点">{detail.location || '-'}</Descriptions.Item>
              <Descriptions.Item label="浏览器">{detail.browser}</Descriptions.Item>
              <Descriptions.Item label="操作系统">{detail.os}</Descriptions.Item>
              <Descriptions.Item label="登录状态"><Tag color={detail.status === 1 ? 'success' : 'error'}>{detail.status === 1 ? '成功' : '失败'}</Tag></Descriptions.Item>
              <Descriptions.Item label="提示消息">{detail.message}</Descriptions.Item>
              <Descriptions.Item label="登录时间">{detail.loginTime}</Descriptions.Item>
            </Descriptions>
            <Descriptions title="设备信息" column={1} bordered size="small" style={{ marginTop: 16 }}>
              <Descriptions.Item label="设备类型">{detail.deviceType || '-'}</Descriptions.Item>
              <Descriptions.Item label="User-Agent"><code style={{ fontSize: 12, wordBreak: 'break-all' }}>{detail.userAgent || '-'}</code></Descriptions.Item>
            </Descriptions>
          </>
        )}
      </Drawer>
    </>
  );
}
