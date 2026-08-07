import { useState } from 'react';
import { Button, Space, Tag, message, Drawer, Descriptions, Modal } from 'antd';
import { DownloadOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import type { ColumnsType } from 'antd/es/table';
import { ProTable, RowActions } from '../../components/pro';
import { logApi } from '../../services/logApi';
import type { OperLogListVO, OperLogDetailVO } from '../../services/logApi';

export default function OperLogPage() {
  const qc = useQueryClient();
  const [exportFilters, setExportFilters] = useState<Record<string, unknown>>({});
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<OperLogDetailVO | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const refresh = () => qc.invalidateQueries({ queryKey: ['oper-logs'] });

  const handleViewDetail = async (id: number) => {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const res = await logApi.getOperLogDetail(id);
      setDetail(res.data);
    } catch { /* handled */ } finally {
      setDetailLoading(false);
    }
  };

  const handleClean = () => {
    Modal.confirm({
      title: '清空操作日志',
      content: '此操作将清空30天前的日志数据且无法恢复，确定继续？',
      okType: 'danger',
      onOk: async () => {
        await logApi.cleanOperLogs({ beforeDays: 30 });
        message.success('清理成功');
        refresh();
      },
    });
  };

  const handleExport = async () => {
    try {
      await logApi.exportOperLogs(exportFilters);
      message.success('导出成功');
    } catch {
      message.error('导出失败，请稍后重试');
    }
  };

  const columns: ColumnsType<OperLogListVO> = [
    { title: '日志编号', dataIndex: 'id', key: 'id', width: 100 },
    { title: '操作用户', dataIndex: 'operator', key: 'operator', width: 120 },
    { title: '操作模块', dataIndex: 'module', key: 'module', width: 120 },
    { title: '动作', dataIndex: 'type', key: 'type', width: 100 },
    { title: 'IP地址', dataIndex: 'operatorIp', key: 'operatorIp', width: 150 },
    {
      title: '执行结果', dataIndex: 'status', key: 'status', width: 100,
      render: (s: number) => <Tag color={s === 1 ? 'success' : 'error'}>{s === 1 ? '成功' : '失败'}</Tag>,
    },
    {
      title: '耗时(ms)', dataIndex: 'costTime', key: 'costTime', width: 100, align: 'right',
      render: (v: number) => v ?? '—',
    },
    { title: '操作时间', dataIndex: 'operateTime', key: 'operateTime', width: 180 },
    {
      title: '操作', key: 'action', width: 100, fixed: 'right',
      render: (_: unknown, r: OperLogListVO) => (
        <RowActions items={[
          { key: 'view', label: '详情', icon: <EyeOutlined />, onClick: () => handleViewDetail(r.id) },
        ]} />
      ),
    },
  ];

  return (
    <>
      <ProTable<OperLogListVO>
        service={logApi.listOperLogs}
        queryKey={['oper-logs']}
        columns={columns}
        rowKey="id"
        scroll={{ x: 1100 }}
        querySchema={[
          { name: 'operator', label: '操作用户' },
          { name: 'module', label: '模块' },
          {
            name: 'status', label: '结果', type: 'select',
            options: [{ label: '成功', value: 1 }, { label: '失败', value: 0 }],
          },
        ]}
        onFiltersChange={setExportFilters}
        toolbar={
          <Space>
            <Button icon={<DownloadOutlined />} onClick={handleExport}>导出</Button>
            <Button danger icon={<DeleteOutlined />} onClick={handleClean}>清空</Button>
          </Space>
        }
      />
      <Drawer
        title="操作日志详情"
        open={detailOpen}
        onClose={() => { setDetailOpen(false); setDetail(null); }}
        width={560}
        loading={detailLoading}
      >
        {detail && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="模块">{detail.module}</Descriptions.Item>
            <Descriptions.Item label="类型">{detail.type}</Descriptions.Item>
            <Descriptions.Item label="描述">{detail.title}</Descriptions.Item>
            <Descriptions.Item label="操作人">{detail.operator}</Descriptions.Item>
            <Descriptions.Item label="IP">{detail.operatorIp}</Descriptions.Item>
            <Descriptions.Item label="请求方法">{detail.method}</Descriptions.Item>
            <Descriptions.Item label="请求URL">{detail.requestUrl}</Descriptions.Item>
            <Descriptions.Item label="请求参数"><pre style={{ maxHeight: 200, overflow: 'auto', fontSize: 12 }}>{detail.requestParams}</pre></Descriptions.Item>
            {detail.responseResult && <Descriptions.Item label="响应结果"><pre style={{ maxHeight: 200, overflow: 'auto', fontSize: 12 }}>{detail.responseResult}</pre></Descriptions.Item>}
            <Descriptions.Item label="状态"><Tag color={detail.status === 1 ? 'success' : 'error'}>{detail.status === 1 ? '成功' : '失败'}</Tag></Descriptions.Item>
            {detail.errorMsg && <Descriptions.Item label="错误信息"><pre style={{ color: 'red', fontSize: 12 }}>{detail.errorMsg}</pre></Descriptions.Item>}
            <Descriptions.Item label="耗时">{detail.costTime}ms</Descriptions.Item>
            <Descriptions.Item label="操作时间">{detail.operateTime}</Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </>
  );
}
