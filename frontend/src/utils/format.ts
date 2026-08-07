/** 格式化运行时长（秒）为 "X天X时X分" */
export function formatUptime(seconds: number | null | undefined): string {
  if (seconds == null) return '-';
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (days > 0) return `${days}天${hours}时${minutes}分`;
  if (hours > 0) return `${hours}时${minutes}分`;
  if (minutes > 0) return `${minutes}分`;
  return `${seconds}秒`;
}

/** 格式化字节数为可读文本 */
export function formatBytes(bytes: number | null | undefined): string {
  if (bytes == null) return '-';
  if (bytes === 0) return '0 B';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

/**
 * 格式化 TTL（秒）：
 *  -1 → 永不过期
 *  -2 → 已过期/不存在
 *  其它 → "X时X分X秒"
 */
export function formatTtl(ttl: number | null | undefined): string {
  if (ttl == null) return '-';
  if (ttl === -1) return '永不过期';
  if (ttl === -2) return '已过期';
  if (ttl < 60) return `${ttl}秒`;
  if (ttl < 3600) return `${Math.floor(ttl / 60)}分${ttl % 60}秒`;
  return `${Math.floor(ttl / 3600)}时${Math.floor((ttl % 3600) / 60)}分`;
}

/** 格式化 Unix 时间戳为本地时间串 */
export function formatTimestamp(ts: string | number | null | undefined): string {
  if (ts == null || ts === '') return '-';
  const n = typeof ts === 'string' ? Number(ts) : ts;
  if (!Number.isFinite(n) || n <= 0) return '-';
  const d = new Date(n * 1000);
  return d.toLocaleString();
}
