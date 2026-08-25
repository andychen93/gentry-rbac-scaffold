import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';

import { DICT_TYPES, DICT_VALUES } from './dictEnum';

/**
 * 后端导出用的职务译文，必须与前端字典语言包逐条相等。
 *
 * **为什么会有两份。** 字典 label 的译文主场在前端（`locales/{lang}/dict.json`，
 * 后端只下发派生的 `i18nKey`）。但用户导出的 Excel 是**后端**生成的，那里拿不到前端语言包，
 * 于是 `export_*.properties` 里必须再放一份 `export.user.post.{code}`。
 *
 * 三个备选方案里这是唯一能让导出真正双语的：
 * - 导出写字典码（`Manager`）→ 中文用户打开 Excel 看到英文标识，而导出是给人看的报表
 * - 导出查 `sys_dict_data.dict_label` → 永远是中文，英文用户拿到中文，等于没做 i18n
 * - 后端再放一份译文 → 双语正确，代价是同一句话有两处
 *
 * 这条测试把那个代价从「可能漂移」降成「构造上不可能漂移」：改了一边不改另一边就红。
 *
 * 它同时兼作**码集合对账**：后端 properties、前端 `DICT_VALUES`、前端 `dict.json`
 * 三处的码必须完全一致。库里的字典表由 E2E 的 `I18N-008` 负责对账，四处合起来闭环。
 */

const PROPERTIES_DIR = path.join(
  process.cwd(),
  '..',
  'backend',
  'gentry-core',
  'src',
  'main',
  'resources',
  'i18n',
);

/** 极简 .properties 解析：本仓库的资源文件没有续行、没有 Unicode 转义 */
function readProperties(file: string): Record<string, string> {
  const text = fs.readFileSync(path.join(PROPERTIES_DIR, file), 'utf-8');
  const out: Record<string, string> = {};
  for (const line of text.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#') || trimmed.startsWith('!')) continue;
    const eq = trimmed.indexOf('=');
    if (eq <= 0) continue;
    out[trimmed.slice(0, eq).trim()] = trimmed.slice(eq + 1).trim();
  }
  return out;
}

const POST_PREFIX = 'export.user.post.';
const DICT_PREFIX = `dict.${DICT_TYPES.userPost}.`;

/** 后端资源包后缀 → 前端语言目录 */
const LOCALES = [
  { properties: 'export_zh_CN.properties', pack: 'zh-CN' },
  { properties: 'export_en_US.properties', pack: 'en-US' },
] as const;

function loadPack(locale: string): Record<string, string> {
  const p = path.join(process.cwd(), 'src', 'locales', locale, 'dict.json');
  return JSON.parse(fs.readFileSync(p, 'utf-8'));
}

describe('后端导出译文与前端字典语言包对账', () => {
  it('职务码集合三处一致（properties / DICT_VALUES / dict.json）', () => {
    const fromCode = [...DICT_VALUES[DICT_TYPES.userPost]].sort();

    for (const { properties, pack } of LOCALES) {
      const props = readProperties(properties);
      const fromProps = Object.keys(props)
        .filter((k) => k.startsWith(POST_PREFIX))
        .map((k) => k.slice(POST_PREFIX.length))
        .sort();
      expect(fromProps, `${properties} 的职务码与 dictEnum.DICT_VALUES 不一致`).toEqual(fromCode);

      const fromPack = Object.keys(loadPack(pack))
        .filter((k) => k.startsWith(DICT_PREFIX))
        .map((k) => k.slice(DICT_PREFIX.length))
        .sort();
      expect(fromPack, `${pack}/dict.json 的职务码与 dictEnum.DICT_VALUES 不一致`).toEqual(fromCode);
    }
  });

  it('每个码的译文两边逐条相等', () => {
    const mismatches: string[] = [];
    for (const { properties, pack } of LOCALES) {
      const props = readProperties(properties);
      const packJson = loadPack(pack);
      for (const code of DICT_VALUES[DICT_TYPES.userPost]) {
        const backend = props[POST_PREFIX + code];
        const frontend = packJson[DICT_PREFIX + code];
        if (backend !== frontend) {
          mismatches.push(
            `${pack}  ${code}: properties="${backend}" vs dict.json="${frontend}"`,
          );
        }
      }
    }
    expect(
      mismatches,
      '导出的 Excel 与界面上的职务名会不一致。改译文要同时改 '
        + `backend/gentry-core/src/main/resources/i18n/export_*.properties 与 `
        + 'frontend/src/locales/*/dict.json\n'
        + mismatches.join('\n'),
    ).toEqual([]);
  });

  it('无默认资源包漏项（export.properties 也要有，否则未匹配 locale 时露出裸 key）', () => {
    const base = readProperties('export.properties');
    const missing = DICT_VALUES[DICT_TYPES.userPost].filter((c) => !base[POST_PREFIX + c]);
    expect(missing, 'export.properties 缺这些码的兜底译文').toEqual([]);
  });
});
