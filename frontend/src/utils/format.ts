import type { TFunction } from 'i18next';

/**
 * 展示层格式化。
 *
 * **时长/TTL 函数显式收 `t`，不去 import i18n 单例**。
 * 这些函数在渲染路径上被调用，如果内部读单例，那么「只用了 formatTtl、自己没有
 * useTranslation」的组件在切语言时不会重渲染，会留在旧语言上 —— 那是最难发现的一类
 * i18n 缺陷。把 `t` 提到参数里，调用方必然持有 `useTranslation()` 的返回值，
 * 语言一变组件必然重渲染。
 *
 * 时长的译文是**整句**（`{{d}}天{{h}}时{{m}}分` / `{{d}}d {{h}}h {{m}}m`），
 * 不是「数字 + 单位」逐段拼。逐段拼要么在英文里丢空格、要么得再引一个分隔符词条，
 * 而且语序被代码锁死；整句让译者自由排。
 */

/** 格式化运行时长（秒）为「X天X时X分」 */
export function formatUptime(seconds: number | null | undefined, t: TFunction): string {
  if (seconds == null) return '-';
  const d = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (d > 0) return t('duration.dhm', { ns: 'common', d, h, m });
  if (h > 0) return t('duration.hm', { ns: 'common', h, m });
  if (m > 0) return t('duration.m', { ns: 'common', m });
  return t('duration.s', { ns: 'common', s: seconds });
}

/** 格式化字节数为可读文本。单位是 SI 符号，不需要翻译 */
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
 *  其它 → 「X时X分」/「X分X秒」/「X秒」
 */
export function formatTtl(ttl: number | null | undefined, t: TFunction): string {
  if (ttl == null) return '-';
  if (ttl === -1) return t('neverExpire', { ns: 'common' });
  if (ttl === -2) return t('expired', { ns: 'common' });
  if (ttl < 60) return t('duration.s', { ns: 'common', s: ttl });
  if (ttl < 3600) return t('duration.ms', { ns: 'common', m: Math.floor(ttl / 60), s: ttl % 60 });
  return t('duration.hm', {
    ns: 'common',
    h: Math.floor(ttl / 3600),
    m: Math.floor((ttl % 3600) / 60),
  });
}

/**
 * 格式化 Unix 时间戳为本地时间串。
 *
 * 用 `toLocaleString()` 的无参形式：它跟随**浏览器**语言，而不是站内选择的语言。
 * 这是刻意的 —— 时间显示属于系统区域设置，用户在 Chrome 里配的格式偏好优先。
 */
export function formatTimestamp(ts: string | number | null | undefined): string {
  if (ts == null || ts === '') return '-';
  const n = typeof ts === 'string' ? Number(ts) : ts;
  if (!Number.isFinite(n) || n <= 0) return '-';
  const d = new Date(n * 1000);
  return d.toLocaleString();
}
