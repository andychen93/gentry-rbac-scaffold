import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';

/**
 * i18n 防线：应用 `src/` 与共享包 `packages/gentry-kit/src/` 下不允许出现中文界面文案。
 *
 * kit 里的 Pro 组件是 t() 重度用户，防线不扩进去就是漏洞——组件改一次、
 * 翻译漏一条，只有 kit 也扫才拦得住。
 *
 * **为什么是 Vitest 而不是 ESLint**：本仓库没有 ESLint（没有 eslint.config.js、
 * package.json 里也没有 lint 脚本），为一条规则搭整套工具链不划算。沿用
 * `theme/argonLessVars.test.ts` 拦裸 hex 的同一套路：一条测试 + 显式豁免清单，
 * 跟着 `npm test` 一起跑，进的是同一道提交门禁。
 *
 * 检测的是**中文字面量**而不是「没调 t()」：后者要做类型/作用域分析才准，
 * 而前者对本仓库足够——界面文案原本 100% 是中文，翻译漏掉必然留下中文。
 * 代价是英文硬编码（`<Button>Save</Button>`）拦不住，那类问题靠 code review
 * 与 E2E 的 I18N-006（逐页断言译文出现）兜。
 */

const SRC_ROOTS = ['src', 'packages/gentry-kit/src'].map((p) => path.join(process.cwd(), p));
const CJK = /[\u4e00-\u9fff]/;

/**
 * 豁免清单。**路径相对各扫描根**（应用条目相对 `src/`，kit 条目相对
 * `packages/gentry-kit/src/`），两个根的文件会拼进同一命名空间统一比对。
 * 加条目必须写清「为什么这里的中文是对的」——只写「暂时」「历史原因」的一律不收。
 */
const ALLOW: { pattern: RegExp; why: string }[] = [
  {
    pattern: /^pages\/dev\//,
    why: '/dev/style 是样式对照页，为便于跟 Argon 原版比对，AGENTS.md 第八章特批可写死',
  },
  {
    pattern: /^components\/layout\/LocaleSwitcher\.tsx$/,
    why: '语言选择器显示语言的**自称**（简体中文 / English）。翻译它反而会让对方语言的用户认不出',
  },
];

/** 只扫源码，不扫测试与语言包 */
function collectSources(dir: string, out: string[] = []): string[] {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'test') continue;
      collectSources(full, out);
      continue;
    }
    if (!/\.tsx?$/.test(entry.name)) continue;
    if (/\.test\.tsx?$/.test(entry.name)) continue;
    out.push(full);
  }
  return out;
}

/**
 * 走遍所有扫描根，产出 `{ root, rel, file }`。
 * rel 是相对各自根的 `/` 分隔路径——两个根下的同名文件（如都有 components/）
 * 靠 root 前缀区分，报告里拼成 `src/xxx` / `gentry-kit/xxx` 可读形式。
 */
function collectAll(): { root: string; rel: string; file: string }[] {
  const out: { root: string; rel: string; file: string }[] = [];
  for (const root of SRC_ROOTS) {
    for (const file of collectSources(root)) {
      const rel = path.relative(root, file).split(path.sep).join('/');
      out.push({ root, rel, file });
    }
  }
  return out;
}

/** 报告用短标签：src → `src/`，gentry-kit → `kit/` */
function tagOf(root: string): string {
  return root.endsWith(path.join('gentry-kit', 'src')) ? 'kit/' : 'src/';
}

/**
 * 去注释，但**保留行号**（把注释内容换成等量空白，换行原样留下）。
 *
 * 覆盖三种写法：块注释 `/* *\/`、JSX 注释 `{/* *\/}`、行注释 `//`。
 * 行注释的处理会误伤 `'http://…'` 这类字符串里的 `//`，但那之后跟中文的情况不存在，
 * 对本规则无影响。
 */
function stripComments(code: string): string {
  const blanked = code.replace(/\/\*[\s\S]*?\*\//g, (m) => m.replace(/[^\n]/g, ' '));
  return blanked
    // \r?\n：Windows 上 git autocrlf 会 checkout 出 CRLF，行尾的 \r 是 JS 正则的行终止符，
    // `.` 匹配不到它，`/\/\/.*$/` 因此失配 → 注释剥离失效、所有中文注释被误报。
    // 按 \r?\n 切（丢弃 \r），行号不变，跨平台一致。
    .split(/\r?\n/)
    .map((line) => line.replace(/\/\/.*$/, ''))
    .join('\n');
}

describe('i18n 防线：src 与 @gentry/kit 下不得硬编码中文界面文案', () => {
  it('除豁免清单外没有中文字面量', () => {
    const offenders: string[] = [];

    for (const { root, rel, file } of collectAll()) {
      if (ALLOW.some((a) => a.pattern.test(rel))) continue;

      const lines = stripComments(fs.readFileSync(file, 'utf-8')).split('\n');
      lines.forEach((line, i) => {
        if (CJK.test(line)) offenders.push(`${tagOf(root)}${rel}:${i + 1}  ${line.trim()}`);
      });
    }

    expect(
      offenders,
      '这些位置有硬编码中文。文案请进 src/locales/{zh-CN,en-US}/{ns}.json，'
        + '用 t() 取；确实该保留中文的请加进本文件的 ALLOW 并写明理由。\n'
        + offenders.join('\n'),
    ).toEqual([]);
  });

  it('豁免清单里的路径都还存在（防止条目腐烂）', () => {
    // 文件被删或改名后豁免条目会静默失效，下次真出问题时拦不住
    const all = collectAll().map((x) => x.rel);
    const dead = ALLOW.filter((a) => !all.some((rel) => a.pattern.test(rel)));
    expect(
      dead.map((d) => String(d.pattern)),
      '这些豁免条目已经匹配不到任何文件，请删掉',
    ).toEqual([]);
  });
});
