import request from './request';
import type { MenuNavItem } from '../types/menu';

export interface LoginDTO {
  tenantCode?: string;
  username: string;
  password: string;
}

export interface UserCreateDTO {
  username: string;
  nickname: string;
  deptId?: number;
  phone?: string;
  email?: string;
  gender?: number;
  postName?: string;
  password: string;
  status?: number;
  roleIds?: number[];
  remark?: string;
}

export interface UserUpdateDTO {
  nickname: string;
  deptId?: number;
  phone?: string;
  email?: string;
  gender?: number;
  postName?: string;
  status?: number;
  roleIds?: number[];
  remark?: string;
}

export interface UserQueryParams {
  pageNum: number;
  pageSize: number;
  deptId?: number;
  username?: string;
  phone?: string;
  status?: number;
}

export interface UserListVO {
  id: number;
  username: string;
  nickname: string;
  phone: string;
  email: string;
  gender: number;
  postName: string;
  deptId: number;
  deptName: string;
  status: number;
  createTime: string;
  roles: { id: number; roleName: string; roleCode: string }[];
}

export interface UserDetailVO {
  id: number;
  username: string;
  nickname: string;
  phone: string;
  email: string;
  gender: number;
  postName: string;
  avatar: string;
  deptId: number;
  deptName: string;
  status: number;
  remark: string;
  roleIds: number[];
  roles: { id: number; roleName: string; roleCode: string }[];
  loginIp: string;
  loginDate: string;
  pwdUpdateTime: string;
  createTime: string;
  updateTime: string;
}

export interface LoginVO {
  token: string;
  userInfo: {
    userId: number;
    username: string;
    nickname: string;
    avatar: string;
    deptId: number;
    deptName: string;
    roles: { id: number; roleCode: string; roleName: string; dataScope: number }[];
    permissions: string[];
    menus: MenuNavItem[];
  };
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

// 认证接口
export const authApi = {
  login: (data: LoginDTO) =>
    request.post<any, { code: number; data: LoginVO }>('/api/v1/auth/login', data),

  logout: () => request.post('/api/v1/auth/logout'),

  getUserInfo: () =>
    request.get<any, { code: number; data: LoginVO['userInfo'] }>('/api/v1/auth/user-info'),

  updatePassword: (data: { oldPassword: string; newPassword: string }) =>
    request.put('/api/v1/auth/password', data),
};

// 用户管理接口
export const userApi = {
  list: (params: UserQueryParams) =>
    request.get<any, { code: number; data: PageResult<UserListVO> }>('/api/v1/users', { params }),

  detail: (id: number) =>
    request.get<any, { code: number; data: UserDetailVO }>(`/api/v1/users/${id}`),

  create: (data: UserCreateDTO) =>
    request.post<any, { code: number; data: UserDetailVO }>('/api/v1/users', data),

  update: (id: number, data: UserUpdateDTO) =>
    request.put(`/api/v1/users/${id}`, data),

  remove: (id: number) =>
    request.delete(`/api/v1/users/${id}`),

  resetPassword: (id: number, data: { newPassword: string }) =>
    request.put(`/api/v1/users/${id}/password/reset`, data),

  // roleIds 用 (number | string)[] 是因为雪花 ID 超过 JS Number 安全整数范围（2^53-1），
  // 调用方必须原样传字符串，不能 map(Number)
  assignRoles: (id: number | string, data: { roleIds: (number | string)[] }) =>
    request.put(`/api/v1/users/${id}/roles`, data),

  updateStatus: (id: number | string, data: { status: number }) =>
    request.put(`/api/v1/users/${id}/status`, data),
};

// 租户选项接口（公开，登录页下拉框）
export interface TenantOptionVO {
  code: string;
  name: string;
}

export const tenantApi = {
  options: () =>
    request.get<any, { code: number; data: TenantOptionVO[] }>('/api/v1/tenants/options'),
};
