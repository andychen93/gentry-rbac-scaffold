import request from './request';
import type { PageResult } from './userApi';

// ============ Types ============

export interface RedisInfoVO {
  redisVersion: string;
  redisMode: string;
  uptimeInSeconds: number;
  connectedClients: number;
  usedMemory: number;
  maxMemory: number;
  usedMemoryPercent: number;
  totalKeys: number | null;
  expiresKeys: number | null;
  avgTtl: number | null;
  aofEnabled: string;
  rdbLastSaveTime: string;
  // 扩展指标
  totalCommandsProcessed: number | null;
  totalConnectionsReceived: number | null;
  instantaneousOpsPerSec: number | null;
  keyspaceHits: number | null;
  keyspaceMisses: number | null;
  hitRate: number | null;
  pubsubChannels: number | null;
  pubsubPatterns: number | null;
}

export interface RedisCommandStatVO {
  name: string;
  calls: number;
  usec: number;
  usecPerCall: number;
}

export interface RedisMonitorVO {
  info: RedisInfoVO;
  dbSize: number;
  commandStats: RedisCommandStatVO[];
}

export interface RedisKeyDefineVO {
  keyType: string;
  keyTemplate: string;
  description: string;
  timeout: number;
}

export interface RedisKeyVO {
  key: string;
  type: string;
  value: string | null;
  ttl: number | null;
}

export interface RedisSlowLogVO {
  id: number;
  timestamp: number;
  durationMicros: number;
  args: string[];
  clientAddress: string | null;
  clientName: string | null;
}

// ============ API ============

export const redisMonitorApi = {
  getInfo: () =>
    request.get<unknown, { code: number; data: RedisMonitorVO }>(
      '/api/v1/monitor/redis/info'
    ),

  getKeyDefines: () =>
    request.get<unknown, { code: number; data: RedisKeyDefineVO[] }>(
      '/api/v1/monitor/redis/key-defines'
    ),

  scanKeys: (params: { pattern: string; pageNum: number; pageSize: number }) =>
    request.get<unknown, { code: number; data: PageResult<RedisKeyVO> }>(
      '/api/v1/monitor/redis/keys',
      { params }
    ),

  getKeyValue: (key: string) =>
    request.get<unknown, { code: number; data: RedisKeyVO }>(
      `/api/v1/monitor/redis/keys/${encodeURIComponent(key)}/value`
    ),

  deleteKey: (key: string) =>
    request.delete(`/api/v1/monitor/redis/keys/${encodeURIComponent(key)}`),

  getSlowLog: (limit: number = 20) =>
    request.get<unknown, { code: number; data: RedisSlowLogVO[] }>(
      '/api/v1/monitor/redis/slowlog',
      { params: { limit } }
    ),

  resetSlowLog: () =>
    request.delete('/api/v1/monitor/redis/slowlog'),
};
