import { useState } from 'react';
import { Button, message } from 'antd';
import {
  PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SettingOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQueryClient } from '@tanstack/react-query';
import { ProTable, StatusSwitch, RowActions } from '../../components/pro';
import { tenantMgmtApi } from '../../services/tenantApi';
import type { TenantListVO, TenantCreateResultVO } from '../../services/tenantApi';
import { useUserStore } from '../../stores/userStore';
import TenantFormModal from './TenantFormModal';
import TenantDetailModal from './TenantDetailModal';
import TenantConfigModal from './TenantConfigModal';
import PasswordShowModal from './PasswordShowModal';

export default function TenantPage() {
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
      await tenantMgmtApi.updateStatus(Number(id), { status: checked ? 1 : 0 });
      message.success('状态更新成功');
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
    { title: '租户编码', dataIndex: 'code', key: 'code', width: 140 },
    { title: '租户名称', dataIndex: 'name', key: 'name', width: 160 },
    { title: '联系人', dataIndex: 'contact', key: 'contact', width: 100 },
    { title: '用户数', dataIndex: 'userCount', key: 'userCount', width: 80, align: 'center' },
    {
      title: '到期时间', dataIndex: 'expireTime', key: 'expireTime', width: 120,
      render: (v: string | null) => v ? v.replace('T', ' ').substring(0, 10) : '永不过期',
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 90, align: 'center',
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
      title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170,
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: '操作', key: 'action', width: 240, fixed: 'right',
      render: (_: unknown, r: TenantListVO) => (
        <RowActions items={[
          {
            key: 'view', label: '详情', icon: <EyeOutlined />,
            onClick: () => setDetailId(r.id),
          },
          {
            key: 'edit', label: '编辑', icon: <EditOutlined />, perm: 'system:tenant:edit',
            onClick: () => { setEditingId(r.id); setFormOpen(true); },
          },
          {
            key: 'config', label: '配置', icon: <SettingOutlined />, perm: 'system:tenant:config',
            onClick: () => setConfigId(r.id),
          },
          {
            key: 'del', label: '删除', icon: <DeleteOutlined />, perm: 'system:tenant:remove',
            danger: true, confirmText: `确定删除租户「${r.name}」？`,
            onClick: async () => {
              await tenantMgmtApi.remove(r.id);
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
      <ProTable<TenantListVO>
        service={tenantMgmtApi.list}
        queryKey={['tenants']}
        columns={columns}
        rowKey="id"
        scroll={{ x: 1200 }}
        querySchema={[
          { name: 'name', label: '租户名称' },
          { name: 'code', label: '租户编码' },
          {
            name: 'status', label: '状态', type: 'select',
            options: [{ label: '正常', value: 1 }, { label: '已禁用', value: 0 }],
          },
        ]}
        toolbar={
          hasPermission('system:tenant:add') ? (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => { setEditingId(null); setFormOpen(true); }}
            >
              新增租户
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
