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

  test('R-007 绑定用户（穿梭框）', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: 'TESTROLE' + TS });
    await row.getByLabel('绑定用户').click();

    const modal = page.locator('.ant-modal');
    await expect(modal).toBeVisible({ timeout: 3000 });
    await expect(modal.getByText('可选用户')).toBeVisible();
    await expect(modal.getByText('已绑定用户')).toBeVisible();

    // 新角色还没绑人，右侧应为空
    const left = modal.locator('.ant-transfer-list').first();
    const right = modal.locator('.ant-transfer-list').last();
    await expect(right.locator('.ant-transfer-list-content-item')).toHaveCount(0);
    await expect(left.locator('.ant-transfer-list-content-item').first()).toBeVisible({ timeout: 5000 });

    // 勾选左侧第一个用户 → 移到右侧
    await left.locator('.ant-transfer-list-content-item').first().click();
    await modal.locator('.ant-transfer-operation button').first().click();
    await expect(right.locator('.ant-transfer-list-content-item')).toHaveCount(1);

    await modal.getByRole('button', { name: /确\s*定/ }).click();
    await expect(page.locator('.ant-message').getByText('绑定用户成功')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(800);
  });

  test('R-007 重开弹窗能回显已绑定用户', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: 'TESTROLE' + TS });
    await row.getByLabel('绑定用户').click();

    const modal = page.locator('.ant-modal');
    await expect(modal).toBeVisible({ timeout: 3000 });
    // 已绑定的那个人应出现在右侧（验证 GET /roles/{id}/users 回显链路）
    const right = modal.locator('.ant-transfer-list').last();
    await expect(right.locator('.ant-transfer-list-content-item')).toHaveCount(1, { timeout: 5000 });

    // 移回左侧 → 解绑（否则角色下有用户，R-006 删除会被 ROLE_IN_USE 拦住）
    await right.locator('.ant-transfer-list-content-item').first().click();
    await modal.locator('.ant-transfer-operation button').last().click();
    await expect(right.locator('.ant-transfer-list-content-item')).toHaveCount(0);

    await modal.getByRole('button', { name: /确\s*定/ }).click();
    await expect(page.locator('.ant-message').getByText('绑定用户成功')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(800);
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
