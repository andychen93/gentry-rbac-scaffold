import request from './request';
import type { PageResult } from './userApi';

// ========== VO 类型 ==========

export interface TenantListVO {
  id: number;
  code: string;
  name: string;
  contact: string;
  phone: string;
  userCount: number;
  deviceCount: number;
  expireTime: string | null;
  status: number;
  createTime: string;
}

export interface TenantStatistics {
  userCount: number;
  deptCount: number;
  roleCount: number;
}

export interface TenantDetailVO {
  id: number;
  code: string;
  name: string;
  contact: string;
  phone: string;
  email: string;
  address: string;
  logo: string;
  domain: string;
  expireTime: string | null;
  accountLimit: number;
  deviceLimit: number;
  status: number;
  remark: string;
  config: Record<string, any> | null;
  createTime: string;
  updateTime: string;
  statistics: TenantStatistics;
}

export interface TenantCreateResultVO {
  tenantId: number;
  tenantCode: string;
  adminUsername: string;
  adminPassword: string;
  message: string;
}

// ========== DTO 类型 ==========

export interface TenantQueryParams {
  pageNum: number;
  pageSize: number;
  name?: string;
  code?: string;
  contact?: string;
  phone?: string;
  status?: number;
}

export interface TenantCreateDTO {
  code: string;
  name: string;
  contact?: string;
  phone?: string;
  email?: string;
  expireTime?: string | null;
  remark?: string;
}

export interface TenantUpdateDTO {
  name: string;
  contact?: string;
  phone?: string;
  email?: string;
  expireTime?: string | null;
  remark?: string;
}

export interface TenantConfigDTO {
  maxDevices?: number;
  maxUsers?: number;
  dataRetentionDays?: number;
  videoEnabled?: boolean;
  alarmEnabled?: boolean;
  reportEnabled?: boolean;
  mapProvider?: string;
}

export interface TenantStatusDTO {
  status: number;
}

// ========== API 接口 ==========

export const tenantMgmtApi = {
  /** TENANT-001 租户列表查询 */
  list: (params: TenantQueryParams) =>
    request.get<any, { code: number; data: PageResult<TenantListVO> }>('/api/v1/tenants', { params }),

  /** TENANT-002 租户详情 */
  detail: (id: number) =>
    request.get<any, { code: number; data: TenantDetailVO }>(`/api/v1/tenants/${id}`),

  /** TENANT-003 新增租户 */
  create: (data: TenantCreateDTO) =>
    request.post<any, { code: number; data: TenantCreateResultVO }>('/api/v1/tenants', data),

  /** TENANT-004 编辑租户 */
  update: (id: number, data: TenantUpdateDTO) =>
    request.put(`/api/v1/tenants/${id}`, data),

  /** TENANT-005 删除租户 */
  remove: (id: number) =>
    request.delete(`/api/v1/tenants/${id}`),

  /** TENANT-006 租户配置 */
  updateConfig: (id: number, data: TenantConfigDTO) =>
    request.put(`/api/v1/tenants/${id}/config`, data),

  /** TENANT-007 切换租户状态 */
  updateStatus: (id: number, data: TenantStatusDTO) =>
    request.put(`/api/v1/tenants/${id}/status`, data),
};
