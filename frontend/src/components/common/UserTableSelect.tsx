import React from 'react';
import { PageSelect } from '../pro/PageSelect';
import { userApi } from '../../services/userApi';
import type { UserListVO } from '../../services/userApi';
import type { ApiResult, PageQuery, PageResult } from '../../types/api';

interface Props {
  /** 受控值：用户名字符串（不是 userId —— 日志表存的就是 username） */
  value?: string;
  onChange?: (value?: string) => void;
  placeholder?: string;
}

/**
 * 用户「下拉 table」选择器（点开是分页表格 + 远程模糊搜索）。
 *
 * 值语义刻意选 username 而不是 id：
 * sys_oper_log.operator / sys_login_log.username 存的都是用户名字符串，
 * 后端按 `LIKE '%operator%'` 过滤，传 id 查不到任何东西。
 *
 * 受控接口（value/onChange）是为了能直接放进 QueryForm 的 type='node' 字段，
 * 由 Form.Item 注入。
 */
const UserTableSelect: React.FC<Props> = ({ value, onChange, placeholder = '点击选择用户' }) => {
  // PageSelect 的 value 是「整条记录」，这里用只含 username 的伪记录回显，
  // 避免为了显示一个名字去额外查一次用户详情
  const pseudoRecord = value ? ({ username: value } as UserListVO) : null;

  return (
    <PageSelect<UserListVO>
      cacheKey="userTableSelect"
      service={(params: PageQuery) =>
        userApi.list(params) as unknown as Promise<ApiResult<PageResult<UserListVO>>>
      }
      columns={[
        { title: '用户名', dataIndex: 'username', width: 120 },
        { title: '昵称', dataIndex: 'nickname', width: 120 },
        { title: '部门', dataIndex: 'deptName', ellipsis: true },
      ]}
      rowKey="id"
      labelField="username"
      searchField="username"
      value={pseudoRecord}
      onChange={(record) => onChange?.(record ? record.username : undefined)}
      placeholder={placeholder}
      allowClear
      popoverWidth={460}
    />
  );
};

export default UserTableSelect;
export { UserTableSelect };
