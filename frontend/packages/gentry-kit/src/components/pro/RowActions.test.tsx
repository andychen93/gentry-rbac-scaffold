import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { EditOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import { RowActions } from './RowActions';
import { PermissionProvider } from './permission';

// 操作项已改为纯图标 + Tooltip，文字不再直接出现在 DOM 里，
// 统一用 aria-label 定位（这也是无障碍名称的来源）。
// 权限判断走 Context 注入（kit 不依赖 userStore），测试里直接包 Provider。
describe('RowActions', () => {
  it('无 perm 项始终渲染；有 perm 且无权限时隐藏', () => {
    render(
      <PermissionProvider can={() => false}>
        <RowActions items={[
          { key: 'view', label: '详情', icon: <EyeOutlined />, onClick: vi.fn() },
          { key: 'edit', label: '编辑', icon: <EditOutlined />, perm: 'x:edit', onClick: vi.fn() },
        ]} />
      </PermissionProvider>,
    );
    expect(screen.getByLabelText('详情')).toBeInTheDocument();
    expect(screen.queryByLabelText('编辑')).toBeNull();
  });

  it('有权限时渲染', () => {
    render(
      <PermissionProvider can={() => true}>
        <RowActions items={[
          { key: 'edit', label: '编辑', icon: <EditOutlined />, perm: 'x:edit', onClick: vi.fn() },
        ]} />
      </PermissionProvider>,
    );
    expect(screen.getByLabelText('编辑')).toBeInTheDocument();
  });

  it('键盘可达：渲染为 role=button 且可聚焦', () => {
    render(
      <PermissionProvider can={() => true}>
        <RowActions items={[
          { key: 'edit', label: '编辑', icon: <EditOutlined />, onClick: vi.fn() },
        ]} />
      </PermissionProvider>,
    );
    const el = screen.getByRole('button', { name: '编辑' });
    expect(el).toHaveAttribute('tabindex', '0');
  });

  it('无 confirmText：点击直接触发 onClick', async () => {
    const onClick = vi.fn();
    render(
      <PermissionProvider can={() => true}>
        <RowActions items={[
          { key: 'edit', label: '编辑', icon: <EditOutlined />, onClick },
        ]} />
      </PermissionProvider>,
    );
    screen.getByLabelText('编辑').click();
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('有 confirmText：点击先弹二次确认，不直接触发 onClick', async () => {
    const onClick = vi.fn();
    render(
      <PermissionProvider can={() => true}>
        <RowActions items={[
          {
            key: 'del', label: '删除', icon: <DeleteOutlined />, danger: true,
            confirmText: '确定删除？', onClick,
          },
        ]} />
      </PermissionProvider>,
    );
    screen.getByLabelText('删除').click();
    // 点击只应打开 Popconfirm，真正的 onClick 要等用户点确认
    expect(onClick).not.toHaveBeenCalled();
    expect(await screen.findByText('确定删除？')).toBeInTheDocument();
  });

  it('danger 项加 is-danger 样式类', () => {
    render(
      <PermissionProvider can={() => true}>
        <RowActions items={[
          { key: 'del', label: '删除', icon: <DeleteOutlined />, danger: true, onClick: vi.fn() },
        ]} />
      </PermissionProvider>,
    );
    expect(screen.getByLabelText('删除')).toHaveClass('is-danger');
  });

  it('can 注入 false 时 perm 项不渲染', () => {
    render(
      <PermissionProvider can={() => false}>
        <RowActions items={[
          { key: 'edit', label: '编辑', icon: <EditOutlined />, perm: 'system:user:edit', onClick: () => {} },
          { key: 'del', label: '删除', icon: <DeleteOutlined />, onClick: () => {} },
        ]} />
      </PermissionProvider>,
    );
    expect(screen.getByLabelText('删除')).toBeInTheDocument();
    expect(screen.queryByLabelText('编辑')).toBeNull();
  });
});
