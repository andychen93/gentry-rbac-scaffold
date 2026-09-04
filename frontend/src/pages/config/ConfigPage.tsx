import { useState } from 'react';
import { Button, Modal, Form, Input, Select, Space, Tag, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ProTable, RowActions } from '@gentry/kit';
import { configApi } from '../../services/configApi';
import type { ConfigVO } from '../../services/configApi';
import { useUserStore } from '../../stores/userStore';

/**
 * 系统参数配置页（key-value CRUD + 缓存刷新）。
 */
export default function ConfigPage() {
  const { t } = useTranslation(['config', 'common']);
  const qc = useQueryClient();
  const hasPermission = useUserStore((s) => s.hasPermission);
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const refresh = () => qc.invalidateQueries({ queryKey: ['configs'] });

  /**
   * 刷新后端参数缓存。
   *
   * 原实现直接 `await` 后无条件弹「缓存已刷新」：请求失败时 await 抛出，
   * 成功提示不会弹，但异常也没人接，变成 onClick 里的未处理 Promise 拒绝；
   * 而且没有 loading，能连点触发多次请求。
   * 顺带重拉一次列表 —— 否则点完界面毫无变化，看不出到底生效没有。
   */
  const handleRefreshCache = async () => {
    setRefreshing(true);
    try {
      await configApi.refreshCache();
      message.success(t('msg.cacheRefreshed'));
      refresh();
    } catch {
      /* 错误提示由 request 拦截器统一弹 */
    } finally {
      setRefreshing(false);
    }
  };

  const handleSave = async () => {
    // 校验失败要直接返回，不能进 loading（validateFields 抛的是 errorFields 对象）
    let values: Record<string, unknown>;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }

    setSaving(true);
    try {
      if (editingId) {
        await configApi.update(editingId, values);
        message.success(t('common:msg.updateSuccess'));
      } else {
        await configApi.create(values);
        message.success(t('common:msg.createSuccess'));
      }
      setFormOpen(false);
      refresh();
    } catch {
      /* 业务错误（如「参数键已存在」）由拦截器弹提示，弹窗保持打开让用户改 */
    } finally {
      setSaving(false);
    }
  };

  const columns: ColumnsType<ConfigVO> = [
    { title: t('table.name'), dataIndex: 'configName', key: 'configName', width: 160 },
    { title: t('table.key'), dataIndex: 'configKey', key: 'configKey', width: 220 },
    { title: t('table.value'), dataIndex: 'configValue', key: 'configValue', width: 200 },
    {
      title: t('common:type'), dataIndex: 'configType', key: 'configType', width: 80,
      // 参数原名叫 t，会遮蔽翻译函数 t —— 改名 configType
      render: (configType: string) =>
        configType === 'Y' ? <Tag color="blue">{t('type.system')}</Tag> : <Tag>{t('type.business')}</Tag>,
    },
    { title: t('common:remark'), dataIndex: 'remark', key: 'remark', ellipsis: true },
    { title: t('common:createTime'), dataIndex: 'createTime', key: 'createTime', width: 180 },
    {
      title: t('table.action'), key: 'action', width: 90, fixed: 'right',
      render: (_: unknown, r: ConfigVO) => (
        <RowActions items={[
          {
            key: 'edit', label: t('common:edit'), icon: <EditOutlined />, perm: 'system:config:edit',
            onClick: () => { setEditingId(r.id); form.setFieldsValue(r); setFormOpen(true); },
          },
          {
            key: 'del', label: t('common:delete'), icon: <DeleteOutlined />, danger: true, perm: 'system:config:remove',
            confirmText: t('confirm.delete', { key: r.configKey }),
            onClick: async () => { await configApi.remove(r.id); message.success(t('common:msg.deleteSuccess')); refresh(); },
          },
        ]} />
      ),
    },
  ];

  return (
    <>
      <ProTable<ConfigVO>
        service={(params) => configApi.list(params)}
        queryKey={['configs']}
        columns={columns}
        rowKey="id"
        scroll={{ x: 1000 }}
        querySchema={[
          { name: 'configKey', label: t('table.key') },
          { name: 'configName', label: t('table.name') },
        ]}
        toolbar={
          /* 必须用 Space 撑间距：ProTable 的 toolbar 只是塞进一个普通 div，
             不会替你加 gap，裸 Fragment 会让两个按钮贴在一起 */
          <Space>
            {hasPermission('system:config:add') && (
              <Button type="primary" icon={<PlusOutlined />}
                onClick={() => { setEditingId(null); form.resetFields(); setFormOpen(true); }}>
                {t('action.create')}
              </Button>
            )}
            {hasPermission('system:config:refresh') && (
              <Button icon={<ReloadOutlined />} loading={refreshing} onClick={handleRefreshCache}>
                {t('action.refreshCache')}
              </Button>
            )}
          </Space>
        }
      />

      <Modal title={editingId ? t('form.title.edit') : t('form.title.create')} open={formOpen} onOk={handleSave}
        onCancel={() => setFormOpen(false)} confirmLoading={saving} width={560}>
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="configName" label={t('table.name')} rules={[{ max: 100, message: t('valid.max100') }]}>
            <Input placeholder={t('form.name.placeholder')} />
          </Form.Item>
          <Form.Item name="configKey" label={t('table.key')}
            rules={[{ required: true, message: t('form.key.required') }, { max: 100 }]}>
            <Input placeholder={t('form.key.placeholder')} disabled={editingId !== null} />
          </Form.Item>
          <Form.Item name="configValue" label={t('table.value')} rules={[{ max: 500 }]}>
            <Input placeholder={t('form.value.placeholder')} />
          </Form.Item>
          <Form.Item name="configType" label={t('common:type')}>
            <Select
              placeholder={t('form.type.placeholder')}
              allowClear
              options={[
                { value: 'N', label: t('type.business') },
                { value: 'Y', label: t('type.system') },
              ]}
            />
          </Form.Item>
          <Form.Item name="remark" label={t('common:remark')} rules={[{ max: 500 }]}>
            <Input.TextArea rows={2} placeholder={t('form.remark.placeholder')} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
