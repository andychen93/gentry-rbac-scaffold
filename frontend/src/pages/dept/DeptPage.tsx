import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Card, Table, Button, Input, Select, Space, Form, Tag, message, Row, Col,
} from 'antd';
import { EditOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { deptApi } from '../../services/deptApi';
import type { DeptTreeVO, DeptQueryParams } from '../../services/deptApi';
import { useUserStore } from '../../stores/userStore';
import { RowActions } from '@gentry/kit';
import DeptFormModal from './DeptFormModal';
import type { ColumnsType } from 'antd/es/table';
import { DICT_TYPES, dictLabel, dictOptions } from '../../locales/dictEnum';

/** 递归收集所有部门 ID（用于展开全部） */
function collectAllKeys(list: DeptTreeVO[]): string[] {
  const keys: string[] = [];
  const walk = (nodes: DeptTreeVO[]) => {
    for (const node of nodes) {
      keys.push(String(node.id));
      if (node.children?.length) walk(node.children);
    }
  };
  walk(list);
  return keys;
}

export default function DeptPage() {
  const { t } = useTranslation(['dept', 'common', 'dict']);
  const [form] = Form.useForm();
  const hasPermission = useUserStore((s) => s.hasPermission);

  // 数据状态
  const [data, setData] = useState<DeptTreeVO[]>([]);
  const [loading, setLoading] = useState(false);

  // 展开状态
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
  const [isExpandAll, setIsExpandAll] = useState(true);

  // 弹窗状态
  const [formModalOpen, setFormModalOpen] = useState(false);
  const [editingDeptId, setEditingDeptId] = useState<number | null>(null);
  const [defaultParentId, setDefaultParentId] = useState(0);

  const allKeys = useMemo(() => collectAllKeys(data), [data]);

  const fetchTree = useCallback(async () => {
    setLoading(true);
    try {
      const values = form.getFieldsValue();
      const params: DeptQueryParams = {};
      if (values.name) params.name = values.name;
      if (values.status !== undefined && values.status !== null) params.status = values.status;
      const res = await deptApi.tree(params);
      const treeData = res.data ?? [];
      setData(treeData);
      // 默认展开全部
      const keys = collectAllKeys(treeData);
      setExpandedKeys(keys);
      setIsExpandAll(true);
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  }, [form]);

  useEffect(() => { fetchTree(); }, []);

  const handleSearch = () => fetchTree();
  const handleReset = () => { form.resetFields(); fetchTree(); };

  const handleToggleExpand = () => {
    if (isExpandAll) {
      setExpandedKeys([]);
      setIsExpandAll(false);
    } else {
      setExpandedKeys(allKeys);
      setIsExpandAll(true);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deptApi.remove(id);
      message.success(t('common:msg.deleteSuccess'));
      fetchTree();
    } catch { /* handled */ }
  };

  const handleAddChild = (parentId: number) => {
    setEditingDeptId(null);
    setDefaultParentId(parentId);
    setFormModalOpen(true);
  };

  const handleEdit = (id: number) => {
    setEditingDeptId(id);
    setDefaultParentId(0);
    setFormModalOpen(true);
  };

  const handleAdd = () => {
    setEditingDeptId(null);
    setDefaultParentId(0);
    setFormModalOpen(true);
  };

  const columns: ColumnsType<DeptTreeVO> = [
    { title: t('table.name'), dataIndex: 'name', key: 'name', width: '30%' },
    { title: t('common:sort'), dataIndex: 'sort', key: 'sort', width: '10%', align: 'center' },
    {
      title: t('common:status'), dataIndex: 'status', key: 'status', width: '15%', align: 'center',
      // 文案取 sys_normal_disable 字典（正常/停用），与状态列的其他页面一致
      render: (status: number) => (
        <Tag color={status === 1 ? 'blue' : 'default'}>
          {dictLabel(t, DICT_TYPES.normalDisable, status)}
        </Tag>
      ),
    },
    {
      title: t('common:createTime'), dataIndex: 'createTime', key: 'createTime', width: '20%',
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: t('table.action'), key: 'action', width: 110,
      render: (_: unknown, record: DeptTreeVO) => (
        <RowActions items={[
          {
            key: 'edit', label: t('common:edit'), icon: <EditOutlined />, perm: 'system:dept:edit',
            onClick: () => handleEdit(record.id),
          },
          {
            key: 'add', label: t('action.addChild'), icon: <PlusOutlined />, perm: 'system:dept:add',
            onClick: () => handleAddChild(record.id),
          },
          {
            key: 'del', label: t('common:delete'), icon: <DeleteOutlined />, perm: 'system:dept:remove',
            danger: true, confirmText: t('confirm.delete', { name: record.name }),
            onClick: () => handleDelete(record.id),
          },
        ]} />
      ),
    },
  ];

  return (
    <>
      {/* 搜索栏（响应式栅格） */}
      <Card style={{ marginBottom: 16 }}>
        <Form form={form} component={false}>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="name" label={t('table.name')} style={{ marginBottom: 0 }}>
                <Input placeholder={t('query.namePlaceholder')} style={{ width: '100%' }} allowClear />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="status" label={t('common:status')} style={{ marginBottom: 0 }}>
                <Select
                  placeholder={t('common:placeholder.select')}
                  style={{ width: '100%' }}
                  allowClear
                  options={dictOptions(t, DICT_TYPES.normalDisable, { numeric: true })}
                />
              </Form.Item>
            </Col>
            {/* 查询/重置靠右，与 ProTable 的 QueryForm 保持一致 */}
            <Col flex="auto" style={{ textAlign: 'right' }}>
              <Form.Item style={{ marginBottom: 0 }}>
                <Space>
                  <Button type="primary" onClick={handleSearch}>{t('common:query')}</Button>
                  <Button onClick={handleReset}>{t('common:reset')}</Button>
                </Space>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Card>

      {/* 部门树形表格 */}
      <Card>
        <div style={{ marginBottom: 16 }}>
          <Space>
            {hasPermission('system:dept:add') && (
              <Button type="primary" onClick={handleAdd}>
                {t('action.create')}
              </Button>
            )}
            <Button
              onClick={handleToggleExpand}
            >
              {isExpandAll ? t('common:collapseAll') : t('common:expandAll')}
            </Button>
            <Button onClick={fetchTree}>{t('common:refresh')}</Button>
          </Space>
        </div>

        <Table<DeptTreeVO>
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          pagination={false}
          expandable={{
            expandedRowKeys: expandedKeys,
            onExpandedRowsChange: (keys) => {
              setExpandedKeys(keys as string[]);
              setIsExpandAll(false);
            },
          }}
        />
      </Card>

      {/* 新增/编辑弹窗 */}
      <DeptFormModal
        open={formModalOpen}
        deptId={editingDeptId}
        defaultParentId={defaultParentId}
        onSuccess={() => { setFormModalOpen(false); fetchTree(); }}
        onCancel={() => setFormModalOpen(false)}
      />
    </>
  );
}
