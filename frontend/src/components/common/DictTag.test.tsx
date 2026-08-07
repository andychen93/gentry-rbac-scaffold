import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { DictTag } from './DictTag';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

vi.mock('../../services/dictApi', () => ({
  dictApi: {
    listData: vi.fn(async () => ({
      code: 0,
      data: [
        { dictLabel: '货运', dictValue: '1', status: 1 },
        { dictLabel: '客车', dictValue: '2', status: 1 },
      ],
    })),
  },
}));

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return React.createElement(QueryClientProvider, { client: qc }, children);
};

describe('DictTag', () => {
  it('按 dictType+value 渲染 label', async () => {
    render(<DictTag dictType="vehicle_type" value="1" />, { wrapper });
    await waitFor(() => expect(screen.getByText('货运')).toBeInTheDocument());
  });
  it('无匹配返回原值', async () => {
    render(<DictTag dictType="vehicle_type" value="9" />, { wrapper });
    await waitFor(() => expect(screen.getByText('9')).toBeInTheDocument());
  });
  it('value 为 null 渲染占位符', async () => {
    render(<DictTag dictType="vehicle_type" value={null} />, { wrapper });
    await waitFor(() => expect(screen.getByText('—')).toBeInTheDocument());
  });
});
