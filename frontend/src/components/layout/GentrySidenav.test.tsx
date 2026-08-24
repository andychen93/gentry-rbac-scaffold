import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import GentrySidenav from './GentrySidenav';
import commonZh from '../../locales/zh-CN/common.json';

const baseProps = {
  items: [{ id: '1', label: '首页', path: '/dashboard' }],
  selectedKey: '1',
  onSelect: vi.fn(),
  onOpenChange: vi.fn(),
  pinned: true,
};

describe('GentrySidenav', () => {
  it('pinned=true 加 is-pinned class', () => {
    const { container } = render(<GentrySidenav {...baseProps} />);
    expect(container.querySelector('.ps-sidenav')).toHaveClass('is-pinned');
  });
  it('pinned=false 加 is-mini class', () => {
    const { container } = render(<GentrySidenav {...baseProps} pinned={false} />);
    expect(container.querySelector('.ps-sidenav')).toHaveClass('is-mini');
  });
  it('mini 态 hover 出现 is-hover class', () => {
    const { container } = render(<GentrySidenav {...baseProps} pinned={false} />);
    fireEvent.mouseEnter(container.querySelector('.ps-sidenav')!);
    expect(container.querySelector('.ps-sidenav')).toHaveClass('is-hover');
    fireEvent.mouseLeave(container.querySelector('.ps-sidenav')!);
    expect(container.querySelector('.ps-sidenav')).not.toHaveClass('is-hover');
  });
  it('渲染 logo 文案（取自语言包 common:app.name）', () => {
    render(<GentrySidenav {...baseProps} />);
    expect(screen.getByText(commonZh['app.name'])).toBeInTheDocument();
  });
  it('有无障碍 aria-label', () => {
    render(<GentrySidenav {...baseProps} />);
    expect(screen.getByLabelText('主导航菜单')).toBeInTheDocument();
  });
});
