import React from 'react';
import { PageSelect } from '../pro/PageSelect';
import { userApi } from '../../services/userApi';
import type { UserListVO } from '../../services/userApi';
import type { ApiResult, PageQuery, PageResult } from '@gentry/kit';
import { useTranslation } from 'react-i18next';

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
const UserTableSelect: React.FC<Props> = ({ value, onChange, placeholder }) => {
  const { t } = useTranslation('common');
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
        { title: t('username'), dataIndex: 'username', width: 120 },
        { title: t('nickname'), dataIndex: 'nickname', width: 120 },
        { title: t('dept'), dataIndex: 'deptName', ellipsis: true },
      ]}
      rowKey="id"
      labelField="username"
      /*
       * 按昵称搜索（后端 UserQueryDTO.nickname → u.nickname LIKE）。
       * 注意搜索字段是 nickname，但回填/提交的值仍是 username ——
       * 日志表 operator 存的是用户名，值必须能直接进查询参数；
       * 昵称只用来「找人」，不参与过滤。
       */
      searchField="nickname"
      value={pseudoRecord}
      onChange={(record) => onChange?.(record ? record.username : undefined)}
      // 兜底值放函数体、不放默认参数：默认参数在组件外求值，那里没有 t
      placeholder={placeholder ?? t('placeholder.searchUser')}
      allowClear
      popoverWidth={460}
    />
  );
};

export default UserTableSelect;
export { UserTableSelect };
