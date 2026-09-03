import request from './request';

// ========== VO 类型 ==========

export interface RoleListVO {
  id: number;
  roleCode: string;
  roleName: string;
  /**
   * 由后端 RoleI18nKeyResolver 从 roleCode 派生（role.{roleCode 小写}），仅内置角色
   * （ADMIN/USER）在语言包里有对应译文；自建角色查不到，前端回退显示 roleName。
   */
  i18nKey?: string | null;
  dataScope: number;
  userCount: number;
  sort: number;
  status: number;
  createTime: string;
}

export interface RoleDetailVO {
  id: number;
  roleCode: string;
  roleName: string;
  i18nKey?: string | null;
  dataScope: number;
  sort: number;
  status: number;
  remark: string | null;
  // 运行时是字符串数组（后端 Long 序列化成字符串防 JS 精度丢失），
  // 用 (number|string)[] 提醒调用方不要 Number()/map(Number) 这两个字段
  menuIds: (number | string)[];
  deptIds: (number | string)[];
  createTime: string;
  updateTime: string;
}

export interface RoleVO {
  id: number;
  roleCode: string;
  roleName: string;
  i18nKey?: string | null;
  dataScope: number;
  sort: number;
  status: number;
  remark: string | null;
  createTime: string;
}

// ========== DTO 类型 ==========

export interface RoleQueryParams {
  pageNum?: number;
  pageSize?: number;
  roleName?: string;
  roleCode?: string;
  status?: number;
}

export interface RoleCreateDTO {
  roleCode: string;
  roleName: string;
  dataScope?: number;
  sort: number;
  status?: number;
  remark?: string;
}

export interface RoleUpdateDTO {
  roleName: string;
  dataScope?: number;
  sort: number;
  status?: number;
  remark?: string;
}

export interface RoleMenuAssignDTO {
  menuIds: (number | string)[];
}

export interface RoleDataScopeDTO {
  dataScope: number;
  deptIds: (number | string)[];
}

export interface RoleStatusDTO {
  status: number;
}

export interface RoleUserAssignDTO {
  // 雪花 ID：运行时是字符串，别 map(Number)
  userIds: (number | string)[];
}

// ========== 分页结果 ==========

interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

// ========== API 接口 ==========

export const roleApi = {
  /** ROLE-001 角色列表查询 */
  list: (params?: RoleQueryParams) =>
    request.get<any, { code: number; data: PageResult<RoleListVO> }>('/api/v1/roles', { params }),

  /** ROLE-002 角色详情 */
  detail: (id: number | string) =>
    request.get<any, { code: number; data: RoleDetailVO }>(`/api/v1/roles/${id}`),

  /** ROLE-003 新增角色 */
  create: (data: RoleCreateDTO) =>
    request.post<any, { code: number; data: RoleVO }>('/api/v1/roles', data),

  /** ROLE-004 编辑角色 */
  update: (id: number | string, data: RoleUpdateDTO) =>
    request.put(`/api/v1/roles/${id}`, data),

  /** ROLE-005 删除角色 */
  remove: (id: number | string) =>
    request.delete(`/api/v1/roles/${id}`),

  /** ROLE-006 分配菜单权限 */
  assignMenus: (id: number | string, data: RoleMenuAssignDTO) =>
    request.put(`/api/v1/roles/${id}/menus`, data),

  /** ROLE-007 设置数据权限 */
  updateDataScope: (id: number | string, data: RoleDataScopeDTO) =>
    request.put(`/api/v1/roles/${id}/data-scope`, data),

  /** ROLE-009 切换角色状态 */
  updateStatus: (id: number | string, data: RoleStatusDTO) =>
    request.put(`/api/v1/roles/${id}/status`, data),

  /** 角色下拉选项（启用角色，供用户分配等场景） */
  options: () =>
    request.get<any, { code: number; data: { id: number; roleCode: string; roleName: string; i18nKey?: string | null }[] }>('/api/v1/roles/options'),

  /** ROLE-008 查看角色已绑定的用户 ID（返回字符串形态的雪花 ID） */
  listUserIds: (id: number | string) =>
    request.get<any, { code: number; data: (number | string)[] }>(`/api/v1/roles/${id}/users`),

  /** ROLE-011 绑定用户（全量覆盖） */
  assignUsers: (id: number | string, data: RoleUserAssignDTO) =>
    request.put(`/api/v1/roles/${id}/users`, data),
};
