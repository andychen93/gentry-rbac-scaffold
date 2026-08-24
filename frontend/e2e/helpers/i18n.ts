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
