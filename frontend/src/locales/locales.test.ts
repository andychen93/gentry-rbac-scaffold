import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';

import { NAMESPACES } from './index';
import { SUPPORTED_LOCALES } from './config';

const LOCALES_DIR = path.join(process.cwd(), 'src', 'locales');

function load(locale: string, ns: string): Record<string, unknown> {
  const p = path.join(LOCALES_DIR, locale, `${ns}.json`);
  return JSON.parse(fs.readFileSync(p, 'utf-8'));
}

describe('语言包结构约束', () => {
  it('每个语言都有全部 namespace 文件', () => {
    for (const locale of SUPPORTED_LOCALES) {
      for (const ns of NAMESPACES) {
        const p = path.join(LOCALES_DIR, locale, `${ns}.json`);
        expect(fs.existsSync(p), `缺文件 ${locale}/${ns}.json`).toBe(true);
      }
    }
  });

  it('中英文 key 集合完全一致（防漏译与孤儿）', () => {
    for (const ns of NAMESPACES) {
      const zh = Object.keys(load('zh-CN', ns)).sort();
      const en = Object.keys(load('en-US', ns)).sort();
      const onlyZh = zh.filter((k) => !en.includes(k));
      const onlyEn = en.filter((k) => !zh.includes(k));
      expect(onlyZh, `${ns}: 中文有但英文缺（漏译）`).toEqual([]);
      expect(onlyEn, `${ns}: 英文有但中文缺（孤儿）`).toEqual([]);
    }
  });

  it('全部是扁平结构_与 keySeparator=false 一致', () => {
    // 开了嵌套的话 t('menu.system.user') 会被当成三层路径去查，永远查不到
    for (const locale of SUPPORTED_LOCALES) {
      for (const ns of NAMESPACES) {
        for (const [k, v] of Object.entries(load(locale, ns))) {
          expect(typeof v, `${locale}/${ns}.json 的 ${k} 不是字符串（嵌套对象？）`).toBe('string');
        }
      }
    }
  });

  it('译文非空且无 TODO 残留', () => {
    for (const locale of SUPPORTED_LOCALES) {
      for (const ns of NAMESPACES) {
        for (const [k, v] of Object.entries(load(locale, ns))) {
          expect((v as string).trim(), `${locale}/${ns}.json 的 ${k} 为空`).not.toBe('');
          expect(v as string, `${locale}/${ns}.json 的 ${k} 含 TODO`).not.toContain('TODO');
        }
      }
    }
  });

  it('插值占位符在中英文之间一致', () => {
    // 漏掉一侧的 {{count}} 会让那个语言下数字消失，且不报错
    const ph = (s: string) => (s.match(/\{\{\s*\w+\s*\}\}/g) ?? []).sort();
    for (const ns of NAMESPACES) {
      const zh = load('zh-CN', ns);
      const en = load('en-US', ns);
      for (const k of Object.keys(zh)) {
        expect(ph(en[k] as string), `${ns}:${k} 的占位符与中文不一致`).toEqual(
          ph(zh[k] as string),
        );
      }
    }
  });

  it('B 类 key 前缀正确_nav 只放 menu 点开头、dict 只放 dict 点开头', () => {
    // 前缀即归属：后端资源文件里不允许出现 menu.* / dict.*，这里反过来约束前端
    for (const locale of SUPPORTED_LOCALES) {
      for (const k of Object.keys(load(locale, 'nav'))) {
        expect(k.startsWith('menu.'), `nav.json 里的 ${k} 不是 menu. 开头`).toBe(true);
      }
      for (const k of Object.keys(load(locale, 'dict'))) {
        expect(k.startsWith('dict.'), `dict.json 里的 ${k} 不是 dict. 开头`).toBe(true);
      }
    }
  });

  it('common 里不得出现语义歧义词', () => {
    // 「操作」在表格表头是 Action、在操作日志语境是 Operation —— 不能共用一条译文。
    // 这类词必须放页面 namespace，见设计 §3.3
    const AMBIGUOUS = ['操作', '成功', '失败'];
    const zh = load('zh-CN', 'common');
    const offenders = Object.entries(zh)
      .filter(([, v]) => AMBIGUOUS.includes((v as string).trim()))
      .map(([k, v]) => `${k}=${v}`);
    expect(offenders, '这些词在不同语境下译文不同，应放页面 namespace').toEqual([]);
  });
});
