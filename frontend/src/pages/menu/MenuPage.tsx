import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Card, Table, Button, Input, Select, Space, Form, Tag, message, Popconfirm, Row, Col,
} from 'antd';
import * as AllIcons from '@ant-design/icons';
import { menuApi } from '../../services/menuApi';
import type { MenuTreeVO, MenuQueryDTO } from '../../services/menuApi';
import { useUserStore } from '../../stores/userStore';
import MenuFormModal from './MenuFormModal';
import type { ColumnsType } from 'antd/es/table';

/** 菜单类型映射 */
const menuTypeMap: Record<number, { label: string; color: string }> = {
  1: { label: '目录', color: 'cyan' },
  2: { label: '菜单', color: 'blue' },
  3: { label: '按钮', color: 'orange' },
};

/** 状态映射 */
const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '禁用', color: 'error' },
  1: { label: '启用', color: 'success' },
};

/** 可见性映射 */
const visibleMap: Record<number, { label: string; color: string }> = {
  0: { label: '隐藏', color: 'red' },
  1: { label: '显示', color: 'green' },
};

/** 渲染图标 */
const renderIcon = (iconName: string | null) => {
  if (!iconName) return '-';
  const IconComp = (AllIcons as Record<string, any>)[iconName];
  return IconComp ? <IconComp /> : iconName;
};

/** 递归收集所有菜单 ID */
function collectAllKeys(list: MenuTreeVO[]): string[] {
  const keys: string[] = [];
  const walk = (nodes: MenuTreeVO[]) => {
    for (const node of nodes) {
      keys.push(String(node.id));
      if (node.children?.length) walk(node.children);
    }
  };
  walk(list);
  return keys;
}

export default function MenuPage() {
  const [form] = Form.useForm();
  const hasPermission = useUserStore((s) => s.hasPermission);

  // 数据状态
  const [data, setData] = useState<MenuTreeVO[]>([]);
  const [loading, setLoading] = useState(false);

  // 展开状态
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
  const [isExpandAll, setIsExpandAll] = useState(true);

  // 弹窗状态
  const [formModalOpen, setFormModalOpen] = useState(false);
  const [editingMenuId, setEditingMenuId] = useState<number | null>(null);
  const [parentMenuId, setParentMenuId] = useState<number | null>(null);
  const [parentMenuName, setParentMenuName] = useState<string | null>(null);

  const allKeys = useMemo(() => collectAllKeys(data), [data]);

  const fetchTree = useCallback(async () => {
    setLoading(true);
    try {
      const values = form.getFieldsValue();
      const params: MenuQueryDTO = {};
      if (values.name) params.name = values.name;
      if (values.status !== undefined && values.status !== null) params.status = values.status;
      if (values.type !== undefined && values.type !== null) params.type = values.type;
      const res = await menuApi.tree(params);
      const treeData = res.data ?? [];
      setData(treeData);
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
      await menuApi.remove(id);
      message.success('删除成功');
      fetchTree();
    } catch { /* handled */ }
  };

  const handleAdd = () => {
    setEditingMenuId(null);
    setParentMenuId(null);
    setParentMenuName(null);
    setFormModalOpen(true);
  };

  const handleAddChild = (record: MenuTreeVO) => {
    setEditingMenuId(null);
    setParentMenuId(record.id);
    setParentMenuName(record.name);
    setFormModalOpen(true);
  };

  const handleEdit = (record: MenuTreeVO) => {
    setEditingMenuId(record.id);
    setParentMenuId(record.parentId !== 0 ? record.parentId : null);
    setParentMenuName(null);
    setFormModalOpen(true);
  };

  const handleStatusChange = async (record: MenuTreeVO) => {
    try {
      await menuApi.update(record.id, {
        name: record.name,
        sort: record.sort,
        status: record.status === 1 ? 0 : 1,
      });
      message.success('状态更新成功');
      fetchTree();
    } catch { /* handled */ }
  };

  const handleVisibleChange = async (record: MenuTreeVO) => {
    try {
      await menuApi.update(record.id, {
        name: record.name,
        sort: record.sort,
        visible: record.visible === 1 ? 0 : 1,
      });
      message.success('可见性更新成功');
      fetchTree();
    } catch { /* handled */ }
  };

  const columns: ColumnsType<MenuTreeVO> = [
    {
      title: '菜单名称', dataIndex: 'name', key: 'name',
      render: (name: string, record: MenuTreeVO) => (
        <Space size={4}>
          {record.icon && renderIcon(record.icon)}
          <span>{name}</span>
        </Space>
      ),
    },
    {
      title: '图标', dataIndex: 'icon', key: 'icon', width: 80, align: 'center',
      render: (icon: string | null) => renderIcon(icon),
    },
    {
      title: '类型', dataIndex: 'type', key: 'type', width: 80, align: 'center',
      render: (type: number) => {
        const item = menuTypeMap[type];
        return item ? <Tag color={item.color}>{item.label}</Tag> : type;
      },
    },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 80, align: 'center' },
    {
      title: '权限标识', dataIndex: 'permission', key: 'permission', width: 180,
      render: (perm: string | null) => perm ? <Tag color="blue">{perm}</Tag> : '-',
    },
    { title: '路由地址', dataIndex: 'path', key: 'path', width: 160, render: (v: string | null) => v || '-' },
    { title: '组件路径', dataIndex: 'component', key: 'component', width: 160, render: (v: string | null) => v || '-' },
    {
      title: '可见', dataIndex: 'visible', key: 'visible', width: 80, align: 'center',
      render: (visible: number, record: MenuTreeVO) => {
        const item = visibleMap[visible];
        return item ? (
          <a onClick={() => handleVisibleChange(record)}>
            <Tag color={item.color}>{item.label}</Tag>
          </a>
        ) : visible;
      },
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80, align: 'center',
      render: (status: number, record: MenuTreeVO) => {
        const item = statusMap[status];
        return item ? (
          <a onClick={() => handleStatusChange(record)}>
            <Tag color={item.color}>{item.label}</Tag>
          </a>
        ) : status;
      },
    },
    {
      title: '操作', key: 'action', width: 200,
      render: (_: unknown, record: MenuTreeVO) => (
        <Space size="small">
          {hasPermission('system:menu:edit') && (
            <a onClick={() => handleEdit(record)}>编辑</a>
          )}
          {hasPermission('system:menu:add') && record.type !== 3 && (
            <a onClick={() => handleAddChild(record)}>新增</a>
          )}
          {hasPermission('system:menu:remove') && (
            <Popconfirm
              title={
                record.children?.length
                  ? `将同时删除所有子菜单和角色关联，确定删除「${record.name}」？`
                  : `确定删除菜单「${record.name}」？`
              }
              onConfirm={() => handleDelete(record.id)}
            >
              <a style={{ color: '#ff4d4f' }}>删除</a>
            </Popconfirm>
          )}
        </Space>
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
              <Form.Item name="name" label="菜单名称" style={{ marginBottom: 0 }}>
                <Input placeholder="请输入菜单名称" style={{ width: '100%' }} allowClear />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="status" label="状态" style={{ marginBottom: 0 }}>
                <Select placeholder="请选择" style={{ width: '100%' }} allowClear>
                  <Select.Option value={1}>启用</Select.Option>
                  <Select.Option value={0}>禁用</Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="type" label="类型" style={{ marginBottom: 0 }}>
                <Select placeholder="请选择" style={{ width: '100%' }} allowClear>
                  <Select.Option value={1}>目录</Select.Option>
                  <Select.Option value={2}>菜单</Select.Option>
                  <Select.Option value={3}>按钮</Select.Option>
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

      {/* 菜单树形表格 */}
      <Card>
        <div style={{ marginBottom: 16 }}>
          <Space>
            {hasPermission('system:menu:add') && (
              <Button type="primary" onClick={handleAdd}>新增菜单</Button>
            )}
            <Button
              onClick={handleToggleExpand}
            >
              {isExpandAll ? '折叠全部' : '展开全部'}
            </Button>
            <Button onClick={fetchTree}>刷新</Button>
          </Space>
        </div>

        <Table<MenuTreeVO>
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
          scroll={{ x: 1400 }}
        />
      </Card>

      {/* 新增/编辑弹窗 */}
      <MenuFormModal
        open={formModalOpen}
        menuId={editingMenuId}
        parentId={parentMenuId}
        parentName={parentMenuName}
        onSuccess={() => { setFormModalOpen(false); fetchTree(); }}
        onCancel={() => setFormModalOpen(false)}
      />
    </>
  );
}
