import { useState } from 'react';
import { Card, Table, Button, Space, Tag, message } from 'antd';
import {
  PlusOutlined, ArrowLeftOutlined, ReloadOutlined,
  EditOutlined, DeleteOutlined, DatabaseOutlined,
} from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import { ProTable, RowActions } from '@gentry/kit';
import { dictApi, DictTypeListVO, DictDataVO } from '../../services/dictApi';
import TypeFormModal from './TypeFormModal';
import DataFormModal from './DataFormModal';
import { DICT_TYPES, dictOptions } from '../../locales/dictEnum';
import { makeDictLabel } from '../../locales/navLabel';

export default function DictPage() {
  const { t } = useTranslation(['dictMgmt', 'common', 'dict']);
  // 列表列显示译文（与全站一致）；能改 label 的地方由 DataFormModal 负责锁定
  const dictLabelOf = makeDictLabel(t);
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

  /*
   * 字典改动后必须连带失效 ['dict'] 前缀。
   *
   * DictTag 用 useQuery(['dict', dictType]) 且 staleTime=5min 缓存字典项，
   * 只清后端 Caffeine 是不够的：后端刷新成功了，其它页面上的 DictTag
   * 在 5 分钟内仍显示旧标签，看起来就像「刷新缓存没生效」。
   * 用前缀失效一次性覆盖所有 dictType。
   */
  const invalidateDictConsumers = () => qc.invalidateQueries({ queryKey: ['dict'] });

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
    message.success(t('common:msg.deleteSuccess'));
    refreshTypes();
    invalidateDictConsumers();   // 删类型会连带逻辑删除其数据项
  };

  const handleDeleteData = async (id: number) => {
    await dictApi.removeData(id);
    message.success(t('common:msg.deleteSuccess'));
    if (activeDict) fetchData(activeDict.dictType);
    invalidateDictConsumers();
  };

  const [refreshing, setRefreshing] = useState(false);

  const handleRefreshCache = async () => {
    setRefreshing(true);
    try {
      await dictApi.refreshCache();
      message.success(t('msg.cacheRefreshed'));
      // 后端缓存清完，把前端两层消费方也一起刷新，否则界面看不出任何变化
      invalidateDictConsumers();
      refreshTypes();
      if (activeDict) await fetchData(activeDict.dictType);
    } catch {
      /* 错误提示由 request 拦截器统一弹 */
    } finally {
      setRefreshing(false);
    }
  };

  const typeColumns: ColumnsType<DictTypeListVO> = [
    {
      title: t('table.dictName'), dataIndex: 'dictName', key: 'dictName', width: 160,
      render: (_v: string, r: DictTypeListVO) => dictLabelOf({ dictLabel: r.dictName, i18nKey: r.i18nKey }),
    },
    { title: t('table.dictType'), dataIndex: 'dictType', key: 'dictType', width: 200 },
    { title: t('table.dataCount'), dataIndex: 'dataCount', key: 'dataCount', width: 100, align: 'center' },
    {
      title: t('common:status'), dataIndex: 'status', key: 'status', width: 80, align: 'center',
      render: (s: number) => (
        <Tag color={s === 1 ? 'success' : 'error'}>{t(`dict.${DICT_TYPES.normalDisable}.${s}`, { ns: 'dict' })}</Tag>
      ),
    },
    { title: t('common:remark'), dataIndex: 'remark', key: 'remark', width: 200, ellipsis: true },
    {
      title: t('common:createTime'), dataIndex: 'createTime', key: 'createTime', width: 180,
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: t('table.action'), key: 'action', width: 110, fixed: 'right',
      render: (_: unknown, record: DictTypeListVO) => (
        <RowActions items={[
          { key: 'data', label: t('action.data'), icon: <DatabaseOutlined />, onClick: () => handleEnterData(record) },
          {
            key: 'edit', label: t('common:edit'), icon: <EditOutlined />,
            onClick: () => { setEditingType(record); setTypeModalMode('edit'); setTypeModalOpen(true); },
          },
          {
            key: 'del', label: t('common:delete'), icon: <DeleteOutlined />, danger: true,
            confirmText: t('confirm.deleteType'),
            onClick: () => handleDeleteType(record.id),
          },
        ]} />
      ),
    },
  ];

  const dataColumns: ColumnsType<DictDataVO> = [
    {
      title: t('data.table.label'), dataIndex: 'dictLabel', key: 'dictLabel', width: 120,
      render: (_v: string, r: DictDataVO) => dictLabelOf(r),
    },
    { title: t('data.table.value'), dataIndex: 'dictValue', key: 'dictValue', width: 120 },
    {
      title: t('data.table.cssClass'), dataIndex: 'cssClass', key: 'cssClass', width: 100,
      render: (v: string) => v ? <Tag color={{ primary: 'blue', success: 'green', warning: 'orange', danger: 'red' }[v] || 'default'}>{v}</Tag> : '-',
    },
    {
      title: t('data.table.isDefault'), dataIndex: 'isDefault', key: 'isDefault', width: 100, align: 'center',
      render: (v: number) => (
        <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? t('common:yes') : t('common:no')}</Tag>
      ),
    },
    { title: t('common:sort'), dataIndex: 'sort', key: 'sort', width: 80, align: 'center' },
    {
      title: t('common:status'), dataIndex: 'status', key: 'status', width: 80, align: 'center',
      render: (s: number) => (
        <Tag color={s === 1 ? 'success' : 'error'}>{t(`dict.${DICT_TYPES.normalDisable}.${s}`, { ns: 'dict' })}</Tag>
      ),
    },
    { title: t('common:remark'), dataIndex: 'remark', key: 'remark', width: 160, ellipsis: true },
    {
      title: t('table.action'), key: 'action', width: 90,
      render: (_: unknown, record: DictDataVO) => (
        <RowActions items={[
          {
            key: 'edit', label: t('common:edit'), icon: <EditOutlined />,
            onClick: () => { setEditingData(record); setDataModalMode('edit'); setDataModalOpen(true); },
          },
          {
            key: 'del', label: t('common:delete'), icon: <DeleteOutlined />, danger: true,
            confirmText: t('confirm.deleteData'),
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
            <Button icon={<ArrowLeftOutlined />} onClick={handleBackToList}>{t('action.backToList')}</Button>
            <span>
              {t('header.current')}
              <strong>{dictLabelOf({ dictLabel: activeDict.dictName, i18nKey: activeDict.i18nKey })}</strong>
              {` (${activeDict.dictType})`}
            </span>
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
                {t('action.createData')}
              </Button>
              <Button icon={<ReloadOutlined />} loading={refreshing} onClick={handleRefreshCache}>
                {t('action.refreshCache')}
              </Button>
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
          onSuccess={() => { setDataModalOpen(false); fetchData(activeDict.dictType); invalidateDictConsumers(); }}
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
          { name: 'dictName', label: t('table.dictName') },
          { name: 'dictType', label: t('table.dictType') },
          {
            name: 'status', label: t('common:status'), type: 'select',
            options: dictOptions(t, DICT_TYPES.normalDisable, { numeric: true }),
          },
        ]}
        toolbar={
          <Space>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => { setEditingType(null); setTypeModalMode('create'); setTypeModalOpen(true); }}
            >
              {t('action.createType')}
            </Button>
            <Button icon={<ReloadOutlined />} loading={refreshing} onClick={handleRefreshCache}>
              {t('action.refreshCache')}
            </Button>
          </Space>
        }
      />
      <TypeFormModal
        open={typeModalOpen}
        mode={typeModalMode}
        record={editingType}
        onSuccess={() => { setTypeModalOpen(false); refreshTypes(); invalidateDictConsumers(); }}
        onCancel={() => setTypeModalOpen(false)}
      />
    </>
  );
}
