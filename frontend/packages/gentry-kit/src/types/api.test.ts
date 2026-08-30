import { describe, it, expect } from 'vitest';
import type { PageResult, PageQuery } from './api';

describe('api 公共类型', () => {
  it('PageResult 结构', () => {
    const r: PageResult<number> = { list: [1], total: 1, pageNum: 1, pageSize: 10, pages: 1 };
    expect(r.list).toHaveLength(1);
  });
  it('PageQuery 结构', () => {
    const q: PageQuery = { pageNum: 1, pageSize: 10 };
    expect(q.pageNum).toBe(1);
  });
});
