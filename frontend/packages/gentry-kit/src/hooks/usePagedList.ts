import { useState, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import type { ApiResult, PageResult, PageQuery } from '../types/api';

export interface UsePagedListOptions<T> {
  service: (params: PageQuery) => Promise<ApiResult<PageResult<T>>>;
  queryKey: unknown[];
  pageSize?: number;
  extraParams?: Record<string, unknown>;
}

export function usePagedList<T>(opts: UsePagedListOptions<T>) {
  const { service, queryKey, pageSize = 10, extraParams = {} } = opts;
  const [page, setPage] = useState(1);
  const [ps, setPs] = useState(pageSize);

  const params: PageQuery = { pageNum: page, pageSize: ps, ...extraParams };

  const { data, isLoading, refetch } = useQuery({
    queryKey: [...queryKey, page, ps, JSON.stringify(extraParams)],
    queryFn: async () => {
      const res = await service(params);
      return res.data; // 拦截器已剥一层，res.data = PageResult
    },
  });

  const refresh = useCallback(() => { refetch(); }, [refetch]);

  return {
    data: data?.list ?? [],
    total: data?.total ?? 0,
    loading: isLoading,
    page, pageSize: ps,
    setPage, setPageSize: setPs,
    refresh,
  };
}
