import request from './request';
import type { PageResult } from '../types/api';

export interface NotificationVO {
  // 雪花 ID：后端序列化成字符串防 JS 精度丢失，别 Number() 它
  id: number | string;
  tenantId: number | string;
  userId?: number | string | null;
  type: string;
  /** 1紧急 2严重 3一般 4提示 */
  level: number;
  title: string;
  content?: string | null;
  bizRef?: string | null;
  /** 实际下发渠道，如 "INAPP" / "INAPP,SMS" */
  channels?: string | null;
  /** 0未读 1已读 */
  readStatus: number;
  /** 后端 JacksonConfig 已统一格式化为 yyyy-MM-dd HH:mm:ss，前端不需要再解析 */
  createTime?: string | null;
}

export interface NotificationPublishDTO {
  /** 不传 = 当前租户内广播 */
  userId?: number | string;
  type?: string;
  /** 1紧急 2严重 3一般 4提示；≤2 会额外触发短信 */
  level?: number;
  title: string;
  content?: string;
  bizRef?: string;
}

export const notificationApi = {
  list: (params: { readStatus?: number; type?: string; pageNum?: number; pageSize?: number } = {}) =>
    request.get<any, { code: number; data: PageResult<NotificationVO> }>('/api/v1/notifications', { params }),

  unreadCount: () =>
    request.get<any, { code: number; data: number }>('/api/v1/notifications/unread-count'),

  markRead: (id: number | string) =>
    request.put<any, { code: number }>(`/api/v1/notifications/${id}/read`),

  markAllRead: () =>
    request.put<any, { code: number; data: number }>('/api/v1/notifications/read-all'),

  /** 发布通知（需 notice:publish） */
  publish: (data: NotificationPublishDTO) =>
    request.post<any, { code: number; data: NotificationVO }>('/api/v1/notifications', data),
};
