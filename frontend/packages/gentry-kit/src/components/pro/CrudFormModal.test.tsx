import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import React from 'react';
import { CrudFormModal } from './CrudFormModal';

const fields = [
  { name: 'name', label: '名称', type: 'input' as const, rules: [{ required: true, message: '必填' }] },
  { name: 'status', label: '状态', type: 'switch' as const },
];

// 与 main.tsx 一致：zhCN locale 让 Modal OK 按钮文案为 "确 定"
const wrapper = ({ children }: { children: React.ReactNode }) =>
  React.createElement(ConfigProvider, { locale: zhCN }, children);

describe('CrudFormModal', () => {
  it('新增模式标题 + 提交 create', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <CrudFormModal
        open
        recordId={null}
        fields={fields}
        title="项"
        onSubmit={onSubmit}
        onSuccess={vi.fn()}
        onCancel={vi.fn()}
      />,
      { wrapper }
    );
    expect(screen.getByText('新增项')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('名称'), { target: { value: 'X' } });
    fireEvent.click(screen.getByRole('button', { name: /确\s*定$/ }));
    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ name: 'X', status: 0 }), false)
    );
  });

  it('编辑模式标题 + onLoad 回填 + 提交 update', async () => {
    const onLoad = vi.fn().mockResolvedValue({ name: 'A', status: 1 });
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <CrudFormModal
        open
        recordId={42}
        fields={fields}
        title="项"
        onLoad={onLoad}
        onSubmit={onSubmit}
        onSuccess={vi.fn()}
        onCancel={vi.fn()}
      />,
      { wrapper }
    );
    expect(screen.getByText('编辑项')).toBeInTheDocument();
    await waitFor(() => expect(onLoad).toHaveBeenCalledWith(42));
    await waitFor(() => expect((screen.getByLabelText('名称') as HTMLInputElement).value).toBe('A'));
    // 等 onLoad 的 loading 退去后点击 OK
    const okBtn = await screen.findByRole('button', { name: /确\s*定$/ });
    fireEvent.click(okBtn);
    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ name: 'A', status: 1 }), true)
    );
  });

  it('switch 字段：回填 0/1 转 boolean，提交 boolean 转 0/1', async () => {
    const onLoad = vi.fn().mockResolvedValue({ name: 'B', status: 0 });
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <CrudFormModal
        open
        recordId={1}
        fields={fields}
        title="项"
        onLoad={onLoad}
        onSubmit={onSubmit}
        onSuccess={vi.fn()}
        onCancel={vi.fn()}
      />,
      { wrapper }
    );
    await waitFor(() => expect(onLoad).toHaveBeenCalled());
    await waitFor(() => expect(screen.getByRole('switch').getAttribute('aria-checked')).toBe('false'));
    fireEvent.click(screen.getByRole('switch')); // 翻为 true
    const okBtn = await screen.findByRole('button', { name: /确\s*定$/ });
    fireEvent.click(okBtn);
    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ name: 'B', status: 1 }), true)
    );
  });

  it('type=render 逃逸口：自定义渲染节点', () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const renderFn = vi.fn(() => <div data-testid="custom">CUSTOM</div>);
    render(
      <CrudFormModal
        open
        recordId={null}
        fields={[{ name: 'x', label: 'X', type: 'render' as const, render: renderFn }]}
        title="T"
        onSubmit={onSubmit}
        onSuccess={vi.fn()}
        onCancel={vi.fn()}
      />,
      { wrapper }
    );
    expect(screen.getByTestId('custom')).toBeInTheDocument();
    expect(renderFn).toHaveBeenCalled();
  });

  it('必填校验失败时不调 onSubmit', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <CrudFormModal
        open
        recordId={null}
        fields={fields}
        title="项"
        onSubmit={onSubmit}
        onSuccess={vi.fn()}
        onCancel={vi.fn()}
      />,
      { wrapper }
    );
    fireEvent.click(screen.getByRole('button', { name: /确\s*定$/ }));
    await waitFor(() => expect(screen.getByText('必填')).toBeInTheDocument());
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
