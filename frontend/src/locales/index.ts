/**
 * react-i18next 装配。
 *
 * 两个关键决策：
 *
 * 1. **`keySeparator: false`（扁平 key）**。后端派生的 B 类 key 形如
 *    `menu.system.user`、`dict.sys_user_gender.1`；若开启 keySeparator，i18next 会把它当
 *    多层嵌套路径去查，而 `dict.sys_user_gender.1` 里的 `1` 作为对象键更是自找麻烦。
 *    关掉之后 JSON 是扁平的、key 原样命中。Ant Design Pro 也是扁平 key。
 *
 * 2. **按 namespace 懒加载**（`resourcesToBackend` + 动态 import）。启动只加载
 *    `common` / `nav` / `dict` —— 侧边栏和字典标签在首屏就要渲染，懒加载会闪。
 *    其余页面 namespace 由页面组件自己 `useTranslation('user')` 触发按需加载。
 */
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import resourcesToBackend from 'i18next-resources-to-backend';

import {
  DEFAULT_LOCALE,
  persistLocale,
  resolveInitialLocale,
  SUPPORTED_LOCALES,
  toAppLocale,
} from './config';
import { syncDayjsLocale } from './antd';

/**
 * 全部 namespace。与 `pages/` 子目录一一对应，另加三个：
 * - `common`：高频通用词条 + `components/` 的文案
 * - `nav`：B 类，后端派生的 `menu.*` key
 * - `dict`：B 类，后端派生的 `dict.*` key
 *
 * 两处刻意改名：`pages/menu` → `menuMgmt`、`pages/dict` → `dictMgmt`，
 * 因为 `nav` 与 `dict` 已占用了 menu/dict 的语义。
 */
export const NAMESPACES = [
  'common',
  'nav',
  'dict',
  'user',
  'role',
  'dept',
  'menuMgmt',
  'dictMgmt',
  'config',
  'log',
  'monitor',
  'profile',
  'home',
  'login',
] as const;

export type Namespace = (typeof NAMESPACES)[number];

/** 首屏必须同步加载的 namespace */
const EAGER_NAMESPACES: Namespace[] = ['common', 'nav', 'dict'];

let setupPromise: Promise<typeof i18n> | null = null;

/**
 * 初始化 i18next。**必须 await 之后再渲染**，否则首屏会闪一下原文。
 * 重复调用返回同一个 Promise，便于测试里安全复用。
 */
export function setupI18n(): Promise<typeof i18n> {
  if (setupPromise) return setupPromise;

  const initialLocale = resolveInitialLocale();
  syncDayjsLocale(initialLocale);
  syncHtmlLang(initialLocale);

  setupPromise = i18n
    .use(
      resourcesToBackend(
        (language: string, namespace: string) =>
          import(`./${language}/${namespace}.json`)
      )
    )
    .use(initReactI18next)
    .init({
      lng: initialLocale,
      fallbackLng: DEFAULT_LOCALE,
      supportedLngs: SUPPORTED_LOCALES,
      ns: EAGER_NAMESPACES,
      defaultNS: 'common',
      // 见文件头注释：扁平 key，让 menu.system.user 这类 key 原样命中
      keySeparator: false,
      nsSeparator: ':',
      // React 自身已转义，再转一次会把中文引号变成实体
      interpolation: { escapeValue: false },
      // 缺 key 时返回 key 而不是 null，配合调用处的 defaultValue 兜底
      returnNull: false,
      react: { useSuspense: false },
      // 开发期把缺失的 key 打到控制台；生产静默，避免刷屏
      saveMissing: import.meta.env.DEV,
      /*
       * 必须是 'current'。默认值 'fallback' 会拿 fallbackLng（zh-CN）去判定，
       * 而懒加载只会加载**当前**语言的 namespace —— 于是英文界面下每个正常命中的 key
       * 都会报一句「missing key: home:xxx (zh-CN)」，全是假警报，真的漏译反而被埋掉。
       */
      saveMissingTo: 'current',
      missingKeyHandler: import.meta.env.DEV
        ? (lngs, ns, key) => {
            /*
             * 懒加载的 namespace 在首帧还没到（useSuspense:false，t() 先返回 key、
             * 加载完再重渲染），这一帧的「缺失」是假警报。只有 namespace 确实加载完了
             * 还查不到，才是真漏译。不加这个判断，每次进页面都刷一屏假警报，
             * 真问题就被埋了。
             */
            if (!i18n.hasLoadedNamespace(ns)) return;
            // eslint-disable-next-line no-console
            console.warn(`[i18n] missing key: ${ns}:${key} (${lngs.join(',')})`);
          }
        : undefined,
    })
    .then(() => i18n);

  return setupPromise;
}

/**
 * 应用服务端下发的用户语言偏好（`UserInfoVO.language`，后端格式 `zh_CN`）。
 *
 * 登录/刷新时调用。**为什么需要这一步**：登录页用的可能是浏览器默认语言，而用户
 * 偏好是另一种；不在渲染业务页面之前切好，界面会先按浏览器语言渲染再纠正、闪一下。
 *
 * 三态语义：`language` 缺失（后端 Jackson NON_NULL 会省略 null 字段）表示
 * 「用户从未选过」，此时**保持当前 locale 不动**（即继续跟随浏览器 / localStorage）。
 */
export async function applyServerLocale(language?: string | null): Promise<void> {
  const target = toAppLocale(language);
  if (!target || target === toAppLocale(i18n.language)) return;
  await i18n.changeLanguage(target);
  syncDayjsLocale(target);
  syncHtmlLang(target);
  persistLocale(target);
}

/**
 * 同步 `<html lang>`。
 *
 * 屏幕阅读器靠这个属性决定用哪种语音朗读；浏览器的「翻译此页」提示、以及
 * CSS 的 `:lang()` 选择器与断字规则也依赖它。切了界面语言却不改 lang，
 * 是一处**无障碍缺陷**——实测批次 1 装配后 lang 一直停在初始值。
 */
export function syncHtmlLang(lng?: string | null): void {
  const target = toAppLocale(lng) ?? DEFAULT_LOCALE;
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('lang', target);
  }
}

export default i18n;
