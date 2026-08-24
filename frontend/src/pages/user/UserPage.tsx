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
import { useTranslation } from 'react-i18next';
import { DICT_TYPES, dictLabel, dictOptions } from '../../locales/dictEnum';

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
  const { t } = useTranslation(['user', 'common', 'dict']);
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
      message.success(t('common:msg.exportSuccess'));
    } catch {
      message.error(t('msg.exportFailed'));
    }
  };

  const handleImport = (file: File) => {
    setImporting(true);
    userApi.importUsers(file)
      .then((res) => {
        message.success(t('msg.importDone', { success: res.success, fail: res.fail }));
        if (res.fail > 0 && res.errors?.length) {
          const detail = res.errors.slice(0, 3)
            .map((e) => t('msg.importErrorRow', {
              row: e.row,
              who: e.username ? `(${e.username})` : '',
              reason: e.msg,
            }))
            .join('; ');
          message.warning(t('msg.importErrorDetail', {
            detail,
            more: res.errors.length > 3 ? t('msg.importMore') : '',
          }));
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
      message.success(t('common:msg.statusUpdated'));
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
    { title: t('common:username'), dataIndex: 'username', key: 'username', width: 120 },
    { title: t('common:nickname'), dataIndex: 'nickname', key: 'nickname', width: 120 },
    { title: t('common:phone'), dataIndex: 'phone', key: 'phone', width: 140 },
    { title: t('common:dept'), dataIndex: 'deptName', key: 'deptName', width: 120 },
    {
      title: t('common:gender'), dataIndex: 'gender', key: 'gender', width: 80,
      // 原来是硬编码 { 0:'未知',1:'男',2:'女' } —— 那是 sys_user_gender 字典的拷贝
      render: (v: number) => dictLabel(t, DICT_TYPES.gender, v ?? 0),
    },
    {
      title: t('table.roles'), dataIndex: 'roles', key: 'roles', width: 160,
      render: (roles: UserListVO['roles']) =>
        roles?.map((r) => (
          <Tag color="blue" key={r.id}>{r.roleName || t('table.roleFallback', { id: r.id })}</Tag>
        )),
    },
    {
      title: t('common:status'), dataIndex: 'status', key: 'status', width: 80,
      render: (s: number, r: UserListVO) => (
        <StatusSwitch
          id={r.id}
          status={s}
          onToggle={handleStatus}
          confirmText={(next) =>
            next
              ? t('confirm.enable', { username: r.username })
              : t('confirm.disable', { username: r.username })
          }
        />
      ),
    },
    { title: t('common:createTime'), dataIndex: 'createTime', key: 'createTime', width: 180 },
    {
      // 「操作」在表头是 Action，与操作日志语境的 Operation 不同，故不进 common
      title: t('table.action'), key: 'action', width: 130, fixed: 'right',
      render: (_: unknown, r: UserListVO) => (
        <RowActions items={[
          {
            key: 'edit', label: t('common:edit'), icon: <EditOutlined />,
            onClick: () => { setEditingId(r.id); setFormOpen(true); },
          },
          {
            key: 'role', label: t('common:role'), icon: <UserSwitchOutlined />,
            onClick: () => handleOpenRoleModal(r),
          },
          {
            key: 'pwd', label: t('action.resetPwd'), icon: <KeyOutlined />,
            onClick: () => { setPasswordTargetUserId(r.id); setPasswordModalOpen(true); },
          },
          {
            key: 'del', label: t('common:delete'), icon: <DeleteOutlined />, danger: true,
            confirmText: t('confirm.delete', { username: r.username }),
            onClick: async () => {
              await userApi.remove(r.id);
              message.success(t('common:msg.deleteSuccess'));
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
      <Card style={{ width: 240, flexShrink: 0 }} title={t('common:dept')} size="small" loading={deptLoading}>
        <Tree
          treeData={deptTree}
          selectedKeys={selectedDeptId ? [String(selectedDeptId)] : []}
          onSelect={handleDeptSelect}
          defaultExpandAll
          blockNode
        />
        {selectedDeptId && (
          <Button type="link" size="small" onClick={() => handleDeptSelect([])}>{t('action.clearFilter')}</Button>
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
            { name: 'username', label: t('common:username') },
            { name: 'phone', label: t('common:phone') },
            {
              name: 'status', label: t('common:status'), type: 'select',
              // 原来写死「正常 / 禁用」，而库里 sys_normal_disable 是「正常 / 停用」——
              // 已经漂移了。改由字典枚举驱动
              options: dictOptions(t, DICT_TYPES.normalDisable, { numeric: true }),
            },
          ]}
          toolbar={
            <Space>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingId(null); setFormOpen(true); }}>{t('action.create')}</Button>
              <Button icon={<DownloadOutlined />} disabled={!hasPermission('system:user:export')} onClick={handleExport}>{t('action.export')}</Button>
              <Upload accept=".xlsx,.xls" showUploadList={false} beforeUpload={(file) => { handleImport(file); return false; }}>
                <Button icon={<UploadOutlined />} loading={importing} disabled={!hasPermission('system:user:import')}>{t('action.import')}</Button>
              </Upload>
              <Button icon={<DownloadOutlined />} onClick={() => userApi.downloadTemplate()}>{t('action.template')}</Button>
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
