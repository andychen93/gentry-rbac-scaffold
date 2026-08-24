import React, { useState } from 'react';
import { Table, Card } from 'antd';
import { usePagedList } from '../../hooks/usePagedList';
import { QueryForm, type QueryField } from './QueryForm';
import type { ApiResult, PageResult, PageQuery } from '../../types/api';
import type { TableProps } from 'antd';
import { useTranslation } from 'react-i18next';

export interface ProTableProps<T> {
  service: (params: PageQuery) => Promise<ApiResult<PageResult<T>>>;
  queryKey: unknown[];
  columns: TableProps<T>['columns'];
  rowKey: string | ((r: T) => string | number);
  querySchema?: QueryField[];
  pageSize?: number;
  rowSelection?: TableProps<T>['rowSelection'];
  scroll?: TableProps<T>['scroll'];
  toolbar?: React.ReactNode;
  /** 查询条件变化时回调（导出/外部消费当前过滤条件） */
  onFiltersChange?: (filters: Record<string, unknown>) => void;
}

function ProTableInner<T>(props: ProTableProps<T>) {
  const { t } = useTranslation('common');
  const { service, queryKey, columns, rowKey, querySchema, pageSize, rowSelection, scroll, toolbar, onFiltersChange } = props;
  const [filters, setFilters] = useState<Record<string, unknown>>({});
  const list = usePagedList<T>({ service, queryKey, pageSize, extraParams: filters });

  const applyFilters = (v: Record<string, unknown>) => {
    setFilters(v);
    list.setPage(1);
    onFiltersChange?.(v);
  };

  return (
    <>
      {querySchema && querySchema.length > 0 && (
        <QueryForm fields={querySchema} onSearch={applyFilters} />
      )}
      <Card>
        {toolbar && <div style={{ marginBottom: 16 }}>{toolbar}</div>}
        <Table<T>
          rowKey={rowKey} columns={columns} dataSource={list.data} loading={list.loading}
          scroll={scroll} rowSelection={rowSelection}
          pagination={{
            current: list.page, pageSize: list.pageSize, total: list.total,
            showSizeChanger: true, showQuickJumper: true, showTotal: (n) => t('total', { count: n }),
            onChange: (p, ps) => { list.setPage(p); list.setPageSize(ps); },
          }}
        />
      </Card>
    </>
  );
}

export const ProTable = React.memo(ProTableInner) as typeof ProTableInner;
