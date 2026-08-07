import request from './request';

// ========== VO 类型 ==========

export interface DeptTreeVO {
  id: number;
  parentId: number;
  name: string;
  leaderId: number | null;
  leaderName: string | null;
  phone: string | null;
  email: string | null;
  sort: number;
  status: number;
  userCount: number;
  createTime: string;
  children: DeptTreeVO[];
}

export interface DeptVO {
  id: number;
  parentId: number;
  ancestors: string;
  name: string;
  leaderId: number | null;
  leaderName: string | null;
  phone: string | null;
  email: string | null;
  sort: number;
  status: number;
  createTime: string;
}

// ========== DTO 类型 ==========

export interface DeptQueryParams {
  name?: string;
  status?: number;
}

export interface DeptCreateDTO {
  parentId?: number;
  name: string;
  leaderId?: number;
  phone?: string;
  email?: string;
  sort: number;
  status?: number;
}

export interface DeptUpdateDTO {
  parentId?: number;
  name: string;
  leaderId?: number;
  phone?: string;
  email?: string;
  sort: number;
  status?: number;
}

// ========== API 接口 ==========

export const deptApi = {
  /** DEPT-001 部门树查询 */
  tree: (params?: DeptQueryParams) =>
    request.get<any, { code: number; data: DeptTreeVO[] }>('/api/v1/depts', { params }),

  /** DEPT-002 部门详情 */
  detail: (id: number) =>
    request.get<any, { code: number; data: DeptTreeVO }>(`/api/v1/depts/${id}`),

  /** DEPT-003 新增部门 */
  create: (data: DeptCreateDTO) =>
    request.post<any, { code: number; data: DeptVO }>('/api/v1/depts', data),

  /** DEPT-004 编辑部门 */
  update: (id: number, data: DeptUpdateDTO) =>
    request.put(`/api/v1/depts/${id}`, data),

  /** DEPT-005 删除部门 */
  remove: (id: number) =>
    request.delete(`/api/v1/depts/${id}`),
};
