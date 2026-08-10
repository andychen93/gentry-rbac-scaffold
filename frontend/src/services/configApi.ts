import request from './request';

export interface ConfigVO {
  id: number;
  configName: string;
  configKey: string;
  configValue: string;
  configType: string; // Y=系统 N=业务
  remark: string;
  createTime: string;
}

interface ConfigQueryParams {
  pageNum?: number;
  pageSize?: number;
  configKey?: string;
  configName?: string;
}

interface PageResult<T> { list: T[]; total: number; pageNum: number; pageSize: number; pages: number; }

export const configApi = {
  list: (params: ConfigQueryParams) =>
    request.get<any, { code: number; data: PageResult<ConfigVO> }>('/api/v1/configs', { params }),
  create: (data: Partial<ConfigVO>) =>
    request.post<any, { code: number; data: ConfigVO }>('/api/v1/configs', data),
  update: (id: number | string, data: Partial<ConfigVO>) =>
    request.put(`/api/v1/configs/${id}`, data),
  remove: (id: number | string) =>
    request.delete(`/api/v1/configs/${id}`),
  refreshCache: () => request.delete('/api/v1/configs/cache'),
};
