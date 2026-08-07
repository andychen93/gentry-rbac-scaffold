import request from './request';

export interface OperLogListVO {
  id: number; module: string; type: string; title: string; operator: string;
  operatorIp: string; location: string; status: number; costTime: number; operateTime: string;
}
export interface OperLogDetailVO extends OperLogListVO {
  operatorId: number; method: string; requestUrl: string; requestParams: string;
  responseResult: string; errorMsg: string;
}
export interface LoginLogListVO {
  id: number; username: string; loginType: string; loginIp: string; location: string;
  browser: string; os: string; status: number; message: string; loginTime: string;
}
export interface LoginLogDetailVO extends LoginLogListVO {
  deviceType: string; userAgent: string;
}
export interface OnlineUserVO {
  tokenId: string; userId: number; username: string; nickname: string; deptName: string;
  loginIp: string; location: string; browser: string; os: string; loginTime: string;
}
interface PageResult<T> { list: T[]; total: number; pageNum: number; pageSize: number; pages: number; }

export const logApi = {
  listOperLogs: (params: any) => request.get<any, { code: number; data: PageResult<OperLogListVO> }>('/api/v1/logs/operation', { params }),
  getOperLogDetail: (id: number) => request.get<any, { code: number; data: OperLogDetailVO }>(`/api/v1/logs/operation/${id}`),
  exportOperLogs: (params: any) => downloadCsv('/api/v1/logs/operation/export', params, 'oper_logs.csv'),
  cleanOperLogs: (data: { beforeDays: number }) => request.delete('/api/v1/logs/operation', { data }),
  listLoginLogs: (params: any) => request.get<any, { code: number; data: PageResult<LoginLogListVO> }>('/api/v1/logs/login', { params }),
  getLoginLogDetail: (id: number) => request.get<any, { code: number; data: LoginLogDetailVO }>(`/api/v1/logs/login/${id}`),
  exportLoginLogs: (params: any) => downloadCsv('/api/v1/logs/login/export', params, 'login_logs.csv'),
  cleanLoginLogs: (data: { beforeDays: number }) => request.delete('/api/v1/logs/login', { data }),
  listOnlineUsers: (params?: { username?: string }) => request.get<any, { code: number; data: OnlineUserVO[] }>('/api/v1/online-users', { params }),
  forceLogout: (tokenId: string) => request.delete(`/api/v1/online-users/${tokenId}`),
};

async function downloadCsv(url: string, params: Record<string, unknown>, filename: string) {
  const query = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, String(value));
    }
  });
  const token = localStorage.getItem('precision_token');
  const response = await fetch(`${url}?${query.toString()}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!response.ok) {
    throw new Error('导出失败');
  }
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
