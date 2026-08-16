import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { argonColors, argonGradients } from './argonColors';
import { buildArgonLessVars } from './argonLessVars';

const LESS_FILE = path.resolve(__dirname, '../styles/argon.less');

/** 去掉 `//` 行尾注释 —— 注释里刻意保留 Argon 原始 hex 作为文档 */
function stripComments(source: string): string {
  return source
    .split('\n')
    .map((line) => {
      const i = line.indexOf('//');
      return i === -1 ? line : line.slice(0, i);
    })
    .join('\n');
}

describe('buildArgonLessVars', () => {
  it('每个色板键都产出对应的 @ps- 变量', () => {
    const out = buildArgonLessVars();
    for (const [key, value] of Object.entries(argonColors)) {
      expect(out).toContain(`@ps-${key}: ${value};`);
    }
  });

  it('每个渐变都产出 @ps-gradient- 变量', () => {
    const out = buildArgonLessVars();
    for (const [key, value] of Object.entries(argonGradients)) {
      expect(out).toContain(`@ps-gradient-${key}: ${value};`);
    }
  });
});

describe('argon.less 不得写死颜色', () => {
  /*
   * 这是「改配色只动 argonColors.ts」这条约定的守卫。
   *
   * argon.less 里的颜色一律走 vite 注入的 @ps-* 变量（见 argonLessVars.ts）。
   * 谁再往里写 hex，颜色就脱离色板、改主题时漏改，这个用例会红。
   * 需要新色时的正确做法：往 argonColors.ts 加一个键，变量自动可用。
   */
  const code = stripComments(fs.readFileSync(LESS_FILE, 'utf-8'));

  it('没有裸 hex 色值', () => {
    const hex = code.match(/#[0-9a-fA-F]{3,8}\b/g) ?? [];
    expect(hex).toEqual([]);
  });

  it('没有裸 rgb()/rgba() 调色板色值（应改用 fade(@ps-*, N%)）', () => {
    // 允许纯黑/纯白的 rgba（阴影叠加用，不属于色板）
    const offenders = (code.match(/rgba?\([^)]*\)/g) ?? []).filter((c) => {
      const nums = c.match(/\d+/g)?.slice(0, 3).map(Number) ?? [];
      if (nums.length < 3) return false;
      const isBlack = nums.every((n) => n === 0);
      const isWhite = nums.every((n) => n === 255);
      return !isBlack && !isWhite;
    });
    expect(offenders).toEqual([]);
  });
});
