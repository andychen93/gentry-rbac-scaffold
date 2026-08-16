import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { PageSelect } from './PageSelect';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React, { useState } from 'react';
import type { ApiResult, PageResult, PageQuery } from '../../types/api';

type Row = { id: number; name: string };

const data: Row[] = Array.from({ length: 12 }, (_, i) => ({ id: i + 1, name: `项${i + 1}` }));

const service = vi.fn(async (params: PageQuery): Promise<ApiResult<PageResult<Row>>> => {
  const pn = (params.pageNum as number) ?? 1;
  const ps = (params.pageSize as number) ?? 5;
  const filtered = params.name ? data.filter((d) => d.name.includes(String(params.name))) : data;
  return {
    code: 0,
    data: {
      list: filtered.slice((pn - 1) * ps, pn * ps),
      total: filtered.length,
      pageNum: pn,
      pageSize: ps,
      pages: Math.ceil(filtered.length / ps),
    },
  };
});

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return React.createElement(QueryClientProvider, { client: qc }, children);
};

describe('PageSelect', () => {
  it('渲染输入框 + placeholder', () => {
    render(
      <PageSelect<Row>
        service={service}
        columns={[{ title: '名', dataIndex: 'name', key: 'name' }]}
        rowKey="id"
        labelField="name"
        placeholder="请选择"
        onChange={vi.fn()}
      />,
      { wrapper },
    );
    expect(screen.getByPlaceholderText('请选择')).toBeInTheDocument();
  });

  it('点击输入框打开弹层并加载数据', async () => {
    render(
      <PageSelect<Row>
        service={service}
        columns={[{ title: '名', dataIndex: 'name', key: 'name' }]}
        rowKey="id"
        labelField="name"
        searchField="name"
        onChange={vi.fn()}
      />,
      { wrapper },
    );
    fireEvent.click(screen.getByRole('textbox'));
    await waitFor(() => expect(screen.getByText('项1')).toBeInTheDocument());
    expect(service).toHaveBeenCalled();
  });

  it('弹层里没有多余的搜索框（输入框即搜索框）', async () => {
    render(
      <PageSelect<Row>
        service={service}
        columns={[{ title: '名', dataIndex: 'name', key: 'name' }]}
        rowKey="id"
        labelField="name"
        searchField="name"
        onChange={vi.fn()}
      />,
      { wrapper },
    );
    fireEvent.click(screen.getByRole('textbox'));
    await waitFor(() => expect(screen.getByText('项1')).toBeInTheDocument());
    // 整个组件只有一个输入框：触发框本身
    expect(screen.getAllByRole('textbox')).toHaveLength(1);
  });

  it('打开弹层后第一次输入就要触发搜索（回归：skip 标志曾吞掉首次输入）', async () => {
    render(
      <PageSelect<Row>
        service={service}
        columns={[{ title: '名', dataIndex: 'name', key: 'name' }]}
        rowKey="id"
        labelField="name"
        searchField="name"
        onChange={vi.fn()}
      />,
      { wrapper },
    );
    const box = screen.getByRole('textbox');
    // 先点开（这一步曾把 skip 标志置位且因 text 未变而不复位）
    fireEvent.click(box);
    await waitFor(() => expect(screen.getByText('项1')).toBeInTheDocument());

    // 紧接着的第一次输入必须真的过滤（项12 唯一命中 '12'）
    fireEvent.change(box, { target: { value: '12' } });
    await waitFor(() => expect(screen.getByText('项12')).toBeInTheDocument(), { timeout: 3000 });
    await waitFor(() => expect(screen.queryByText('项1')).not.toBeInTheDocument());
  });

  it('点行选中回填 label 并关闭', async () => {
    // value=record 受控：用 StatefulWrapper 把 onChange 回灌到 value，验证 round-trip
    function StatefulWrapper() {
      const [val, setVal] = useState<Row | null>(null);
      return (
        <PageSelect<Row>
          service={service}
          columns={[{ title: '名', dataIndex: 'name', key: 'name' }]}
          rowKey="id"
          labelField="name"
          value={val}
          onChange={setVal}
        />
      );
    }
    render(<StatefulWrapper />, { wrapper });
    fireEvent.click(screen.getByRole('textbox'));
    await waitFor(() => expect(screen.getByText('项1')).toBeInTheDocument());
    fireEvent.click(screen.getByText('项1'));
    // 受控 round-trip：onChange 回灌 → value 更新 → 输入框回填 label
    await waitFor(() => expect(screen.getByRole('textbox')).toHaveValue('项1'));
  });
});
