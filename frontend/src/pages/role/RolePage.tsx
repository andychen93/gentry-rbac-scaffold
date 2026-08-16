import { useState } from 'react';
import { Button, Tag, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SafetyOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import { useQueryClient } from '@tanstack/react-query';
import { ProTable, StatusSwitch, RowActions } from '../../components/pro';
import { roleApi } from '../../services/roleApi';
import type { RoleListVO } from '../../services/roleApi';
import { useUserStore } from '../../stores/userStore';
import RoleFormModal from './RoleFormModal';

const DATA_SCOPE_MAP: Record<number, string> = {
  1: '全部数据',
  2: '本部门及子部门',
  3: '本部门',
  4: '仅本人',
  5: '自定义',
};

export default function RolePage() {
  const navigate = useNavigate();
  const hasPermission = useUserStore((s) => s.hasPermission);
  const qc = useQueryClient();
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const refresh = () => qc.invalidateQueries({ queryKey: ['roles'] });

  const handleStatus = async (id: string | number, checked: boolean) => {
    try {
      // id 是雪花 ID，超过 JS Number 安全整数范围就不能 Number(id)（会精度丢失变成别的 ID）
      await roleApi.updateStatus(id, { status: checked ? 1 : 0 });
      message.success('状态更新成功');
      refresh();
    } catch {
      refresh(); // 回滚 Switch 状态
    }
  };

  const columns: ColumnsType<RoleListVO> = [
    { title: '角色编码', dataIndex: 'roleCode', key: 'roleCode', width: 140 },
    { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 140 },
    {
      title: '数据权限', dataIndex: 'dataScope', key: 'dataScope', width: 140,
      render: (v: number) => <Tag>{DATA_SCOPE_MAP[v] ?? '未知'}</Tag>,
    },
    { title: '关联用户', dataIndex: 'userCount', key: 'userCount', width: 90, align: 'center' },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'center' },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 90, align: 'center',
      render: (s: number, r: RoleListVO) => (
        <StatusSwitch
          id={r.id}
          status={s}
          onToggle={handleStatus}
          disabled={!hasPermission('system:role:edit')}
        />
      ),
    },
    {
      title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170,
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: '操作', key: 'action', width: 110, fixed: 'right',
      render: (_: unknown, r: RoleListVO) => (
        <RowActions items={[
          {
            key: 'edit', label: '编辑', icon: <EditOutlined />, perm: 'system:role:edit',
            onClick: () => { setEditingId(r.id); setFormOpen(true); },
          },
          {
            key: 'perm', label: '权限', icon: <SafetyOutlined />, perm: 'system:role:assignMenu',
            onClick: () => navigate(`/system/roles/${r.id}/permissions`),
          },
          {
            key: 'del', label: '删除', icon: <DeleteOutlined />, perm: 'system:role:remove',
            danger: true, confirmText: `确定删除角色「${r.roleName}」？`,
            onClick: async () => {
              await roleApi.remove(r.id);
              message.success('删除成功');
              refresh();
            },
          },
        ]} />
      ),
    },
  ];

  return (
    <>
      <ProTable<RoleListVO>
        service={roleApi.list}
        queryKey={['roles']}
        columns={columns}
        rowKey="id"
        scroll={{ x: 1100 }}
        querySchema={[
          { name: 'roleName', label: '角色名称' },
          { name: 'roleCode', label: '角色编码' },
          {
            name: 'status', label: '状态', type: 'select',
            options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }],
          },
        ]}
        toolbar={
          hasPermission('system:role:add') ? (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => { setEditingId(null); setFormOpen(true); }}
            >
              新增角色
            </Button>
          ) : undefined
        }
      />
      <RoleFormModal
        open={formOpen}
        roleId={editingId}
        onSuccess={() => { setFormOpen(false); refresh(); }}
        onCancel={() => setFormOpen(false)}
      />
    </>
  );
}
