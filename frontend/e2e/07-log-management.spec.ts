import { test, expect } from '@playwright/test';
import { ensureLoggedIn, gotoPage } from './helpers/auth';

test.describe('日志管理 (LOG-001 ~ LOG-006)', () => {
  test.beforeEach(async ({ page }) => {
    await ensureLoggedIn(page);
  });

  test('LOG-001 操作日志页面加载', async ({ page }) => {
    await gotoPage(page, '/monitor/operlog');
    await expect(page.locator('.ant-table').first()).toBeVisible({ timeout: 10000 });
  });

  test('LOG-004 登录日志页面加载', async ({ page }) => {
    await gotoPage(page, '/monitor/loginlog');
    await expect(page.locator('.ant-table').first()).toBeVisible({ timeout: 10000 });
    // 登录日志一定有数据（本次登录就会写一条）
    await expect(page.locator('.ant-table-row').first()).toBeVisible({ timeout: 10000 });
  });

  test('LOG-006 在线用户页面加载', async ({ page }) => {
    await gotoPage(page, '/monitor/online');
    await expect(page.locator('.ant-table').first()).toBeVisible({ timeout: 10000 });
    // 当前会话本身就是一个在线用户
    await expect(page.locator('.ant-table-row').first()).toBeVisible({ timeout: 10000 });
  });
});
