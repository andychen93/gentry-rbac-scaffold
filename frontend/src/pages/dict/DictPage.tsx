import { useState } from 'react';
import { Card, Table, Button, Space, Tag, message } from 'antd';
import {
  PlusOutlined, ArrowLeftOutlined, ReloadOutlined,
  EditOutlined, DeleteOutlined, DatabaseOutlined,
} from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import type { ColumnsType } from 'antd/es/table';
import { ProTable, RowActions } from '../../components/pro';
import { dictApi, DictTypeListVO, DictDataVO } from '../../services/dictApi';
import TypeFormModal from './TypeFormModal';
import DataFormModal from './DataFormModal';

export default function DictPage() {
  const qc = useQueryClient();
  const [activeDict, setActiveDict] = useState<DictTypeListVO | null>(null);

  // Data list state（字典数据项子页保留手写 Table，不分页）
  const [dataList, setDataList] = useState<DictDataVO[]>([]);
  const [dataLoading, setDataLoading] = useState(false);

  // Modal state
  const [typeModalOpen, setTypeModalOpen] = useState(false);
  const [typeModalMode, setTypeModalMode] = useState<'create' | 'edit'>('create');
  const [editingType, setEditingType] = useState<DictTypeListVO | null>(null);
  const [dataModalOpen, setDataModalOpen] = useState(false);
  const [dataModalMode, setDataModalMode] = useState<'create' | 'edit'>('create');
  const [editingData, setEditingData] = useState<DictDataVO | null>(null);

  const refreshTypes = () => qc.invalidateQueries({ queryKey: ['dictTypes'] });

  const fetchData = async (dictType: string) => {
    setDataLoading(true);
    try {
      const res = await dictApi.listData(dictType);
      setDataList(res.data ?? []);
    } catch { /* handled by interceptor */ } finally {
      setDataLoading(false);
    }
  };

  const handleEnterData = (record: DictTypeListVO) => {
    setActiveDict(record);
    fetchData(record.dictType);
  };

  const handleBackToList = () => {
    setActiveDict(null);
    refreshTypes();
  };

  const handleDeleteType = async (id: number) => {
    await dictApi.removeType(id);
    message.success('删除成功');
    refreshTypes();
  };

  const handleDeleteData = async (id: number) => {
    await dictApi.removeData(id);
    message.success('删除成功');
    if (activeDict) fetchData(activeDict.dictType);
  };

  const handleRefreshCache = async () => {
    await dictApi.refreshCache();
    message.success('缓存刷新成功');
  };

  const typeColumns: ColumnsType<DictTypeListVO> = [
    { title: '字典名称', dataIndex: 'dictName', key: 'dictName', width: 160 },
    { title: '字典类型', dataIndex: 'dictType', key: 'dictType', width: 200 },
    { title: '数据项数', dataIndex: 'dataCount', key: 'dataCount', width: 100, align: 'center' },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80, align: 'center',
      render: (s: number) => <Tag color={s === 1 ? 'success' : 'error'}>{s === 1 ? '正常' : '停用'}</Tag>,
    },
    { title: '备注', dataIndex: 'remark', key: 'remark', width: 200, ellipsis: true },
    {
      title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180,
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: '操作', key: 'action', width: 110, fixed: 'right',
      render: (_: unknown, record: DictTypeListVO) => (
        <RowActions items={[
          { key: 'data', label: '数据', icon: <DatabaseOutlined />, onClick: () => handleEnterData(record) },
          {
            key: 'edit', label: '编辑', icon: <EditOutlined />,
            onClick: () => { setEditingType(record); setTypeModalMode('edit'); setTypeModalOpen(true); },
          },
          {
            key: 'del', label: '删除', icon: <DeleteOutlined />, danger: true,
            confirmText: '确定删除该字典类型？',
            onClick: () => handleDeleteType(record.id),
          },
        ]} />
      ),
    },
  ];

  const dataColumns: ColumnsType<DictDataVO> = [
    { title: '字典标签', dataIndex: 'dictLabel', key: 'dictLabel', width: 120 },
    { title: '字典键值', dataIndex: 'dictValue', key: 'dictValue', width: 120 },
    {
      title: '样式', dataIndex: 'cssClass', key: 'cssClass', width: 100,
      render: (v: string) => v ? <Tag color={{ primary: 'blue', success: 'green', warning: 'orange', danger: 'red' }[v] || 'default'}>{v}</Tag> : '-',
    },
    {
      title: '是否默认', dataIndex: 'isDefault', key: 'isDefault', width: 100, align: 'center',
      render: (v: number) => <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '是' : '否'}</Tag>,
    },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 80, align: 'center' },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80, align: 'center',
      render: (s: number) => <Tag color={s === 1 ? 'success' : 'error'}>{s === 1 ? '正常' : '停用'}</Tag>,
    },
    { title: '备注', dataIndex: 'remark', key: 'remark', width: 160, ellipsis: true },
    {
      title: '操作', key: 'action', width: 90,
      render: (_: unknown, record: DictDataVO) => (
        <RowActions items={[
          {
            key: 'edit', label: '编辑', icon: <EditOutlined />,
            onClick: () => { setEditingData(record); setDataModalMode('edit'); setDataModalOpen(true); },
          },
          {
            key: 'del', label: '删除', icon: <DeleteOutlined />, danger: true,
            confirmText: '确定删除该数据项？',
            onClick: () => handleDeleteData(record.id),
          },
        ]} />
      ),
    },
  ];

  // ============ 字典数据项子页（保留手写 Card+Table，不分页） ============
  if (activeDict) {
    return (
      <>
        <Card style={{ marginBottom: 16 }}>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={handleBackToList}>返回列表</Button>
            <span>字典类型：<strong>{activeDict.dictName}</strong>（{activeDict.dictType}）</span>
          </Space>
        </Card>
        <Card>
          <div style={{ marginBottom: 16 }}>
            <Space>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => { setEditingData(null); setDataModalMode('create'); setDataModalOpen(true); }}
              >
                新增数据项
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleRefreshCache}>刷新缓存</Button>
            </Space>
          </div>
          <Table<DictDataVO>
            rowKey="id"
            columns={dataColumns}
            dataSource={dataList}
            loading={dataLoading}
            pagination={false}
          />
        </Card>
        <DataFormModal
          open={dataModalOpen}
          mode={dataModalMode}
          dictType={activeDict.dictType}
          record={editingData}
          onSuccess={() => { setDataModalOpen(false); fetchData(activeDict.dictType); }}
          onCancel={() => setDataModalOpen(false)}
        />
      </>
    );
  }

  // ============ 字典类型主列表（ProTable） ============
  return (
    <>
      <ProTable<DictTypeListVO>
        service={dictApi.listTypes}
        queryKey={['dictTypes']}
        columns={typeColumns}
        rowKey="id"
        scroll={{ x: 1000 }}
        querySchema={[
          { name: 'dictName', label: '字典名称' },
          { name: 'dictType', label: '字典类型' },
          {
            name: 'status', label: '状态', type: 'select',
            options: [{ label: '正常', value: 1 }, { label: '停用', value: 0 }],
          },
        ]}
        toolbar={
          <Space>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => { setEditingType(null); setTypeModalMode('create'); setTypeModalOpen(true); }}
            >
              新增类型
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleRefreshCache}>刷新缓存</Button>
          </Space>
        }
      />
      <TypeFormModal
        open={typeModalOpen}
        mode={typeModalMode}
        record={editingType}
        onSuccess={() => { setTypeModalOpen(false); refreshTypes(); }}
        onCancel={() => setTypeModalOpen(false)}
      />
    </>
  );
}
