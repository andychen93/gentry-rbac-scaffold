import { message } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import { ProTable, RowActions } from '../../components/pro';
import { logApi, OnlineUserVO } from '../../services/logApi';
import type { ApiResult, PageQuery, PageResult } from '@gentry/kit';

/**
 * 在线用户 service 适配器：
 * 后端 `/api/v1/online-users` 返回 List（无分页），此处客户端切片成 PageResult，
 * 以满足 ProTable service 签名（参考 vendorApi.listPaged 模式）。
 */
async function listOnlineUsersPaged(
  params: PageQuery,
): Promise<ApiResult<PageResult<OnlineUserVO>>> {
  const { pageNum = 1, pageSize = 10, username } = params;
  const res = await logApi.listOnlineUsers(
    username !== undefined && username !== '' ? { username: String(username) } : undefined,
  );
  const all = res.data ?? [];
  const total = all.length;
  const start = (pageNum - 1) * pageSize;
  const list = all.slice(start, start + pageSize);
  return {
    code: 0,
    message: 'ok',
    data: {
      list,
      total,
      pageNum,
      pageSize,
      pages: pageSize > 0 ? Math.ceil(total / pageSize) : 0,
    },
  };
}

export default function OnlineUserPage() {
  const { t } = useTranslation(['log', 'common']);
  const qc = useQueryClient();

  const handleForceLogout = async (record: OnlineUserVO) => {
    try {
      await logApi.forceLogout(record.tokenId);
      message.success(t('online.msg.forced'));
      qc.invalidateQueries({ queryKey: ['online-users'] });
    } catch { /* handled by interceptor */ }
  };

  const columns: ColumnsType<OnlineUserVO> = [
    { title: t('common:username'), dataIndex: 'username', key: 'username', width: 110 },
    { title: t('common:nickname'), dataIndex: 'nickname', key: 'nickname', width: 110 },
    { title: t('common:dept'), dataIndex: 'deptName', key: 'deptName', width: 120 },
    { title: t('common:loginIp'), dataIndex: 'loginIp', key: 'loginIp', width: 130 },
    { title: t('common:location'), dataIndex: 'location', key: 'location', width: 110 },
    { title: t('common:browser'), dataIndex: 'browser', key: 'browser', width: 110 },
    { title: t('common:os'), dataIndex: 'os', key: 'os', width: 120 },
    {
      title: t('common:loginTime'), dataIndex: 'loginTime', key: 'loginTime', width: 170,
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: t('table.action'), key: 'action', width: 70, fixed: 'right',
      render: (_: unknown, record: OnlineUserVO) => (
        <RowActions items={[
          {
            key: 'force', label: t('online.action.force'), icon: <LogoutOutlined />, danger: true,
            confirmText: t('online.confirm.force', { username: record.username }),
            onClick: () => handleForceLogout(record),
          },
        ]} />
      ),
    },
  ];

  return (
    <ProTable<OnlineUserVO>
      service={listOnlineUsersPaged}
      queryKey={['online-users']}
      columns={columns}
      rowKey="tokenId"
      scroll={{ x: 1100 }}
      querySchema={[
        { name: 'username', label: t('common:username') },
      ]}
    />
  );
}
