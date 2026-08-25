import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Card, Table, Button, Input, Select, Space, Form, Tag, message, Row, Col,
} from 'antd';
import * as AllIcons from '@ant-design/icons';
import { EditOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { menuApi } from '../../services/menuApi';
import type { MenuTreeVO, MenuQueryDTO } from '../../services/menuApi';
import { useUserStore } from '../../stores/userStore';
import { RowActions } from '../../components/pro';
import MenuFormModal from './MenuFormModal';
import type { ColumnsType } from 'antd/es/table';
import { DICT_TYPES, dictLabel, dictOptions } from '../../locales/dictEnum';
import { makeNavLabel } from '../../locales/navLabel';

/*
 * 只留颜色，文案全部走字典/语言包。
 *
 * 原来这三张 Map 各带一份中文 label，其中 menuTypeMap 与 dict.sys_menu_type.*、
 * statusMap 与 dict.sys_normal_disable.* 是重复真源，而且 statusMap 写「启用/禁用」、
 * 字典写「正常/停用」，已经漂移。
 */
const TYPE_COLOR: Record<number, string> = { 1: 'cyan', 2: 'blue', 3: 'orange' };
const STATUS_COLOR: Record<number, string> = { 0: 'error', 1: 'success' };
const VISIBLE_COLOR: Record<number, string> = { 0: 'red', 1: 'green' };

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
  const { t } = useTranslation(['menuMgmt', 'common', 'dict', 'nav']);
  const navLabel = makeNavLabel(t);
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
      message.success(t('common:msg.deleteSuccess'));
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
    setParentMenuName(navLabel(record));
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
      message.success(t('common:msg.statusUpdated'));
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
      message.success(t('msg.visibleUpdated'));
      fetchTree();
    } catch { /* handled */ }
  };

  const columns: ColumnsType<MenuTreeVO> = [
    {
      title: t('table.name'), dataIndex: 'name', key: 'name',
      // 菜单名是 B 类内容：后端下发 i18nKey，前端翻；派生不出 key 时回退库里的 name
      render: (_name: string, record: MenuTreeVO) => (
        <Space size={4}>
          {record.icon && renderIcon(record.icon)}
          <span>{navLabel(record)}</span>
        </Space>
      ),
    },
    {
      title: t('table.icon'), dataIndex: 'icon', key: 'icon', width: 80, align: 'center',
      render: (icon: string | null) => renderIcon(icon),
    },
    {
      title: t('common:type'), dataIndex: 'type', key: 'type', width: 80, align: 'center',
      render: (type: number) =>
        TYPE_COLOR[type] ? (
          <Tag color={TYPE_COLOR[type]}>{dictLabel(t, DICT_TYPES.menuType, type)}</Tag>
        ) : (
          type
        ),
    },
    { title: t('common:sort'), dataIndex: 'sort', key: 'sort', width: 80, align: 'center' },
    {
      title: t('table.permission'), dataIndex: 'permission', key: 'permission', width: 180,
      render: (perm: string | null) => perm ? <Tag color="blue">{perm}</Tag> : '-',
    },
    { title: t('table.path'), dataIndex: 'path', key: 'path', width: 160, render: (v: string | null) => v || '-' },
    { title: t('table.component'), dataIndex: 'component', key: 'component', width: 160, render: (v: string | null) => v || '-' },
    {
      title: t('table.visible'), dataIndex: 'visible', key: 'visible', width: 80, align: 'center',
      render: (visible: number, record: MenuTreeVO) =>
        VISIBLE_COLOR[visible] ? (
          <a onClick={() => handleVisibleChange(record)}>
            <Tag color={VISIBLE_COLOR[visible]}>
              {visible === 1 ? t('common:visible.show') : t('common:visible.hide')}
            </Tag>
          </a>
        ) : (
          visible
        ),
    },
    {
      title: t('common:status'), dataIndex: 'status', key: 'status', width: 80, align: 'center',
      render: (status: number, record: MenuTreeVO) =>
        STATUS_COLOR[status] ? (
          <a onClick={() => handleStatusChange(record)}>
            <Tag color={STATUS_COLOR[status]}>{dictLabel(t, DICT_TYPES.normalDisable, status)}</Tag>
          </a>
        ) : (
          status
        ),
    },
    {
      title: t('table.action'), key: 'action', width: 110,
      render: (_: unknown, record: MenuTreeVO) => (
        <RowActions items={[
          {
            key: 'edit', label: t('common:edit'), icon: <EditOutlined />, perm: 'system:menu:edit',
            onClick: () => handleEdit(record),
          },
          // 按钮类型(type=3)没有下级，不展示「新增下级」
          ...(record.type !== 3 ? [{
            key: 'add', label: t('action.addChild'), icon: <PlusOutlined />, perm: 'system:menu:add',
            onClick: () => handleAddChild(record),
          }] : []),
          {
            key: 'del', label: t('common:delete'), icon: <DeleteOutlined />, perm: 'system:menu:remove',
            danger: true,
            confirmText: record.children?.length
              ? t('confirm.deleteWithChildren', { name: navLabel(record) })
              : t('confirm.delete', { name: navLabel(record) }),
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
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="type" label={t('common:type')} style={{ marginBottom: 0 }}>
                <Select
                  placeholder={t('common:placeholder.select')}
                  style={{ width: '100%' }}
                  allowClear
                  options={dictOptions(t, DICT_TYPES.menuType, { numeric: true })}
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

      {/* 菜单树形表格 */}
      <Card>
        <div style={{ marginBottom: 16 }}>
          <Space>
            {hasPermission('system:menu:add') && (
              <Button type="primary" onClick={handleAdd}>{t('action.create')}</Button>
            )}
            <Button
              onClick={handleToggleExpand}
            >
              {isExpandAll ? t('common:collapseAll') : t('common:expandAll')}
            </Button>
            <Button onClick={fetchTree}>{t('common:refresh')}</Button>
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
