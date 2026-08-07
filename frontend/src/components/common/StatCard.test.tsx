import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { createElement, type ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import StatCard from './StatCard';

// useQuery 需要 QueryClientProvider，包一个 wrapper
const wrapper = ({ children }: { children: ReactNode }) => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return createElement(QueryClientProvider, { client: qc }, children);
};

describe('StatCard', () => {
  it('渲染 label 与 value', () => {
    render(<StatCard variant="primary" label="车辆总数" value={2418} />, { wrapper });
    expect(screen.getByText('车辆总数')).toBeInTheDocument();
    expect(screen.getByText('2,418')).toBeInTheDocument();
  });
  it('应用 variant 渐变 class', () => {
    const { container } = render(<StatCard variant="info" label="在线" value={10} />, { wrapper });
    expect(container.firstChild).toHaveClass('ps-stat-card', 'ps-stat-card--info');
  });
  it('progress 渲染进度条', () => {
    const { container } = render(<StatCard variant="danger" label="报警" value={37} progress={20} />, { wrapper });
    expect(container.querySelector('.ps-stat-card__progress-bar')).toHaveStyle({ width: '20%' });
  });
  it('无 progress 不渲染进度条', () => {
    const { container } = render(<StatCard variant="default" label="x" value={1} />, { wrapper });
    expect(container.querySelector('.ps-stat-card__progress')).toBeNull();
  });
  it('fetcher 自动拉数渲染', async () => {
    render(<StatCard variant="info" label="在线" fetcher={() => Promise.resolve(42)} />, { wrapper });
    await waitFor(() => expect(screen.getByText('42')).toBeInTheDocument());
  });
});
