import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

const TS = Date.now().toString().slice(-6);

test.describe.serial('字典管理 CRUD', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
    await gotoPage(page, '/system/dict');
  });

  test.afterAll(async () => {
    await page.close();
  });

  test('DT-001 字典类型列表加载', async () => {
    await expect(page.locator('.ant-table').first()).toBeVisible({ timeout: 10000 });
  });

  test('DT-003 新增字典类型', async () => {
    const addBtn = page.getByRole('button', { name: /新增/ });
    if (await addBtn.first().isVisible({ timeout: 3000 }).catch(() => false)) {
      await addBtn.first().click();
      await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 3000 });

      await page.locator('.ant-modal #dictName').or(page.locator('.ant-modal input').first()).fill('测试字典' + TS);
      const typeInput = page.locator('.ant-modal #dictType').or(page.locator('.ant-modal input').nth(1));
      if (await typeInput.isVisible()) {
        await typeInput.fill('test_dict_' + TS);
      }

      await page.locator('.ant-modal .ant-btn-primary').click();
      await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
      await page.waitForTimeout(1000);
    }
  });

  test('DT-005 刷新缓存会真的重拉数据（原先只弹提示、界面无变化）', async () => {
    /*
     * 后端 Caffeine 缓存本身是有效的（可用「绕过 Service 直接改库 → 接口仍返回旧值
     * → 刷新后返回新值」验证）。但这个按钮原先只 await + 弹 toast，
     * 不重拉任何数据，点了界面毫无变化，看起来就像没生效。
     */
    const calls: string[] = [];
    page.on('request', (r) => {
      if (r.url().includes('/api/v1/dict/')) calls.push(`${r.method()} ${new URL(r.url()).pathname}`);
    });

    await page.getByRole('button', { name: '刷新缓存' }).first().click();
    await expect(page.locator('.ant-message').getByText('缓存刷新成功')).toBeVisible({ timeout: 5000 });

    // 既要打刷新接口，也要重新拉列表
    await expect.poll(() => calls.some((c) => c.startsWith('DELETE') && c.endsWith('/dict/cache')), { timeout: 5000 }).toBe(true);
    await expect.poll(() => calls.some((c) => c.startsWith('GET') && c.includes('/dict/types')), { timeout: 8000 }).toBe(true);
  });

  test('DT-004 删除字典类型', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: '测试字典' + TS });
    if (await row.isVisible({ timeout: 3000 }).catch(() => false)) {
      await row.getByLabel('删除').click();
      await page.locator('.ant-popconfirm .ant-btn-primary').or(page.locator('.ant-popover .ant-btn-primary')).click();
      await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
      await page.waitForTimeout(1000);
    }
  });
});
