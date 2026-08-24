import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import UserTableSelect from './UserTableSelect';
import MenuTableSelect from './MenuTableSelect';

/*
 * 只钉一件事：**不传 placeholder 时占位符不能是空的**。
 *
 * i18n 改造把 `placeholder = '输入昵称搜索用户'` 这类中文默认参数删掉是对的
 * （默认参数在组件外求值，拿不到 t，也不随语言切换重算），但兜底得挪进函数体。
 * 批次 2/3 漏了这一步，两个组件的占位符直接变空，E2E 才发现。
 */
vi.mock('../../services/userApi', () => ({
  userApi: {
    list: vi.fn(async () => ({
      code: 0,
      data: { list: [], total: 0, pageNum: 1, pageSize: 5, pages: 0 },
    })),
  },
}));
vi.mock('../../services/menuApi', () => ({
  menuApi: {
    tree: vi.fn(async () => ({ code: 0, data: [] })),
    list: vi.fn(async () => ({ code: 0, data: [] })),
  },
}));

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return React.createElement(QueryClientProvider, { client: qc }, children);
};

describe('下拉 table 选择器的默认占位符', () => {
  it('UserTableSelect 回落到 common:placeholder.searchUser', () => {
    render(<UserTableSelect />, { wrapper });
    expect(screen.getByPlaceholderText('输入昵称搜索用户')).toBeInTheDocument();
  });

  it('MenuTableSelect 回落到 common:placeholder.searchMenu', () => {
    render(<MenuTableSelect />, { wrapper });
    expect(screen.getByPlaceholderText('输入名称搜索模块')).toBeInTheDocument();
  });
});
