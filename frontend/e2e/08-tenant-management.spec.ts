import { test, expect } from '@playwright/test';
import { login, gotoPage, waitForTableLoaded } from './helpers/auth';

// 租户管理是 SUPER_ADMIN 专属（ADMIN 没有 system:tenant:list 权限），必须用 chenli 登录
test.describe('租户管理 (T-001 ~ T-005)', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, 'chenli', 'Chenli@2026');
    await gotoPage(page, '/system/tenants');
  });

  test('T-001 租户列表加载', async ({ page }) => {
    await expect(page.locator('.ant-table').first()).toBeVisible({ timeout: 10000 });
    // V2__init_data.sql 建的默认租户一定在列表里
    await expect(page.getByText('默认租户').first()).toBeVisible({ timeout: 10000 });
  });

  test('T-002 新增租户弹窗', async ({ page }) => {
    await waitForTableLoaded(page);
    const addBtn = page.getByRole('button', { name: /新增/ });
    await expect(addBtn.first()).toBeVisible({ timeout: 10000 });
    await addBtn.first().click();
    await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 5000 });
    await page.locator('.ant-modal .ant-modal-close').click();
  });

  test('T-003 ADMIN 无租户管理权限', async ({ page }) => {
    await login(page, 'admin', 'Abc@123456');
    // ADMIN 的菜单里没有租户管理，直接访问会落到「无权限」或被重定向走
    await page.goto('/system/tenants');
    await expect(page.getByTestId('session-restore')).toHaveCount(0, { timeout: 15000 });
    await page.waitForLoadState('networkidle');
    await expect(page.getByText('默认租户')).toHaveCount(0);
  });
});
