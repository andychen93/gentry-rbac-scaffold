import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Card, Table, Button, Input, Select, Space, Form, Tag, message, Row, Col,
} from 'antd';
import { EditOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { deptApi } from '../../services/deptApi';
import type { DeptTreeVO, DeptQueryParams } from '../../services/deptApi';
import { useUserStore } from '../../stores/userStore';
import { RowActions } from '../../components/pro';
import DeptFormModal from './DeptFormModal';
import type { ColumnsType } from 'antd/es/table';

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
      message.success('删除成功');
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
    { title: '部门名称', dataIndex: 'name', key: 'name', width: '30%' },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: '10%', align: 'center' },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: '15%', align: 'center',
      render: (status: number) => (
        <Tag color={status === 1 ? 'blue' : 'default'}>
          {status === 1 ? '正常' : '停用'}
        </Tag>
      ),
    },
    {
      title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: '20%',
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: '操作', key: 'action', width: 110,
      render: (_: unknown, record: DeptTreeVO) => (
        <RowActions items={[
          {
            key: 'edit', label: '编辑', icon: <EditOutlined />, perm: 'system:dept:edit',
            onClick: () => handleEdit(record.id),
          },
          {
            key: 'add', label: '新增下级', icon: <PlusOutlined />, perm: 'system:dept:add',
            onClick: () => handleAddChild(record.id),
          },
          {
            key: 'del', label: '删除', icon: <DeleteOutlined />, perm: 'system:dept:remove',
            danger: true, confirmText: `确定删除部门「${record.name}」？`,
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
              <Form.Item name="name" label="部门名称" style={{ marginBottom: 0 }}>
                <Input placeholder="请输入部门名称" style={{ width: '100%' }} allowClear />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="status" label="状态" style={{ marginBottom: 0 }}>
                <Select placeholder="请选择" style={{ width: '100%' }} allowClear>
                  <Select.Option value={1}>正常</Select.Option>
                  <Select.Option value={0}>停用</Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item style={{ marginBottom: 0 }}>
                <Space>
                  <Button type="primary" onClick={handleSearch}>查询</Button>
                  <Button onClick={handleReset}>重置</Button>
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
                新增部门
              </Button>
            )}
            <Button
              onClick={handleToggleExpand}
            >
              {isExpandAll ? '折叠全部' : '展开全部'}
            </Button>
            <Button onClick={fetchTree}>刷新</Button>
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
