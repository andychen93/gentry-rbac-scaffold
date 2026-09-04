import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryForm } from './QueryForm';

describe('QueryForm', () => {
  it('按 schema 渲染字段 + 查询/重置按钮', () => {
    render(<QueryForm fields={[{ name: 'kw', label: '关键字' }]} onSearch={vi.fn()} />);
    expect(screen.getByText('关键字')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '查询' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '重置' })).toBeInTheDocument();
  });
  it('点查询回调当前值', () => {
    const onSearch = vi.fn();
    render(<QueryForm fields={[{ name: 'kw', label: '关键字' }]} onSearch={onSearch} />);
    fireEvent.change(screen.getByPlaceholderText('请输入'), { target: { value: 'x' } });
    fireEvent.click(screen.getByRole('button', { name: '查询' }));
    expect(onSearch).toHaveBeenCalledWith(expect.objectContaining({ kw: 'x' }));
  });
  it('点重置清空并回调', () => {
    const onSearch = vi.fn();
    render(<QueryForm fields={[{ name: 'kw', label: '关键字' }]} onSearch={onSearch} />);
    fireEvent.change(screen.getByPlaceholderText('请输入'), { target: { value: 'x' } });
    fireEvent.click(screen.getByRole('button', { name: '重置' }));
    expect(onSearch).toHaveBeenCalledWith({});
  });
});
