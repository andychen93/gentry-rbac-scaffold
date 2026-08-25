import { useState } from 'react';
import { Button, message } from 'antd';
import {
  PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SettingOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import { ProTable, StatusSwitch, RowActions } from '../../components/pro';
import { tenantMgmtApi } from '../../services/tenantApi';
import type { TenantListVO, TenantCreateResultVO } from '../../services/tenantApi';
import { useUserStore } from '../../stores/userStore';
import TenantFormModal from './TenantFormModal';
import TenantDetailModal from './TenantDetailModal';
import TenantConfigModal from './TenantConfigModal';
import PasswordShowModal from './PasswordShowModal';
import { DICT_TYPES, dictOptions } from '../../locales/dictEnum';

export default function TenantPage() {
  const { t } = useTranslation(['tenant', 'common', 'dict']);
  const hasPermission = useUserStore((s) => s.hasPermission);
  const qc = useQueryClient();
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [configId, setConfigId] = useState<number | null>(null);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [createResult, setCreateResult] = useState<TenantCreateResultVO | null>(null);
  const refresh = () => qc.invalidateQueries({ queryKey: ['tenants'] });

  const handleStatus = async (id: string | number, checked: boolean) => {
    try {
      // id 是雪花 ID，超过 JS Number 安全整数范围就不能 Number(id)（会精度丢失变成别的 ID）
      await tenantMgmtApi.updateStatus(id, { status: checked ? 1 : 0 });
      message.success(t('common:msg.statusUpdated'));
      refresh();
    } catch {
      refresh(); // 回滚 Switch 状态
    }
  };

  // 新建租户成功后 → 弹出管理员账号密码（保留原 onCreated 流）
  const handleCreated = (result: TenantCreateResultVO) => {
    setFormOpen(false);
    setCreateResult(result);
    setPasswordModalOpen(true);
    refresh();
  };

  const columns: ColumnsType<TenantListVO> = [
    { title: t('table.code'), dataIndex: 'code', key: 'code', width: 140 },
    { title: t('table.name'), dataIndex: 'name', key: 'name', width: 160 },
    { title: t('table.contact'), dataIndex: 'contact', key: 'contact', width: 100 },
    { title: t('common:userCount'), dataIndex: 'userCount', key: 'userCount', width: 80, align: 'center' },
    {
      title: t('table.expireTime'), dataIndex: 'expireTime', key: 'expireTime', width: 120,
      render: (v: string | null) => (v ? v.replace('T', ' ').substring(0, 10) : t('common:neverExpire')),
    },
    {
      title: t('common:status'), dataIndex: 'status', key: 'status', width: 90, align: 'center',
      render: (s: number, r: TenantListVO) => (
        <StatusSwitch
          id={r.id}
          status={s}
          onToggle={handleStatus}
          disabled={!hasPermission('system:tenant:edit')}
        />
      ),
    },
    {
      title: t('common:createTime'), dataIndex: 'createTime', key: 'createTime', width: 170,
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: t('table.action'), key: 'action', width: 130, fixed: 'right',
      render: (_: unknown, r: TenantListVO) => (
        <RowActions items={[
          {
            key: 'view', label: t('common:detail'), icon: <EyeOutlined />,
            onClick: () => setDetailId(r.id),
          },
          {
            key: 'edit', label: t('common:edit'), icon: <EditOutlined />, perm: 'system:tenant:edit',
            onClick: () => { setEditingId(r.id); setFormOpen(true); },
          },
          {
            key: 'config', label: t('action.config'), icon: <SettingOutlined />, perm: 'system:tenant:config',
            onClick: () => setConfigId(r.id),
          },
          {
            key: 'del', label: t('common:delete'), icon: <DeleteOutlined />, perm: 'system:tenant:remove',
            danger: true, confirmText: t('confirm.delete', { name: r.name }),
            onClick: async () => {
              await tenantMgmtApi.remove(r.id);
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
      <ProTable<TenantListVO>
        service={tenantMgmtApi.list}
        queryKey={['tenants']}
        columns={columns}
        rowKey="id"
        scroll={{ x: 1200 }}
        querySchema={[
          { name: 'name', label: t('table.name') },
          { name: 'code', label: t('table.code') },
          {
            name: 'status', label: t('common:status'), type: 'select',
            options: dictOptions(t, DICT_TYPES.normalDisable, { numeric: true }),
          },
        ]}
        toolbar={
          hasPermission('system:tenant:add') ? (
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

      {/* 弹窗（保留手写 FormModal —— TenantFormModal 含 onCreated 密码返回流，CrudFormModal 不覆盖） */}
      <TenantFormModal
        open={formOpen}
        tenantId={editingId}
        onSuccess={() => { setFormOpen(false); refresh(); }}
        onCreated={handleCreated}
        onCancel={() => setFormOpen(false)}
      />
      <TenantDetailModal
        open={!!detailId}
        tenantId={detailId}
        onCancel={() => setDetailId(null)}
      />
      <TenantConfigModal
        open={!!configId}
        tenantId={configId}
        onSuccess={() => { setConfigId(null); refresh(); }}
        onCancel={() => setConfigId(null)}
      />
      <PasswordShowModal
        open={passwordModalOpen}
        result={createResult}
        onConfirm={() => { setPasswordModalOpen(false); setCreateResult(null); }}
      />
    </>
  );
}
