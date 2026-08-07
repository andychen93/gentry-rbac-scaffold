import { test, expect } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

test.describe('工作台首页', () => {
  test('H-001 登录后默认落地在工作台', async ({ page }) => {
    await login(page, 'admin');
    await expect(page).toHaveURL(/\/home$/);
  });

  test('H-002 统计卡片、登录身份、快捷入口都渲染', async ({ page }) => {
    await login(page, 'admin');
    await gotoPage(page, '/home');

    // 4 张统计卡片
    await expect(page.locator('.ps-stat-card')).toHaveCount(4);
    // 统计值从接口拉到（不是占位符 —）
    await expect(page.locator('.ps-stat-card__value').first()).not.toHaveText('—', {
      timeout: 10000,
    });

    // 当前登录身份
    await expect(page.getByText('当前登录身份')).toBeVisible();
    await expect(page.getByText('ADMIN', { exact: false }).first()).toBeVisible();

    // 快捷入口：ADMIN 能看到用户管理，看不到租户管理（无 system:tenant:list）
    const quickLinks = page.locator('.ant-card', { hasText: '系统管理快捷入口' });
    await expect(quickLinks.getByText('用户管理')).toBeVisible();
    await expect(quickLinks.getByText('租户管理')).toHaveCount(0);
  });

  test('H-003 快捷入口可跳转到系统管理页面', async ({ page }) => {
    await login(page, 'admin');
    await gotoPage(page, '/home');

    await page.locator('.ant-card', { hasText: '系统管理快捷入口' }).getByText('用户管理').click();
    await expect(page).toHaveURL(/\/system\/users$/);
    await expect(page.locator('.ant-table').first()).toBeVisible({ timeout: 10000 });
  });

  test('H-004 SUPER_ADMIN 快捷入口包含租户管理', async ({ page }) => {
    await login(page, 'chenli');
    await gotoPage(page, '/home');

    const quickLinks = page.locator('.ant-card', { hasText: '系统管理快捷入口' });
    await expect(quickLinks.getByText('租户管理')).toBeVisible();
  });
});
