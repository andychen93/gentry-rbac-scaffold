/**
 * 国际化基础配置与语言码转换。
 *
 * 这里刻意不引入 `i18next-browser-languagedetector`：检测逻辑只有
 * 「localStorage → navigator → 默认」三步，`resolveInitialLocale()` 十行写完；
 * 而且真正的优先级还要让服务端的 `UserInfoVO.language` 插队，引入那个包反而要
 * 配一堆 order/caches 去关掉它的默认行为。
 */

/** 支持的语言。加语言时必须同步补 `locales/{lang}/*.json` 与后端 `gentry.i18n.supported-locales` */
export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const;

export type AppLocale = (typeof SUPPORTED_LOCALES)[number];

export const DEFAULT_LOCALE: AppLocale = 'zh-CN';

/** localStorage 键。登录前唯一可用的语言来源 */
export const LOCALE_STORAGE_KEY = 'gentry_locale';

/**
 * 前端（BCP47 连字符）→ 后端（Java 下划线）。
 * `zh-CN` → `zh_CN`
 */
export function toBackendLocale(locale: AppLocale): string {
  return locale.replace('-', '_');
}

/**
 * 任意来源的语言串 → 受支持的 {@link AppLocale}；不受支持返回 null。
 *
 * 接受 `zh-CN` / `zh_CN` / `zh` / `ZH-cn` 等写法。第 ② 步的「仅语言码匹配」与后端
 * `GentryLocaleResolver.matchHeader` 的退化逻辑对称——两边都要能把 `en` 收敛到
 * `en-US`，否则浏览器只发 `en` 时会出现「界面英文而错误提示中文」，那是最难查的一类
 * i18n 缺陷。
 */
export function toAppLocale(raw?: string | null): AppLocale | null {
  if (!raw) return null;
  const normalized = raw.trim().replace('_', '-').toLowerCase();
  if (!normalized) return null;

  // ① 精确匹配（大小写无关）
  const exact = SUPPORTED_LOCALES.find((l) => l.toLowerCase() === normalized);
  if (exact) return exact;

  // ② 仅语言码匹配：'zh' → 'zh-CN'，'en' → 'en-US'，'zh-TW' → 'zh-CN'
  const lang = normalized.split('-')[0];
  return SUPPORTED_LOCALES.find((l) => l.toLowerCase().split('-')[0] === lang) ?? null;
}

/**
 * 应用启动时的初始语言：localStorage → 浏览器语言 → 默认。
 *
 * 已登录用户的偏好（`sys_user.language`）不在这里处理——它要等
 * `/api/v1/auth/user-info` 返回后才知道，由 `App` 在渲染业务页面前插队切换。
 */
export function resolveInitialLocale(): AppLocale {
  let stored: string | null = null;
  try {
    stored = localStorage.getItem(LOCALE_STORAGE_KEY);
  } catch {
    // 隐私模式下 localStorage 可能抛异常，忽略即可
  }
  return toAppLocale(stored) ?? toAppLocale(navigator?.language) ?? DEFAULT_LOCALE;
}

export function persistLocale(locale: AppLocale): void {
  try {
    localStorage.setItem(LOCALE_STORAGE_KEY, locale);
  } catch {
    // 同上：持久化失败不影响本次会话
  }
}
