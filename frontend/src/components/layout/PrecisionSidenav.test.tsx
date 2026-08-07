import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import PrecisionSidenav from './PrecisionSidenav';

const baseProps = {
  items: [{ id: '1', label: '首页', path: '/dashboard' }],
  selectedKey: '1',
  onSelect: vi.fn(),
  onOpenChange: vi.fn(),
  pinned: true,
};

describe('PrecisionSidenav', () => {
  it('pinned=true 加 is-pinned class', () => {
    const { container } = render(<PrecisionSidenav {...baseProps} />);
    expect(container.querySelector('.ps-sidenav')).toHaveClass('is-pinned');
  });
  it('pinned=false 加 is-mini class', () => {
    const { container } = render(<PrecisionSidenav {...baseProps} pinned={false} />);
    expect(container.querySelector('.ps-sidenav')).toHaveClass('is-mini');
  });
  it('mini 态 hover 出现 is-hover class', () => {
    const { container } = render(<PrecisionSidenav {...baseProps} pinned={false} />);
    fireEvent.mouseEnter(container.querySelector('.ps-sidenav')!);
    expect(container.querySelector('.ps-sidenav')).toHaveClass('is-hover');
    fireEvent.mouseLeave(container.querySelector('.ps-sidenav')!);
    expect(container.querySelector('.ps-sidenav')).not.toHaveClass('is-hover');
  });
  it('渲染 logo 文案 Precision', () => {
    render(<PrecisionSidenav {...baseProps} />);
    expect(screen.getByText('Precision')).toBeInTheDocument();
  });
  it('有无障碍 aria-label', () => {
    render(<PrecisionSidenav {...baseProps} />);
    expect(screen.getByLabelText('主导航菜单')).toBeInTheDocument();
  });
});
