import { useState } from 'react';
import { Button, Tag, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { PlusOutlined, EditOutlined, DeleteOutlined, SafetyOutlined, UserSwitchOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import { useQueryClient } from '@tanstack/react-query';
import { ProTable, StatusSwitch, RowActions } from '@gentry/kit';
import { roleApi } from '../../services/roleApi';
import type { RoleListVO } from '../../services/roleApi';
import { useUserStore } from '../../stores/userStore';
import RoleFormModal from './RoleFormModal';
import UserAssignModal from './UserAssignModal';
import { DICT_TYPES, dictLabel, dictOptions } from '../../locales/dictEnum';
import { makeRoleLabel } from '../../locales/navLabel';

/*
 * 数据权限档位的文案**不在本文件**。原来这里有一张 DATA_SCOPE_MAP，
 * RoleFormModal 和 PermissionPage 各有一份 DATA_SCOPE_OPTIONS —— 三份拷贝而且
 * 已经漂移了（本页写「本部门」，另两处写「本部门数据」）。现在统一取
 * dict namespace 的 dict.sys_data_scope.*，与库里 sys_data_scope 字典同源。
 */

export default function RolePage() {
  const { t } = useTranslation(['role', 'common', 'dict']);
  const navigate = useNavigate();
  const hasPermission = useUserStore((s) => s.hasPermission);
  const qc = useQueryClient();
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [userModalOpen, setUserModalOpen] = useState(false);
  const [userModalRole, setUserModalRole] = useState<RoleListVO | null>(null);
  const refresh = () => qc.invalidateQueries({ queryKey: ['roles'] });

  const handleStatus = async (id: string | number, checked: boolean) => {
    try {
      // id 是雪花 ID，超过 JS Number 安全整数范围就不能 Number(id)（会精度丢失变成别的 ID）
      await roleApi.updateStatus(id, { status: checked ? 1 : 0 });
      message.success(t('common:msg.statusUpdated'));
      refresh();
    } catch {
      refresh(); // 回滚 Switch 状态
    }
  };

  // 角色名走 i18nKey 翻译（内置角色 ADMIN/USER），闭包必须在渲染时调用，
  // 不能预算缓存——理由同 makeNavLabel 头注释：切语言要响应式生效
  const roleLabel = makeRoleLabel(t);

  const columns: ColumnsType<RoleListVO> = [
    { title: t('table.code'), dataIndex: 'roleCode', key: 'roleCode', width: 140 },
    {
      title: t('table.name'), dataIndex: 'roleName', key: 'roleName', width: 140,
      render: (_: string, r: RoleListVO) => roleLabel(r),
    },
    {
      title: t('table.dataScope'), dataIndex: 'dataScope', key: 'dataScope', width: 140,
      render: (v: number) => <Tag>{dictLabel(t, DICT_TYPES.dataScope, v)}</Tag>,
    },
    { title: t('table.userCount'), dataIndex: 'userCount', key: 'userCount', width: 90, align: 'center' },
    { title: t('common:sort'), dataIndex: 'sort', key: 'sort', width: 70, align: 'center' },
    {
      title: t('common:status'), dataIndex: 'status', key: 'status', width: 90, align: 'center',
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
      title: t('common:createTime'), dataIndex: 'createTime', key: 'createTime', width: 170,
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: t('table.action'), key: 'action', width: 140, fixed: 'right',
      render: (_: unknown, r: RoleListVO) => (
        <RowActions items={[
          {
            key: 'edit', label: t('common:edit'), icon: <EditOutlined />, perm: 'system:role:edit',
            onClick: () => { setEditingId(r.id); setFormOpen(true); },
          },
          {
            key: 'perm', label: t('action.perm'), icon: <SafetyOutlined />, perm: 'system:role:assignMenu',
            onClick: () => navigate(`/system/roles/${r.id}/permissions`),
          },
          {
            key: 'users', label: t('action.bindUser'), icon: <UserSwitchOutlined />,
            perm: 'system:role:assignUser',
            onClick: () => { setUserModalRole(r); setUserModalOpen(true); },
          },
          {
            key: 'del', label: t('common:delete'), icon: <DeleteOutlined />, perm: 'system:role:remove',
            danger: true, confirmText: t('confirm.delete', { name: roleLabel(r) }),
            onClick: async () => {
              await roleApi.remove(r.id);
              message.success(t('common:msg.deleteSuccess'));
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
          { name: 'roleName', label: t('table.name') },
          { name: 'roleCode', label: t('table.code') },
          {
            name: 'status', label: t('common:status'), type: 'select',
            // 选项取库里 sys_normal_disable 字典（正常/停用），不再硬编码「启用/禁用」
            options: dictOptions(t, DICT_TYPES.normalDisable, { numeric: true }),
          },
        ]}
        toolbar={
          hasPermission('system:role:add') ? (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => { setEditingId(null); setFormOpen(true); }}
            >
              {t('action.create')}
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
      {userModalRole && (
        <UserAssignModal
          open={userModalOpen}
          roleId={userModalRole.id}
          roleName={roleLabel(userModalRole)}
          /* 绑定完要刷列表：「关联用户」列的 userCount 会变 */
          onSuccess={() => { setUserModalOpen(false); refresh(); }}
          onCancel={() => setUserModalOpen(false)}
        />
      )}
    </>
  );
}
