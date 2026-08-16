import { message } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import type { ColumnsType } from 'antd/es/table';
import { ProTable, RowActions } from '../../components/pro';
import { logApi, OnlineUserVO } from '../../services/logApi';
import type { ApiResult, PageQuery, PageResult } from '../../types/api';

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
  const qc = useQueryClient();

  const handleForceLogout = async (record: OnlineUserVO) => {
    try {
      await logApi.forceLogout(record.tokenId);
      message.success('已强制下线');
      qc.invalidateQueries({ queryKey: ['online-users'] });
    } catch { /* handled by interceptor */ }
  };

  const columns: ColumnsType<OnlineUserVO> = [
    { title: '用户名', dataIndex: 'username', key: 'username', width: 110 },
    { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 110 },
    { title: '部门', dataIndex: 'deptName', key: 'deptName', width: 120 },
    { title: '登录IP', dataIndex: 'loginIp', key: 'loginIp', width: 130 },
    { title: '登录地点', dataIndex: 'location', key: 'location', width: 110 },
    { title: '浏览器', dataIndex: 'browser', key: 'browser', width: 110 },
    { title: '操作系统', dataIndex: 'os', key: 'os', width: 120 },
    {
      title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', width: 170,
      render: (v: string) => v?.replace('T', ' '),
    },
    {
      title: '操作', key: 'action', width: 70, fixed: 'right',
      render: (_: unknown, record: OnlineUserVO) => (
        <RowActions items={[
          {
            key: 'force', label: '强退', icon: <LogoutOutlined />, danger: true,
            confirmText: `确定强制下线用户「${record.username}」？这将中断其当前所有操作`,
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
        { name: 'username', label: '用户名' },
      ]}
    />
  );
}
