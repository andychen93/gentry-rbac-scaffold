import fs from 'node:fs';
import path from 'node:path';

/**
 * 读语言包给断言用。
 *
 * **为什么不直接 `import ... from '../src/locales/zh-CN/common.json'`**：
 * Playwright 以 ESM 跑 spec，Node 要求 JSON 导入带 `with { type: 'json' }`，
 * 而加了之后 tsc 的 module 设置又不认。用 fs 读最省事，也和
 * `src/locales/locales.test.ts` 的做法一致。
 */
export function pack(locale: 'zh-CN' | 'en-US', ns: string): Record<string, string> {
  const p = path.join(process.cwd(), 'src', 'locales', locale, `${ns}.json`);
  return JSON.parse(fs.readFileSync(p, 'utf-8'));
}

/**
 * 在页面文本里找**裸 key**（`t()` 查不到时会原样返回 key）。
 *
 * 做法是拿语言包里的 key 全集去正向匹配，而不是用「点分小写标识符」这类正则 ——
 * 正则会把数据误判成 key：参数配置页的 `sys.captcha.enabled` 就撞上了
 * `captcha.*` 这条模式。用真实 key 集合匹配没有这个问题，而且新增词条自动纳入。
 *
 * 只查含点的 key：`save` / `back` 这种单段 key 太容易和正常文本或数据撞车。
 */
export function rawKeysIn(text: string): string[] {
  const dir = path.join(process.cwd(), 'src', 'locales', 'en-US');
  const found: string[] = [];
  for (const file of fs.readdirSync(dir)) {
    if (!file.endsWith('.json')) continue;
    const ns = file.replace(/\.json$/, '');
    for (const key of Object.keys(JSON.parse(fs.readFileSync(path.join(dir, file), 'utf-8')))) {
      if (!key.includes('.')) continue;
      if (text.includes(key)) found.push(`${ns}:${key}`);
    }
  }
  return found;
}

/**
 * 由 `permission` / `path` 派生菜单 i18n key —— **必须与后端
 * `MenuI18nKeyResolver.resolve` 逐字一致**。
 *
 * 这里刻意重写一遍而不是「相信后端下发的 i18nKey」：对账要的就是两侧独立算一遍再比。
 * 若只拿后端给的 key 去查语言包，规则本身改错时两边一起错，测不出来。
 *
 * ```
 * system:user:add  -> menu.system.user.add
 * /system          -> menu.system
 * ```
 */
export function deriveMenuKey(permission?: string | null, routePath?: string | null): string | null {
  const normalize = (raw: string, sep: string) =>
    raw
      .split(sep)
      .join('.')
      .replace(/\.{2,}/g, '.') // 折叠连续点：`//system` 会产出 menu..system
      .replace(/\.+$/, ''); // 去尾点：`/system/` 会产出 menu.system.
  if (permission && permission.trim()) return `menu.${normalize(permission.trim(), ':')}`;
  if (routePath && routePath.trim()) return `menu${normalize(routePath.trim(), '/')}`;
  return null;
}
