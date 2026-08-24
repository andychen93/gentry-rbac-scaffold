import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import AppFooter from './AppFooter';
import commonZh from '../../locales/zh-CN/common.json';

describe('AppFooter', () => {
  it('渲染版权文案', () => {
    render(<AppFooter />);
    // 品牌名走语言包（common:app.name），不是常量 —— 断言真译文能顺带发现 key 拼错
    expect(screen.getByText(new RegExp(commonZh['app.name']))).toBeInTheDocument();
    expect(screen.getByText(/©/)).toBeInTheDocument();
  });
});
