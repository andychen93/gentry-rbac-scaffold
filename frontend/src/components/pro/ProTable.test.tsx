import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { ProTable } from './ProTable';
import type { ApiResult, PageResult } from '../../types/api';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

const service = vi.fn(async (): Promise<ApiResult<PageResult<{ id: number; name: string }>>> => ({
  code: 0, data: { list: [{ id: 1, name: 'A' }], total: 1, pageNum: 1, pageSize: 10, pages: 1 },
}));

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return React.createElement(QueryClientProvider, { client: qc }, children);
};

describe('ProTable', () => {
  it('拉取并渲染数据', async () => {
    render(<ProTable service={service} queryKey={['t']} columns={[{ title: '名', dataIndex: 'name', key: 'name' }]} rowKey="id" />, { wrapper });
    await waitFor(() => expect(screen.getByText('A')).toBeInTheDocument());
  });
  it('渲染查询表单（querySchema）', () => {
    render(<ProTable service={service} queryKey={['t']} columns={[]} rowKey="id"
      querySchema={[{ name: 'kw', label: '关键字' }]} />, { wrapper });
    expect(screen.getByText('关键字')).toBeInTheDocument();
  });
});
