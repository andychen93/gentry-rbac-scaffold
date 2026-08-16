import { useState, useEffect } from 'react';
import { Button, Card, Tree, Tag, message, Space, Upload } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, UserSwitchOutlined, KeyOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQueryClient } from '@tanstack/react-query';
import { ProTable, StatusSwitch, RowActions } from '../../components/pro';
import { userApi } from '../../services/userApi';
import { useUserStore } from '../../stores/userStore';
import type { UserListVO } from '../../services/userApi';
import { deptApi } from '../../services/deptApi';
import type { DeptTreeVO } from '../../services/deptApi';
import UserFormModal from './UserFormModal';
import RoleAssignModal from './RoleAssignModal';
import PasswordResetModal from './PasswordResetModal';

interface TreeNode {
  title: string;
  key: string;
  children?: TreeNode[];
}

function buildDeptTreeNodes(list: DeptTreeVO[]): TreeNode[] {
  return list.map((d) => ({
    title: d.name,
    key: String(d.id),
    children: d.children?.length ? buildDeptTreeNodes(d.children) : undefined,
  }));
}

export default function UserPage() {
  const qc = useQueryClient();

  // 部门树状态（保留左侧导航 UX，deptId 通过 queryKey+service 注入 ProTable）
  const [deptTree, setDeptTree] = useState<TreeNode[]>([]);
  const [deptLoading, setDeptLoading] = useState(false);
  const [selectedDeptId, setSelectedDeptId] = useState<number | null>(null);

  // 弹窗状态
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [roleTargetUserId, setRoleTargetUserId] = useState<number | null>(null);
  const [currentRoleIds, setCurrentRoleIds] = useState<number[]>([]);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [passwordTargetUserId, setPasswordTargetUserId] = useState<number | null>(null);
  const [importing, setImporting] = useState(false);
  const [exportFilters, setExportFilters] = useState<Record<string, unknown>>({});
  const hasPermission = useUserStore((s) => s.hasPermission);

  useEffect(() => {
    setDeptLoading(true);
    deptApi.tree()
      .then((res) => setDeptTree(buildDeptTreeNodes(res.data ?? [])))
      .catch(() => setDeptTree([]))
      .finally(() => setDeptLoading(false));
  }, []);

  const refresh = () => qc.invalidateQueries({ queryKey: ['users'] });

  const handleExport = async () => {
    try {
      await userApi.exportUsers({ pageNum: 1, pageSize: 10, ...exportFilters, deptId: selectedDeptId ?? undefined });
      message.success('导出成功');
    } catch {
      message.error('导出失败');
    }
  };

  const handleImport = (file: File) => {
    setImporting(true);
    userApi.importUsers(file)
      .then((res) => {
        message.success(`导入完成：成功 ${res.success} 条，失败 ${res.fail} 条`);
        if (res.fail > 0 && res.errors?.length) {
          const detail = res.errors.slice(0, 3)
            .map((e) => `第${e.row}行${e.username ? '(' + e.username + ')' : ''}${e.msg}`).join('；');
          message.warning(`失败明细：${detail}${res.errors.length > 3 ? '...' : ''}`);
        }
        refresh();
      })
      .catch(() => { /* 拦截器已弹 toast */ })
      .finally(() => setImporting(false));
  };

  const handleStatus = async (id: string | number, checked: boolean) => {
    try {
      // id 是雪花 ID，超过 JS Number 安全整数范围就不能 Number(id)（会精度丢失变成别的 ID）
      await userApi.updateStatus(id, { status: checked ? 1 : 0 });
      message.success('状态更新成功');
      refresh();
    } catch {
      refresh(); // 回滚 Switch 状态
    }
  };

  const handleOpenRoleModal = async (record: UserListVO) => {
    try {
      const res = await userApi.detail(record.id);
      setRoleTargetUserId(record.id);
      setCurrentRoleIds(res.data.roleIds || []);
      setRoleModalOpen(true);
    } catch { /* handled by interceptor */ }
  };

  const handleDeptSelect = (selectedKeys: React.Key[]) => {
    setSelectedDeptId(selectedKeys.length > 0 ? Number(selectedKeys[0]) : null);
  };

  const columns: ColumnsType<UserListVO> = [
    { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
    { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120 },
    { title: '手机号', dataIndex: 'phone', key: 'phone', width: 140 },
    { title: '部门', dataIndex: 'deptName', key: 'deptName', width: 120 },
    {
      title: '性别', dataIndex: 'gender', key: 'gender', width: 80,
      render: (v: number) => ({ 0: '未知', 1: '男', 2: '女' }[v] || '未知'),
    },
    {
      title: '角色', dataIndex: 'roles', key: 'roles', width: 160,
      render: (roles: UserListVO['roles']) =>
        roles?.map((r) => <Tag color="blue" key={r.id}>{r.roleName || `角色${r.id}`}</Tag>),
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (s: number, r: UserListVO) => (
        <StatusSwitch
          id={r.id}
          status={s}
          onToggle={handleStatus}
          confirmText={(next) =>
            next
              ? `确定启用用户「${r.username}」？启用后该账号可以正常登录。`
              : `确定停用用户「${r.username}」？停用后该账号将无法登录，已登录的会话不受影响。`
          }
        />
      ),
    },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
    {
      title: '操作', key: 'action', width: 130, fixed: 'right',
      render: (_: unknown, r: UserListVO) => (
        <RowActions items={[
          {
            key: 'edit', label: '编辑', icon: <EditOutlined />,
            onClick: () => { setEditingId(r.id); setFormOpen(true); },
          },
          {
            key: 'role', label: '角色', icon: <UserSwitchOutlined />,
            onClick: () => handleOpenRoleModal(r),
          },
          {
            key: 'pwd', label: '重置密码', icon: <KeyOutlined />,
            onClick: () => { setPasswordTargetUserId(r.id); setPasswordModalOpen(true); },
          },
          {
            key: 'del', label: '删除', icon: <DeleteOutlined />, danger: true,
            confirmText: `确定删除用户 ${r.username}？`,
            onClick: async () => {
              await userApi.remove(r.id);
              message.success('删除成功');
              refresh();
            },
          },
        ]} />
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', gap: 16 }}>
      {/* 左侧部门树（保留导航 UX：点击部门 → queryKey 变化 → refetch） */}
      <Card style={{ width: 240, flexShrink: 0 }} title="部门" size="small" loading={deptLoading}>
        <Tree
          treeData={deptTree}
          selectedKeys={selectedDeptId ? [String(selectedDeptId)] : []}
          onSelect={handleDeptSelect}
          defaultExpandAll
          blockNode
        />
        {selectedDeptId && (
          <Button type="link" size="small" onClick={() => handleDeptSelect([])}>清除筛选</Button>
        )}
      </Card>

      {/* 右侧用户列表（ProTable + QueryForm 栅格响应式） */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <ProTable<UserListVO>
          service={(params) => userApi.list({ ...params, deptId: selectedDeptId ?? undefined })}
          queryKey={['users', selectedDeptId ?? 'all']}
          columns={columns}
          rowKey="id"
          scroll={{ x: 1200 }}
          onFiltersChange={(f) => setExportFilters(f as Record<string, unknown>)}
          querySchema={[
            { name: 'username', label: '用户名' },
            { name: 'phone', label: '手机号' },
            {
              name: 'status', label: '状态', type: 'select',
              options: [{ label: '正常', value: 1 }, { label: '禁用', value: 0 }],
            },
          ]}
          toolbar={
            <Space>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingId(null); setFormOpen(true); }}>新增用户</Button>
              <Button icon={<DownloadOutlined />} disabled={!hasPermission('system:user:export')} onClick={handleExport}>导出</Button>
              <Upload accept=".xlsx,.xls" showUploadList={false} beforeUpload={(file) => { handleImport(file); return false; }}>
                <Button icon={<UploadOutlined />} loading={importing} disabled={!hasPermission('system:user:import')}>导入</Button>
              </Upload>
              <Button icon={<DownloadOutlined />} onClick={() => userApi.downloadTemplate()}>模板</Button>
            </Space>
          }
        />
      </div>

      {/* 弹窗（保留手写 FormModal —— 用户表单含部门/角色分配，CrudFormModal 不覆盖） */}
      <UserFormModal
        open={formOpen}
        userId={editingId}
        onSuccess={() => { setFormOpen(false); refresh(); }}
        onCancel={() => setFormOpen(false)}
      />
      <RoleAssignModal
        open={roleModalOpen}
        userId={roleTargetUserId!}
        currentRoleIds={currentRoleIds}
        onSuccess={() => { setRoleModalOpen(false); refresh(); }}
        onCancel={() => setRoleModalOpen(false)}
      />
      <PasswordResetModal
        open={passwordModalOpen}
        userId={passwordTargetUserId!}
        onSuccess={() => { setPasswordModalOpen(false); }}
        onCancel={() => setPasswordModalOpen(false)}
      />
    </div>
  );
}
