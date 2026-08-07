import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { RowActions } from './RowActions';
import { useUserStore } from '../../stores/userStore';

describe('RowActions', () => {
  it('无 perm 项始终渲染；有 perm 且无权限时隐藏', () => {
    useUserStore.setState({ hasPermission: () => false });
    render(<RowActions items={[
      { key: 'view', label: '详情', onClick: vi.fn() },
      { key: 'edit', label: '编辑', perm: 'x:edit', onClick: vi.fn() },
    ]} />);
    expect(screen.getByText('详情')).toBeInTheDocument();
    expect(screen.queryByText('编辑')).toBeNull();
  });
  it('有权限时渲染', () => {
    useUserStore.setState({ hasPermission: () => true });
    render(<RowActions items={[{ key: 'edit', label: '编辑', perm: 'x:edit', onClick: vi.fn() }]} />);
    expect(screen.getByText('编辑')).toBeInTheDocument();
  });
});
