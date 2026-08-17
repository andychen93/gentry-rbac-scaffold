import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

/**
 * 顶栏通知铃铛 + 站内通知模块（从源项目移植）。
 *
 * 覆盖：铃铛渲染 → 发布通知后 SSE 实时到达（不刷新页面）→ 未读角标 →
 * 弹层列表 → 标记已读 → 全部已读。
 */
test.describe.serial('消息通知 (NTF)', () => {
  let page: Page;
  let token: string;
  const TS = Date.now().toString().slice(-6);

  /** 直接调后端发布通知，模拟"别处产生了一条通知" */
  async function publish(title: string, level: number) {
    const res = await page.request.post('/api/v1/notifications', {
      headers: { Authorization: `Bearer ${token}` },
      data: { title, content: '内容' + TS, level },
    });
    expect((await res.json()).code).toBe(0);
  }

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page, 'chenli');
    await gotoPage(page, '/home');
    token = (await page.evaluate(() => localStorage.getItem('gentry_token'))) ?? '';
    expect(token).not.toBe('');
  });

  test.afterAll(async () => {
    await page.close();
  });

  test('NTF-001 顶栏渲染通知铃铛', async () => {
    await expect(page.getByRole('button', { name: '通知' })).toBeVisible({ timeout: 10000 });
  });

  test('NTF-002 发布通知后未读角标实时增加（SSE，不刷新页面）', async () => {
    const bell = page.getByRole('button', { name: '通知' });
    await expect(bell).toBeVisible();

    await publish('SSE实时通知' + TS, 3);

    // 不做任何刷新：角标应由 SSE 推送驱动出现
    const badge = page.locator('.ant-badge-count');
    await expect(badge).toBeVisible({ timeout: 15000 });
    await expect(badge).toHaveText(/[1-9]\d*/);
  });

  test('NTF-003 高级别通知弹出警示提示', async () => {
    await publish('紧急通知' + TS, 1);
    // level<=2 会额外 message.warning
    await expect(page.locator('.ant-message').getByText(/紧急/).first())
      .toBeVisible({ timeout: 15000 });
  });

  test('NTF-004 点铃铛展开列表，含刚发布的通知', async () => {
    await page.getByRole('button', { name: '通知' }).click();
    const pop = page.locator('.ant-popover:visible');
    await expect(pop).toBeVisible({ timeout: 5000 });
    await expect(pop.getByText('通知').first()).toBeVisible();
    await expect(pop.getByText('SSE实时通知' + TS)).toBeVisible({ timeout: 5000 });
    // 级别标签
    await expect(pop.getByText('一般').first()).toBeVisible();
  });

  test('NTF-005 点条目标记已读，未读数下降', async () => {
    const pop = page.locator('.ant-popover:visible');
    const badge = page.locator('.ant-badge-count');
    const before = Number((await badge.textContent()) ?? '0');
    expect(before).toBeGreaterThan(0);

    await pop.locator('.ant-list-item').filter({ hasText: 'SSE实时通知' + TS }).first().click();
    await expect
      .poll(async () => Number((await badge.textContent().catch(() => '0')) ?? '0'), { timeout: 10000 })
      .toBeLessThan(before);
  });

  test('NTF-006 全部已读后角标清零', async () => {
    const pop = page.locator('.ant-popover:visible');
    await pop.getByRole('button', { name: '全部已读' }).click();
    await expect(page.locator('.ant-message').getByText('已全部标记为已读')).toBeVisible({ timeout: 5000 });
    // 角标为 0 时 antd 不再渲染 .ant-badge-count
    await expect(page.locator('.ant-badge-count')).toHaveCount(0, { timeout: 10000 });
  });
});
