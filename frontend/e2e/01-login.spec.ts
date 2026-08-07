import { test, expect } from '@playwright/test';
import { loginViaUi } from './helpers/auth';
import { APP_NAME } from '../src/config/app';

test.describe('登录功能', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
  });

  test('登录页正常渲染', async ({ page }) => {
    await expect(page.getByText(APP_NAME)).toBeVisible();
    await expect(page.getByText('默认登录')).toBeVisible();
    await expect(page.getByText('租户登录')).toBeVisible();
  });

  test('默认登录 - 正确账密', async ({ page }) => {
    await loginViaUi(page, 'admin', 'Abc@123456');
    await expect(page).not.toHaveURL(/\/login/);
  });

  test('默认登录 - 错误密码', async ({ page }) => {
    await page.locator('#defaultLogin_username').fill('admin');
    await page.locator('#defaultLogin_password').fill('WrongPassword1');
    await page.locator('button[type="submit"]').first().click();
    // 应提示错误消息
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
  });

  test('默认登录 - 空用户名提交', async ({ page }) => {
    await page.locator('button[type="submit"]').first().click();
    await expect(page.getByText('请输入用户名')).toBeVisible({ timeout: 3000 });
  });

  test('切换到租户登录 Tab', async ({ page }) => {
    await page.getByText('租户登录').click();
    await expect(page.locator('.ant-select')).toBeVisible();
  });
});
