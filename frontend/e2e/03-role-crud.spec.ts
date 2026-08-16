import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

const TS = Date.now().toString().slice(-6);

test.describe.serial('角色管理 CRUD', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
    await gotoPage(page, '/system/roles');
  });

  test.afterAll(async () => {
    await page.close();
  });

  test('R-001 角色列表加载', async () => {
    await expect(page.locator('.ant-table')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('管理员').first()).toBeVisible();
  });

  test('R-002 新增角色', async () => {
    await page.getByRole('button', { name: /新增/ }).click();
    await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 3000 });

    await page.locator('.ant-modal #roleCode').fill('TESTROLE' + TS);
    await page.locator('.ant-modal #roleName').fill('测试角色' + TS);

    await page.locator('.ant-modal .ant-btn-primary').click();
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(1000);
  });

  test('R-003 编辑角色', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: 'TESTROLE' + TS });
    await row.getByLabel('编辑').click();
    await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 3000 });

    await page.locator('.ant-modal #roleName').clear();
    await page.locator('.ant-modal #roleName').fill('已编辑角色' + TS);

    await page.locator('.ant-modal .ant-btn-primary').click();
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(1000);
  });

  test('R-006 删除角色', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: 'TESTROLE' + TS });
    if (await row.isVisible({ timeout: 3000 }).catch(() => false)) {
      await row.getByLabel('删除').click();
      // Popconfirm 确认按钮
      await page.locator('.ant-popconfirm .ant-btn-primary').or(page.locator('.ant-popover .ant-btn-primary')).click();
      await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
      await page.waitForTimeout(1000);
    }
  });
});
