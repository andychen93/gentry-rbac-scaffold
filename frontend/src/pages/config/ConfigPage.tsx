import { useState } from 'react';
import { Button, Modal, Form, Input, Select, Space, Tag, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQueryClient } from '@tanstack/react-query';
import { ProTable, RowActions } from '../../components/pro';
import { configApi } from '../../services/configApi';
import type { ConfigVO } from '../../services/configApi';
import { useUserStore } from '../../stores/userStore';

/**
 * 系统参数配置页（key-value CRUD + 缓存刷新）。
 */
export default function ConfigPage() {
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
      message.success('缓存已刷新');
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
        message.success('编辑成功');
      } else {
        await configApi.create(values);
        message.success('新增成功');
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
    { title: '参数名称', dataIndex: 'configName', key: 'configName', width: 160 },
    { title: '参数键', dataIndex: 'configKey', key: 'configKey', width: 220 },
    { title: '参数值', dataIndex: 'configValue', key: 'configValue', width: 200 },
    {
      title: '类型', dataIndex: 'configType', key: 'configType', width: 80,
      render: (t: string) => (t === 'Y' ? <Tag color="blue">系统</Tag> : <Tag>业务</Tag>),
    },
    { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
    {
      title: '操作', key: 'action', width: 90, fixed: 'right',
      render: (_: unknown, r: ConfigVO) => (
        <RowActions items={[
          {
            key: 'edit', label: '编辑', icon: <EditOutlined />, perm: 'system:config:edit',
            onClick: () => { setEditingId(r.id); form.setFieldsValue(r); setFormOpen(true); },
          },
          {
            key: 'del', label: '删除', icon: <DeleteOutlined />, danger: true, perm: 'system:config:remove',
            confirmText: `确定删除参数「${r.configKey}」？`,
            onClick: async () => { await configApi.remove(r.id); message.success('删除成功'); refresh(); },
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
          { name: 'configKey', label: '参数键' },
          { name: 'configName', label: '参数名称' },
        ]}
        toolbar={
          /* 必须用 Space 撑间距：ProTable 的 toolbar 只是塞进一个普通 div，
             不会替你加 gap，裸 Fragment 会让两个按钮贴在一起 */
          <Space>
            {hasPermission('system:config:add') && (
              <Button type="primary" icon={<PlusOutlined />}
                onClick={() => { setEditingId(null); form.resetFields(); setFormOpen(true); }}>
                新增参数
              </Button>
            )}
            {hasPermission('system:config:refresh') && (
              <Button icon={<ReloadOutlined />} loading={refreshing} onClick={handleRefreshCache}>
                刷新缓存
              </Button>
            )}
          </Space>
        }
      />

      <Modal title={editingId ? '编辑参数' : '新增参数'} open={formOpen} onOk={handleSave}
        onCancel={() => setFormOpen(false)} confirmLoading={saving} width={560}>
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="configName" label="参数名称" rules={[{ max: 100, message: '最长100字符' }]}>
            <Input placeholder="如：登录失败最大次数" />
          </Form.Item>
          <Form.Item name="configKey" label="参数键"
            rules={[{ required: true, message: '请输入参数键' }, { max: 100 }]}>
            <Input placeholder="如 sys.login.maxFailCount" disabled={editingId !== null} />
          </Form.Item>
          <Form.Item name="configValue" label="参数值" rules={[{ max: 500 }]}>
            <Input placeholder="参数值" />
          </Form.Item>
          <Form.Item name="configType" label="类型">
            <Select placeholder="选择类型" allowClear>
              <Select.Option value="N">业务</Select.Option>
              <Select.Option value="Y">系统</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="remark" label="备注" rules={[{ max: 500 }]}>
            <Input.TextArea rows={2} placeholder="备注说明" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
