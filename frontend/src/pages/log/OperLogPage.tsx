import { useState } from 'react';
import { Button, Space, Tag, message, Drawer, Descriptions, Modal } from 'antd';
import { DownloadOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import { ProTable, RowActions } from '../../components/pro';
import UserTableSelect from '../../components/common/UserTableSelect';
import MenuTableSelect from '../../components/common/MenuTableSelect';
import { logApi } from '../../services/logApi';
import type { OperLogListVO, OperLogDetailVO } from '../../services/logApi';

export default function OperLogPage() {
  const { t } = useTranslation(['log', 'common']);
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
      title: t('oper.clean.title'),
      content: t('oper.clean.content'),
      okType: 'danger',
      onOk: async () => {
        await logApi.cleanOperLogs({ beforeDays: 30 });
        message.success(t('msg.cleaned'));
        refresh();
      },
    });
  };

  const handleExport = async () => {
    try {
      await logApi.exportOperLogs(exportFilters);
      message.success(t('common:msg.exportSuccess'));
    } catch {
      message.error(t('msg.exportFailed'));
    }
  };

  const columns: ColumnsType<OperLogListVO> = [
    // 雪花 ID 是 18 位数字，width 100 装不下必然折行
    { title: t('oper.table.id'), dataIndex: 'id', key: 'id', width: 190 },
    { title: t('oper.table.operator'), dataIndex: 'operator', key: 'operator', width: 120 },
    /*
     * module / type 列的**值**来自 @Log(module="订单", type="INSERT") 注解字面量，
     * 库里存的就是中文。这一版 @Log 不做 i18n（见设计 §9.4），所以值仍是中文，
     * 只有表头翻译。下个版本给注解加 key 后这里一并改成 t()。
     */
    { title: t('oper.table.module'), dataIndex: 'module', key: 'module', width: 120 },
    { title: t('oper.table.type'), dataIndex: 'type', key: 'type', width: 100 },
    { title: t('common:ip'), dataIndex: 'operatorIp', key: 'operatorIp', width: 150 },
    {
      title: t('oper.table.result'), dataIndex: 'status', key: 'status', width: 100,
      render: (s: number) => (
        <Tag color={s === 1 ? 'success' : 'error'}>{s === 1 ? t('result.success') : t('result.fail')}</Tag>
      ),
    },
    {
      title: t('oper.table.cost'), dataIndex: 'costTime', key: 'costTime', width: 100, align: 'right',
      render: (v: number) => v ?? '—',
    },
    { title: t('oper.table.time'), dataIndex: 'operateTime', key: 'operateTime', width: 180 },
    {
      title: t('table.action'), key: 'action', width: 70, fixed: 'right',
      render: (_: unknown, r: OperLogListVO) => (
        <RowActions items={[
          { key: 'view', label: t('common:detail'), icon: <EyeOutlined />, onClick: () => handleViewDetail(r.id) },
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
        scroll={{ x: 1300 }}
        querySchema={[
          { name: 'operator', label: t('oper.table.operator'), type: 'node', node: <UserTableSelect /> },
          { name: 'module', label: t('oper.query.module'), type: 'node', node: <MenuTableSelect /> },
          {
            name: 'status', label: t('oper.query.result'), type: 'select',
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
            <Button danger icon={<DeleteOutlined />} onClick={handleClean}>{t('oper.action.clean')}</Button>
          </Space>
        }
      />
      <Drawer
        title={t('oper.detail.title')}
        open={detailOpen}
        onClose={() => { setDetailOpen(false); setDetail(null); }}
        width={560}
        loading={detailLoading}
      >
        {detail && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label={t('oper.detail.module')}>{detail.module}</Descriptions.Item>
            <Descriptions.Item label={t('common:type')}>{detail.type}</Descriptions.Item>
            <Descriptions.Item label={t('oper.detail.desc')}>{detail.title}</Descriptions.Item>
            <Descriptions.Item label={t('oper.detail.operator')}>{detail.operator}</Descriptions.Item>
            <Descriptions.Item label={t('common:ip')}>{detail.operatorIp}</Descriptions.Item>
            <Descriptions.Item label={t('oper.detail.method')}>{detail.method}</Descriptions.Item>
            <Descriptions.Item label={t('oper.detail.url')}>{detail.requestUrl}</Descriptions.Item>
            <Descriptions.Item label={t('oper.detail.params')}><pre style={{ maxHeight: 200, overflow: 'auto', fontSize: 12 }}>{detail.requestParams}</pre></Descriptions.Item>
            {detail.responseResult && <Descriptions.Item label={t('oper.detail.response')}><pre style={{ maxHeight: 200, overflow: 'auto', fontSize: 12 }}>{detail.responseResult}</pre></Descriptions.Item>}
            <Descriptions.Item label={t('common:status')}><Tag color={detail.status === 1 ? 'success' : 'error'}>{detail.status === 1 ? t('result.success') : t('result.fail')}</Tag></Descriptions.Item>
            {detail.errorMsg && <Descriptions.Item label={t('oper.detail.errorMsg')}><pre style={{ color: 'red', fontSize: 12 }}>{detail.errorMsg}</pre></Descriptions.Item>}
            <Descriptions.Item label={t('oper.detail.cost')}>{detail.costTime}ms</Descriptions.Item>
            <Descriptions.Item label={t('oper.table.time')}>{detail.operateTime}</Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </>
  );
}
