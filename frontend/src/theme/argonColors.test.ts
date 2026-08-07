import { describe, it, expect } from 'vitest';
import { argonColors, argonGradients, argonShadow, argonRadius, argonSidebarWidth } from './argonColors';

describe('argonColors', () => {
  it('基础色板与 _variables.scss 一致', () => {
    expect(argonColors.primary).toBe('#5e72e4');
    expect(argonColors.info).toBe('#11cdef');
    expect(argonColors.success).toBe('#2dce89');
    expect(argonColors.warning).toBe('#fb6340');
    expect(argonColors.danger).toBe('#f5365c');
    expect(argonColors.default).toBe('#172b4d');
    expect(argonColors.bodyBg).toBe('#f8f9fe');
    expect(argonColors.gray100).toBe('#f6f9fc');
  });
  it('渐变用 87deg 公式', () => {
    expect(argonGradients.primary).toMatch(/^linear-gradient\(87deg/);
    expect(argonGradients.primary).toContain('#5e72e4');
  });
  it('阴影与圆角常量', () => {
    expect(argonShadow).toBe('0 0 2rem 0 rgba(136, 152, 170, 0.15)');
    expect(argonRadius).toBe(6);
  });
  it('侧栏宽度 mini/open', () => {
    expect(argonSidebarWidth).toEqual({ mini: 62, open: 250 });
  });
});
