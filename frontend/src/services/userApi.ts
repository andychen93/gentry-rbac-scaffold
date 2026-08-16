import request from './request';
import type { MenuNavItem } from '../types/menu';

export interface LoginDTO {
  tenantCode?: string;
  username: string;
  password: string;
  uuid?: string;      // 验证码标识
  captcha?: string;   // 验证码答案
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
  passwordExpired?: boolean;
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

  // 获取登录验证码（公开接口，返回 data URI 图片 + uuid）
  getCaptcha: () =>
    request.get<any, { code: number; data: { uuid: string; img: string } }>('/api/v1/auth/captcha'),

  // 获取当前用户完整资料（含手机/邮箱/职务等，仅需登录）
  getProfile: () =>
    request.get<any, { code: number; data: UserDetailVO }>('/api/v1/auth/profile'),

  // 修改当前用户个人资料
  updateProfile: (data: { nickname?: string; phone?: string; email?: string; gender?: number; postName?: string }) =>
    request.put('/api/v1/auth/profile', data),
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

  // 导出用户（Excel，按当前查询条件）
  exportUsers: (params: Record<string, unknown>) =>
    downloadExcel('/api/v1/users/export', params, 'users.xlsx'),

  // 下载导入模板
  downloadTemplate: () =>
    downloadExcel('/api/v1/users/import/template', {}, 'user_import_template.xlsx'),

  // 导入用户（Excel），返回成功/失败统计与错误明细
  importUsers: (file: File) =>
    uploadFile<UserImportResult>('/api/v1/users/import', file),
  /** 用户下拉选项（租户内启用用户），供「角色→绑定用户」穿梭框取候选 */
  options: () =>
    request.get<any, { code: number; data: UserOptionVO[] }>('/api/v1/users/options'),
};

/** 用户下拉选项（id 为字符串形态的雪花 ID） */
export interface UserOptionVO {
  id: number | string;
  username: string;
  nickname: string | null;
  deptName: string | null;
}

/** 用户导入结果 */
export interface UserImportResult {
  success: number;
  fail: number;
  errors: { row: number; username: string; msg: string }[];
}

// 租户选项接口（公开，登录页下拉框）
export interface TenantOptionVO {
  code: string;
  name: string;
}

export const tenantApi = {
  options: () =>
    request.get<any, { code: number; data: TenantOptionVO[] }>('/api/v1/tenants/options'),
};

/** 下载二进制（Excel），绕过 axios 拦截器（拦截器会把响应解包成 JSON） */
async function downloadExcel(url: string, params: Record<string, unknown>, filename: string) {
  const query = new URLSearchParams();
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') query.append(k, String(v));
  });
  const token = localStorage.getItem('precision_token');
  const sep = url.includes('?') ? '&' : '?';
  const response = await fetch(`${url}${sep}${query.toString()}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!response.ok) throw new Error('下载失败');
  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
}

/** 上传文件（FormData），返回后端 R.data */
async function uploadFile<T>(url: string, file: File): Promise<T> {
  const form = new FormData();
  form.append('file', file);
  const token = localStorage.getItem('precision_token');
  const response = await fetch(url, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    body: form,
  });
  const json = await response.json();
  if (json.code !== 0) throw new Error(json.message || '上传失败');
  return json.data as T;
}
