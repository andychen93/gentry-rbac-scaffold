import request from './request';

// ========== VO 类型 ==========

export interface RoleListVO {
  id: number;
  roleCode: string;
  roleName: string;
  dataScope: number;
  dataScopeName: string;
  userCount: number;
  sort: number;
  status: number;
  createTime: string;
}

export interface RoleDetailVO {
  id: number;
  roleCode: string;
  roleName: string;
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
};
