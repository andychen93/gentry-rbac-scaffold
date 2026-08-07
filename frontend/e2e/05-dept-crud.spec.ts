import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

const TS = Date.now().toString().slice(-6);

test.describe.serial('部门管理 CRUD', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
    await gotoPage(page, '/system/dept');
  });

  test.afterAll(async () => {
    await page.close();
  });

  test('D-001 部门树加载', async () => {
    await expect(page.locator('.ant-table').first()).toBeVisible({ timeout: 10000 });
  });

  test('D-002 新增部门', async () => {
    await page.getByRole('button', { name: /新增/ }).click();
    await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 3000 });

    await page.locator('.ant-modal #name').fill('测试部门' + TS);
    const sortInput = page.locator('.ant-modal #sort');
    if (await sortInput.isVisible()) {
      await sortInput.clear();
      await sortInput.fill('99');
    }

    await page.locator('.ant-modal .ant-btn-primary').click();
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(1000);
  });

  test('D-003 编辑部门', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: '测试部门' + TS });
    if (await row.isVisible({ timeout: 3000 }).catch(() => false)) {
      await row.getByText('编辑').click();
      await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 3000 });

      await page.locator('.ant-modal #name').clear();
      await page.locator('.ant-modal #name').fill('已编辑部门' + TS);

      await page.locator('.ant-modal .ant-btn-primary').click();
      await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
      await page.waitForTimeout(1000);
    }
  });

  test('D-004 删除部门', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: '已编辑部门' + TS }).or(
      page.locator('.ant-table-row').filter({ hasText: '测试部门' + TS })
    );
    if (await row.first().isVisible({ timeout: 3000 }).catch(() => false)) {
      await row.first().getByText('删除').click();
      await page.locator('.ant-popconfirm .ant-btn-primary').or(page.locator('.ant-popover .ant-btn-primary')).click();
      await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
      await page.waitForTimeout(1000);
    }
  });
});
