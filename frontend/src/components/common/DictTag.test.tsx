import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { DictTag } from './DictTag';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

/*
 * 用内置字典 sys_user_gender 作样例。
 *
 * 原来这里用的是 `vehicle_type`（货运/客车）—— 从高精度定位平台带进来的业务概念残留，
 * RBAC 脚手架里不该出现。顺带这组用例现在也覆盖了「i18nKey 命中语言包时优先用译文」。
 */
vi.mock('../../services/dictApi', () => ({
  dictApi: {
    listData: vi.fn(async () => ({
      code: 0,
      data: [
        // i18nKey 命中 dict.json → 显示译文
        { dictLabel: '男', dictValue: '1', status: 1, i18nKey: 'dict.sys_user_gender.1' },
        // i18nKey 派生不出（模拟派生项目新增的字典项）→ 回退库里的 dictLabel
        { dictLabel: '其他', dictValue: '3', status: 1, i18nKey: null },
      ],
    })),
  },
}));

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return React.createElement(QueryClientProvider, { client: qc }, children);
};

describe('DictTag', () => {
  it('按 dictType+value 渲染 label（i18nKey 命中时用译文）', async () => {
    render(<DictTag dictType="sys_user_gender" value="1" />, { wrapper });
    await waitFor(() => expect(screen.getByText('男')).toBeInTheDocument());
  });
  it('i18nKey 缺失时回退库里的 dictLabel', async () => {
    render(<DictTag dictType="sys_user_gender" value="3" />, { wrapper });
    await waitFor(() => expect(screen.getByText('其他')).toBeInTheDocument());
  });
  it('无匹配返回原值', async () => {
    render(<DictTag dictType="sys_user_gender" value="9" />, { wrapper });
    await waitFor(() => expect(screen.getByText('9')).toBeInTheDocument());
  });
  it('value 为 null 渲染占位符', async () => {
    render(<DictTag dictType="sys_user_gender" value={null} />, { wrapper });
    await waitFor(() => expect(screen.getByText('—')).toBeInTheDocument());
  });
});
