import { describe, it, expect, vi } from 'vitest';
import { createElement, type ReactNode } from 'react';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { usePagedList } from './usePagedList';
import type { ApiResult, PageResult } from '../types/api';

const makeService = (total = 30) =>
  vi.fn(async (_q: any): Promise<ApiResult<PageResult<{ id: number }>>> => ({
    code: 0,
    data: { list: Array.from({ length: 10 }, (_, i) => ({ id: i })), total, pageNum: 1, pageSize: 10, pages: 3 },
  }));

// useQuery 需要 QueryClientProvider，包一个 wrapper（用 createElement 避免 .ts 文件 JSX 报错）
const createWrapper = () => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: ReactNode }) =>
    createElement(QueryClientProvider, { client: qc }, children);
};

describe('usePagedList', () => {
  it('初始拉取第一页', async () => {
    const service = makeService();
    const { result } = renderHook(
      () => usePagedList<{ id: number }>({ service, queryKey: ['x'] }),
      { wrapper: createWrapper() },
    );
    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.data).toHaveLength(10));
    expect(result.current.total).toBe(30);
    expect(service).toHaveBeenCalledWith(expect.objectContaining({ pageNum: 1, pageSize: 10 }));
  });
  it('setPage 触发重新拉取', async () => {
    const service = makeService();
    const { result } = renderHook(
      () => usePagedList<{ id: number }>({ service, queryKey: ['x'] }),
      { wrapper: createWrapper() },
    );
    await waitFor(() => expect(result.current.data).toHaveLength(10));
    act(() => result.current.setPage(2));
    await waitFor(() => expect(service).toHaveBeenLastCalledWith(expect.objectContaining({ pageNum: 2 })));
  });
  it('refresh 刷新当前页', async () => {
    const service = makeService();
    const { result } = renderHook(
      () => usePagedList<{ id: number }>({ service, queryKey: ['x'] }),
      { wrapper: createWrapper() },
    );
    await waitFor(() => expect(result.current.data).toHaveLength(10));
    const calls = service.mock.calls.length;
    result.current.refresh();
    await waitFor(() => expect(service.mock.calls.length).toBeGreaterThan(calls));
  });
});
